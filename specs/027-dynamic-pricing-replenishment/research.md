# Phase 0 Research — Dynamic Pricing & Replenishment

All Technical Context unknowns are resolved below. Each item: **Decision · Rationale · Alternatives
considered**. These supersede the inline assumptions in `spec.md` with concrete technical choices.

Today pricing is **static**: `Product.sellingPrice/mrp/dp` are flat per product, `CustomerGroup`
carries a single `defaultDiscountPercentage`, and order/invoice lines snapshot a price computed at entry
time with an ad-hoc `Discount {percent, value}` JSON — there is no rule engine, no quantity/seasonal/
promo tiers, and no precedence model. There are **no reorder points**: `InventoryItem.reorderLevel`
(spec 014) exists but nothing computes it, and there is no safety-stock/EOQ/auto-reorder logic. This
feature adds a deterministic **price-rule engine** evaluable **offline on-device at order time**, and a
**replenishment engine** that turns inventory movement history (spec 014) plus the demand forecast
(feature 022) into safety-stock / reorder-point / EOQ and draft purchase suggestions.

---

## R1. Price-rule engine — model and scope

- **Decision**: Introduce a new backend `pricing` bounded context owning a **`PriceRule`** entity: a
  workspace-scoped, **effective-dated**, prioritized rule that matches on a **scope** (product / category
  / all), an optional **customer-group**, an optional **quantity band** (`minQty`/`maxQty`), and an
  optional **season/promo window** (`startsAt`/`endsAt`), and applies an **adjustment** (percent-off,
  flat-amount-off, or fixed-override price). It subsumes today's `CustomerGroup.defaultDiscountPercentage`
  (modeled as the lowest-priority group rule) and the product tiered fields. The 009-commerce-pricing
  `PriceList`/`PriceTier` model is folded in as the "fixed-override + quantity tier" rule kinds rather
  than a parallel system.
- **Rationale**: A single rule table with typed scope/condition/adjustment columns expresses all the
  asked-for tiers (customer-group, quantity, seasonal, promo) with one precedence model and one evaluator
  — which is exactly what makes deterministic offline evaluation tractable (R4). Reusing the existing
  group discount as a rule means no double source of truth.
- **Alternatives considered**: Separate tables per tier kind (group-discount, qty-break, promo) (rejected
  — N evaluators, N precedence questions, N sync delegates). Keep the ad-hoc per-line `Discount` JSON as
  the model (rejected — not reusable, not effective-dated, can't express precedence). A rules DSL/Drools
  engine (rejected — non-deterministic to port to Kotlin/Native, overkill for SMB pricing).

## R2. Rule precedence & conflict resolution

- **Decision**: Deterministic, **total ordering** of matched rules: (1) higher explicit `priority`
  wins; (2) tie-break by **specificity** (product-scoped > category-scoped > all; customer-group-scoped >
  ungrouped; quantity-banded > unbanded; dated promo > evergreen); (3) final tie-break by **most recently
  effective** (`startsAt` desc) then `uid` (stable). Exactly **one** winning rule sets the unit price;
  rules are **not stacked** by default. A workspace setting `pricing/allow_stacking` (off by default) can
  permit one additional non-conflicting promo on top, applied in a fixed order (base rule → promo) so the
  result is still deterministic.
- **Rationale**: SMB users expect "the best/most-specific applicable price", and a *total* order with
  stable tie-breaks guarantees every client (and the backend) picks the **same** winner from the same
  rule set — the precondition for offline determinism (R4). Non-stacking by default avoids the
  combinatorial ambiguity that makes promo engines unpredictable; opt-in stacking is bounded and ordered.
- **Alternatives considered**: "Lowest price wins" (rejected — a deep-but-wrong promo can undercut a
  contractual group price; not what the owner configured). Free stacking of all matches (rejected —
  order-dependent, non-deterministic, hard to audit). First-match-by-insertion-order (rejected — fragile,
  not specificity-aware).

## R3. Effective-dated rules & "as-of" evaluation

- **Decision**: Every rule carries `startsAt`/`endsAt` (`Instant`, nullable = open-ended) and an `active`
  flag. Evaluation is **as-of a reference instant** — the **order/invoice business date** (R6), not
  "now" — so a back-dated document prices with the rules that were effective *then*, and a future-dated
  promo doesn't leak into today's pricing. The resolved price + the winning `ruleUid` + the inputs
  (qty, group, as-of) are **snapshotted onto the order/invoice line** at entry time (extending the
  existing line snapshot fields), so a later rule edit never re-prices a committed document.
- **Rationale**: Pricing must be reproducible and auditable; "as-of the document date" is the only choice
  that keeps a re-opened/edited document and a reprint consistent. Snapshotting the winning rule on the
  line makes the applied price defensible and decouples committed documents from later rule changes.
- **Alternatives considered**: Evaluate against "now" always (rejected — edits/reprints of an old order
  would silently change its price). Re-evaluate on every read (rejected — non-reproducible, breaks audit,
  expensive).

## R4. OFFLINE-deterministic price evaluation — where rules are evaluated

- **Decision**: Price evaluation is a **pure, deterministic function** `resolvePrice(product, qty,
  customerGroup, asOf, rules) → PriceResolution` implemented **once in shared logic** and run **on-device
  at order time** against the locally-synced `PriceRule` rows (Room). The **identical algorithm** runs on
  the backend (same precedence in R2, same as-of in R3) and **re-validates** the client-resolved price on
  push; on disagreement the backend's resolution is authoritative and is reflected back (last-write-wins
  on the *rule set*, not the price). Rules sync to mobile via the canonical `/sync` contract so the device
  always has the current rule set offline.
- **Rationale**: Order capture must work fully offline (field sales, poor connectivity), so the price
  *must* be computable on-device — fetching a price from the backend at line-entry is a non-starter. The
  only way the on-device price and the server price agree is a **single, deterministic specification**
  (total ordering + as-of + integer-minor-unit math) applied to the same rule rows on both sides. The
  backend re-validation catches a device running a stale rule set without blocking the offline sale.
- **Rationale (math determinism)**: All adjustment arithmetic uses **integer minor units** with a single,
  specified rounding rule (half-up to the currency's minor scale) so percent-off computes identically on
  Kotlin/Native, JVM, and the backend — `Double` percentages would diverge across platforms.
- **Alternatives considered**: Backend-only pricing API called per line (rejected — fails offline; the
  whole order flow would block on connectivity). Two implementations (a "fast" client one and a
  "correct" server one) (rejected — guaranteed drift; the bug class the payment-ledger work already
  warned against). Caching last-known prices (rejected — ignores qty/group/date conditions).

## R5. Money representation & rounding (pricing)

- **Decision**: Backend **`BigDecimal`, `DECIMAL(19,4)`**; mobile **`Long` minor units** via a `Money`
  value type. All rule adjustments compute in minor units; round **half-up** to the currency minor scale
  at the resolved unit price. The resolved price is carried on the API as `{amount_minor: Long, currency}`
  (matching the 009-commerce-pricing money contract) and snapshotted on the line in minor units (mobile)
  / `DECIMAL(19,4)` (backend).
- **Rationale**: Identical-result arithmetic across platforms (R4) requires exact integer math; the
  existing payment module already proved `Long` minor units are KMP-safe in `commonMain` (no stdlib
  `BigDecimal` on Native). A single specified rounding point keeps the client and server byte-identical.
- **Alternatives considered**: Reuse the legacy `Double` price/discount fields (rejected — precision and
  cross-platform divergence). Round per-adjustment then re-round (rejected — accumulation error; round
  once at the final unit price).

## R6. Evaluation reference & period bucketing — business timezone

- **Decision**: The **as-of** instant and any seasonal-window comparison use the **workspace business
  timezone** (via `BusinessLocaleProvider`/`LocalAppLocale.timeZoneId` on mobile; the `business` module's
  public timezone service on the backend), never the device or server local zone. A promo "valid 1–31
  Oct" is evaluated against the document's business-local date; storage stays UTC `Instant`.
- **Rationale**: A seasonal/promo window is a *business* calendar concept; evaluating it in device/UTC
  time would activate/expire a promo at the wrong moment for non-UTC businesses (the documented
  `/cmp-practices §12` computation trap). Consistent zone resolution is also what keeps the client and
  server as-of evaluation in agreement (R4).
- **Alternatives considered**: UTC/device-zone window checks (rejected — off-by-hours promo activation at
  the day boundary; client/server disagreement).

## R7. Reorder point, safety stock & EOQ model

- **Decision**: The new `pricing` context (or a sibling `replenishment` service within it) computes, per
  inventory item: **average daily demand (d)** and **demand std-dev (σ_d)** from the demand signal
  (feature 022 `DemandForecast` / movement history, spec 014 `InventoryTransaction`), a **lead time (L)**
  setting per supplier/workspace, then **safety stock = z · σ_d · √L** (z from a configurable service-
  level, default ~1.65 ≈ 95%), **reorder point = d · L + safety stock**, and an **EOQ = √(2 · D · S / H)**
  (annual demand D, ordering cost S, holding cost H — both from settings). Results are written back as a
  suggested `reorderLevel` and surfaced as **reorder suggestions / draft purchase** rows; inventory (spec
  014) remains the owner of `InventoryItem.reorderLevel` — replenishment **suggests**, the user/inventory
  **commits**.
- **Rationale**: These are the standard, well-understood inventory formulas; they are cheap, explainable,
  and need only `d`, `σ_d`, and `L` — exactly what feature 022's demand signal and spec 014's movement
  ledger already provide. Keeping the suggestion/commit split respects module boundaries and lets the user
  override.
- **Alternatives considered**: A single fixed reorder threshold per item (rejected — ignores demand
  variability and lead time; the user's actual ask). Continuous-review optimization / (s,S) policy
  modeling (deferred — heavier than the pragmatic core; safety-stock + ROP + EOQ covers P1). Folding the
  math into inventory (rejected — couples policy/forecasting into inventory's bounded context).

## R8. Demand input from forecasting (boundary with feature 022)

- **Decision**: Replenishment **consumes** the demand signal published by `analytics` (feature 022) —
  `DemandForecastUpdatedEvent` (Spring `ApplicationEvent`) + a public `DemandSignalService` exposing
  avg daily demand + variability per product — and **falls back** to its own moving average over spec
  014's `InventoryTransaction` history when no forecast exists. It never reads the analytics tables
  directly.
- **Rationale**: Respects module boundaries (Principle IX): analytics measures/forecasts, replenishment
  decides. The event/public-service contract is exactly how 022 is designed to feed downstream consumers.
  The local moving-average fallback keeps replenishment useful before the forecast batch runs (and
  on-device).
- **Alternatives considered**: Replenishment computing its own seasonality (rejected — duplicates 022's
  Holt-Winters). Direct cross-module table reads (rejected — boundary violation).

## R9. Evaluation at order time — integration with order/invoice

- **Decision**: At line entry, the order/invoice ViewModel (mobile) and service (backend) call the shared
  `resolvePrice(...)` and **snapshot** the resolved unit price + `appliedRuleUid` + `priceSource`
  (PRICE_RULE | CATALOG_FALLBACK) + as-of onto the line, **extending the existing snapshot fields**
  (`OrderItem`/`InvoiceItem` already carry `unitPrice`/`sellingPrice`/`mrp`/`dp`/`discount`). The legacy
  ad-hoc `Discount {percent, value}` continues to represent a *manual* line override that wins over the
  rule (manual override is a user action, audited via `priceSource = MANUAL`).
- **Rationale**: Reuses the order/invoice line snapshot mechanism that already freezes price at entry;
  adds only a few columns rather than reshaping the line. Letting a manual override beat the rule matches
  user expectation and is explicitly auditable.
- **Alternatives considered**: Compute price only at finalize (rejected — the user must see the priced
  line as they build the order). Replace the manual `Discount` path (rejected — manual override is a real
  workflow; keep it, just record it as a source).

## R10. Rule sync, settings & numbering

- **Decision**: `PriceRule` and `ReorderSuggestion`/`PurchaseDraft` ride the **canonical `/sync`
  contract** (`GET/POST /pricing/v1/rules/sync`, `/pricing/v1/suggestions/sync`) with UID-keyed bulk
  upsert and in-band soft-delete. Pricing/replenishment policy (`pricing/allow_stacking`,
  `replenishment/service_level_z`, `replenishment/default_lead_time_days`, `replenishment/ordering_cost`,
  `replenishment/holding_cost_pct`) is declared via a `PricingSettingDefinitions :
  SettingDefinitionProvider` (`requiresModule = "dynamic-pricing"`) and stored as `StoreSetting`
  (`module_code='pricing'`), read with `SettingService` (backend) / `StoreSettingsProvider` (mobile).
  Purchase-draft numbers use the existing `sequence` module (`SequenceDefinition`/`SequenceFormatter`,
  series `PUR`).
- **Rationale**: One sync engine, one settings store, one numbering mechanism — every cross-cutting
  concern reuses proven infrastructure exactly as inventory (014) and payment (013) do; rules must be
  offline (R4), which the `/sync` mirror provides.
- **Alternatives considered**: A bespoke rule-distribution channel (rejected — duplicates `/sync`).
  Hard-coded service-level/lead-time constants (rejected — workspaces differ). New numbering (rejected —
  `sequence` exists).

## R11. Agent queryability (NL pricing/replenishment questions)

- **Decision**: Register `pricing` and `replenishment` as **agent-queryable modules** — add
  `ModuleQuerySchema` (curated rule/suggestion tables/columns) + `ModuleQueryExecutor`
  (`@QuerySchemaKey("pricing")` / `@QueryExecutorKey("pricing")`) so questions like "which items need
  reordering" or "what's the VIP price for SKU X" resolve through the on-device SafeQuery path.
- **Rationale**: Free, mechanical reuse of the agent's text-to-SQL path (the documented 2-file pattern);
  no central wiring. Keeps NL coverage consistent with the rest of the app.
- **Alternatives considered**: A bespoke pricing Q&A path (rejected — reinvents SafeQuery + guardrails).

---

## Resolved unknowns summary

| Unknown (Technical Context) | Resolution |
|---|---|
| Price-rule model | One effective-dated, scoped, prioritized `PriceRule`; subsumes group discount + 009 tiers (R1) |
| Precedence / conflict | Total order: priority → specificity → recency; non-stacking default, bounded opt-in stack (R2) |
| Effective-dating | `startsAt`/`endsAt`; evaluate as-of the document business date; snapshot winner on line (R3) |
| Offline price eval | Single deterministic `resolvePrice` on-device over synced rules; backend re-validates (R4) |
| Money / rounding | `DECIMAL(19,4)` / `Long` minor units; half-up once at resolved unit price (R5) |
| Reference & bucketing | As-of + seasonal windows in business timezone; store UTC (R6) |
| Reorder / safety / EOQ | `SS = z·σ·√L`, `ROP = d·L + SS`, `EOQ = √(2DS/H)` from settings (R7) |
| Demand input | Consume 022's `DemandForecastUpdatedEvent`/`DemandSignalService`; movement-history fallback (R8) |
| Order-time integration | Shared `resolvePrice` snapshots price + ruleUid + source on the line; manual override = MANUAL source (R9) |
| Sync / settings / numbering | Canonical `/sync` for rules+suggestions; `StoreSetting` policy; `sequence` `PUR` (R10) |
| Agent queryability | Register `pricing`/`replenishment` ModuleQuerySchema + Executor (R11) |
</content>
