# Implementation Plan: Dynamic Pricing & Replenishment

**Branch**: `claude/indian-retail-ecosystem-877med` (spec dir `027-dynamic-pricing-replenishment`) | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/027-dynamic-pricing-replenishment/spec.md`

## Summary

Replace static pricing with a **dynamic, effective-dated price-rule engine** (customer-group, quantity,
seasonal, and promo tiers with deterministic precedence) that is evaluated **offline on-device at order
time**, and add an **auto-reorder / safety-stock replenishment** engine that turns inventory movement
history (spec 014) and the demand forecast (feature 022) into reorder points, EOQ, and draft purchase
suggestions.

Technical approach: a new backend bounded context (`pricing`) owning a single `PriceRule` entity
(scope + customer-group + quantity band + season/promo window + adjustment), evaluated by a **pure,
deterministic `resolvePrice` function** with a total precedence ordering (priority → specificity →
recency) **as-of the document business date**. The *same* function is implemented once in the mobile
`feature/pricing` `commonMain` and run on-device against locally-synced `PriceRule` rows so order capture
prices fully offline; the backend re-validates on push (authoritative on the rule set). A
`replenishment` service computes `SS = z·σ·√L`, `ROP = d·L + SS`, `EOQ = √(2DS/H)` from the demand signal
published by feature 022 (`DemandForecastUpdatedEvent`/`DemandSignalService`, with a movement-history
fallback) and emits reorder suggestions / draft purchases; inventory (spec 014) stays the owner of
`InventoryItem.reorderLevel`. All money is `BigDecimal`/`DECIMAL(19,4)` backend, `Long` minor units on
mobile, with a single half-up rounding point so the client and server resolve **byte-identical** prices.
Seasonal/as-of evaluation uses the **workspace business timezone**. Full rationale in
[research.md](./research.md); entities in [data-model.md](./data-model.md); APIs in [contracts/](./contracts/).

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Java 21 (Spring Boot 4.0); Mobile Kotlin Multiplatform 2.4
(Compose Multiplatform 1.11).
**Primary Dependencies**: Backend — Spring Data JPA, Flyway, Jackson (SNAKE_CASE), `core`
(`OwnableBaseDomain`, `ApiResponse`, `PageResponse`, `TenantContextHolder`), `sequence` module (purchase
numbers), `setting` module (policy); consumes `CustomerGroup.defaultDiscountPercentage` (customer),
`Product`/`ProductVariant` pricing (product), `InventoryItem`/`InventoryTransaction` (inventory, spec
014), and feature 022's `DemandForecastUpdatedEvent`/`DemandSignalService` — all via public service
interfaces + events. Mobile — Room KMP (`PriceRule`, `ReorderSuggestion` mirrors), Ktor, Metro DI,
Navigation3, kotlinx.datetime, `data/sync` (`CentralSyncService`/`SyncDelegate`), `data/common`
(`ApiUrlBuilder`, `BusinessLocaleProvider`/`LocalAppLocale`, `formatMoney`), `feature/order` &
`feature/invoice` (line snapshot), `feature/inventory` (stock/movement), `feature/agent` (SafeQuery).
**Storage**: Backend — PostgreSQL/MySQL via Flyway; `price_rule`, `reorder_suggestion`, `purchase_draft`;
money `DECIMAL(19,4)`, windows `Instant`/`TIMESTAMPTZ`. Mobile — Room (workspace-scoped DB `pricing`),
money `Long` minor units, dates ISO-8601 UTC strings.
**Testing**: Backend — JUnit/Testcontainers (`./gradlew :pricing:test`), incl. precedence determinism,
as-of/timezone window evaluation, and minor-unit rounding parity; replenishment formula tests on
synthetic series. Mobile — `./gradlew :feature:pricing:check` incl. a **client/server `resolvePrice`
parity** test vector; 3-target compile gates.
**Target Platform**: Backend service (Linux); Mobile Android (minSdk 24) / iOS / Desktop (JVM).
**Project Type**: Mobile + API — new backend module + new KMP feature module. Web (Angular) admin for
rules is a tracked follow-up.
**Performance Goals**: `resolvePrice` per line perceived-instant on-device (<10 ms over the workspace
rule set); rule/suggestion sync batches 100/page like existing entities; replenishment batch O(items).
**Constraints**: Order-time price evaluation MUST work fully offline and resolve **deterministically**
identical to the backend; all adjustment math in integer minor units with a single half-up rounding;
seasonal/as-of windows in the **business timezone** (never device/UTC); committed lines snapshot the
applied rule (never re-priced by later edits); workspace data isolation; module boundaries (suggest, not
mutate inventory).
**Scale/Scope**: Per workspace — hundreds–low-thousands of rules, thousands of products. P1 ≈ 1 backend
entity (`PriceRule`) + `resolvePrice` shared on both sides; P2 adds `ReorderSuggestion`/`PurchaseDraft`
+ replenishment formulas.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How this plan complies |
|---|---|---|
| I. Type Safety (Instant/TIMESTAMPTZ) | ✅ PASS | Rule windows + audit timestamps `Instant` → `TIMESTAMPTZ`/`TIMESTAMP`; no `LocalDateTime`. Money `BigDecimal`/`DECIMAL(19,4)` (mobile `Long` minor units), never floating point in rule math. |
| II. DTO & Contract Isolation | ✅ PASS | `PriceRuleRequest`/`Response`, `PriceResolutionResponse`, `ReorderSuggestionResponse` in `pricing/domain/dto/`; entities never exposed; converters with `@field:` validation. |
| III. Global JSON SNAKE_CASE | ✅ PASS | Global Jackson; snake_case params/fields (`min_qty`, `customer_group_id`, `starts_at`, `amount_minor`); no `@JsonProperty` for standard fields. |
| IV. Multi-Tenant Isolation | ✅ PASS | All entities extend `OwnableBaseDomain` (`@TenantId ownerId`); tenant set by controller/`SessionUserFilter`; services never mutate tenant context. |
| V. API Response Standardization | ✅ PASS | All endpoints `ApiResponse<T>`; rule/suggestion sync → `ApiResponse<PageResponse<T>>`/`ApiResponse<List<T>>` via the canonical `/sync` contract. |
| VI. Centralized Exception Handling | ✅ PASS | No business try/catch in controllers; typed pricing/replenishment exceptions bubble to the global handler. |
| VII. Efficient Data Loading | ✅ PASS | Rule lookup indexed by `(owner_id, scope, customer_group_id, active)`; `@NamedEntityGraph` where rules carry tiers; derived queries preferred. |
| VIII. Angular Material 3 Exclusivity | ✅ N/A (this phase) | Web rule admin deferred; when added, Angular Material 3 only. |
| IX. Domain-Driven Module Boundaries | ✅ PASS | New `pricing` bounded context; reads customer/product/inventory and 022's demand signal **only** via public services + events; **suggests** reorder levels — never writes `inventory`/`order` tables. |
| X. Compose Multiplatform Parity | ✅ PASS | Shared `resolvePrice` + UI in `feature/pricing/src/commonMain`; thin platform DI. Web parity tracked as follow-up. |
| XI. Security & Secrets Hygiene | ✅ PASS | No secrets; standard JWT/workspace auth. |
| Flyway | ✅ PASS | Versioned migration in **both** `mysql/` and `postgresql/`; `pricing` added to `migrationModules`; next version via `flywayInfo`. |
| Testing & Quality Gates | ✅ PASS | Backend ≥80% on precedence/as-of/rounding + replenishment formulas; mobile `check` + a shared parity test vector + 3-target compile gates. |

**Result**: PASS — no violations; Complexity Tracking not required. Web deferral is a documented scope
decision.

## Project Structure

### Documentation (this feature)

```
specs/027-dynamic-pricing-replenishment/
├── plan.md              # This file (/speckit.plan output)
├── spec.md              # Feature specification (/speckit.specify output)
├── research.md          # Phase 0 output — design decisions + rationale
├── data-model.md        # Phase 1 output — PriceRule, ReorderSuggestion, PurchaseDraft, line snapshot fields
├── quickstart.md        # Phase 1 output — author a rule, price an offline order, generate a reorder draft
├── contracts/           # Phase 1 output — API contracts
│   ├── README.md
│   ├── pricing-sync.md          # /pricing/v1/rules/sync (+ resolve endpoint for re-validation)
│   └── replenishment-sync.md    # /pricing/v1/suggestions/sync + purchase-draft
├── checklists/
│   └── requirements.md  # spec quality checklist
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```
# Backend — ampairs/ (this repo)
pricing/
└── src/main/
    ├── kotlin/com/ampairs/pricing/
    │   ├── domain/
    │   │   ├── model/          # PriceRule, ReorderSuggestion, PurchaseDraft (+ PurchaseDraftLine)
    │   │   ├── enums/          # RuleScope (PRODUCT/CATEGORY/ALL), AdjustmentType (PERCENT_OFF/AMOUNT_OFF/FIXED_PRICE), PriceSource (PRICE_RULE/CATALOG_FALLBACK/MANUAL), SuggestionStatus
    │   │   ├── pricing/        # PriceResolver — the deterministic resolvePrice(product, qty, group, asOf, rules) (shared spec, JVM impl)
    │   │   └── dto/            # PriceRuleRequest/Response, PriceResolutionResponse, ReorderSuggestionResponse + converters
    │   ├── repository/         # PriceRuleRepository, ReorderSuggestionRepository (+ /sync feed queries)
    │   ├── service/            # PriceRuleService (bulkUpsert, resolve, re-validate), ReplenishmentService (SS/ROP/EOQ), PurchaseDraftService
    │   ├── controller/         # PricingController (rules/sync + resolve), ReplenishmentController (suggestions/sync, draft)
    │   ├── config/             # PricingSettingDefinitions (stacking, service-level z, lead time, ordering/holding cost), Constants
    │   ├── event/              # listens DemandForecastUpdatedEvent (022); InventoryLowStockEvent/InventoryStockUpdatedEvent (014) → recompute suggestion
    │   └── batch/              # @Scheduled replenishment recompute (per workspace)
    └── resources/db/migration/
        ├── mysql/V1.0.x__create_pricing_tables.sql
        └── postgresql/V1.0.x__create_pricing_tables.sql
# wiring: settings.gradle.kts (include "pricing"); ampairs_service/build.gradle.kts
#         (implementation(project(":pricing")) + "pricing" in migrationModules)
# order/invoice modules: add line snapshot columns (applied_rule_uid, price_source, resolved_unit_price) — additive
# customer/product/inventory/analytics: expose/confirm public service interfaces consumed here

# Mobile — ampairs-app/ (sibling repo)
feature/pricing/src/
├── commonMain/kotlin/com/ampairs/pricing/
│   ├── data/api/          # PricingApi(+Impl), ApiUrlBuilder.pricingUrl
│   ├── data/db/           # PriceRuleEntity, ReorderSuggestionEntity + DAOs + PricingRoomDatabase
│   ├── data/repository/   # PriceRuleRepository (local-only), ReorderSuggestionRepository
│   ├── domain/            # Money (Long minor units), PriceResolver (THE SAME resolvePrice as backend), SS/ROP/EOQ math
│   ├── di/                # PricingModule.kt
│   ├── sync/              # PriceRuleSyncDelegate, ReorderSuggestionSyncDelegate
│   ├── agent/             # PricingQuerySchemaModule + PricingQueryExecutor (@QuerySchemaKey/@QueryExecutorKey "pricing")
│   └── ui/                # RuleList/RuleForm, ReorderSuggestions, PurchaseDraft + ViewModels
├── androidMain/ iosMain/ desktopMain/   # PricingModule.{platform}.kt (@SingleIn(WorkspaceScope::class))
# order/invoice feature modules: call PriceResolver at line entry; add snapshot fields to OrderItem/InvoiceItem entities
# wiring: settings.gradle.kts (:feature:pricing); SyncEntity.PRICE_RULE / REORDER_SUGGESTION;
#         shared/ Routes + entry provider; ModuleRegistry ("dynamic-pricing" → Route.Pricing);
#         data/common ApiUrlBuilder.pricingUrl(...)
```

**Structure Decision**: Mobile + API. The backend `pricing/` module mirrors existing bounded contexts;
the **deterministic `PriceResolver`** is specified once and implemented on both the backend (JVM,
`BigDecimal`) and mobile (`commonMain`, `Long` minor units) against the **same** precedence/as-of/
rounding rules, validated by a shared test vector so order-time pricing is identical offline and online.
Replenishment **suggests** — inventory (014) owns the committed `reorderLevel`.

## Phased Breakdown

### P1 — MVP: dynamic price-rule engine, offline-deterministic at order time

- **Backend entity**: `PriceRule` (`uid`, `scope` + `scopeRefId?`, `customerGroupId?`, `minQty`/`maxQty
  (15,3)?`, `startsAt`/`endsAt: Instant?`, `priority: Int`, `adjustmentType`,
  `adjustmentValue(19,4)`/`fixedPriceMinor`, `active`) — UID-keyed, effective-dated.
- **Shared logic**: `PriceResolver.resolvePrice(product, qty, customerGroup, asOf, rules) →
  PriceResolution{unitPriceMinor, currency, appliedRuleUid?, priceSource}` with the **total precedence
  order** (R2), **as-of business date** (R3/R6), and **integer minor-unit half-up rounding** (R5).
  Identical impl backend + mobile; one shared test vector.
- **Endpoints**: canonical `GET/POST /pricing/v1/rules/sync`; `POST /pricing/v1/resolve` (re-validation /
  preview) → `ApiResponse<PriceResolutionResponse>`.
- **Order-time integration**: `OrderItem`/`InvoiceItem` gain snapshot fields (`applied_rule_uid`,
  `price_source`, `resolved_unit_price`); the existing manual `Discount {percent,value}` becomes
  `price_source = MANUAL` and wins over the rule.
- **Mobile/offline**: `PriceRuleSyncDelegate` mirrors rules to Room; the order/invoice ViewModels call
  the shared `PriceResolver` **on-device at line entry** (no network); backend re-validates on push and
  is authoritative on the rule set. `CustomerGroup.defaultDiscountPercentage` is migrated to a
  lowest-priority group rule.

### P2 — Replenishment: safety stock, reorder point, EOQ, reorder suggestions

- **Backend entities**: `ReorderSuggestion` (`inventoryItemId`, `avgDailyDemand`, `demandStdDev`,
  `leadTimeDays`, `safetyStock`, `reorderPoint`, `eoq`, `suggestedQty`, `status`, `generatedAt`),
  `PurchaseDraft` + lines (numbered via `sequence` series `PUR`).
- **Backend service**: `ReplenishmentService` consumes feature 022's `DemandForecastUpdatedEvent`/
  `DemandSignalService` for `d`/`σ_d` (movement-history fallback over spec 014 `InventoryTransaction`),
  reads `service_level_z`/`lead_time`/`ordering_cost`/`holding_cost_pct` settings, computes
  `SS = z·σ·√L`, `ROP = d·L + SS`, `EOQ = √(2DS/H)`, and writes suggestions. Recompute is event-driven
  (`InventoryLowStockEvent`/`InventoryStockUpdatedEvent`) + nightly batch. It **suggests** `reorderLevel`
  to inventory (spec 014 owns the committed value).
- **Endpoints**: `GET/POST /pricing/v1/suggestions/sync`; `POST /pricing/v1/purchase-drafts`.
- **Mobile/offline**: `ReorderSuggestionSyncDelegate` (pull + local accept); a reorder-suggestion list +
  draft-purchase screen; an on-device moving-average fallback estimates demand when offline.

### P3 — NL queryability, dashboard hooks, web admin (deferred)

- **Agent**: register `PricingQuerySchemaModule`/`PricingQueryExecutor` (and replenishment) so "which
  items need reordering" / "what's the VIP price for SKU X" resolve via the on-device SafeQuery path.
- **Analytics hook**: surface "rules applied" and "reorder due" tiles on the feature 022 dashboard.
- **Web admin**: Angular Material 3 rule/effective-date management UI (tracked follow-up).

## Complexity Tracking

*No constitution violations — section intentionally empty.*
</content>
