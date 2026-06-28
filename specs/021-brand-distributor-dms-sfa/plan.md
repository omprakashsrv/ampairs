# Implementation Plan: Brand → Distributor DMS + Sales Force Automation

**Branch**: `021-brand-distributor-dms-sfa` (dev branch `claude/brand-distributor-dms-sfa-12692h`) | **Date**: 2026-06-28 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/021-brand-distributor-dms-sfa/spec.md`

## Summary

Take Ampairs up-market from a **single-tier** business app to a **multi-tier trade platform**: the
**brand → distributor → retailer** chain, with **secondary-sales visibility** rolling up to the brand and
a **field-sales-rep (SFA) app** that captures **visits, counter orders, attendance and geo offline** in
rural beats. Reference systems: BeatRoute, Bizom, FieldAssist.

Technical approach: a new `trade` (DMS/SFA) bounded context that layers a **`TradeNetwork`/`TradeLink`**
graph **on top of existing workspaces** — each tier stays its own isolated tenant, and the only
cross-tenant trust edge is an explicit, **consented `TradeLink`**. Secondary sales originate as the
distributor's normal `order`/`invoice` documents (tagged + rolled into deterministic, recomputable
`SecondarySalesSnapshot`s); the brand reads those snapshots **pull-based and consented**, with any genuine
cross-tenant SQL using `nativeQuery = true` behind a live-consent gate. The SFA rep app rides the existing
offline `/sync` engine (Room `synced=false` + `SyncDelegate`), scoped to the **distributor** workspace.
Trade schemes, claims/settlement, targets and distributor-stock visibility follow the same snapshot rail.
Money is `DECIMAL(19,4)` backend / `Long` minor units mobile. Full rationale in [research.md](./research.md).

**Clarifications folded in (2026-06-28 session):**
- **Retailer PII**: a `TradeLink` shares outlets **coded/aggregated by default**; identified-retailer
  sharing (name/area) is an explicit opt-in on the link scope; full contact PII never crosses. → drives
  `ConsentScope` flags + `NetworkRetailer` projection.
- **Snapshot freshness**: snapshots are **event-triggered, coalesced to ≤ once per ~5 min per distributor**
  (figures ≤ ~5 min stale). → debounced rebuild queue, not per-write recompute.
- **Geo at check-in**: **capture + flag** out-of-radius visits, **never block**. → `Visit.geoFenceStatus`
  (IN_RADIUS / OUT_OF_RADIUS / NO_LOCATION), no hard gate.
- **Ad-hoc & new outlet**: reps may make **unplanned visits** to any of their distributor's outlets and
  **register a new retailer offline**. → `Visit.adHoc` flag; rep can author a `customer` create over sync.
- **Primary orders**: brand → distributor is a **handshake** (brand-tenant order → surfaced over link →
  distributor confirms → becomes a normal order in the distributor tenant), never a silent cross-tenant
  write. → `PrimaryOrderLink` referencing the brand-side order uid + distributor-side confirmation.

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Java 21 (Spring Boot 4.0); Mobile Kotlin Multiplatform 2.4
(Compose Multiplatform 1.11).
**Primary Dependencies**: Backend — Spring Data JPA, Flyway, Jackson (SNAKE_CASE), `core`
(`OwnableBaseDomain`, `BaseDomain`, `ApiResponse`, `PageResponse`, `TenantContextHolder`); **public service
interfaces** of `workspace` (membership/roles), `order` (`OrderService.bulkUpsertOrders`/`getOrdersAfterSync`),
`invoice` (`InvoiceService` + existing `InvoiceFinalizedEvent`/`InvoiceCancelledEvent` from the `event`
module), `product`/`inventory`, `customer` (`CustomerService`) for snapshots and tagging; Spring
`ApplicationEventPublisher` for cross-module events. Mobile — Room KMP, Ktor, Metro DI, Navigation3,
kotlinx.datetime, **Moko Permissions + Play Services Location** for geo/attendance, existing `data/sync`
(`CentralSyncService`, `SyncDelegate`), `data/common` (`ApiUrlBuilder`, `WorkspaceAwareDatabaseFactory`).
**Storage**: Backend — PostgreSQL (runtime) + MySQL parity via Flyway (next free global version **V1.0.117**),
money `DECIMAL(19,4)`, timestamps `TIMESTAMPTZ`/`TIMESTAMP`; snapshot tables versioned. Mobile — Room
(workspace-scoped DB `trade`), money `Long` minor units, geo as lat/lng `Double` captured on-device.
**Testing**: Backend — JUnit/Testcontainers (`./gradlew :trade:test`), incl. cross-tenant consent-gate
tests (brand cannot read without a live `TradeLink`), snapshot-recompute determinism, claim-lifecycle,
PII-projection (coded vs identified). Mobile — `./gradlew :feature:trade:check`; 3-target compile gates;
offline author→sync round-trip incl. offline new-outlet + ad-hoc visit.
**Target Platform**: Backend service (Linux); Mobile Android (minSdk 24) / iOS / Desktop (JVM) — the rep
app is primarily Android (field devices).
**Project Type**: Mobile + API. The SFA rep app is **offline-first**; the brand DMS view is online
dashboards over published snapshots.
**Performance Goals**: Counter order + visit capture perceived instant offline (<2 s, no network);
secondary-sales/stock figures ≤ ~5 min stale (coalesced rebuild); dashboards render rollups for a brand
with hundreds of distributors without perceptible lag; sync batches 100 records/page.
**Constraints**: **Tenant isolation is absolute** — no tier sees another's data except through a consented
`TradeLink`; cross-tenant reads use `nativeQuery=true` + a service-layer consent check. Rep app must
author fully offline (incl. new outlets). Snapshots must be deterministic + recomputable (no drift on
backdated docs). Retailer contact PII never crosses a link.
**Scale/Scope**: P1 distributor SFA + link plumbing; per distributor: tens of reps, thousands of outlets,
high offline order volume. Brand (P2): hundreds of linked distributors aggregated. ~14–16 backend entities
across phases, ~8–10 sync entities (SFA), ~6–8 mobile screens.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How this plan complies |
|---|---|---|
| I. Type Safety (Instant/TIMESTAMPTZ, money) | ✅ PASS | All timestamps `Instant` → `TIMESTAMPTZ`/`TIMESTAMP`; money `BigDecimal`/`DECIMAL(19,4)`; no `LocalDateTime`/`Double` money. Geo lat/lng `Double` is fine (not money). |
| II. DTO & Contract Isolation | ✅ PASS | Request/Response DTOs in `trade/domain/dto/`; entities never exposed; `entity.asResponse()`/`request.toEntity()` converters with `@field:` validation. |
| III. Global JSON SNAKE_CASE | ✅ PASS | Global Jackson strategy; no `@JsonProperty` for standard fields. |
| IV. Multi-Tenant Isolation | ⚠ PASS (with documented cross-tenant edge) | Every entity extends `OwnableBaseDomain` (`@TenantId ownerId`); tenant set by `SessionUserFilter`. **Cross-tier reads are the explicit exception**: served via consented snapshot publication; any genuine cross-tenant SQL uses `nativeQuery=true` + a `TradeLink`-consent check (rule 05). The `TradeLink` is the sole, auditable trust edge. See Complexity Tracking. |
| V. API Response Standardization | ✅ PASS | All endpoints return `ApiResponse<T>`; sync pull returns `ApiResponse<PageResponse<T>>`. |
| VI. Centralized Exception Handling | ✅ PASS | No business try/catch in controllers; typed `TradeException`/`ConsentRequiredException`/`ClaimStateException`/`LinkStateException` bubble to the global handler. |
| VII. Efficient Data Loading | ✅ PASS | `@NamedEntityGraph` for beat+outlets, journeyPlan+plannedVisits, scheme+claims; derived queries; `@Query`/`nativeQuery` only for snapshot rollups + consented cross-tenant rollups. |
| VIII. Angular Material 3 Exclusivity | ✅ N/A (this phase) | Web brand-dashboard parity deferred to P3; will use Angular Material 3 only. |
| IX. Domain-Driven Module Boundaries | ✅ PASS | New `trade` bounded context; reads `workspace`/`order`/`invoice`/`inventory`/`customer` **only** via public service interfaces + `event`-module events, never repositories. |
| X. Compose Multiplatform Parity | ✅ PASS | Shared SFA logic/UI in `feature/trade/src/commonMain`; thin platform DI; geo/permissions via expect/actual + Moko/Play Services in platform sets. Parity tracked in this spec. |
| XI. Security & Secrets Hygiene | ✅ PASS | No new secrets; standard JWT/workspace auth; cross-tenant access strictly consent-gated and revocable; retailer PII never crosses a link. |
| Flyway | ✅ PASS | Versioned migration in **both** `mysql/` and `postgresql/` at **V1.0.117**; `trade` added to `migrationModules`; verify with `flywayInfo` before commit. |
| Offline `/sync` contract | ✅ PASS | SFA entities (visit, field-order, attendance, beat, journey-plan) ride the canonical `GET/POST /trade/v1/{resource}/sync`; snapshots are pull-only (server-computed). |
| Testing & Quality Gates | ✅ PASS | Backend ≥80% critical / ≥90% endpoints on consent gate + snapshot recompute + claim lifecycle + PII projection; mobile `check` + 3-target compile + offline round-trip. |

**Result**: PASS — the only flagged item (IV) is the **deliberate cross-tier visibility boundary**, which
complies with rule 05 (cross-tenant reads need `nativeQuery=true` + consent). It is the feature's central
design point, documented in Complexity Tracking, not a violation.

## Project Structure

### Documentation (this feature)

```
specs/021-brand-distributor-dms-sfa/
├── plan.md              # This file
├── spec.md              # Feature specification (with Clarifications session 2026-06-28)
├── research.md          # Phase 0 — hierarchy, cross-tenant aggregation, offline SFA, snapshots, clarifications
├── data-model.md        # Phase 1 — entities, TradeLink consent, snapshot keys, state machines
├── quickstart.md        # Phase 1 — link a brand↔distributor, capture a beat order, roll up secondary sales
├── contracts/
│   ├── README.md
│   ├── trade-sfa-sync.md          # canonical /sync endpoints for visit/field-order/attendance/beat/journey-plan
│   ├── trade-network-actions.md   # link invite/accept/revoke, scheme publish, claim submit/approve, primary order
│   └── trade-snapshots.md         # secondary-sales / distributor-stock snapshot reads (consented)
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```
# Backend — ampairs/ (this repo)
trade/
└── src/main/
    ├── kotlin/com/ampairs/trade/
    │   ├── domain/
    │   │   ├── model/          # TradeNetwork, TradeLink (consented edge), ConsentScope (embeddable),
    │   │   │                   # NetworkRetailer, Beat, BeatOutlet, JourneyPlan(PJP), PlannedVisit, Visit,
    │   │   │                   # Attendance, FieldOrder (counter order ref), PrimaryOrderLink, SalesTarget,
    │   │   │                   # SecondarySalesSnapshot, DistributorStockSnapshot (versioned),
    │   │   │                   # TradeScheme, SchemeClaim, ClaimSettlement
    │   │   ├── enums/          # TradeTier, LinkStatus, ConsentLevel, VisitOutcome, GeoFenceStatus,
    │   │   │                   # ClaimStatus, SchemeType, SalesType (PRIMARY/SECONDARY/TERTIARY)
    │   │   └── dto/            # request/response DTOs + converters
    │   ├── repository/         # Spring Data repos (+ @EntityGraph; @Query/nativeQuery for snapshots/rollups)
    │   ├── service/            # TradeLinkService (consent edge), SnapshotService (debounced recompute),
    │   │                       # BeatService, VisitService, AttendanceService, SchemeService, ClaimService,
    │   │                       # TargetService, PrimaryOrderService, CrossTenantReadGuard (consent check)
    │   ├── controller/         # TradeNetworkController, TradeSyncController (SFA /sync),
    │   │                       # SnapshotController, SchemeClaimController, PrimaryOrderController
    │   ├── config/             # Constants, TradeSettingDefinitions
    │   └── listener/           # listeners on invoice/order events → tag SECONDARY + enqueue snapshot rebuild
    └── resources/db/migration/
        ├── mysql/V1.0.117__create_trade_module_tables.sql
        └── postgresql/V1.0.117__create_trade_module_tables.sql
# wiring: settings.gradle.kts (include("trade")); ampairs_service/build.gradle.kts
#         (implementation(project(":trade")) + "trade" in migrationModules)
# workspace module: add FIELD_REP role (level 30, between GUEST=20 and MEMBER=40) via WorkspaceRole enum
# reads order/invoice/inventory/customer PUBLIC SERVICE INTERFACES + event-module events only — no cross-module repo access

# Mobile — ampairs-app/ (sibling repo) — OFFLINE-FIRST SFA rep app
feature/trade/src/
├── commonMain/kotlin/com/ampairs/trade/
│   ├── data/api/          # TradeApi(+Impl), ApiUrlBuilder.tradeUrl
│   ├── data/db/           # Room entities + DAOs + TradeRoomDatabase (visit/field-order/attendance/beat/journey-plan)
│   ├── data/repository/   # VisitRepository, FieldOrderRepository, AttendanceRepository (local-only)
│   ├── domain/            # Money (minor units), models, geo capture, enums
│   ├── di/                # TradeModule.kt
│   ├── sync/              # Visit/FieldOrder/Attendance/Beat/JourneyPlan SyncDelegates (canonical /sync)
│   └── ui/                # screens + ViewModels (today's beat, outlet visit, take order, add outlet,
│                          #   check-in/out, my targets, beat scorecard)
├── androidMain/ iosMain/ desktopMain/   # TradeModule.{platform}.kt (@SingleIn(WorkspaceScope::class));
#                                          geo/permissions actuals (Play Services / Moko)
# wiring: settings.gradle.kts (:feature:trade); SyncEntity enum additions;
#         shared/ Routes + entry provider; ModuleRegistry ("dms-sfa" → Route.Trade);
#         data/common ApiUrlBuilder.tradeUrl(...)
# brand DMS dashboards (secondary-sales/stock/targets) are online pull-only views over snapshots
```

**Structure Decision**: Mobile + API. Backend `trade/` mirrors existing bounded contexts (`order/`'s
`domain/{model,dto,enums}` + `repository` + `service` + `controller` + `listener` layout); the new
wrinkles are `CrossTenantReadGuard` + versioned snapshot tables. The mobile `feature/trade/` SFA module
mirrors `feature/order`'s offline-first shape (SyncDelegate-owned API, workspace-scoped DB) and adds geo/
attendance platform actuals. The brand DMS view is online (pull-only snapshot reads). Web (Angular) brand
dashboard is a P3 follow-up.

## Phased Delivery

### Phase 1 (MVP) — Distributor SFA app + network/link plumbing
- **Entities**: `TradeNetwork`, `TradeLink` (consented edge) + `ConsentScope`, `NetworkRetailer`, `Beat`,
  `BeatOutlet`, `JourneyPlan`/PJP, `PlannedVisit`, `Visit`, `Attendance`, `FieldOrder`.
- **Services**: `TradeLinkService` (invite/accept/revoke/consent scope — incl. coded-vs-identified default),
  `BeatService`, `VisitService` (geo-fence flag, ad-hoc), `AttendanceService`; `CrossTenantReadGuard` scaffold.
- **Endpoints**: SFA canonical `/sync` (`/trade/v1/{visits|field-orders|attendance|beats|journey-plans}/sync`,
  `GET`+`POST`); `POST /trade/v1/links` (invite), `POST /trade/v1/links/{uid}/accept`,
  `POST /trade/v1/links/{uid}/revoke`.
- **Workspace**: add `FIELD_REP` role (level 30, additive) + beat scoping.
- **Mobile**: offline rep app — today's beat (PJP), outlet visit (geo/time/outcome + flag), add new outlet
  offline, take order at counter, check-in/out attendance, my targets; all author-offline via `/sync`.

### Phase 2 — Brand DMS visibility (secondary sales + stock + targets + primary order)
- **Entities**: `SecondarySalesSnapshot`, `DistributorStockSnapshot` (versioned, recomputable),
  `SalesTarget`, `PrimaryOrderLink`.
- **Services**: `SnapshotService` (event-driven, **debounced ≤ 5 min/distributor** recompute from
  distributor invoice/order/inventory), `TargetService`, `PrimaryOrderService`; activate
  `CrossTenantReadGuard` (consent check before any `nativeQuery` rollup, PII projection per scope).
- **Endpoints**: `GET /trade/v1/snapshots/secondary-sales`, `.../distributor-stock`, `GET /trade/v1/targets`
  (all `TradeLink`-scoped to the calling brand); `POST /trade/v1/primary-orders` (brand authors),
  `POST /trade/v1/primary-orders/{uid}/confirm` (distributor confirms → distributor-tenant order).
- **Events**: distributor `InvoiceFinalizedEvent`/`InvoiceCancelledEvent` (existing) + order events → tag
  `SECONDARY` → enqueue debounced snapshot rebuild; inventory change → stock-snapshot rebuild.
- **Mobile/Web**: brand DMS dashboards (online, pull-only over snapshots) — secondary-sales by SKU/beat/
  area, distributor days-of-stock, target vs achievement.

### Phase 3 — Trade schemes, claims/settlement, analytics, tertiary
- `TradeScheme` (slab/value/qty/free-goods) published down links; `SchemeClaim` accrued from qualifying
  secondary sales; `ClaimSettlement` lifecycle (`DRAFT→SUBMITTED→APPROVED|REJECTED→SETTLED`) with optional
  spec-013 ledger adjustment on settlement.
- Tertiary-sales estimation (secondary − stock delta); advanced RTM analytics (productivity, fill-rate).
- Angular web parity (Material 3) for the brand dashboard.

## Complexity Tracking

| Violation / Added complexity | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| Cross-tenant read edge (`TradeLink` consent + `nativeQuery=true` rollups) | A brand must see aggregated secondary sales/stock across many independent distributor tenants — the core DMS value | A parent-child mega-tenant breaks `@TenantId` isolation, the offline-per-workspace DB model, per-tier billing/RBAC, and the consent boundary |
| Versioned recomputable snapshot tables (secondary-sales/stock) | Out-of-order/backdated distributor docs and offline multi-rep authoring would corrupt incrementally-mutated totals | Live cross-tenant queries breach isolation/consent and couple the brand to the distributor's live schema; running totals drift on backdated docs (spec-013 lesson) |
| Debounced snapshot rebuild queue (≤ 5 min/distributor) | Per-write recompute for a brand with hundreds of distributors is wasteful; raw real-time isn't required (clarified ≤ ~5 min staleness acceptable) | Per-write recompute wastes compute at scale; pure nightly batch is too stale for replenishment signals |
| New `FIELD_REP` role + beat scoping in `workspace` | SFA reps must be limited to their distributor's beats | A global cross-tenant role leaks distributor data and explodes membership; no scoping means any member sees all outlets |
| Primary-order handshake (`PrimaryOrderLink` + confirm step) instead of direct write | Each tenant must stay authoritative for its own orders; the brand can't silently write into the distributor's tenant | A direct cross-tenant order insert breaches isolation/consent and bypasses the distributor's own order validation/pricing |
| Snapshot publication pipeline (event-driven rebuild) | Decouples the brand's read path from the distributor's live writes and keeps the consent/isolation boundary intact | Direct live cross-tenant reads or nightly ETL are either isolation-breaking or stale and still need the consent edge |
