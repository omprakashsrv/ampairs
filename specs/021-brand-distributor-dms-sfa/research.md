# Phase 0 Research — Brand → Distributor DMS + Sales Force Automation

All Technical Context unknowns are resolved below. Each item: **Decision · Rationale · Alternatives
considered**. These supersede the inline assumptions in `spec.md` with concrete technical choices.

The defining shift: Ampairs today is **single-tier** — one business per workspace, fully isolated by
`@TenantId`. This feature introduces a **multi-tier trade chain** (brand → distributor → retailer) with
**secondary-sales visibility** up the chain, and a **field-sales-rep (SFA) app** that must work **offline
in rural beats**. The two hard problems are: (1) letting a brand see *aggregated* data across many
**independent distributor workspaces** without breaking tenant isolation, and (2) running order-capture
and visit-logging offline at a retailer counter. Reference systems: BeatRoute, Bizom, FieldAssist.

---

## R1. Trade-chain terminology baseline (so the model uses the right words)

- **Decision**: Adopt FMCG-standard terms verbatim in the model and APIs: **primary sales** = brand →
  distributor; **secondary sales** = distributor → retailer; **tertiary sales** = retailer → end-consumer.
  **RTM** (route-to-market) = the distributor/beat structure that moves goods to retail. A **beat plan** =
  a named, day-of-week-scheduled list of retailer outlets a rep visits on a route. **PJP** = Permanent
  Journey Plan (the rep's recurring beat calendar). A **trade scheme / promotion** = a brand-funded
  incentive (slab/discount/free-goods) the distributor claims. **Claims/settlement** = the distributor
  reimbursement cycle for scheme spend.
- **Rationale**: These are industry-standard; using them precisely keeps the model legible to anyone from
  the FMCG/CPG world and matches the reference tools (BeatRoute/Bizom/FieldAssist).
- **Alternatives considered**: Invent Ampairs-specific names (rejected — confuses domain experts, breaks
  the up-market positioning); model only "sales" generically (rejected — primary/secondary/tertiary have
  distinct owners, visibility rules, and claim implications).

## R2. Org-hierarchy model — the brand→distributor→retailer chain (the crux)

- **Decision**: A new `trade` (DMS/SFA) bounded context introduces a **`TradeOrg` hierarchy** layered
  **on top of** existing workspaces, **without** collapsing tenant isolation. Each tier is still its own
  **workspace** (a distributor runs Ampairs as a normal single-tier tenant today). A `TradeNetwork`
  aggregate links a **brand workspace** to many **distributor workspaces** via an explicit, **consented**
  `TradeLink` (brandWorkspaceId, distributorWorkspaceId, status, agreed visibility scope). Retailers are
  the distributor's existing `customer` records, optionally **claimed** into the network as
  `NetworkRetailer` so the brand can see them by code (not raw PII unless consented).
- **Rationale**: Distributors are independent businesses; forcing them into one mega-tenant would violate
  isolation, ownership, and the offline model. Linking **workspace-to-workspace** through an explicit,
  consented edge preserves each tier's autonomy while enabling the brand's aggregated view — and lets a
  distributor join multiple brands' networks. The `TradeLink` is the single, auditable place where
  cross-tenant visibility is granted.
- **Alternatives considered**: One workspace with sub-orgs / a parent-child `Workspace.parentId`
  (rejected — breaks `@TenantId` isolation, the offline-per-workspace DB model, and per-tier billing/RBAC;
  a distributor's data would live under the brand's tenant); a separate "brand DB" replicating distributor
  data (rejected — stale, duplicative, no consent boundary). See R3 for how the brand *reads* across links.

## R3. Cross-tenant aggregation boundary (how a brand sees secondary sales)

- **Decision**: Secondary-sales visibility is a **consented, pull-based, aggregate read** across the
  `TradeLink`, served by the **distributor's tenant** and **published** to the brand — never a direct
  cross-tenant query from the brand into distributor tables in normal flows. Two mechanisms:
  (a) the distributor's workspace **emits periodic `SecondarySalesSnapshot`s** (aggregates: SKU × period ×
  retailer-or-area, qty/value) into the `trade` network store under the consented scope; (b) the brand
  reads those snapshots filtered to its own `TradeLink`s. Any genuinely cross-tenant SQL (admin/network
  rollups) uses **`nativeQuery = true`** to bypass `@TenantId` auto-filtering **and** is gated by a live
  `TradeLink` consent check at the service layer. Raw retailer PII crosses only if the `TradeLink` scope
  explicitly allows it; otherwise the brand sees coded/aggregated retailers.
- **Rationale**: This honours the constitution's multi-tenancy rules (cross-tenant reads must be
  `nativeQuery=true` and consented) while giving the brand the secondary-sales picture it's buying. Pushing
  **aggregate snapshots** (not live row access) keeps the distributor in control of granularity, satisfies
  data-minimisation, and avoids coupling the brand's read path to the distributor's live schema.
- **Alternatives considered**: Brand directly queries distributor tables live (rejected — tenant-isolation
  violation, no consent gate, brittle coupling); a nightly ETL into a brand-owned warehouse (rejected for
  Phase 1 — heavy, stale, still needs the consent edge; revisit for analytics at scale).

## R4. Secondary-sales capture — where the data originates

- **Decision**: Secondary sales originate as **orders/invoices the distributor already creates** in their
  existing `order`/`invoice` modules (distributor → retailer = a normal sale in the distributor tenant).
  The `trade` module **tags** those documents as secondary-sales (via a `TradeContext` on the order/an
  event listener) and rolls them into `SecondarySalesSnapshot`s. **Tertiary sales** (retailer → consumer)
  are captured only where the retailer also runs Ampairs (rare) or are estimated from secondary + stock —
  Phase 3. **Primary sales** (brand → distributor) are orders in the **brand's** tenant addressed to the
  distributor.
- **Rationale**: Reuses the proven order/invoice pipeline instead of duplicating a sales model; the only
  new concept is the **tagging + rollup**. Distributors keep one source of truth for their sales; the trade
  layer is a read/aggregation overlay.
- **Alternatives considered**: A separate parallel "secondary sales" entry flow (rejected — duplicate data
  entry, drift from the real invoices); compute secondary sales only from stock movement (rejected — can't
  attribute to retailer/beat).

## R5. Field-sales-rep (SFA) app — offline-first by necessity

- **Decision**: The rep app rides the **existing offline `/sync` engine** (Room `synced=false` +
  `markPendingPush` + `{Name}SyncDelegate`, per the canonical contract). Reps **author offline**: outlet
  **visits**, **orders at the retailer counter**, **attendance** (check-in/out), **geo-stamps**, survey/
  audit responses, and stock-availability checks. Every such entity is workspace-scoped (the
  **distributor's** workspace — the rep belongs to a distributor) and syncs when connectivity returns.
  Geo and timestamps are captured **at author time on-device** (the on-device clock + GPS), stored, and
  reconciled server-side; server `updatedAt` remains the sync-tracking authority.
- **Rationale**: Rural beats have no reliable connectivity; the whole SFA value collapses if order-capture
  blocks on network. Ampairs already has a battle-tested offline-first stack — reusing it (rather than a
  bespoke queue) is the lowest-risk path and gives multi-device merge for free. Authoring under the
  distributor tenant keeps the rep's data correctly scoped and visible up-chain via R3.
- **Alternatives considered**: Online-only rep app (rejected — unusable in rural India, the core SFA
  market); a custom offline queue separate from `/sync` (rejected — reinvents `CentralSyncService`,
  diverges from the platform, double maintenance).

## R6. Beat plan / PJP & route-to-market model

- **Decision**: A `Beat` (named route) owns an ordered list of `BeatOutlet`s (links to the distributor's
  `customer` retailers, with sequence + visit-day). A `JourneyPlan`/PJP assigns a rep a recurring weekly
  beat calendar; a `PlannedVisit` is the day's expected stop, and an authored `Visit` (offline) is the
  actual stop with geo/time, outcome (productive/unproductive), order taken, and notes. Adherence =
  planned vs actual (visit %, on-time %, line-cuts).
- **Rationale**: This is the standard FieldAssist/Bizom beat model; separating *planned* (PJP/PlannedVisit)
  from *actual* (Visit) is what makes adherence/productivity reporting possible and matches how reps
  actually work a route. Reusing existing `customer` records as outlets avoids a parallel retailer master.
- **Alternatives considered**: Free-form visit logging with no plan (rejected — no adherence metric, the
  key SFA KPI); a brand-owned outlet master separate from the distributor's customers (rejected —
  duplication, sync conflicts).

## R7. Trade schemes / promotions & claims-settlement

- **Decision**: A brand authors a `TradeScheme` (type: slab/value/qty/free-goods/display; eligibility:
  SKU/category × geography × period; funding source). Schemes are **published down the `TradeLink`** to
  distributors. Distributor secondary-sales that match a scheme accrue a `SchemeClaim` (computed from the
  `SecondarySalesSnapshot`/qualifying invoices), which the distributor **submits** and the brand
  **approves/settles** through a `ClaimSettlement` lifecycle (`DRAFT → SUBMITTED → APPROVED|REJECTED →
  SETTLED`). Claim money is `DECIMAL(19,4)` backend / `Long` minor units mobile, and settlement posts a
  reference the distributor can reconcile (optionally a spec-013 ledger adjustment in the distributor
  tenant).
- **Rationale**: Trade-scheme claims are the financial backbone of distribution; modelling scheme →
  qualifying-sales → claim → settlement as an explicit lifecycle gives auditable reimbursement and matches
  Bizom/BeatRoute claim flows. Computing claims from the *same* secondary-sales rollup keeps brand and
  distributor numbers reconciled.
- **Alternatives considered**: Off-system (spreadsheet) claims (rejected — the pain point being solved);
  auto-credit without an approval lifecycle (rejected — brands need to review/dispute before paying).

## R8. Targets & achievement (primary + secondary)

- **Decision**: A `SalesTarget` set by tier and grain (brand→distributor primary targets; distributor/
  rep→beat secondary targets) over a period × SKU/category × geography, with `achievement` derived from
  primary orders (brand tenant) or `SecondarySalesSnapshot` (distributor tenant). Rep-level targets drive
  the SFA scorecard; distributor-level targets drive the brand DMS dashboard.
- **Rationale**: Targets vs achievement is the universal sales-management KPI; deriving achievement from
  the existing sales rollups (not a separate count) keeps it consistent with claims and dashboards.
- **Alternatives considered**: Manual achievement entry (rejected — error-prone, gameable); a single
  global target with no tier grain (rejected — brand and rep need different grains).

## R9. Stock-at-distributor visibility & no-stale-data across the chain

- **Decision**: The brand sees **distributor stock** as a consented aggregate read of the distributor's
  existing `inventory` (`Inventory.quantityOnHand` per SKU) via the same `TradeLink`-gated snapshot
  mechanism as secondary sales (`DistributorStockSnapshot`, SKU × warehouse-or-area × asOf). This powers
  **fill-rate / days-of-stock / out-of-stock** signals and replenishment nudges. No live cross-tenant
  inventory query in normal flows.
- **Rationale**: Stock visibility is the second big DMS value (after secondary sales) — it drives
  replenishment and prevents stockouts. Snapshotting (vs live access) keeps the consent/isolation boundary
  intact and is resilient to the distributor being offline.
- **Alternatives considered**: Live inventory read by the brand (rejected — isolation/consent breach);
  brand-maintained shadow stock (rejected — guaranteed to drift).

## R10. Cross-tenant identity, RBAC & the rep persona

- **Decision**: A field rep is a **`WorkspaceMember`** of the **distributor** workspace with a new
  `FIELD_REP` role (extends the existing `OWNER>ADMIN>MEMBER>VIEWER` ladder) scoped to assigned beats. A
  brand user is a member of the **brand** workspace; the brand's access to distributor data is mediated by
  the `TradeLink` scope, **not** by brand users being members of the distributor workspace. Network admin
  actions (approving links, schemes) require brand `ADMIN/OWNER`; claim approval is brand-side, claim
  submission distributor-side.
- **Rationale**: Keeps RBAC inside each tenant (no cross-tenant membership sprawl) and makes the
  `TradeLink` the sole cross-tenant trust edge — clean to audit and revoke. The `FIELD_REP` role + beat
  scoping is how SFA tools limit a rep to their route.
- **Alternatives considered**: Make brand users members of every distributor workspace (rejected —
  unmanageable membership explosion, leaks full distributor data, breaks the consent model); a global cross-
  tenant super-role (rejected — security/tenant-isolation nightmare).

## R11. Idempotency, conflict resolution & snapshot determinism

- **Decision**: Offline-authored SFA entities use **client-generated UIDs** (existing `UidGenerator`
  pattern) so retries/merges are idempotent on the canonical `/sync` push; last-write-wins by server
  `updatedAt` with local-unsynced-wins on pull (existing rules). `SecondarySalesSnapshot`/
  `DistributorStockSnapshot` are **deterministic, versioned aggregates** keyed by
  `(distributorWorkspaceId, grain, period, version)` — recomputed (not incrementally mutated) so an
  out-of-order/backdated distributor invoice simply triggers a snapshot rebuild, never a corrupt total
  (same philosophy as spec-013's recomputable balance). Brand reads always reference a snapshot version.
- **Rationale**: Offline multi-rep authoring + cross-tenant rollups are both prone to duplication and
  out-of-order arrival; UID-keyed upserts and recompute-from-source snapshots make both idempotent and
  self-healing without distributed locking.
- **Alternatives considered**: Server-assigned IDs for offline rows (rejected — breaks the offline-author
  model and dedup); incrementally mutated running snapshot totals (rejected — backdated invoices corrupt
  them, exactly the spec-013 lesson).

## R12. Phasing & scope discipline (this is a large up-market expansion)

- **Decision**: Phase strictly. **P1** = the SFA rep app (offline visits, counter orders, attendance/geo,
  beats/PJP) on the **distributor** tenant + the `TradeLink`/`TradeNetwork` plumbing and consent edge.
  **P2** = the brand DMS view: `SecondarySalesSnapshot` + `DistributorStockSnapshot` rollups, targets vs
  achievement, primary-order placement brand→distributor. **P3** = trade schemes + claims/settlement
  lifecycle, advanced analytics, tertiary-sales estimation, web parity. Each phase is independently
  shippable and de-risks the cross-tenant boundary incrementally.
- **Rationale**: This feature is genuinely a new product line (a DMS/SFA platform). Shipping the rep app
  first delivers standalone value to distributors and exercises the offline path; the cross-tenant brand
  view (the riskiest boundary) lands second on proven plumbing; the financial claims layer (highest
  correctness bar) lands last.
- **Alternatives considered**: Build the full brand-down stack at once (rejected — enormous surface,
  cross-tenant + offline + financial risk all simultaneously); ship the brand dashboard first (rejected —
  there's no secondary data to show until distributors/reps are capturing it).

---

## Resolved unknowns summary

| Unknown (Technical Context) | Resolution |
|---|---|
| Terminology | FMCG-standard primary/secondary/tertiary, RTM, beat/PJP, scheme, claim (R1) |
| Org-hierarchy model | Workspace-per-tier + `TradeNetwork`/`TradeLink` consented edge (R2) |
| Cross-tenant aggregation | Consented pull of distributor-published snapshots; `nativeQuery=true`+consent for rollups (R3) |
| Secondary-sales source | Tag/roll-up existing distributor order/invoice docs (R4) |
| Field-rep app | Offline-first on the existing `/sync` engine, distributor-scoped (R5) |
| Beat plan / RTM | `Beat`/`BeatOutlet` + PJP `JourneyPlan`/`PlannedVisit` vs actual `Visit` (R6) |
| Schemes & claims | `TradeScheme` → `SchemeClaim` → `ClaimSettlement` lifecycle (R7) |
| Targets & achievement | Tier/grain `SalesTarget`, achievement from sales rollups (R8) |
| Distributor stock visibility | `DistributorStockSnapshot`, `TradeLink`-gated (R9) |
| Cross-tenant RBAC / rep | `FIELD_REP` role in distributor tenant; `TradeLink` = sole trust edge (R10) |
| Idempotency / snapshots | UID-keyed offline upserts; deterministic recomputable versioned snapshots (R11) |
| Phasing | P1 SFA → P2 brand DMS view → P3 schemes/claims/analytics (R12) |
