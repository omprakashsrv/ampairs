# Module Boundaries — Feature 021 (Brand → Distributor DMS + SFA)

Defines which module owns which part of feature 021 and its sub-specs, per Constitution Principle IX
(Domain-Driven Module Boundaries). The feature introduces **four new backend bounded contexts** —
`trade`, `sfa`, `dms`, `claim` — and makes small additive changes to existing modules. Cross-module access is via
**public service interfaces + events only**, never direct repository access.

> **Status note:** `plan.md`, `tasks.md`, `data-model.md`, and `contracts/` were authored against a single
> `ampairs/trade/...` path. The four-module split below supersedes that; the path remap is listed under
> **Reconciliation** and is a flagged follow-up.

---

## The four new modules

### 1. `trade` — cross-tenant network & consent core (the trust edge)
The single place that holds the cross-tenant edge between a brand workspace and a distributor workspace.
- **Owns**: `TradeNetwork`, `TradeLink` + `ConsentScope`, `NetworkRetailer`, `NetworkBrand` (Hop A
  attribution), `NetworkProduct` (Hop B SKU map) + NPI import, `PrimaryOrderLink`; services
  `TradeLinkService`, `NetworkBrandService`, `NetworkProductService`, `PrimaryOrderService`; and
  **`CrossTenantReadGuard`** (the active-link + scope check every cross-tenant read passes through).
- **Depends on**: `workspace`, `core`. Nothing else.
- **Why its own context**: the consent edge + `nativeQuery`-gated cross-tenant reads are the feature's single
  most security-sensitive surface; isolating them makes the trust boundary auditable.

### 2. `sfa` — distributor field-sales automation (offline, distributor-only)
The distributor's internal field-ops tool. No cross-tenant concern; fully offline-first.
- **Owns**: `Beat`, `BeatOutlet`, `JourneyPlan` (PJP), `PlannedVisit`, `Visit`, `Attendance`,
  `FieldOrder` (ref), `Leave`, `VisitSurveyResponse`; services `BeatService`, `VisitService`,
  `AttendanceService`, `JourneyPlanService`/adherence, `AttendanceSummaryService`, `VisitProductivityService`,
  survey-rollup, `LeaveService`; the SFA `/sync` controllers + `VisitSurveyStandardFieldProvider` (registered
  via the `form` SPI).
- **Depends on**: `workspace` (FIELD_REP role), `customer` (outlets), `order` (counter orders), `form`
  (survey templates), `setting`, `event`, `core`.
- **Independent of `trade`/`dms`** — ships standalone as the **MVP** (US1). Its sales output reaches the
  brand only indirectly, via the `order`/`invoice` documents that `dms` reads.

### 3. `dms` — brand distribution-management visibility (cross-tenant aggregates)
The brand-facing, online, read-mostly visibility layer over published aggregates.
- **Owns**: `SecondarySalesSnapshot`, `DistributorStockSnapshot` (versioned, recomputable), `SalesTarget`;
  services `SnapshotService` (event-driven, debounced), `TargetService`; the listeners that tag SECONDARY +
  enqueue snapshot rebuilds.
- **Depends on**: `trade` (consent via `CrossTenantReadGuard` + NetworkBrand/NetworkProduct attribution),
  `order`/`invoice`/`product`/`inventory` (source data via public services + events), `event`, `core`.
- **Does NOT depend on `sfa`** — it reads SFA's effect through the `order`/`invoice` modules, not the SFA
  entities.

### 4. `claim` — trade-scheme claims & settlement (the reimbursement layer)
The brand-funded **reimbursement** lifecycle. NOTE: the brand-funded **scheme definition + application**
(QPS/TPR/BOGO/volume, stamping `fundingBrandId` at order time) **already lives in `pricing`** (spec 015,
`Offer`/PriceList engine); `claim` reuses it and owns ONLY the settlement that 015 explicitly deferred.
- **Owns**: `SchemeClaim`, `ClaimSettlement`; service `ClaimService` (accrue from `pricing`-tagged,
  `fundingBrandId`-attributed qualifying secondary sales → submit → approve/reject → settle; optional
  `payment` ledger on settle). Does **not** re-define `TradeScheme` — that is `pricing`/015.
- **Depends on**: `pricing` (brand-funded scheme + funding attribution), `dms` (qualifying
  `SecondarySalesSnapshot`s), `trade` (consent/link), `payment` (ledger), `event`, `core`.
- **`claim` vs `pricing` boundary**: `pricing` = *scheme definition + discount application at the point of
  sale* (stamps `fundingBrandId`); `claim` = *cross-tenant brand→distributor reimbursement* (accrue→settle)
  computed from those tagged sales. Complementary, not duplicative — `claim` builds on `pricing`.

### Dependency graph (acyclic)
```
sfa    → workspace, customer, order, form, setting, event, core       (standalone; the MVP)
trade  → workspace, core
dms    → trade, order, invoice, product, inventory, event, core
claim  → pricing, dms, trade, payment, event, core
```
No cycles: `sfa` depends on none of the others; `dms` → `trade`; `claim` → `pricing`/`dms`/`trade`; nothing
depends on `sfa`.

---

## Additive changes to existing modules (NOT new modules)

| Module | Additive change |
|---|---|
| `workspace` | Add `FIELD_REP` role to `WorkspaceRole` (level 30, between GUEST=20 and MEMBER=40) + beat scoping |
| `form` | Add `EntityType.VISIT_SURVEY`; the `VisitSurveyStandardFieldProvider` lives in **`sfa`** (form SPI) |
| `order` | Counter orders (a `FieldOrder` references a real order here); primary-order confirm creates an order here. Read by `sfa`/`dms`/`trade` via `OrderService` |
| `invoice` | Secondary-sales source docs + `InvoiceFinalizedEvent`/`InvoiceCancelledEvent` (consumed by `dms`) |
| `product` / `inventory` | `ProductBrand`/`Product`/barcode (Hop A/B + NPI), distributor `Inventory` (stock snapshots) — read by `trade`/`dms` via `ProductService`; NPI import creates products via `ProductService` |
| `customer` | Retail outlets (beat outlets reference customers; offline new-outlet via customer `/sync`) |
| `setting` | `sfa`/`trade`/`dms`/`claim` register their `*SettingDefinitions` (geo-fence radius, auto-close cutoff, snapshot-coalesce window) |
| `payment` | Optional spec-013 ledger adjustment on claim settlement; its `AgingService` is the read-model pattern `sfa` summaries mirror |
| `event` | Shared domain-event classes the new modules publish/consume |
| `ampairs_service` | Add `trade`, `sfa`, `dms`, `claim` to `include(...)` + `migrationModules` |

---

## Capability / sub-spec → module map

| Capability / sub-spec | Backend | Mobile |
|---|---|---|
| Network, links, consent, NetworkRetailer (parent FR-001–007) | `trade` | `feature/trade` |
| Product linking Hop A/B + NPI import (FR-018a–d, sub-spec product-brand-attribution) | `trade` (+ `product`) | `feature/trade` |
| Primary-order handshake (FR-024a) | `trade` (+ `order`) | `feature/trade` |
| Beats / PJP / planned-visits / adherence (sfa-field-operations §1) | `sfa` | `feature/sfa` |
| Attendance capture + summary + leave (sfa-field-ops + field-ops-reporting A) | `sfa` (+ `workspace`) | `feature/sfa` |
| Store visits + survey + productivity (sfa-field-ops + field-ops-reporting B) | `sfa` (+ `form`, `order`) | `feature/sfa` |
| Secondary-sales / distributor-stock snapshots, targets (FR-018–025) | `dms` | `feature/dms` |
| Trade-scheme **claims & settlement** (FR-027–029; scheme *definition* = `pricing`/spec-015) | `claim` (+ `pricing`, `dms`, `payment`) | `feature/dms` |

### Mobile (`ampairs-app`) modules
Follows the existing `feature/{x}` (+ `feature/{x}-api`) pattern:
- **`feature/sfa`** — the offline rep app (today's beat, visit, attendance, counter order, survey, leave view)
  = the MVP. Reuses `feature/order` (counter orders), `feature/customer` (new outlet), `feature/form` (survey
  render), `data/sync`, `data/common`, `shared`.
- **`feature/trade`** — distributor-side linking/designation/NPI (accept links, designate brand label, map
  SKUs, available-for-import) + primary-order confirm.
- **`feature/dms`** — brand dashboards (secondary-sales/stock/targets/schemes/claims, pull-only over
  snapshots).
- Add a `-api` module only where another feature consumes one of these.

---

## Migration ownership (global Flyway versions, per-module dirs)

Each module writes BOTH `postgresql/` and `mysql/`. Versions stay global (verify with `flywayInfo`).

| Module | Tables |
|---|---|
| `trade` | trade_networks, trade_links, network_retailers, network_brands, network_products, primary_order_links |
| `sfa` | beats, beat_outlets, journey_plans, planned_visits, visits, attendance, field_orders, leaves, visit_survey_responses |
| `dms` | secondary_sales_snapshots, distributor_stock_snapshots, sales_targets |
| `claim` | scheme_claims, claim_settlements (scheme *definition* tables live in `pricing`/spec-015) |

---

## Reconciliation (follow-up — current artifacts still use a single `trade`)

`plan.md` / `tasks.md` / `data-model.md` / `contracts/` reference `ampairs/trade/...` and a single
`feature/trade`. To adopt this split they must be remapped:
- `ampairs/trade/...` → split across `ampairs/trade/`, `ampairs/sfa/`, `ampairs/dms/` per the tables above.
- `feature/trade` (mobile) → `feature/sfa` (rep app/MVP), `feature/trade` (linking), `feature/dms` (brand
  dashboards).
- Flyway: `V1.0.117` (network) stays `trade`; SFA tables move to an `sfa` migration; snapshot/target
  tables to `dms`, claim/settlement to `claim`, leave/survey to `sfa` migrations — reassign versions and update the migration-ordering
  note + `migrationModules`.
- Phase/task module paths and `SyncEntity` registrations follow the module each entity lands in.

This is a sizable, mechanical reconciliation; until done, `/speckit-analyze` will flag the path mismatch.
