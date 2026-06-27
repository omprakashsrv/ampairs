# Implementation Plan: Analytics & Forecasting Dashboard

**Branch**: `claude/analytics-forecasting-dashboard-0cqkrv` (spec dir `022-analytics-forecasting-dashboard`) | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/022-analytics-forecasting-dashboard/spec.md`

## Summary

Give a retail/wholesale owner a **business-intelligence dashboard** — sales, collections & aging, top
products/customers, GST summary, inventory turns/low-stock — plus a simple **AI demand forecast** and
**reorder signal**, all working **offline on mobile** and answerable in natural language through the
existing on-device agent.

Technical approach: a new backend bounded context (`analytics`) that builds a **period-grained
materialized read model** (`KpiDailySummary`) incrementally from the domain events the platform already
emits (`InvoiceFinalizedEvent`, `OrderEvents`, `InventoryStockUpdatedEvent`, …), serves dashboard reads,
and runs a **Holt-Winters exponential-smoothing** demand forecast as a batch job. A new Compose
Multiplatform feature module (`feature/analytics`) computes the same KPIs **on-the-fly and offline** from
the already-synced Room DBs (`invoice`, `order`, `payment`, `inventory`), bucketing every period in the
**workspace business timezone** (via `BusinessLocaleProvider`/`LocalAppLocale`), and surfaces NL Q&A
through the agent's `SafeQueryService` SafeQuery path. The forecast is pulled read-only to mobile and a
demand signal is published (Spring `ApplicationEvent` + public service) for replenishment (feature 027)
and inventory (spec 014). Money is `BigDecimal`/`DECIMAL(19,4)` backend, `Long` minor units on mobile.
Full rationale in [research.md](./research.md); entities in [data-model.md](./data-model.md); APIs in
[contracts/](./contracts/).

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Java 21 (Spring Boot 4.0); Mobile Kotlin Multiplatform 2.4
(Compose Multiplatform 1.11).
**Primary Dependencies**: Backend — Spring Data JPA, Flyway, Jackson (SNAKE_CASE), `core`
(`OwnableBaseDomain`, `ApiResponse`, `PageResponse`, `TenantContextHolder`), Spring scheduling
(`@Scheduled`) + `@TransactionalEventListener`; consumes domain events from `invoice`/`order`/`payment`/
`inventory`/`customer`; reads business timezone via the `business` module public service. Mobile — Room
KMP (read-only aggregate DAOs over existing feature DBs), Ktor (read API + forecast pull), Metro DI,
Navigation3, kotlinx.datetime, `data/common` (`ApiUrlBuilder`, `BusinessLocaleProvider`, `LocalAppLocale`,
`formatMoney`/`formatDate`), `feature/agent` SafeQuery path (`ModuleQuerySchema`/`ModuleQueryExecutor`),
existing `data/sync` for the pull-only `DemandForecast` mirror.
**Storage**: Backend — PostgreSQL/MySQL via Flyway; `KpiDailySummary`, `DemandForecast` tables; money
`DECIMAL(19,4)`, timestamps `TIMESTAMPTZ`/`TIMESTAMP`, bucket key `business_date DATE`. Mobile — no new
write tables; on-the-fly aggregate queries over existing DBs + a small read-only Room mirror for
`DemandForecast` (money `Long` minor units).
**Testing**: Backend — JUnit/Testcontainers (`./gradlew :analytics:test`), incl. business-timezone
bucketing and summary recompute idempotence; forecast unit tests on synthetic seasonal series. Mobile —
`./gradlew :feature:analytics:check`; 3-target compile gates.
**Target Platform**: Backend service (Linux); Mobile Android (minSdk 24) / iOS / Desktop (JVM).
**Project Type**: Mobile + API — new backend module + new KMP feature module. Web (Angular) dashboard is
a tracked follow-up.
**Performance Goals**: Dashboard tiles render perceived-instant from local data (<200 ms per tile on SMB
volumes); backend trend/forecast read O(days) over the summary; nightly reconcile + forecast batch
completes within the maintenance window per workspace.
**Constraints**: Offline-first — every KPI computable on-device with no network; period bucketing in the
**business timezone** (never device/UTC); heavy ML stays backend; NL Q&A reuses the agent SafeQuery
guardrails (SELECT-only, allow-list, LIMIT, reader connection); workspace data isolation.
**Scale/Scope**: Per workspace — thousands of invoices/orders, thousands of products/customers. P1 ≈ 2
backend entities (`KpiDailySummary`, `DemandForecast`), ~1 read controller + forecast batch, ~1 mobile
feature module with a dashboard + NL panel and ~5 KPI groups.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How this plan complies |
|---|---|---|
| I. Type Safety (Instant/TIMESTAMPTZ) | ✅ PASS | All timestamps `Instant` → `TIMESTAMPTZ`/`TIMESTAMP`; bucket key a `LocalDate business_date` derived in the business zone (not a `LocalDateTime`). Money `BigDecimal`/`DECIMAL(19,4)`. |
| II. DTO & Contract Isolation | ✅ PASS | Read DTOs (`KpiResponse`, `TrendPointResponse`, `DemandForecastResponse`) in `analytics/domain/dto/`; entities never exposed; `entity.asResponse()` converters. |
| III. Global JSON SNAKE_CASE | ✅ PASS | Global Jackson strategy; snake_case params (`from_date`, `to_date`, `period`, `metric_group`); no `@JsonProperty` for standard fields. |
| IV. Multi-Tenant Isolation | ✅ PASS | `KpiDailySummary`/`DemandForecast` extend `OwnableBaseDomain` (`@TenantId ownerId`); tenant set by `SessionUserFilter`/controller; the nightly batch iterates workspaces explicitly (cross-tenant reads use `nativeQuery = true`). |
| V. API Response Standardization | ✅ PASS | All endpoints return `ApiResponse<T>`; the forecast pull uses the canonical `GET /sync` → `ApiResponse<PageResponse<T>>`. |
| VI. Centralized Exception Handling | ✅ PASS | No business try/catch in controllers; typed exceptions bubble to the global handler. |
| VII. Efficient Data Loading | ✅ PASS | Read model is pre-aggregated; covering indexes on `(owner_id, business_date, metric_group)` and forecast `(owner_id, product_id, period_start)`; derived queries preferred. |
| VIII. Angular Material 3 Exclusivity | ✅ N/A (this phase) | Web dashboard deferred; when added it will use Angular Material 3 only. |
| IX. Domain-Driven Module Boundaries | ✅ PASS | New `analytics` bounded context; consumes other modules **only** via published domain events + public service interfaces; publishes `DemandForecastUpdatedEvent` for 027/inventory — never writes their tables. |
| X. Compose Multiplatform Parity | ✅ PASS | Shared logic/UI in `feature/analytics/src/commonMain`; thin platform DI. Web parity tracked as follow-up. |
| XI. Security & Secrets Hygiene | ✅ PASS | No secrets; standard JWT/workspace auth; on-device NL uses the sandboxed local SafeQuery path (no data leaves the device). |
| Flyway | ✅ PASS | Versioned migration in **both** `mysql/` and `postgresql/`; `analytics` added to `migrationModules`; next version via `flywayInfo`. |
| Testing & Quality Gates | ✅ PASS | Backend ≥80% on bucketing/recompute/forecast; mobile `check` + 3-target compile gates. |

**Result**: PASS — no violations; Complexity Tracking not required. Web deferral is a documented scope
decision, not a principle violation.

## Project Structure

### Documentation (this feature)

```
specs/022-analytics-forecasting-dashboard/
├── plan.md              # This file (/speckit.plan output)
├── spec.md              # Feature specification (/speckit.specify output)
├── research.md          # Phase 0 output — design decisions + rationale
├── data-model.md        # Phase 1 output — KpiDailySummary, DemandForecast, MetricDefinition catalog
├── quickstart.md        # Phase 1 output — how to exercise the dashboard + forecast
├── contracts/           # Phase 1 output — API contracts
│   ├── README.md
│   ├── dashboard-read.md        # KPI / trend / aging / GST / top-N read endpoints
│   └── forecast-sync.md         # pull-only DemandForecast /sync feed
├── checklists/
│   └── requirements.md  # spec quality checklist
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```
# Backend — ampairs/ (this repo)
analytics/
└── src/main/
    ├── kotlin/com/ampairs/analytics/
    │   ├── domain/
    │   │   ├── model/          # KpiDailySummary, DemandForecast
    │   │   ├── enums/          # MetricGroup (SALES, COLLECTIONS, AGING, TOP_PRODUCT, TOP_CUSTOMER, GST_SUMMARY, INVENTORY), Period (DAY/WEEK/MONTH)
    │   │   ├── catalog/        # MetricDefinition registry (id, group, unit, aggregation, source)
    │   │   └── dto/            # KpiResponse, TrendPointResponse, AgingResponse, GstSummaryResponse, DemandForecastResponse + converters
    │   ├── repository/         # KpiDailySummaryRepository, DemandForecastRepository (+ @EntityGraph, /sync feed query)
    │   ├── service/            # KpiRollupService (incremental upsert + reconcile), DashboardReadService, ForecastService (Holt-Winters), DemandSignalService
    │   ├── controller/         # AnalyticsController (dashboard reads), DemandForecastController (/sync pull)
    │   ├── config/             # AnalyticsSettingDefinitions (dashboard layout settings), Constants
    │   ├── event/              # @TransactionalEventListener(AFTER_COMMIT) on Invoice/Order/Inventory/Payment events; publishes DemandForecastUpdatedEvent
    │   └── batch/              # @Scheduled nightly reconcile + forecast job
    └── resources/db/migration/
        ├── mysql/V1.0.x__create_analytics_tables.sql
        └── postgresql/V1.0.x__create_analytics_tables.sql
# wiring: settings.gradle.kts (include "analytics"); ampairs_service/build.gradle.kts
#         (implementation(project(":analytics")) + "analytics" in migrationModules)
# business module: expose a public service to resolve the workspace business TimeZone (read-only)

# Mobile — ampairs-app/ (sibling repo)
feature/analytics/src/
├── commonMain/kotlin/com/ampairs/analytics/
│   ├── data/api/          # AnalyticsApi(+Impl), ApiUrlBuilder.analyticsUrl (deep-history reads + forecast)
│   ├── data/db/           # DemandForecastEntity + DAO + AnalyticsRoomDatabase (forecast mirror ONLY)
│   ├── data/query/        # cross-module read facade calling per-feature aggregate DAOs
│   ├── domain/            # MetricDefinition, KpiResult, Period, aging/turns math, simple EWMA fallback
│   ├── di/                # AnalyticsModule.kt
│   ├── sync/              # DemandForecastSyncDelegate (PULL-ONLY)
│   └── ui/                # DashboardScreen + widgets, NlQueryPanel, ExportSheet + ViewModels
├── androidMain/ iosMain/ desktopMain/   # AnalyticsModule.{platform}.kt (@SingleIn(WorkspaceScope::class))
# per-feature additions (small, in each existing module):
#   feature/invoice|order|payment|inventory: add analytics aggregate DAO queries (date-bounded GROUP BY)
#   add ModuleQuerySchema/ModuleQueryExecutor where a module is not yet agent-queryable (see R8)
# wiring: settings.gradle.kts (:feature:analytics); SyncEntity.DEMAND_FORECAST;
#         shared/ Routes.Analytics + entry provider; ModuleRegistry ("analytics-dashboard" → Route.Analytics);
#         data/common ApiUrlBuilder.analyticsUrl(...)
```

**Structure Decision**: Mobile + API. The backend `analytics/` module is a new bounded context that only
*reads* other modules via events/public services and *publishes* a demand signal — it owns no source
data. The mobile `feature/analytics/` module computes KPIs on-the-fly from the existing workspace-scoped
Room DBs and pulls only the forecast, mirroring how `feature/agent` already reads those DBs read-only.

## Phased Breakdown

### P1 — MVP: offline KPI dashboard (sales, collections/aging, top-N, GST, inventory)

- **Backend entities**: `KpiDailySummary` (`owner_id`, `business_date`, `metric_group`, dimension keys
  `dim_product_id?`/`dim_customer_id?`/`tax_rate?`, measures `gross_minor`/`net_minor`/`tax_minor`/
  `count`/`qty(15,3)` as `DECIMAL(19,4)`), unique `(owner_id, business_date, metric_group, dim_*)`.
- **Backend services**: `KpiRollupService` — `@TransactionalEventListener(AFTER_COMMIT)` on
  `InvoiceFinalizedEvent`/`InvoicePaidEvent`/`OrderEvents`/`InventoryStockUpdatedEvent` upserts the
  affected day's buckets (business-zone date, R7); `@Scheduled` nightly reconcile recomputes trailing N
  days from source tables (recompute endpoint `POST /analytics/v1/recompute`). `DashboardReadService`
  serves KPI/trend/aging/GST/top-N from the summary.
- **Endpoints**: `GET /analytics/v1/dashboard/kpis?from_date&to_date&period&metric_group`,
  `…/trend`, `…/aging`, `…/gst-summary`, `…/top?dimension=product|customer&limit`,
  `POST /analytics/v1/recompute` — all `ApiResponse<…>`.
- **Mobile/offline**: `feature/analytics` dashboard computes every P1 KPI **on-device** via per-feature
  aggregate DAOs (no network): Sales from `invoice`/`order` DB; Collections & aging from `payment` DB
  (`LedgerEntryEntity`/`PartyBalanceEntity`/open bills); GST split from `InvoiceEntity.total_tax` + line
  tax JSON + `placeOfSupply`; Inventory value/low-stock/turns from `InventoryItemEntity` + movement DAO.
  All bucketing via `LocalAppLocale.timeZoneId`. Backend read API used only for periods outside the
  device sync window. CSV/PDF export on-device (`formatMoney`, business zone).

### P2 — Demand forecasting + reorder signal

- **Backend entity**: `DemandForecast` (`owner_id`, `product_id`, `period_start: LocalDate`, `horizon`,
  `mean_qty(15,3)`, `std_dev_qty(15,3)`, `method`=HOLT_WINTERS|MOVING_AVG, `generated_at: Instant`).
- **Backend service**: `ForecastService` runs the **Holt-Winters** fit (level+trend+seasonality) over the
  daily sales series from finalized invoices/confirmed orders in the nightly batch; falls back to moving
  average with sparse history. `DemandSignalService` derives avg daily demand + variability and publishes
  `DemandForecastUpdatedEvent` (consumed by feature 027 for reorder point/safety stock; inventory owns
  `reorderLevel`).
- **Endpoint**: canonical pull-only `GET /analytics/v1/forecasts/sync` (`last_sync,page,size,…`) →
  `ApiResponse<PageResponse<DemandForecastResponse>>`.
- **Mobile/offline**: `DemandForecastSyncDelegate` (PULL-ONLY) mirrors forecasts into `feature/analytics`
  Room read-only; dashboard shows expected-demand sparklines; an on-device **simple EWMA** fallback
  renders an offline estimate when no backend forecast is present.

### P3 — Natural-language Q&A + dashboard configuration + server export

- **NL Q&A**: wire the dashboard's NL panel to the agent `SafeQueryService` path (R8); add curated
  `ModuleQuerySchema`/`ModuleQueryExecutor` for any not-yet-queryable module; map common questions to
  one-tap KPI tiles (deterministic, model-independent), free-form falls through to SafeQuery.
- **Dashboard config**: declarative `MetricDefinition` catalog + `DashboardWidget` layout persisted as
  `StoreSetting` (`module_code='analytics'`) via `AnalyticsSettingDefinitions`, riding `SyncEntity.STORE`.
- **Server export**: `GET /analytics/v1/export?format=csv|pdf&…` for historical depth beyond the device
  sync window.

## Complexity Tracking

*No constitution violations — section intentionally empty.*
</content>
