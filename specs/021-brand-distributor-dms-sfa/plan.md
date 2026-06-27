# Implementation Plan: Brand → Distributor DMS + Sales Force Automation

**Branch**: `claude/indian-retail-ecosystem-877med` (spec dir `021-brand-distributor-dms-sfa`) | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)
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

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Java 21 (Spring Boot 4.0); Mobile Kotlin Multiplatform 2.4
(Compose Multiplatform 1.11).
**Primary Dependencies**: Backend — Spring Data JPA, Flyway, Jackson (SNAKE_CASE), `core`
(`OwnableBaseDomain`, `ApiResponse`, `PageResponse`, `TenantContextHolder`); **public service interfaces**
of `workspace` (link/membership/roles), `order`, `invoice`, `product`/`inventory`, `customer` for
snapshots and tagging; Spring `ApplicationEventPublisher` for cross-module events. Mobile — Room KMP, Ktor,
Metro DI, Navigation3, kotlinx.datetime, **Moko Permissions + Play Services Location** for geo/attendance,
existing `data/sync` (`CentralSyncService`, `SyncDelegate`), `data/common` (`ApiUrlBuilder`,
`WorkspaceAwareDatabaseFactory`).
**Storage**: Backend — PostgreSQL/MySQL via Flyway, money `DECIMAL(19,4)`, timestamps `TIMESTAMPTZ`/
`TIMESTAMP`; snapshot tables versioned. Mobile — Room (workspace-scoped DB `trade`), money `Long` minor
units, geo as lat/lng `Double` captured on-device.
**Testing**: Backend — JUnit/Testcontainers (`./gradlew :trade:test`), incl. cross-tenant consent-gate
tests (brand cannot read without a live `TradeLink`), snapshot-recompute determinism, claim-lifecycle.
Mobile — `./gradlew :feature:trade:check`; 3-target compile gates; offline author→sync round-trip tests.
**Target Platform**: Backend service (Linux); Mobile Android (minSdk 24) / iOS / Desktop (JVM) — the rep
app is primarily Android (field devices).
**Project Type**: Mobile + API. The SFA rep app is **offline-first**; the brand DMS view is online
dashboards over published snapshots.
**Performance Goals**: Counter order + visit capture perceived instant offline (<2 s, no network);
secondary-sales/stock dashboards render rollups for a brand with hundreds of distributors without lag;
sync batches 100 records/page.
**Constraints**: **Tenant isolation is absolute** — no tier sees another's data except through a consented
`TradeLink`; cross-tenant reads use `nativeQuery=true` + a service-layer consent check. Rep app must
author fully offline. Snapshots must be deterministic + recomputable (no drift on backdated docs).
**Scale/Scope**: P1 distributor SFA + link plumbing; per distributor: tens of reps, thousands of outlets,
high offline order volume. Brand (P2): hundreds of linked distributors aggregated. ~10–14 backend entities
across phases, ~8–10 sync entities (SFA), ~6–8 mobile screens.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How this plan complies |
|---|---|---|
| I. Type Safety (Instant/TIMESTAMPTZ) | ✅ PASS | All timestamps `Instant` → `TIMESTAMPTZ`/`TIMESTAMP`; money `BigDecimal`/`DECIMAL(19,4)`; no `LocalDateTime`/`Double` money. |
| II. DTO & Contract Isolation | ✅ PASS | Request/Response DTOs in `trade/domain/dto/`; entities never exposed; `entity.asResponse()`/`request.toEntity()` converters with validation. |
| III. Global JSON SNAKE_CASE | ✅ PASS | Global Jackson strategy; no `@JsonProperty` for standard fields. |
| IV. Multi-Tenant Isolation | ⚠ PASS (with documented cross-tenant edge) | Every entity extends `OwnableBaseDomain` (`@TenantId`); tenant set by `SessionUserFilter`. **Cross-tier reads are the explicit exception**: served via consented snapshot publication, and any genuine cross-tenant SQL uses `nativeQuery=true` + a `TradeLink`-consent check (per rule 05). The `TradeLink` is the sole, auditable trust edge. |
| V. API Response Standardization | ✅ PASS | All endpoints return `ApiResponse<T>`; sync pull returns `ApiResponse<PageResponse<T>>`. |
| VI. Centralized Exception Handling | ✅ PASS | No business try/catch in controllers; typed `TradeException`/`ConsentRequiredException`/`ClaimStateException` bubble to the global handler. |
| VII. Efficient Data Loading | ✅ PASS | `@NamedEntityGraph` for beat+outlets, scheme+claims; derived queries; `@Query`/`nativeQuery` only for snapshot rollups + consented cross-tenant rollups. |
| VIII. Angular Material 3 Exclusivity | ✅ N/A (this phase) | Web brand-dashboard parity deferred to P3; will use Angular Material 3 only. |
| IX. Domain-Driven Module Boundaries | ✅ PASS | New `trade` bounded context; reads `workspace`/`order`/`invoice`/`inventory`/`customer` **only** via public service interfaces + events, never repositories. |
| X. Compose Multiplatform Parity | ✅ PASS | Shared SFA logic/UI in `feature/trade/src/commonMain`; thin platform DI; geo/permissions via expect/actual + Moko/Play Services in platform sets. |
| XI. Security & Secrets Hygiene | ✅ PASS | No new secrets; standard JWT/workspace auth; cross-tenant access strictly consent-gated and revocable. |
| Flyway | ✅ PASS | Versioned migration in **both** `mysql/` and `postgresql/`; `trade` added to `migrationModules`; next version via `flywayInfo`. |
| Offline `/sync` contract | ✅ PASS | SFA entities (visit, order, attendance, geo, beat) ride the canonical `GET/POST /trade/v1/{resource}/sync`; snapshots are pull-only (server-computed). |
| Testing & Quality Gates | ✅ PASS | Backend ≥80% on consent gate + snapshot recompute + claim lifecycle; mobile `check` + 3-target compile + offline round-trip. |

**Result**: PASS — the only flagged item (IV) is the **deliberate cross-tier visibility boundary**, which
complies with rule 05 (cross-tenant reads need `nativeQuery=true` + consent). It is the feature's central
design point, documented in Complexity Tracking, not a violation.

## Project Structure

### Documentation (this feature)

```
specs/021-brand-distributor-dms-sfa/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 — hierarchy, cross-tenant aggregation, offline SFA, snapshots
├── data-model.md        # Phase 1 — entities, TradeLink consent, snapshot keys, state machines
├── quickstart.md        # Phase 1 — link a brand↔distributor, capture a beat order, roll up secondary sales
├── contracts/
│   ├── README.md
│   ├── trade-sfa-sync.md          # canonical /sync endpoints for visit/order/attendance/beat
│   ├── trade-network-actions.md   # link invite/accept, scheme publish, claim submit/approve
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
    │   │   ├── model/          # TradeNetwork, TradeLink (consented edge), NetworkRetailer,
    │   │   │                   # Beat, BeatOutlet, JourneyPlan/PJP, PlannedVisit, Visit, Attendance,
    │   │   │                   # FieldOrder (counter order ref), SalesTarget,
    │   │   │                   # SecondarySalesSnapshot, DistributorStockSnapshot (versioned),
    │   │   │                   # TradeScheme, SchemeClaim, ClaimSettlement
    │   │   ├── enums/          # TradeTier, LinkStatus, ConsentScope, VisitOutcome, ClaimStatus,
    │   │   │                   # SchemeType, SalesType (PRIMARY/SECONDARY/TERTIARY), TradeRole
    │   │   └── dto/            # request/response DTOs + converters
    │   ├── repository/         # Spring Data repos (+ @EntityGraph; @Query/nativeQuery for snapshots/rollups)
    │   ├── service/            # TradeLinkService (consent edge), SnapshotService (recompute),
    │   │                       # BeatService, VisitService, SchemeService, ClaimService, TargetService,
    │   │                       # CrossTenantReadGuard (TradeLink consent check before nativeQuery)
    │   ├── controller/         # TradeNetworkController, TradeSyncController (SFA /sync),
    │   │                       # SnapshotController, SchemeClaimController
    │   ├── config/             # Constants, TradeSettingDefinitions
    │   └── event/              # listeners on order/invoice events → tag + enqueue snapshot rebuild
    └── resources/db/migration/
        ├── mysql/V1.0.x__create_trade_module_tables.sql
        └── postgresql/V1.0.x__create_trade_module_tables.sql
# wiring: settings.gradle.kts (include "trade"); ampairs_service/build.gradle.kts
#         (implementation(project(":trade")) + "trade" in migrationModules)
# workspace module: add FIELD_REP role (additive to WorkspaceRole ladder) via public service
# reads order/invoice/inventory/customer PUBLIC SERVICE INTERFACES only — no cross-module repo access

# Mobile — ampairs-app/ (sibling repo) — OFFLINE-FIRST SFA rep app
feature/trade/src/
├── commonMain/kotlin/com/ampairs/trade/
│   ├── data/api/          # TradeApi(+Impl), ApiUrlBuilder.tradeUrl
│   ├── data/db/           # Room entities + DAOs + TradeRoomDatabase (visit/order/attendance/beat)
│   ├── data/repository/   # VisitRepository, FieldOrderRepository, AttendanceRepository (local-only)
│   ├── domain/            # Money (minor units), models, geo capture, enums
│   ├── di/                # TradeModule.kt
│   ├── sync/              # Visit/FieldOrder/Attendance/Beat SyncDelegates (canonical /sync)
│   └── ui/                # screens + ViewModels (today's beat, outlet visit, take order,
│                          #   check-in/out, my targets, beat scorecard)
├── androidMain/ iosMain/ desktopMain/   # TradeModule.{platform}.kt (@SingleIn(WorkspaceScope::class));
#                                          geo/permissions actuals (Play Services / Moko)
# wiring: settings.gradle.kts (:feature:trade); SyncEntity enum additions;
#         shared/ Routes + entry provider; ModuleRegistry ("dms-sfa" → Route.Trade);
#         data/common ApiUrlBuilder.tradeUrl(...)
# brand DMS dashboards (secondary-sales/stock/targets) are online pull-only views over snapshots
```

**Structure Decision**: Mobile + API. Backend `trade/` mirrors existing bounded contexts; the new wrinkle
is `CrossTenantReadGuard` + versioned snapshot tables. The mobile `feature/trade/` SFA module mirrors
`feature/order`'s offline-first shape (SyncDelegate-owned API, workspace-scoped DB) and adds geo/
attendance platform actuals. The brand DMS view is online (pull-only snapshot reads). Web (Angular) brand
dashboard is a P3 follow-up.

## Phased Delivery

### Phase 1 (MVP) — Distributor SFA app + network/link plumbing
- **Entities**: `TradeNetwork`, `TradeLink` (consented edge), `NetworkRetailer`, `Beat`, `BeatOutlet`,
  `JourneyPlan`/PJP, `PlannedVisit`, `Visit`, `Attendance`, `FieldOrder`.
- **Services**: `TradeLinkService` (invite/accept/consent scope), `BeatService`, `VisitService`,
  `AttendanceService`; `CrossTenantReadGuard` scaffold.
- **Endpoints**: SFA canonical `/sync` (`/trade/v1/{visits|field-orders|attendance|beats}/sync`,
  `GET`+`POST`); `POST /trade/v1/links` (invite), `POST /trade/v1/links/{uid}/accept`.
- **Workspace**: add `FIELD_REP` role (additive) + beat scoping.
- **Events**: none cross-tenant yet; counter orders flow into the distributor's `order` module.
- **Mobile**: offline rep app — today's beat (PJP), outlet visit (geo/time/outcome), take order at counter,
  check-in/out attendance, my targets; all author-offline via `/sync`.

### Phase 2 — Brand DMS visibility (secondary sales + stock + targets)
- **Entities**: `SecondarySalesSnapshot`, `DistributorStockSnapshot` (versioned, recomputable),
  `SalesTarget`.
- **Services**: `SnapshotService` (event-driven recompute from distributor order/invoice/inventory),
  `TargetService`; activate `CrossTenantReadGuard` (consent check before any `nativeQuery` rollup).
- **Endpoints**: `GET /trade/v1/snapshots/secondary-sales`, `.../distributor-stock`,
  `GET /trade/v1/targets` — all `TradeLink`-scoped to the calling brand; primary-order placement
  brand→distributor reuses the `order` module addressed across the link.
- **Events**: distributor `OrderFinalizedEvent`/`InvoiceFinalizedEvent` → tag `SECONDARY` → enqueue
  snapshot rebuild; inventory change → stock-snapshot rebuild.
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
| New `FIELD_REP` role + beat scoping in `workspace` | SFA reps must be limited to assigned beats within the distributor tenant | A global cross-tenant role leaks distributor data and explodes membership; no scoping means any member sees all outlets |
| Snapshot publication pipeline (event-driven rebuild) | Decouples the brand's read path from the distributor's live writes and keeps the consent/isolation boundary intact | Direct live cross-tenant reads or nightly ETL are either isolation-breaking or stale and still need the consent edge |
