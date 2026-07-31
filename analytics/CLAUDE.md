# analytics — module guide

Analytics & forecasting for the Ampairs backend (spec `022`). Computes per-workspace KPI rollups and
demand forecasts; serves the dashboard reads, a CSV export, and a pull-only forecast `/sync` feed.

## Bounded-context rules (this module)
- **Cross-module data comes through public services + DTOs only** (Principle IX) — never another
  module's JPA entity or repository. Analytics consumes:
  - `invoice.InvoiceAnalyticsQueryService` → `FinalizedInvoiceProjection` (sales/GST/line items)
  - `payment.PaymentAnalyticsQueryService` → collections + aging projections
  - `inventory.InventoryAnalyticsQueryService` → stock value / low-stock / turns inputs
  - `business.BusinessService` (via `BusinessTimeZoneProvider`) → the workspace `ZoneId` + currency
- **Business-timezone bucketing.** A day/week/month bucket is resolved in the workspace zone
  (`BusinessTimeZoneProvider.currentZone()`), never the server default — a `2026-06-30T18:30:00Z`
  invoice belongs to `2026-07-01` in `Asia/Kolkata`. Storage stays UTC `Instant`.
- Workspace scope is set by the controller (`TenantContextHolder`); services never set it.

## Components
| Area | Type | Notes |
|---|---|---|
| Daily aggregates | `KpiDailySummary` (`kpi_daily_summary`) | one row per (metric group, business day, dims); the durable rollup |
| Rollup / recompute | `KpiRollupService` | reconciles a date range idempotently; business-tz bucketing |
| Dashboard reads | `DashboardReadService` | `kpis` / `trend` / `aging` / `gstSummary` / `top` |
| Export | `AnalyticsExportService` | flat `section,metric,value,currency` CSV |
| Forecast maths | `domain/forecast/DemandForecasting` | additive Holt-Winters + MA fallback, pure/testable |
| Forecast run | `ForecastService` | builds per-product daily series → fits → upserts `DemandForecast` |
| Forecast read | `DemandForecastReadService` | incremental `/forecasts/sync` feed (includes retired rows) |
| Demand signal | `DemandSignalService` + `DemandForecastUpdatedEvent` (in `event`) | per-day mean/variability for replenishment |
| Nightly batch | `AnalyticsNightlyBatch` | cross-tenant recompute + forecast per workspace |
| Settings | `config/AnalyticsSettingDefinitions` | `analytics/dashboard_layout` (CSV of tile keys) |

## Endpoints (`/api/analytics/v1`)
- `GET /dashboard/kpis|trend|aging|gst-summary|top` → `ApiResponse<…>`
- `GET /export?format=csv&from_date&to_date[&period]` → `text/csv` attachment (NOT `ApiResponse`-wrapped)
- `POST /recompute` → reconcile a KPI date range
- `GET /forecasts/sync?last_sync&page&size&sort_by&sort_dir` → `ApiResponse<PageResponse<DemandForecastResponse>>`
- `POST /forecasts/recompute` → refit forecasts for the current workspace

## Migrations
`V1.0.105__create_analytics_tables.sql` (both `mysql/` + `postgresql/`) — `kpi_daily_summary` and
`demand_forecast`. Flyway versions are global; pick the next unused number for any change.

## Tests
Mock-based unit + MockMvc controller tests (no Docker in the dev sandbox): `KpiRollupServiceTest`,
`DashboardReadServiceTest` (incl. the GST intra/inter split), `DemandForecastingTest`,
`DemandSignalServiceTest`, `ForecastServiceTest`, `AnalyticsExportServiceTest`,
`AnalyticsExportControllerTest`, `AnalyticsDashboardControllerTest`, `DemandForecastSyncControllerTest`.
Run: `./gradlew :analytics:test`. Full Testcontainers coverage (idempotence, source fidelity) is a
follow-up gated on Docker.
