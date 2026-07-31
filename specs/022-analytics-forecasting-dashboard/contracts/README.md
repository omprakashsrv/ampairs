# API Contracts — Analytics & Forecasting Dashboard

All endpoints live in the new backend `analytics` bounded context under `/analytics/v1/...`, return the
standard `ApiResponse<T>` envelope (Principle V), use **global SNAKE_CASE** JSON (no `@JsonProperty`),
emit timestamps as ISO-8601 UTC `Instant`, and money as `DECIMAL(19,4)` decimal numbers. Every request
is workspace-scoped: the client MUST send `X-Workspace-ID`; `SessionUserFilter` derives the tenant and
rejects requests that omit it (Principle IV).

## Contract files

| File | Scope |
|---|---|
| [dashboard-read.md](./dashboard-read.md) | Read endpoints: KPIs, trend, aging, GST summary, top-N, and the recompute trigger. Backed by `KpiDailySummary` (server) and mirrored on-device by on-the-fly Room aggregates. |
| [forecast-sync.md](./forecast-sync.md) | Pull-only `DemandForecast` `/sync` feed (canonical offline-sync contract) consumed by `DemandForecastSyncDelegate`. |

## Conventions used in both files

- **Wrapper**: success → `{ "success": true, "data": <T>, "error": null, "timestamp", "path", "trace_id" }`;
  errors bubble to the global handler → `success:false` + `error` (Principle VI). Paginated payloads use
  `PageResponse<T>` (`content`, `page_number`, `page_size`, `total_elements`, `total_pages`, `first`,
  `last`, `has_next`, `has_previous`, `empty`).
- **Query params**: snake_case — `from_date`, `to_date`, `period`, `metric_group`, `dimension`, `limit`,
  `last_sync`, `page`, `size`, `sort_by`, `sort_dir`.
- **Dates**: `from_date` / `to_date` are inclusive `LocalDate` (`YYYY-MM-DD`) in the **workspace business
  timezone** (R7). `period` ∈ `DAY|WEEK|MONTH` controls bucketing.
- **Auth**: JWT bearer + `X-Workspace-ID`. Read endpoints require workspace membership; `recompute`
  requires an admin/owner role.
- **Offline parity (Principle X / R3)**: every read endpoint has an exact on-device equivalent computed
  from synced Room data; the API is used only for history beyond the device's sync window. The same
  `MetricDefinition` drives both so values agree to the last currency unit (FR-027 / SC-004).

## Endpoint summary

| Method | Path | Returns |
|---|---|---|
| GET | `/analytics/v1/dashboard/kpis` | `ApiResponse<KpiResponse>` |
| GET | `/analytics/v1/dashboard/trend` | `ApiResponse<List<TrendPointResponse>>` |
| GET | `/analytics/v1/dashboard/aging` | `ApiResponse<AgingResponse>` |
| GET | `/analytics/v1/dashboard/gst-summary` | `ApiResponse<GstSummaryResponse>` |
| GET | `/analytics/v1/dashboard/top` | `ApiResponse<List<TopEntryResponse>>` |
| POST | `/analytics/v1/recompute` | `ApiResponse<RecomputeResultResponse>` |
| GET | `/analytics/v1/forecasts/sync` | `ApiResponse<PageResponse<DemandForecastResponse>>` |
| GET | `/analytics/v1/export` | CSV/PDF stream (P3) |

> POST `/analytics/v1/forecasts/sync` is intentionally **absent** — forecasts are server-generated and
> pull-only (no client upserts), the documented exception to the bidirectional `/sync` contract.
