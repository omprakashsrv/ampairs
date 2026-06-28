# Phase 0 Research — Analytics & Forecasting Dashboard

All Technical Context unknowns are resolved below. Each item: **Decision · Rationale · Alternatives
considered**. These supersede the inline assumptions in `spec.md` with concrete technical choices.
The platform already captures every business mutation (`WorkspaceEvent` + Spring `ApplicationEvent`s
such as `InvoiceFinalizedEvent`, `OrderEvents`, `InventoryStockUpdatedEvent`) but has **no KPI /
dashboard read model on any client**. This feature closes that gap with a deliberately split compute
model: simple, deterministic stats run on-device (offline); heavy ML and cross-tenant rollups stay on
the backend.

---

## R1. Read model — materialized summaries vs on-the-fly aggregation

- **Decision**: **Hybrid, period-grained materialized read model on the backend** plus **on-the-fly
  aggregation on mobile**. Backend introduces a new `analytics` bounded context owning
  `KpiDailySummary` rows (one per `workspaceId × businessDate × metricGroup`) rebuilt incrementally
  from the existing domain events; the dashboard read API serves pre-aggregated buckets. Mobile derives
  the same KPIs **on-the-fly** from the already-synced Room tables (`InvoiceEntity`, `OrderEntity`,
  `LedgerEntryEntity`/`PartyBalanceEntity`, `InventoryItemEntity`) via indexed DAO aggregate queries —
  no separate summary table is synced.
- **Rationale**: On the backend a dashboard that re-scans `invoice`/`order`/`payment` per request does
  not scale to a month-over-month trend view across thousands of invoices; a daily materialized summary
  makes the read O(days) and lets trends/forecasts read one small table. On mobile the dataset for one
  workspace is already local and bounded (tens of thousands of rows), SQLite aggregate queries over
  indexed `*_date`/`status` columns are sub-100 ms, and a synced summary table would add a whole
  `SyncDelegate` + drift risk for data the device can compute exactly. Computing the *same* metric two
  ways is acceptable because each KPI has a single closed-form definition (see R7) applied to identical
  source rows.
- **Alternatives considered**: Pure on-the-fly on the backend (rejected — trend/forecast reads rescan
  large tables every call). A synced `KpiSummary` SyncEntity mirrored to Room (rejected — the device
  already holds the source rows; syncing derived data invites stale/conflicting buckets and doubles the
  sync surface). A separate OLAP store / Kafka projection (rejected for P1 — premature; the event table
  + a daily rollup job covers SMB scale without new infrastructure).

## R2. Backend aggregation strategy — event-driven incremental vs nightly batch

- **Decision**: **Incremental upsert driven by domain events, with a nightly reconcile sweep.** An
  `@TransactionalEventListener(phase = AFTER_COMMIT)` on `InvoiceFinalizedEvent`, `InvoicePaidEvent`,
  `OrderEvents`, `InventoryStockUpdatedEvent` etc. upserts the affected `KpiDailySummary` bucket
  (keyed by the document's **business date**, R7). A scheduled job recomputes the trailing N days from
  the source tables to heal any missed/duplicated event (the summary is fully recomputable, like the
  payment ledger's `recompute-balance`).
- **Rationale**: Events already exist and fire `AFTER_COMMIT`, so the summary stays near-real-time at
  near-zero marginal cost; the nightly reconcile makes the read model **self-healing** and order-
  independent (backdated edits just trigger a re-roll of that day). A pure batch lag of up to 24 h is
  unacceptable for a "today's sales" tile; pure event handling with no reconcile drifts on missed
  events.
- **Alternatives considered**: Nightly batch only (rejected — stale intraday KPIs). Event-only, no
  reconcile (rejected — no recovery path for a dropped event; can't represent backdated edits). CDC /
  debezium (rejected — infra weight beyond P1).

## R3. Offline KPI computation on mobile — deterministic SQL over Room

- **Decision**: Each dashboard KPI is a **deterministic aggregate DAO query** in `feature/analytics`
  (`commonMain`) over the already-synced feature DBs, executed read-only and bucketed in the **business
  timezone** (R7). KPIs render from local data with **zero network dependency**; a "last synced" stamp
  communicates freshness. The dashboard never blocks on the backend read API — the backend summary is
  used only for (a) periods/parties whose source rows predate the device's sync window and (b) the
  forecast series (R5).
- **Rationale**: The product's core promise is offline-first; a dashboard that needs connectivity fails
  the field-sales use case. The source rows are local and the KPI math is simple sums/counts/averages,
  so on-device compute is correct, instant, and private. This mirrors how the on-device agent's
  SafeQuery path already reads these same DBs read-only (R8).
- **Alternatives considered**: Fetch KPIs from the backend read API and cache (rejected — offline gap,
  staleness, and it bypasses the local source of truth the user just edited offline). Maintain a Room
  `kpi_summary` rollup updated on every write (rejected — extra write-path coupling and recompute logic
  the on-the-fly query makes unnecessary at this scale).

## R4. Cross-DB KPIs — per-module queries, composed in the ViewModel

- **Decision**: Because each feature DB is a **separate workspace-scoped Room database** (no cross-DB
  JOINs possible), every KPI is computed **within one module's DB** and the dashboard ViewModel
  **composes** the per-module results. "Sales today" reads `invoice`/`order` DB; "collections & aging"
  reads `payment` DB; "inventory turns / low stock" reads `inventory` DB; "GST summary" reads `invoice`
  DB (`taxInfos`/`placeOfSupply`). Cross-entity views that truly need a join (e.g. "sales by customer
  name") are resolved by a second lookup keyed by `customerId`, never a SQL join.
- **Rationale**: Matches the existing architecture exactly — the agent's SafeQuery path is also "one
  module's DB per query" by construction (`.claude/memory/feedback_agent_models.md`). It keeps each KPI
  query inside a `@Inject` DAO owned by that feature, avoids a god-database, and survives workspace
  switches cleanly (each DB is `@SingleIn(WorkspaceScope::class)`).
- **Alternatives considered**: A unified analytics Room DB duplicating rows from every feature
  (rejected — second copy to keep in sync, defeats the source-of-truth model). Backend-only cross-module
  KPIs (rejected — offline gap; the backend `analytics` summary still serves these but can't be the
  *only* path).

## R5. Demand forecasting — algorithm and where it runs

- **Decision**: **Start simple and split by horizon/weight.** Backend `analytics` computes the demand
  forecast as a **batch job** producing a `DemandForecast` per `productId × period`: P1 uses
  **moving average + Holt-Winters exponential smoothing** (level + trend + multiplicative seasonality)
  over the daily sales series derived from finalized invoices/confirmed orders; it falls back to simple
  moving average when history < 2 seasonal cycles. The forecast (mean + a simple confidence band) is
  exposed on the read API and **synced to mobile read-only** (pull-only `DemandForecast` rows). On-device
  the app computes only a **trailing moving average / simple exponential smoothing** for instant offline
  "expected demand" sparklines when no backend forecast is present.
- **Rationale**: Holt-Winters is the right first-rung algorithm for retail demand — it captures trend
  and weekly/seasonal cycles, is cheap, explainable, and needs no training infra, unlike ARIMA/Prophet/
  ML which want more data and a Python/ML runtime the JVM backend doesn't have. Running the heavy fit on
  the backend keeps the model consistent across devices and feeds replenishment (feature 027); the
  on-device simple-EWMA fallback keeps a useful number offline without shipping a forecasting engine to
  Kotlin/Native.
- **Alternatives considered**: ARIMA/Prophet/LSTM (rejected for P1 — data-hungry, opaque, infra-heavy;
  candidates for a later phase once the read model proves out). Forecast entirely on-device (rejected —
  inconsistent across devices, no seasonality with sparse local history, can't pool signal). No
  forecast, only history (rejected — replenishment needs a forward demand input, FR for 027).

## R6. Reorder-point feed into inventory (boundary with feature 027)

- **Decision**: `analytics` **publishes** a forecast/velocity signal; it does **not** write inventory.
  It exposes (a) `DemandForecast` (mean demand + std-dev per product/period) and (b) a derived
  **average daily demand & demand variability** read API. Feature 027's replenishment service consumes
  these to compute safety stock / reorder point / EOQ and to draft purchase suggestions, and
  `inventory` (spec 014) owns `InventoryItem.reorderLevel`. Cross-module flow is a **public service
  interface + Spring `ApplicationEvent`** (`DemandForecastUpdatedEvent`), never a direct repo write.
- **Rationale**: Respects module boundaries (Principle IX) — analytics measures, replenishment decides,
  inventory stores. A reorder-point suggestion is a *pricing/ops policy* decision (027), not an analytics
  fact; keeping analytics read-only avoids two modules both owning `reorderLevel`.
- **Alternatives considered**: Analytics writing `reorderLevel` directly (rejected — crosses into
  inventory's bounded context and couples measurement to policy). Inventory computing its own forecast
  (rejected — duplicates the seasonality math that belongs in analytics).

## R7. Period bucketing — business timezone, not device/UTC

- **Decision**: All day/week/month bucketing uses the **workspace business timezone**, never the device
  or server local zone. Storage stays UTC `Instant` (backend) / ISO-8601 UTC strings (mobile). Backend
  resolves the business `TimeZone` from the `business` module profile and buckets each document's
  `Instant` with `instant.atZone(businessZone).toLocalDate()` when writing `KpiDailySummary.businessDate`
  (a `LocalDate`). Mobile reads the zone from `BusinessLocaleProvider` / `LocalAppLocale.timeZoneId` and
  buckets with `instant.toLocalDateTime(TimeZone.of(locale.timeZoneId))` — in non-composable
  ViewModel/DAO code the zone is injected, never `TimeZone.currentSystemDefault()`.
- **Rationale**: A sale at 23:30 IST is the *same business day* regardless of the device's zone; bucketing
  in device/UTC time lands KPIs on the wrong day/month (the documented "computation trap" in
  `/cmp-practices §12`). A pre-bucketed `businessDate` on the summary makes backend trend reads a trivial
  `GROUP BY business_date` and keeps the device and server in agreement.
- **Alternatives considered**: UTC bucketing (rejected — off-by-one day at the day boundary for non-UTC
  businesses). Device-timezone bucketing (rejected — two devices in different zones would disagree on
  "today"). Per-user timezone (rejected — the KPI is a *business* fact, not a personal one).

## R8. Natural-language Q&A — reuse the on-device agent SafeQuery path

- **Decision**: NL analytics ("how many customers", "total sales this month", "top 5 products") is
  served by the **existing on-device agent SafeQuery pipeline** — not a new engine. Analytics adds
  curated `ModuleQuerySchema`s where a queryable module is missing and relies on the agent's
  `LlmIntentResolver → AgentOrchestrator → SafeQueryService` (validated by `SafeSqlValidator`,
  executed by each module's `ModuleQueryExecutor`). Where a question maps to a **named dashboard KPI**,
  the dashboard offers it as a one-tap tile so the answer is instant and model-independent; free-form
  questions fall through to SafeQuery (which needs a chat model loaded, RAM ≥ 3 GB per `RamTiers`).
- **Rationale**: The text-to-SQL path already exists, is sandboxed (SELECT-only, table allow-list,
  enforced LIMIT, reader connection), and already covers customer/product/invoice/inventory/order/
  payment. Building a parallel NL engine would duplicate the guardrails and the model plumbing. The KPI
  tiles cover the common questions deterministically; SafeQuery covers the long tail.
- **Alternatives considered**: A bespoke analytics intent parser (rejected — reinvents SafeQuery and its
  safety guarantees). Sending NL questions to a backend LLM (rejected — offline gap, privacy, and it
  bypasses the on-device data the user just edited). Hard-coding every phrasing to a KPI (rejected —
  brittle; SafeQuery generalizes).

## R9. KPI / metric catalog & dashboard config

- **Decision**: A **declarative widget catalog**. Each KPI is a typed `MetricDefinition` (id, group,
  unit, aggregation, source module, period support) registered once; the dashboard is an ordered list of
  `DashboardWidget` config rows (widget type, metric id, period preset, optional filter) persisted as
  **workspace settings** via the existing `setting` module (`StoreSetting`, `module_code='analytics'`)
  and a `AnalyticsSettingDefinitions : SettingDefinitionProvider`. P1 metric groups: **Sales**
  (gross/net, count, AOV), **Collections & Aging** (collected, outstanding, aging buckets from
  `payment`), **Top Products / Customers**, **GST Summary** (output tax by rate / CGST-SGST-IGST split
  from `Invoice.taxInfos`/`placeOfSupply`), **Inventory** (stock value, low-stock count, inventory
  turns).
- **Rationale**: A declarative catalog lets the same `MetricDefinition` drive the backend summary, the
  on-device DAO query, and the agent's NL mapping without per-widget code. Reusing `StoreSetting` for
  layout avoids new settings infra and rides the existing `STORE` sync (matches inventory policy in spec
  014).
- **Alternatives considered**: Hard-coded dashboard (rejected — no per-workspace customization, every
  new KPI is a code change on three layers). A bespoke dashboard-config table + SyncDelegate (rejected —
  duplicates `StoreSetting`/`SyncEntity.STORE`).

## R10. GST summary semantics

- **Decision**: The GST summary KPI reads finalized `Invoice` rows and splits output tax using
  `Invoice.taxInfos` (per-line `name`/`percentage`/`value`) and `placeOfSupply` vs `sellerPlaceOfSupply`
  to classify **intra-state (CGST+SGST)** vs **inter-state (IGST)**, bucketed by tax rate and business
  month (R7). Backend materializes a `GST_SUMMARY` metric group in `KpiDailySummary`; mobile computes the
  same split on-device from the synced `InvoiceEntity` (`total_tax` + the line tax JSON).
- **Rationale**: Reuses the invoice module's existing GST snapshot (taxInfos already carry the rate and
  amount, `placeOfSupply` already drives IGST vs CGST/SGST at invoicing) — no new tax computation.
  Monthly GST-rate buckets are exactly what a filing-prep summary needs.
- **Alternatives considered**: Recompute tax from the tax module (rejected — the invoice already
  snapshots the authoritative tax at issue time; recomputing risks divergence from the filed invoice).

## R11. Export

- **Decision**: P1 = **CSV/PDF export of the rendered KPI tables** generated on-device from the same
  local aggregates (CSV in `commonMain`; PDF via the existing platform print path used by invoices),
  amounts formatted with `formatMoney(amount, LocalAppLocale.current)` (currency symbol passed as a
  `String` into any non-composable HTML/CSV builder). Backend offers an equivalent server-side export
  endpoint for periods outside the device's sync window.
- **Rationale**: Reuses the existing on-device formatting + print stack; keeps export offline-capable
  and currency/timezone-correct by construction. Server export covers historical depth.
- **Alternatives considered**: Backend-only export (rejected — offline gap). Excel/XLSX (deferred — CSV
  is sufficient for P1 and avoids a new dependency).

## R12. Top-N and aging performance on mobile

- **Decision**: Top-products/top-customers use indexed `GROUP BY … ORDER BY SUM(...) DESC LIMIT N`
  queries; aging buckets are computed from `payment` open-bill rows by `due date` vs the business
  "today". Add covering indexes on the `*_date`, `status`, `customer_id`/`product_id` columns used by
  the aggregate queries. The dashboard caps each query with a date range so a multi-year workspace never
  scans the whole table for a "this month" tile.
- **Rationale**: SQLite handles `GROUP BY`/`SUM` over indexed, date-bounded ranges well below the
  perceived-instant threshold for SMB volumes; bounding by period is what keeps it fast as history grows.
- **Alternatives considered**: In-memory aggregation in Kotlin over a full table read (rejected — loads
  every row into memory; the DB does this faster). Precomputed rollups in Room (rejected — see R1/R3).

---

## Resolved unknowns summary

| Unknown (Technical Context) | Resolution |
|---|---|
| Read model vs on-the-fly | Backend materialized `KpiDailySummary`; mobile on-the-fly over Room (R1) |
| Backend aggregation trigger | Event-driven incremental upsert + nightly reconcile (R2) |
| Offline KPI compute | Deterministic DAO aggregates over synced feature DBs (R3) |
| Cross-DB KPIs | Per-module queries composed in the ViewModel; no cross-DB join (R4) |
| Forecasting algorithm / locus | Holt-Winters exp. smoothing on backend batch; simple EWMA fallback on-device (R5) |
| Reorder feed | Analytics publishes demand signal; 027 decides, inventory stores (R6) |
| Period bucketing | Business timezone via `BusinessLocaleProvider`; store UTC (R7) |
| NL Q&A | Reuse on-device agent SafeQuery + KPI tiles for common questions (R8) |
| KPI catalog / dashboard config | Declarative `MetricDefinition` + `StoreSetting` layout (R9) |
| GST summary | From `Invoice.taxInfos` + `placeOfSupply`, monthly rate buckets (R10) |
| Export | On-device CSV/PDF from local aggregates; server export for deep history (R11) |
| Mobile top-N / aging perf | Indexed, date-bounded `GROUP BY … LIMIT N` (R12) |
</content>
</invoke>
