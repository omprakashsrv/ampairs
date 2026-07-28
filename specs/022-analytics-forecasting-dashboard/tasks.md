---
description: "Task list for Analytics & Forecasting Dashboard (022)"
---

# Tasks: Analytics & Forecasting Dashboard

**Input**: Design documents from `/specs/022-analytics-forecasting-dashboard/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: INCLUDED — the plan's Testing section and the constitution's Testing & Quality Gates require
backend ≥80% coverage on bucketing/recompute/forecast (JUnit + Testcontainers) and mobile
`:feature:analytics:check` + 3-target compile gates.

**Two repos**:
- **Backend** tasks → `ampairs/` (this repo), module `analytics/`.
- **Mobile** tasks → `ampairs-app/` (sibling repo), module `feature/analytics/` (+ small per-feature
  additions). Mobile paths below are relative to the `ampairs-app` repo root.

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: US1 (P1 dashboard), US2 (P2 forecast), US3 (P3 NL/config/export)
- Exact file paths included. `(BE)` = backend repo, `(MOB)` = mobile repo.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create both module skeletons and wire them into their builds.

- [X] T001 [P] (BE) Create `analytics` backend module skeleton: `analytics/build.gradle.kts`
  (depends on `core`, `business` public API; Spring Data JPA, Flyway), package tree
  `analytics/src/main/kotlin/com/ampairs/analytics/{domain/model,domain/enums,domain/catalog,domain/dto,repository,service,controller,config,event,batch}` and `analytics/src/test/kotlin/...`.
- [X] T002 (BE) Wire backend module: add `include("analytics")` to `settings.gradle.kts`,
  `implementation(project(":analytics"))` to `ampairs_service/build.gradle.kts`, and add `"analytics"`
  to the `migrationModules` list in `ampairs_service/build.gradle.kts`. (depends on T001)
- [ ] T003 [P] (MOB) Create `feature/analytics` module skeleton: `feature/analytics/build.gradle.kts`
  (KMP: android/ios/desktop targets, Metro, Room KMP, Ktor, kotlinx.datetime, depends on `data/common`,
  `data/sync`, `feature/agent` query API; `compose.resources { packageOfResClass = "ampairsapp.feature.analytics.generated.resources" }`),
  package tree `feature/analytics/src/{commonMain,androidMain,iosMain,desktopMain}/kotlin/com/ampairs/analytics/{data/api,data/db,data/query,domain,di,sync,ui}` + `commonMain/composeResources/values/strings.xml`.
- [ ] T004 (MOB) Wire mobile module: add `:feature:analytics` to `settings.gradle.kts` and as a
  dependency of `shared/build.gradle.kts`. (depends on T003)

**Checkpoint**: Both modules compile empty (`./gradlew :analytics:compileKotlin` (BE);
`./gradlew :feature:analytics:compileKotlinMetadata` (MOB)).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared enums, catalog, business-timezone resolution, DI/DB scaffolding, and cross-cutting
wiring every story needs. **No user-story work starts until this phase is done.**

### Backend foundations
- [X] T005 [P] (BE) Enums in `analytics/domain/enums/`: `MetricGroup`, `Period`, `TaxKind`,
  `AgingBucket`, `ForecastMethod`, `Confidence`, `MetricUnit`, `Aggregation` (per data-model §4).
- [X] T006 [P] (BE) `MetricDefinition` catalog registry in `analytics/domain/catalog/MetricCatalog.kt`
  with the P1 metric ids (data-model §1.3).
- [X] T007a (BE) **Discovery**: grep the `business` module for an existing public service exposing the
  workspace timezone. FOUND: `BusinessService.getBusinessProfile(): Business` exposes `timezone: String`
  (IANA id; no dedicated ZoneId accessor). `BusinessTimeZoneProvider` wraps it. (prerequisite for T007)
- [X] T007 (BE) Business-timezone resolution: based on T007a, either reuse the existing public accessor
  or add a read-only interface method returning the workspace `ZoneId` in the `business` module; inject
  it into `analytics`. Cross-module access via public service interface only (Principle IX) — analytics
  never reads the `business` repository directly. (depends on T007a)
- [X] T008 (BE) Flyway migration `V1.0.105__create_analytics_tables.sql` in BOTH
  `analytics/src/main/resources/db/migration/mysql/` and `.../postgresql/`: tables `kpi_daily_summary`
  and `demand_forecast` with the columns, unique keys, and indexes from data-model §1.1/§1.2 (vendor
  `TIMESTAMP` vs `TIMESTAMPTZ`). NOTE: Flyway shares ONE global version timeline across all modules
  (highest existing = `V1.0.104`), so analytics uses `V1.0.105` — NOT a per-module `V1.0.0`.
- [ ] T009 (BE) Verify migration applies on Postgres: `./gradlew :ampairs_service:flywayMigrate` then
  `flywayInfo` shows `V1.0.105` applied. (depends on T008; requires a running DB — not run in sandbox)

### Mobile foundations
- [ ] T010 [P] (MOB) Domain models in `feature/analytics/.../domain/`: `MetricDefinition`, `KpiResult`,
  `Period`, `AgingBucket`, aging/turns math helpers, business-zone bucketing util (injected `TimeZone`,
  never `currentSystemDefault()` — cmp-practices §12 / R7).
- [X] T011 [P] (MOB) `ApiUrlBuilder.analyticsUrl(path)` → `/api/analytics/{path}` in
  `data/common/.../ApiUrlBuilder.kt`. DONE (ampairs-app branch). Verification: ampairs-app CI (KMP build
  is egress-blocked locally — `api.foojay.io` 403 for the JetBrains-vendor toolchain; confirmed).
- [X] T012 [P] (MOB) `SyncEntity.DEMAND_FORECAST` in `data/sync/.../SyncEntity.kt`. DONE (ampairs-app branch).
- [X] T013 (MOB) `DemandForecastEntity` + `DemandForecastDao` forecast mirror. DONE + CI-green (all 3
  targets). ADAPTED to main's DB consolidation: the entity/DAO live in `:data:database` (not the feature
  module) inside the single `AmpairsWorkspaceDatabase` (registered + `abstract fun demandForecastDao()`,
  version 2→3, `WORKSPACE_MIGRATION_2_3` wired into the 3 platform `DatabaseModule`s, DAO `@Provides` in
  `WorkspaceDatabaseDaoModule`). No per-feature `AnalyticsRoomDatabase` — that pattern is obsolete.
  (metro-di §5). (depends on T003)
- [ ] T014 (MOB) `Route.Analytics` in `shared/.../navigation/Routes.kt`, an `AnalyticsEntryProvider`,
  register in `CombinedEntryProvider`, and map `"analytics-dashboard" → Route.Analytics` in
  `feature/workspace/.../ModuleRegistry.kt`. (depends on T004)

**Checkpoint**: Schema live; both modules compile with foundations. 3-target compile gate green
(`androidApp:compileDebugKotlinAndroid`, `shared:compileKotlinIosSimulatorArm64`, `desktopApp:compileKotlin`).

---

## Phase 3: User Story 1 — Offline KPI Dashboard (Priority: P1) 🎯 MVP

**Goal**: Sales, collections & aging, top products/customers, GST summary, and inventory KPIs for a
selected period — computed offline on-device and served from the backend summary for deep history.

**Independent Test**: With the device offline after a sync, open the dashboard; every P1 tile renders
correct figures for Day/Week/Month bucketed in the business timezone; export works; and on-device values
match the backend `…/dashboard/kpis` to the last currency unit.

### Tests for User Story 1 ⚠️ (write first, ensure they fail)
- [~] T015 [P] [US1] (BE) Testcontainers test: business-timezone bucketing — a `2026-06-30T18:30:00Z`
  invoice lands on `2026-07-01` for `Asia/Kolkata` (`analytics/src/test/.../KpiRollupServiceTimezoneTest.kt`). (R7/SC-003)
  PARTIAL: covered now by a mock-based unit test (`KpiRollupServiceTest`, 3 tests passing — IST→Jul-1,
  UTC→Jun-30, bucket increment). The full Testcontainers DB test is deferred to CI (no Docker in sandbox).
- [ ] T016 [P] [US1] (BE) Testcontainers test: recompute idempotence — recomputing a day twice yields
  identical `kpi_daily_summary` rows (`.../KpiRollupReconcileTest.kt`). (SC-004)
- [ ] T016a [P] [US1] (BE) Testcontainers test: source-state fidelity (`.../KpiSourceFidelityTest.kt`) —
  (a) **backdated edit**: editing/backdating a finalized invoice re-rolls the affected business day's
  summary, not just today (FR-014); (b) **exclusions**: drafts and cancelled/voided documents do NOT
  contribute, and refunds/credit notes/partial payments reduce sales/collections/outstanding correctly
  (FR-013).
- [ ] T017 [P] [US1] (BE) Contract test for each dashboard read endpoint (kpis/trend/aging/gst-summary/top)
  asserting `ApiResponse` envelope + snake_case shape per contracts/dashboard-read.md
  (`.../AnalyticsControllerTest.kt`).
- [ ] T018 [P] [US1] (BE) GST split unit test: intra (CGST+SGST) vs inter (IGST) from `taxInfos` +
  `placeOfSupply` reconciles to invoice tax (`.../GstSummaryServiceTest.kt`). (R10/SC-006)
- [ ] T019 [P] [US1] (MOB) Aggregate-DAO unit tests for sales/aging/GST/top/inventory math over an
  in-memory Room DB with seeded rows (`feature/analytics/src/commonTest/.../KpiQueryTest.kt`). Include a
  **device-timezone-agreement** case: the same seeded rows bucketed with an injected business `TimeZone`
  produce identical day/month totals regardless of the simulated device zone (SC-003, two-device
  agreement) — assert no use of `currentSystemDefault()`. (Workspace isolation/FR-026 is enforced
  structurally on the backend via `OwnableBaseDomain`/`TenantContextHolder` in T027 and on mobile by the
  per-workspace `@SingleIn(WorkspaceScope::class)` DBs.)

### Backend implementation (US1)
- [X] T020 [P] [US1] (BE) `KpiDailySummary` entity in `analytics/domain/model/` extending
  `OwnableBaseDomain`, with `@NamedEntityGraph` if needed (data-model §1.1).
- [X] T021 [P] [US1] (BE) Read DTOs + `asResponse()` converters in `analytics/domain/dto/`:
  `KpiResponse`/`KpiValueResponse`, `TrendPointResponse`, `AgingResponse`/`AgingBucketResponse`,
  `GstSummaryResponse` (+ splits/rate rows), `TopEntryResponse`, `RecomputeResultResponse`
  (contracts/dashboard-read.md §DTOs).
- [X] T022 [US1] (BE) `KpiDailySummaryRepository` with the upsert + read/`GROUP BY` queries and the
  covering indexes referenced by data-model §1.1. (depends on T020)
- [X] T023 [US1] (BE) `KpiRollupService`: business-zone bucketing + idempotent `reconcile(from,to)` that
  rebuilds invoice-derived buckets from source via the invoice public read service. (depends on T022, T007)
  DONE (Option A): SALES (gross/net/tax/count), GST_SUMMARY (per rate × intra/inter), TOP_CUSTOMER.
  Delete-then-rebuild = idempotent (SC-004); excludes drafts/de-finalized by construction (FR-013/014).
  Source: new public `InvoiceAnalyticsQueryService.finalizedBetween(...)` (projection DTO, no entity leak).
- [X] T024 [US1] (BE) `@TransactionalEventListener(AFTER_COMMIT)` in `analytics/event/` on a finalized
  invoice → `KpiRollupService.reconcileDayOf(...)`. (depends on T023)
  DONE for invoice: `AnalyticsInvoiceEventListener` reconciles the affected business day (self-healing —
  handles redelivery/backdated edits). `InventoryStockUpdatedEvent`/`OrderConfirmedEvent` do NOT exist;
  order/payment/inventory reconcile sources are separate later increments.
- [X] T025 [US1] (BE) `@Scheduled` nightly reconcile job in `analytics/batch/AnalyticsNightlyBatch`
  calling `reconcileTrailing(N)`. (depends on T023)
  DONE: enumerates active workspaces (`WorkspaceRepository.findActiveWorkspaceUids`, native cross-tenant
  query) and runs reconcile + forecast per workspace under its `TenantContextHolder` (set/cleared in
  try/finally, entry-point pattern), one workspace's failure not aborting the rest. Cron
  `analytics.nightly.cron` (default 02:30). Also satisfies the T040 batch.
- [X] T026 [US1] (BE) `DashboardReadService` serving kpis/trend/gst-summary/top from the summary;
  GST split logic per R10. (depends on T022)
  DONE — ALL P1 KPI groups: SALES kpis (gross/net/tax/count/AOV), day/week/month trend, GST summary
  (intra CGST+SGST / inter IGST, by-rate), top customers AND top products (invoice line items →
  TOP_PRODUCT buckets with qty), all from the invoice reconcile. COLLECTIONS + AGING via a LIVE read of
  the payment module; INVENTORY (stock value / low-stock count / turns) via a LIVE read of the product
  module. The entire P1 dashboard read surface is backend-complete.
- [X] T027 [US1] (BE) `AnalyticsController` `GET /dashboard/{kpis,trend,aging,gst-summary,top}` +
  `POST /analytics/v1/recompute`. (depends on T026, T023, T021)
  DONE: kpis (incl. COLLECTIONS), trend, aging, gst-summary, top + recompute, `ApiResponse<T>`, tenant
  via `SessionUserFilter` (matches every other controller — no manual context).

### Mobile implementation (US1)
- [X] T028 [P] [US1] (MOB) Per-feature aggregate DAO queries (date-bounded `GROUP BY`) added to the
  consolidated `:data:database` `*AgentDao`s (main merged all workspace @Entity/@Dao into one DB, so the
  aggregates live on the existing read-only agent DAOs, not per-feature DBs): `InvoiceAgentDao` (net/tax/
  count + GST intra-vs-inter via `place_of_supply IS [NOT] seller_place_of_supply` + `gstByRateBetween` +
  `salesTrendDaily`), `InventoryAgentDao` (stock value/low-stock/turns via inventory_transactions STOCK_OUT),
  `PaymentAgentDao` (`sumOutstanding` from party_balance + `openInvoicesForAging` invoice⨝allocation).
  CI-green (slices 3a `8d56769` + 3b `2bb1808`). No new covering indexes — queries reuse existing PKs/indexes.
- [ ] T029 [US1] (MOB) `data/query/` cross-module read facade composing the per-feature DAO results in the
  ViewModel (no cross-DB join; second keyed lookup for names — R4). (depends on T028)
- [ ] T030 [US1] (MOB) `AnalyticsApi(+Impl)` in `data/api/` for deep-history reads via
  `ApiUrlBuilder.analyticsUrl(...)` (used only outside the device sync window). (depends on T011)
- [ ] T030a [US1] (MOB) Sync-window boundary handling (FR-011): determine the earliest locally-synced
  business date per source; when the requested period extends earlier, fetch the remainder via `T030`'s
  API when online and merge with local aggregates; when offline, render a **reduced-coverage badge**
  ("showing data from {date}") instead of silently undercounting. Surface coverage state in
  `DashboardUiState`. (depends on T030, T029)
- [ ] T031 [US1] (MOB) `DashboardViewModel` (`@ContributesIntoMap(WorkspaceScope::class)`+`@ViewModelKey`)
  exposing `StateFlow<DashboardUiState>`; period selector recomputes; freshness ("last synced") stamp;
  business-zone bucketing via injected `LocalAppLocale.timeZoneId`. (depends on T029, T010)
- [ ] T032 [US1] (MOB) `DashboardScreen` + KPI widgets (Compose, commonMain): money via
  `formatMoney(amount, LocalAppLocale.current)`, dates via `formatDate(...)`; all strings from
  `stringResource`; `collectAsStateWithLifecycle`. (depends on T031)
- [ ] T033 [US1] (MOB) On-device CSV/PDF `ExportSheet` from the same local aggregates (currency symbol
  passed as a String into the non-composable builder; PDF via existing print path). (depends on T032)

**Checkpoint**: P1 dashboard works fully offline (MVP). Run T015–T019; `./gradlew :analytics:test` (BE)
and `:feature:analytics:check` + 3-target compile (MOB) green.

---

## Phase 4: User Story 2 — Demand Forecast + Reorder Signal (Priority: P2)

**Goal**: Per-product expected demand (Holt-Winters with moving-average fallback) generated on the
backend, pulled read-only to mobile, with an offline EWMA fallback; demand signal published for
replenishment/inventory.

**Independent Test**: For a product with history, the forecast shows expected demand + trend; products
trending to stock-out surface as reorder candidates; offline with empty mirror a local EWMA estimate
still renders.

### Tests for User Story 2 ⚠️
- [X] T034 [P] [US2] (BE) Holt-Winters unit test on a synthetic seasonal series (`DemandForecastingTest`).
  DONE: held-out-week MAPE is ≤ 80% of the naïve "same as last period" baseline (SC-007 bar met);
  method/confidence thresholds asserted (HOLT_WINTERS/HIGH at ≥2 cycles, MOVING_AVG/MEDIUM at 1–2,
  MOVING_AVG/LOW under 1); non-negativity on a zero-heavy series. (R5)
- [ ] T035 [P] [US2] (BE) Contract test for `GET /analytics/v1/forecasts/sync` — `ApiResponse<PageResponse>`,
  `last_sync`/paging, includes retired rows (`.../DemandForecastSyncControllerTest.kt`).
- [ ] T036 [P] [US2] (MOB) `DemandForecastSyncDelegate` pull test (upsert + drop inactive + checkpoint
  advance) over in-memory Room (`feature/analytics/src/commonTest/.../ForecastSyncDelegateTest.kt`).

### Backend implementation (US2)
- [X] T037 [P] [US2] (BE) `DemandForecast` entity (data-model §1.2) + `DemandForecastResponse` DTO +
  `asResponse()`. DONE (with the foundation/migration in the first backend increment).
- [X] T038 [US2] (BE) `DemandForecastRepository` with the `/sync` feed query
  (`updatedAt >= last_sync ASC`) + indexes. DONE.
- [X] T039 [US2] (BE) `ForecastService` building the per-product daily sales series from finalized
  invoice line items (via `InvoiceAnalyticsQueryService`) and fitting `DemandForecasting` (additive
  Holt-Winters + MA fallback), persisting/​upserting `DemandForecast` rows. (depends on T022)
  Note: additive (not multiplicative) weekly seasonality — robust to zero-heavy retail series.
- [X] T040 [US2] (BE) Forecast batch writing `DemandForecast` rows. (depends on T039, T038)
  DONE: the cross-tenant nightly `AnalyticsNightlyBatch` (T025) calls `ForecastService.recompute` per
  workspace; a manual `POST /analytics/v1/forecasts/recompute` also exists for on-demand runs.
- [X] T041 [US2] (BE) `DemandSignalService` (avg daily demand + variability per product from the latest
  forecast) + publish `DemandForecastUpdatedEvent` after each forecast run. (R6/FR-018)
  DONE: `DemandForecastUpdatedEvent` lives in the `event` module (consumers depend on `event`, not
  analytics); `ForecastService` publishes it per workspace run; `DemandSignalService` exposes per-day
  mean/variability (horizon-total ÷ horizon, std ÷ √horizon). Analytics writes no inventory/replenishment
  tables — it only measures + signals. Consumer (feature 027) not yet built; this is the integration hook.
- [X] T042 [US2] (BE) `DemandForecastController` `GET /analytics/v1/forecasts/sync` →
  `ApiResponse<PageResponse<DemandForecastResponse>>`. (depends on T038)
  DONE: controller + `DemandForecastReadService` (incremental `last_sync` paging, ASC). Rows are served
  once the forecast batch (T037–T041) writes them.

### Mobile implementation (US2)
- [X] T043 [US2] (MOB) `DemandForecastSyncDelegate` (PULL-ONLY) `@ContributesIntoMap(WorkspaceScope::class)`
  + `@SyncEntityKey(SyncEntity.DEMAND_FORECAST)`; batched incremental `pull()` into the mirror, `push`=no-op
  (`SyncResult.Success(0)`). DONE + CI-green. Also added `AnalyticsApi`/`AnalyticsApiImpl`
  (`@ContributesBinding(AppScope)`, `ApiUrlBuilder.analyticsUrl("v1/forecasts/sync")`) +
  `DemandForecastResponse` DTO. Mirrors the pricing `GeoZoneSyncDelegate` pattern.
- [ ] T044 [US2] (MOB) On-device simple EWMA fallback in `domain/` for offline "expected demand" when no
  backend forecast is present. (depends on T010)
- [ ] T045 [US2] (MOB) Forecast sparkline widget + reorder-candidate indicator on the dashboard, reading
  the mirror with EWMA fallback. (depends on T043, T044, T032)

**Checkpoint**: US1 + US2 both work independently. Run T034–T036; suites green.

---

## Phase 5: User Story 3 — NL Q&A + Dashboard Config + Server Export (Priority: P3)

**Goal**: Natural-language answers via the agent SafeQuery path with deterministic KPI-tile mapping for
common questions; customizable dashboard layout persisted as a workspace setting; server-side export for
deep history.

**Independent Test**: Common questions return correct answers offline matching tiles; tiles can be
added/removed/reordered and sync across devices; an out-of-window export is produced server-side.

### Tests for User Story 3 ⚠️
- [ ] T046 [P] [US3] (MOB) `ModuleQueryExecutor`/`ModuleQuerySchema` tests for any newly-queryable module
  (column names match `@Entity`; SELECT-only validator passes) per feedback_agent_models Rule 7.
- [ ] T047 [P] [US3] (BE) Export endpoint test: `GET /analytics/v1/export?format=csv` streams localized
  rows for an out-of-window range (`.../AnalyticsExportControllerTest.kt`).

### Implementation (US3)
- [ ] T048 [P] [US3] (MOB) Add curated `ModuleQuerySchema` + `ModuleQueryExecutor` for any P1-source
  module not yet agent-queryable (mechanical 2-file pattern, feedback_agent_models Rule 7).
- [ ] T049 [US3] (MOB) `NlQueryPanel` + ViewModel: map common questions → one-tap KPI tile (deterministic);
  free-form falls through to the agent `SafeQueryService`; clear "couldn't answer that" on failure
  (FR-023). (depends on T032, T048)
- [ ] T050 [P] [US3] (MOB) `AnalyticsSettingDefinitions : SettingDefinitionProvider`
  (`module_code='analytics'`) for dashboard layout; persist add/remove/reorder via `StoreSetting` riding
  `SyncEntity.STORE`; dashboard reads layout. (depends on T031)
- [X] T051 [P] [US3] (BE) `GET /analytics/v1/export?format=csv&…` + `AnalyticsExportService` streaming a
  flat `section,metric,value,currency` CSV across all KPI groups (reuses `DashboardReadService`; not
  `ApiResponse`-wrapped; `Content-Disposition` attachment). (depends on T026)
  CSV only — PDF deferred (R11: CSV sufficient for P1, avoids a new dependency); `format != csv` → 400.

**Checkpoint**: All three stories independently functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T052 [P] (BE) Backend coverage ≥80% on bucketing/recompute/forecast; fill gaps; `./gradlew :analytics:test`.
- [ ] T053 [P] (MOB) `:feature:analytics:check` + 3-target compile gate; fix any KMP platform leaks.
- [ ] T054 [P] Run `quickstart.md` end-to-end (backend recompute + reads; mobile offline walkthrough;
  parity spot-check SC-004).
- [ ] T055 [P] Docs: add `analytics/CLAUDE.md` (BE) and a short `feature/analytics` note (MOB); update
  module ownership table if needed.
- [ ] T056 Performance pass: confirm each tile renders <1s and a period switch recomputes <1s
  (SC-001/SC-002) on the spec's SMB volume baseline (per Assumptions: ~thousands of invoices/orders and
  thousands of products/customers per workspace — seed a dataset at that scale); verify date-bounded
  queries use the covering indexes (no full-table scans) via query plans.

---

## Dependencies & Execution Order

### Phase dependencies
- **Setup (P1)** → no deps. **Foundational (P2)** depends on Setup and **blocks all stories**.
- **US1 (P3)** depends on Foundational. **US2 (P4)** depends on Foundational (+ reuses US1's
  `KpiDailySummaryRepository`/sales series for the forecast). **US3 (P5)** depends on Foundational (+ US1
  tiles for NL mapping and layout). **Polish (P6)** after the desired stories.

### Within each story
- Tests written first (and failing) → entities/DTOs → repository → service → controller/UI → integration.
- Models before services; services before endpoints; backend and mobile sub-tracks run in parallel.

### Parallel opportunities
- Setup: T001/T003 in parallel (different repos). Foundational: T005, T006, T010, T011, T012 are [P].
- US1: all test tasks (T015–T019 + T016a) [P]; T020/T021 [P]; backend (T020–T027) and mobile
  (T028–T033, T030a) tracks run concurrently. US2 backend (T037–T042) ∥ mobile (T043–T045). US3
  T048/T050/T051 [P].
- With two developers: one drives backend, one drives mobile, per story.

---

## Implementation Strategy

**MVP = US1 only**: Setup → Foundational → US1 → STOP & validate offline dashboard + device/server parity.
Deploy/demo. Then add US2 (forecast), then US3 (NL/config/export) — each an independently testable,
deployable increment.

---

## Summary

- **Total tasks**: 59 (T001–T056 + T007a, T016a, T030a).
- **Per story**: Setup 4 · Foundational 11 (incl. T007a) · US1 21 (T015–T033 + T016a, T030a) · US2 12 (T034–T045) · US3 6 (T046–T051) · Polish 5.
- **Tests included** (plan + constitution require them): backend Testcontainers (bucketing, recompute
  idempotence, GST split, Holt-Winters, contracts) + mobile DAO/sync tests and `check`/compile gates.
- **MVP scope**: User Story 1 (offline KPI dashboard) — fully usable on its own.

## Notes
- `[P]` = different files/repos, no dependency. `[Story]` maps to US1/US2/US3 for traceability.
- Backend = `ampairs/`, Mobile = `ampairs-app/` (sibling repo) — most within-story tracks are parallel.
- Commit after each task or logical group; stop at any checkpoint to validate the story independently.
- Follow project rules: `Instant`/`TIMESTAMPTZ`, `ApiResponse`/`PageResponse`, snake_case, DTO isolation,
  `OwnableBaseDomain` tenancy, event-only module boundaries (BE); Metro `WorkspaceScope`, offline-first
  repos, `formatMoney`/`formatDate`, KMP-safe commonMain (MOB).
