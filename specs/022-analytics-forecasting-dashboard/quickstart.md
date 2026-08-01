# Quickstart — Analytics & Forecasting Dashboard

How to exercise the dashboard, forecast, and NL Q&A end-to-end across the backend (`ampairs`) and the
mobile app (`ampairs-app`). Aligns with [plan.md](./plan.md) phases P1–P3.

## Prerequisites

- Backend: Java 21, Docker (Testcontainers/Postgres), the new `analytics` module wired into
  `settings.gradle.kts` + `migrationModules`.
- A workspace with seeded **finalized** invoices/orders, payments (some partially paid / overdue), and
  inventory items with stock movements — across at least 2–3 months so trends and forecasts are meaningful.
- A configured **business timezone** on the workspace business profile (e.g. `Asia/Kolkata`) — the whole
  point of R7 is exercised only when the business zone differs from UTC/your device.

---

## A. Backend — bring up the read model

```bash
# 1. Migrate (writes kpi_daily_summary + demand_forecast in both vendors)
./gradlew :ampairs_service:flywayInfo          # confirm next analytics version is V1.0.0
./gradlew :ampairs_service:flywayMigrate

# 2. Run the service
./gradlew :ampairs_service:bootRun
```

### A1. Materialize summaries from existing data
The event listeners only catch *new* mutations, so seed history via the reconcile trigger:
```bash
curl -s -X POST "$BASE/analytics/v1/recompute" \
  -H "Authorization: Bearer $JWT" -H "X-Workspace-ID: $WS" \
  -H 'Content-Type: application/json' \
  -d '{"from_date":"2026-04-01","to_date":"2026-06-30"}' | jq
# → days_recomputed, rows_upserted
```

### A2. Read the dashboard
```bash
H=(-H "Authorization: Bearer $JWT" -H "X-Workspace-ID: $WS")

curl -s "${H[@]}" "$BASE/analytics/v1/dashboard/kpis?from_date=2026-06-01&to_date=2026-06-30&period=MONTH&metric_group=SALES" | jq
curl -s "${H[@]}" "$BASE/analytics/v1/dashboard/trend?from_date=2026-04-01&to_date=2026-06-30&period=WEEK&metric_id=sales.gross" | jq
curl -s "${H[@]}" "$BASE/analytics/v1/dashboard/aging" | jq
curl -s "${H[@]}" "$BASE/analytics/v1/dashboard/gst-summary?from_date=2026-06-01&to_date=2026-06-30" | jq
curl -s "${H[@]}" "$BASE/analytics/v1/dashboard/top?dimension=product&limit=5&from_date=2026-06-01&to_date=2026-06-30" | jq
```

### A3. Verify business-timezone bucketing (R7 / SC-003)
Finalize an invoice timestamped `2026-06-30T18:30:00Z` (= `2026-07-01 00:00 IST`). Confirm it lands in
**July** for an `Asia/Kolkata` workspace, not June. This is the core correctness test.

### A4. Idempotent recompute (SC-004)
Run A1 twice for the same range; row counts/values are identical (no double counting). The
`./gradlew :analytics:test` suite asserts this plus the bucketing and Holt-Winters cases.

---

## B. Backend — demand forecast (P2)

```bash
# Trigger the nightly batch on demand (test profile) or wait for the @Scheduled run, then pull:
curl -s "${H[@]}" "$BASE/analytics/v1/forecasts/sync?size=100&sort_by=updatedAt&sort_dir=ASC" | jq '.data.content[0]'
# → mean_qty, std_dev_qty, method (HOLT_WINTERS once ≥2 seasonal cycles, else MOVING_AVG), confidence
```
Replenishment (feature 027) and inventory (014) receive `DemandForecastUpdatedEvent` in-process — assert
the published signal in their listeners, not by analytics writing inventory.

---

## C. Mobile — offline dashboard (P1, the core promise)

```bash
cd ../ampairs-app
./gradlew :feature:analytics:check
# 3-target compile gate:
./gradlew androidApp:compileDebugKotlinAndroid shared:compileKotlinIosSimulatorArm64 desktopApp:compileKotlin
```

Manual run:
1. Launch the app, select the seeded workspace, let it sync (invoice/order/payment/inventory).
2. **Enable airplane mode.**
3. Open **Dashboard** (`Route.Analytics`, via `ModuleRegistry` "analytics-dashboard").
4. Confirm every P1 tile renders from local data with no network: Sales (gross/net/count/AOV),
   Collections & aging, Top products, Top customers, GST summary, Inventory value / low-stock / turns.
   A "last synced" stamp shows freshness (FR-010).
5. Toggle period **Day → Week → Month**; all tiles recompute (< 1 s — SC-001/SC-002), bucketed in the
   workspace business zone (not the device zone).
6. **Export** the visible figures (CSV/PDF) — money in workspace currency, dates in workspace format.

### C1. Parity check (FR-027 / SC-004)
For the same period, the offline tile value MUST equal the backend `…/dashboard/kpis` value to the last
currency unit. Spot-check `sales.gross` for "This Month".

---

## D. Mobile — forecast mirror + NL Q&A (P2/P3)

- **Forecast (P2)**: with connectivity, the `DemandForecastSyncDelegate` pulls forecasts into the
  read-only mirror; product demand sparklines appear. Go offline with an empty mirror → a local **EWMA**
  estimate still renders (FR-019), never an empty view.
- **NL Q&A (P3)**: in the dashboard's NL panel ask "total sales this month", "top 5 products",
  "how many customers". Common questions resolve to a **one-tap KPI tile** (deterministic, offline,
  model-independent); free-form questions fall through to the agent **SafeQuery** path (needs a chat
  model loaded, RAM ≥ 3 GB). Verify offline answers match the corresponding tile (SC-008) and that an
  unanswerable/unsafe question returns a clear "couldn't answer that" (FR-023).
- **Dashboard config (P3)**: add/remove/reorder tiles; the layout persists as `StoreSetting`
  (`module_code='analytics'`) and appears on a second device after sync (SC-009 / FR-024).

---

## E. Acceptance mapping

| Check | Spec ref |
|---|---|
| Offline P1 tiles render correct + fast | SC-001, FR-009 |
| Period switch recomputes | SC-002, FR-002 |
| Midnight sale on correct business day | SC-003, FR-008 |
| Tiles reconcile with records; device == server | SC-004, FR-027 |
| GST intra/inter reconciles to invoices | SC-006, FR-006 |
| Forecast beats naïve baseline | SC-007, FR-015/016 |
| NL answers match tiles offline | SC-008, FR-020/021 |
| Layout syncs across devices | SC-009, FR-024 |
| Export localized & fast | SC-010, FR-012 |
