# Phase 1 Data Model — Analytics & Forecasting Dashboard

Derives from [spec.md](./spec.md) (entities, FRs), [plan.md](./plan.md) (structure), and
[research.md](./research.md) (R1–R12). Money/time conventions follow the constitution: timestamps are
`Instant` → `TIMESTAMPTZ`/`TIMESTAMP`; money is `BigDecimal` → `DECIMAL(19,4)`; bucket keys are
`LocalDate` → `DATE`; tenant scoping via `OwnableBaseDomain` (`@TenantId ownerId`).

> **Money-unit resolution (settles a plan inconsistency).** Backend stores money as **`BigDecimal` /
> `DECIMAL(19,4)`** in major currency units; columns are named `*_amount` (NOT `*_minor`). The "minor
> units / `Long`" representation is a **mobile-only** convention applied after the read crosses the
> `/sync` boundary. The `gross_minor`/`net_minor` naming in plan.md §P1 is superseded here by
> `gross_amount`/`net_amount`/`tax_amount`.

---

## 1. Backend entities (`analytics` bounded context)

### 1.1 `KpiDailySummary`  (table `kpi_daily_summary`)

One materialized row per `workspace × businessDate × metricGroup × dimension`. Fully recomputable from
source tables (R1/R2). Extends `OwnableBaseDomain` (provides `id`, `uid`, `ownerId`, `createdAt`,
`updatedAt`, `active`).

| Field | Type | DB column | Notes |
|---|---|---|---|
| `businessDate` | `LocalDate` | `business_date DATE NOT NULL` | Bucketed in the **business timezone** (R7), never UTC/device. |
| `metricGroup` | `MetricGroup` (enum) | `metric_group VARCHAR(32) NOT NULL` | SALES, COLLECTIONS, AGING, TOP_PRODUCT, TOP_CUSTOMER, GST_SUMMARY, INVENTORY. |
| `period` | `Period` (enum) | `period VARCHAR(8) NOT NULL DEFAULT 'DAY'` | Storage grain is DAY; WEEK/MONTH are roll-ups at read time (`GROUP BY`). |
| `dimProductId` | `String?` | `dim_product_id VARCHAR(64)` | Set for TOP_PRODUCT / INVENTORY; else NULL-sentinel `''`. |
| `dimCustomerId` | `String?` | `dim_customer_id VARCHAR(64)` | Set for TOP_CUSTOMER / COLLECTIONS; else `''`. |
| `taxRate` | `BigDecimal?` | `tax_rate DECIMAL(7,4)` | Set for GST_SUMMARY rate buckets; else NULL. |
| `taxKind` | `TaxKind?` (enum) | `tax_kind VARCHAR(8)` | INTRA (CGST+SGST) / INTER (IGST) for GST_SUMMARY (R10). |
| `agingBucket` | `AgingBucket?` (enum) | `aging_bucket VARCHAR(16)` | CURRENT, D1_30, D31_60, D61_90, D90_PLUS for AGING. |
| `grossAmount` | `BigDecimal` | `gross_amount DECIMAL(19,4) NOT NULL DEFAULT 0` | Gross sales / collected / outstanding per group. |
| `netAmount` | `BigDecimal` | `net_amount DECIMAL(19,4) NOT NULL DEFAULT 0` | Net of tax/discount where applicable. |
| `taxAmount` | `BigDecimal` | `tax_amount DECIMAL(19,4) NOT NULL DEFAULT 0` | Output tax (GST_SUMMARY / SALES). |
| `qty` | `BigDecimal` | `qty DECIMAL(19,3) NOT NULL DEFAULT 0` | Units (TOP_PRODUCT / INVENTORY). |
| `docCount` | `Int` | `doc_count INT NOT NULL DEFAULT 0` | Document/txn count (SALES count, AOV denominator). |
| `recomputedAt` | `Instant` | `recomputed_at TIMESTAMPTZ` | Last reconcile/upsert time, for freshness + idempotence. |

**Uniqueness / integrity**
- Unique business key: `(owner_id, business_date, metric_group, dim_product_id, dim_customer_id, tax_rate, tax_kind, aging_bucket)`.
  Nullable dimension columns use a sentinel (`''` for ids, a 0-row for rates) so the unique index is
  enforceable across vendors. Upsert is `INSERT … ON CONFLICT (key) DO UPDATE` (Postgres) /
  `INSERT … ON DUPLICATE KEY UPDATE` (MySQL).

**Indexes**
- `ux_kpi_summary_key` — UNIQUE on the business key above (drives the upsert).
- `ix_kpi_summary_read` — `(owner_id, metric_group, business_date)` (drives KPI/trend reads, R7).
- `ix_kpi_summary_dim_product` — `(owner_id, metric_group, dim_product_id, business_date)`.
- `ix_kpi_summary_dim_customer` — `(owner_id, metric_group, dim_customer_id, business_date)`.

**Validation rules**
- `businessDate` MUST be derived as `documentInstant.atZone(businessZone).toLocalDate()` (R7).
- Amounts MUST be ≥ 0 for SALES/COLLECTIONS/INVENTORY groups; AGING `grossAmount` is outstanding ≥ 0.
- A summary row MUST NOT mix metric groups; dimension columns MUST be populated per the group's contract
  above (else the sentinel).
- Only **finalized** documents contribute (drafts/cancelled excluded — FR-013).

**Lifecycle / recompute (R2)**
1. `@TransactionalEventListener(AFTER_COMMIT)` on `InvoiceFinalizedEvent` / `InvoicePaidEvent` /
   `OrderEvents` / `InventoryStockUpdatedEvent` → upsert the affected `(date, group, dim)` buckets.
2. Nightly `@Scheduled` reconcile recomputes the trailing N days from source tables (heals missed/dup
   events; backdated edits re-roll their day). Manual trigger: `POST /analytics/v1/recompute`.
3. Recompute is **idempotent**: recomputing a day yields identical rows (tested — plan Testing).

---

### 1.2 `DemandForecast`  (table `demand_forecast`)

One row per `workspace × productId × periodStart` (R5). Pull-only to mobile via `/sync`. Extends
`OwnableBaseDomain`.

| Field | Type | DB column | Notes |
|---|---|---|---|
| `productId` | `String` | `product_id VARCHAR(64) NOT NULL` | FK by UID to product (no cross-module DB FK). |
| `periodStart` | `LocalDate` | `period_start DATE NOT NULL` | Start of the forecast period (business zone). |
| `horizon` | `Int` | `horizon INT NOT NULL DEFAULT 1` | Number of periods ahead. |
| `meanQty` | `BigDecimal` | `mean_qty DECIMAL(19,3) NOT NULL` | Expected demand (units). |
| `stdDevQty` | `BigDecimal` | `std_dev_qty DECIMAL(19,3) NOT NULL DEFAULT 0` | Demand variability → safety-stock input (R6). |
| `method` | `ForecastMethod` (enum) | `method VARCHAR(16) NOT NULL` | HOLT_WINTERS / MOVING_AVG (fallback). |
| `confidence` | `Confidence` (enum) | `confidence VARCHAR(8) NOT NULL DEFAULT 'LOW'` | HIGH/MEDIUM/LOW by history depth (FR-016). |
| `generatedAt` | `Instant` | `generated_at TIMESTAMPTZ NOT NULL` | Batch run time; also the `/sync` `updatedAt` source. |

**Uniqueness / indexes**
- Unique: `(owner_id, product_id, period_start, horizon)`.
- `ix_forecast_sync` — `(owner_id, updated_at)` for the `/sync` incremental feed (sorted ASC).
- `ix_forecast_product` — `(owner_id, product_id, period_start)`.

**Validation rules**
- `meanQty` ≥ 0; `stdDevQty` ≥ 0; `horizon` ≥ 1.
- `method = HOLT_WINTERS` only when history ≥ 2 seasonal cycles, else `MOVING_AVG` with `confidence ≤ MEDIUM` (R5/FR-016).

**Published signal (R6, no inventory writes)**
- After a batch run, publish `DemandForecastUpdatedEvent(ownerId, productId, periodStart, meanQty, stdDevQty)`
  (Spring `ApplicationEvent`) + a public `DemandSignalService` interface returning avg daily demand &
  variability — consumed by feature 027 (reorder point/safety stock) and inventory (spec 014). Analytics
  **never** writes `InventoryItem.reorderLevel` (FR-018).

---

### 1.3 `MetricDefinition` (catalog — code, not a table) (R9)

A declarative in-code registry (`domain/catalog/`) that drives the backend summary, the mobile DAO query,
and the agent NL mapping from one source of truth. No DB table.

| Attribute | Type | Notes |
|---|---|---|
| `id` | `String` | Stable metric id, e.g. `sales.gross`, `collections.outstanding`, `gst.output_by_rate`, `inventory.turns`. |
| `group` | `MetricGroup` | One of the P1 groups. |
| `unit` | `MetricUnit` | MONEY / COUNT / QTY / RATIO / PERCENT. |
| `aggregation` | `Aggregation` | SUM / COUNT / AVG / RATIO / LAST. |
| `sourceModule` | `String` | invoice / order / payment / inventory (which DB/source it derives from). |
| `periods` | `Set<Period>` | Supported presets (DAY/WEEK/MONTH). |

P1 catalog (minimum): SALES (`sales.gross`, `sales.net`, `sales.count`, `sales.aov`), COLLECTIONS
(`collections.collected`, `collections.outstanding`, `collections.aging`), TOP_PRODUCT (`top.product`),
TOP_CUSTOMER (`top.customer`), GST_SUMMARY (`gst.output_by_rate`, `gst.cgst_sgst`, `gst.igst`), INVENTORY
(`inventory.stock_value`, `inventory.low_stock_count`, `inventory.turns`).

---

## 2. Mobile model (`feature/analytics`)

### 2.1 No new write tables — on-the-fly aggregates (R3/R4)
P1 KPIs are computed by **read-only aggregate DAO queries** over the already-synced, workspace-scoped
feature DBs — no analytics write table is synced:

| KPI group | Source DB | Reads |
|---|---|---|
| Sales | invoice / order | finalized `InvoiceEntity` (+ `OrderEntity`) date-bounded `SUM`/`COUNT`/`AVG`. |
| Collections & aging | payment | `LedgerEntryEntity` / `PartyBalanceEntity` / open-bill rows by due date vs business "today". |
| GST summary | invoice | `InvoiceEntity.total_tax` + per-line tax JSON + `placeOfSupply` vs seller place (R10). |
| Top products / customers | invoice / order | `GROUP BY dim ORDER BY SUM(...) DESC LIMIT N` (R12). |
| Inventory value / low-stock / turns | inventory | `InventoryItemEntity` + movement DAO. |

All bucketing uses `LocalAppLocale.timeZoneId`; in non-composable ViewModel/DAO code the business zone is
**injected** (never `TimeZone.currentSystemDefault()`) — R7 / cmp-practices §12.

### 2.2 `DemandForecastEntity` (read-only mirror) — `AnalyticsRoomDatabase`
The **only** mobile write table — a pull-only mirror of backend `DemandForecast`.

| Field | Type | Notes |
|---|---|---|
| `uid` | `String` (PK) | Server UID. |
| `productId` | `String` | |
| `periodStart` | `String` (ISO date) | |
| `horizon` | `Int` | |
| `meanQtyMilli` | `Long` | mean qty × 1000 (3 dp as integer). |
| `stdDevQtyMilli` | `Long` | std-dev × 1000. |
| `method` | `String` | HOLT_WINTERS / MOVING_AVG. |
| `confidence` | `String` | HIGH/MEDIUM/LOW. |
| `generatedAt` | `String` (ISO instant) | sync checkpoint source. |
| `synced` | `Boolean` | always `true` (pull-only; no local writes pushed). |

Synced by `DemandForecastSyncDelegate` (PULL-ONLY) under `SyncEntity.DEMAND_FORECAST` — no push path
(plan; offline-sync skill). Money on mobile is `Long` minor units; quantities are milli-units (`×1000`).

### 2.3 Dashboard layout (workspace setting, not a new entity) (R9)
Tile layout persists as `StoreSetting` (`module_code='analytics'`) via `AnalyticsSettingDefinitions`,
riding existing `SyncEntity.STORE`. Shape per tile: `{ widgetType, metricId, periodPreset, filter? }`,
ordered list. No new sync surface (FR-024).

---

## 3. Entity relationships

```
workspace (owner_id) ──< KpiDailySummary        (materialized; recomputable from source)
workspace (owner_id) ──< DemandForecast ──(productId, by UID)──> product (other module, no DB FK)
DemandForecast ──publishes──> DemandForecastUpdatedEvent ──> feature 027 (reorder) / inventory 014
MetricDefinition (code catalog) ──drives──> KpiDailySummary rollup + mobile DAO query + agent NL map
StoreSetting(module_code='analytics') ──holds──> Dashboard layout (rides SyncEntity.STORE)
mobile DemandForecastEntity ──mirrors (pull-only)──> backend DemandForecast
```

Cross-module reads are via **published domain events + public service interfaces only** (Principle IX);
analytics owns no source data and writes no other module's tables.

---

## 4. Enums

- `MetricGroup`: SALES, COLLECTIONS, AGING, TOP_PRODUCT, TOP_CUSTOMER, GST_SUMMARY, INVENTORY
- `Period`: DAY, WEEK, MONTH
- `TaxKind`: INTRA, INTER
- `AgingBucket`: CURRENT, D1_30, D31_60, D61_90, D90_PLUS
- `ForecastMethod`: HOLT_WINTERS, MOVING_AVG
- `Confidence`: HIGH, MEDIUM, LOW
- `MetricUnit`: MONEY, COUNT, QTY, RATIO, PERCENT
- `Aggregation`: SUM, COUNT, AVG, RATIO, LAST

All enums serialize as their UPPER_SNAKE name (global SNAKE_CASE applies to field names, not enum values).

---

## 5. Migration (Flyway)

New module `analytics` → first migration `V1.0.0__create_analytics_tables.sql`, written in **both**
`analytics/src/main/resources/db/migration/mysql/` and `…/postgresql/` (vendor-specific
`TIMESTAMP` vs `TIMESTAMPTZ`). Add `"analytics"` to `migrationModules` in
`ampairs_service/build.gradle.kts` and `include("analytics")` in `settings.gradle.kts`. Confirm the next
version with `./gradlew :ampairs_service:flywayInfo` before finalizing (per-module sequences are
independent — `order` started at `V1.0.0`).

Tables: `kpi_daily_summary`, `demand_forecast` (+ indexes in §1). No changes to other modules' tables.
