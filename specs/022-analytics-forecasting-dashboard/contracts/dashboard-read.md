# Contract — Dashboard Read API

Read-only KPI endpoints served by `DashboardReadService` from `KpiDailySummary`, plus the recompute
trigger. All return `ApiResponse<T>`, snake_case JSON, business-timezone bucketing (R7). Mirrored exactly
on-device by Room aggregates (R3/R4); the API is the deep-history path only.

Common query params (unless noted): `from_date` (incl., `YYYY-MM-DD`), `to_date` (incl.),
`period` ∈ `DAY|WEEK|MONTH` (default `MONTH`). All dates are workspace-business-zone.

---

## 1. GET `/analytics/v1/dashboard/kpis`

Headline metrics for one metric group over a period. → FR-001, FR-003, FR-007.

**Query**: `from_date`, `to_date`, `period`, `metric_group` ∈
`SALES|COLLECTIONS|GST_SUMMARY|INVENTORY` (default `SALES`).

**200** `ApiResponse<KpiResponse>`:
```json
{
  "success": true,
  "data": {
    "metric_group": "SALES",
    "period": "MONTH",
    "from_date": "2026-06-01",
    "to_date": "2026-06-30",
    "currency_code": "INR",
    "values": [
      { "metric_id": "sales.gross",  "unit": "MONEY", "value": "920710.5000" },
      { "metric_id": "sales.net",    "unit": "MONEY", "value": "812345.0000" },
      { "metric_id": "sales.tax",    "unit": "MONEY", "value": "108365.5000" },
      { "metric_id": "sales.count",  "unit": "COUNT", "value": "342" },
      { "metric_id": "sales.aov",    "unit": "MONEY", "value": "2692.13" }
    ],
    "computed_from": "2026-06-27T03:15:00Z"
  },
  "error": null
}
```
`values[]` content depends on `metric_group` (INVENTORY → `inventory.stock_value`,
`inventory.low_stock_count`, `inventory.turns`; COLLECTIONS → `collections.collected`,
`collections.outstanding`). `computed_from` is the summary `recomputed_at` (freshness, FR-010).

**Errors**: `400` invalid date range / unknown `metric_group`; `404` no workspace context.

---

## 2. GET `/analytics/v1/dashboard/trend`

Time series of one metric for charting. → FR-002.

**Query**: `from_date`, `to_date`, `period`, `metric_id` (e.g. `sales.gross`).

**200** `ApiResponse<List<TrendPointResponse>>`:
```json
{ "success": true, "data": [
  { "bucket_start": "2026-06-01", "bucket_label": "Jun W1", "value": "210400.0000" },
  { "bucket_start": "2026-06-08", "bucket_label": "Jun W2", "value": "188900.0000" }
], "error": null }
```
Buckets are contiguous (gaps filled with `0`) so the chart has no holes. `bucket_start` is the business-
zone period start.

---

## 3. GET `/analytics/v1/dashboard/aging`

Receivables aging snapshot as of business "today". → FR-004.

**Query**: `as_of_date` (optional, default today). No `period`.

**200** `ApiResponse<AgingResponse>`:
```json
{ "success": true, "data": {
  "as_of_date": "2026-06-27",
  "currency_code": "INR",
  "total_outstanding": "455000.0000",
  "buckets": [
    { "bucket": "CURRENT",  "amount": "120000.0000", "doc_count": 18 },
    { "bucket": "D1_30",    "amount": "180000.0000", "doc_count": 22 },
    { "bucket": "D31_60",   "amount": "95000.0000",  "doc_count": 9 },
    { "bucket": "D61_90",   "amount": "40000.0000",  "doc_count": 4 },
    { "bucket": "D90_PLUS", "amount": "20000.0000",  "doc_count": 2 }
  ]
}, "error": null }
```
Reflects partial payments and credit notes (FR-013): `amount` is the open balance, not invoice face value.

---

## 4. GET `/analytics/v1/dashboard/gst-summary`

Output tax split for filing prep, grouped by rate and intra/inter-state. → FR-006 / R10 / SC-006.

**Query**: `from_date`, `to_date` (a GST month is the typical range), `period` (default `MONTH`).

**200** `ApiResponse<GstSummaryResponse>`:
```json
{ "success": true, "data": {
  "from_date": "2026-06-01", "to_date": "2026-06-30", "currency_code": "INR",
  "taxable_value": "812345.0000",
  "total_tax": "108365.5000",
  "intra_state": { "cgst": "29180.0000", "sgst": "29180.0000" },
  "inter_state": { "igst": "50005.5000" },
  "by_rate": [
    { "tax_rate": "5.0000",  "taxable_value": "120000.0000", "tax_amount": "6000.0000",  "kind": "INTRA" },
    { "tax_rate": "18.0000", "taxable_value": "400000.0000", "tax_amount": "72000.0000", "kind": "INTER" }
  ]
}, "error": null }
```
Derived from each finalized invoice's snapshotted tax (`taxInfos` + `placeOfSupply` vs seller place) —
no recomputation against the tax module (R10).

---

## 5. GET `/analytics/v1/dashboard/top`

Top-N entities by revenue for the period. → FR-005 / R12.

**Query**: `from_date`, `to_date`, `period`, `dimension` ∈ `product|customer`, `limit` (default 5, max 50).

**200** `ApiResponse<List<TopEntryResponse>>`:
```json
{ "success": true, "data": [
  { "rank": 1, "id": "PRD20260101ABCDEF...", "name": "Basmati Rice 25kg",
    "gross_amount": "210400.0000", "qty": "842.000", "doc_count": 96 },
  { "rank": 2, "id": "PRD2026...", "name": "Sunflower Oil 15L",
    "gross_amount": "188900.0000", "qty": "611.000", "doc_count": 74 }
], "error": null }
```
`name` is resolved via a second keyed lookup (no cross-DB join on mobile — R4).

---

## 6. POST `/analytics/v1/recompute`

Manually trigger a reconcile of the materialized summary for a date range (admin/owner). → R2.

**Body**:
```json
{ "from_date": "2026-06-01", "to_date": "2026-06-27", "metric_groups": ["SALES","GST_SUMMARY"] }
```
`metric_groups` optional (default all). Idempotent — re-running yields identical summary rows (SC-004).

**200** `ApiResponse<RecomputeResultResponse>`:
```json
{ "success": true, "data": {
  "from_date": "2026-06-01", "to_date": "2026-06-27",
  "days_recomputed": 27, "rows_upserted": 1340, "duration_ms": 4120
}, "error": null }
```
**Errors**: `403` insufficient role; `400` invalid range.

---

## 7. GET `/analytics/v1/export`  (P3)

Server-side export for history beyond the device sync window. → FR-025 / R11.

**Query**: `format` ∈ `csv|pdf`, `from_date`, `to_date`, `period`, `metric_group` (optional, repeatable).
**200**: streamed `text/csv` or `application/pdf` (not wrapped in `ApiResponse`); money/dates localized to
the workspace. On-device CSV/PDF export covers the in-window case offline (FR-012).

---

## DTOs (response, in `analytics/domain/dto/`)

```kotlin
data class KpiResponse(val metricGroup: String, val period: String, val fromDate: LocalDate,
    val toDate: LocalDate, val currencyCode: String, val values: List<KpiValueResponse>,
    val computedFrom: Instant?)
data class KpiValueResponse(val metricId: String, val unit: String, val value: BigDecimal)
data class TrendPointResponse(val bucketStart: LocalDate, val bucketLabel: String, val value: BigDecimal)
data class AgingResponse(val asOfDate: LocalDate, val currencyCode: String,
    val totalOutstanding: BigDecimal, val buckets: List<AgingBucketResponse>)
data class AgingBucketResponse(val bucket: String, val amount: BigDecimal, val docCount: Int)
data class GstSummaryResponse(val fromDate: LocalDate, val toDate: LocalDate, val currencyCode: String,
    val taxableValue: BigDecimal, val totalTax: BigDecimal, val intraState: IntraSplit,
    val interState: InterSplit, val byRate: List<GstRateRow>)
data class TopEntryResponse(val rank: Int, val id: String, val name: String,
    val grossAmount: BigDecimal, val qty: BigDecimal, val docCount: Int)
data class RecomputeResultResponse(val fromDate: LocalDate, val toDate: LocalDate,
    val daysRecomputed: Int, val rowsUpserted: Int, val durationMs: Long)
```
All via `entity.asResponse()` converters — entities never exposed (Principle II).
