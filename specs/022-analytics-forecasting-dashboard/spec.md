# Feature Specification: Analytics & Forecasting Dashboard

**Feature Branch**: `022-analytics-forecasting-dashboard`  
**Created**: 2026-06-27  
**Status**: Draft  
**Input**: User description: "Give a retail/wholesale owner a business-intelligence dashboard — sales, collections & aging, top products/customers, GST summary, inventory turns/low-stock — plus a simple AI demand forecast and reorder signal, all working offline on mobile and answerable in natural language through the existing on-device assistant."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See the business at a glance, offline (Priority: P1)

A shop owner opens the app on the sales floor — often with no connectivity — and wants an immediate read
on how the business is doing: today's and this month's sales, how much money is still owed and how
overdue it is, which products and customers drive the most revenue, the GST collected for the period,
and the value and health of current stock. They can switch the period (day / week / month), and export
the numbers to share with their accountant.

**Why this priority**: This is the core promise of the feature and the minimum that delivers standalone
value. Owners make daily decisions (what to restock, who to chase for payment, how much GST to set
aside) from exactly these numbers. It must work with no network because field sales and many small shops
operate on intermittent connectivity.

**Independent Test**: With the device in airplane mode after an initial data sync, open the dashboard and
confirm every tile (sales, collections & aging, top products, top customers, GST summary, inventory
value / low-stock / turns) renders correct figures for the selected period, that switching day/week/month
re-computes them, and that an export of the visible figures can be produced — all without connectivity.

**Acceptance Scenarios**:

1. **Given** the device has synced invoices, orders, payments and inventory and is then offline,
   **When** the owner opens the dashboard for "This Month", **Then** sales (gross/net, invoice count,
   average order value), amount collected, total outstanding with aging buckets, top products, top
   customers, the GST summary, and inventory value / low-stock count / inventory turns all display within
   a perceived-instant time, with a "last synced" freshness indicator.
2. **Given** the dashboard is showing "This Month", **When** the owner switches the period to "Today" or
   "This Week", **Then** every tile re-computes for the new period in the workspace's business timezone.
3. **Given** a sale was recorded at 11:30 PM in the workspace's business timezone, **When** the owner
   views "Today", **Then** that sale is counted on the correct business day regardless of the device's
   own timezone.
4. **Given** the owner is viewing the dashboard, **When** they choose to export, **Then** a shareable
   document of the visible figures is produced with money shown in the workspace currency and dates in the
   workspace format.
5. **Given** the requested period extends further back than the data held on the device, **When** the
   device is online, **Then** the dashboard fills the older portion from the server; **When** offline,
   **Then** it clearly indicates the figures cover only locally available history.

---

### User Story 2 - Know what to reorder before stock runs out (Priority: P2)

The owner wants a forward-looking signal: for each product, roughly how much demand to expect in the
coming period, and which items are trending toward a stock-out so they can reorder in time.

**Why this priority**: Forecasting turns the dashboard from a rear-view mirror into a planning tool and
directly reduces lost sales and dead stock. It builds on the P1 sales history but is independently
valuable and can ship after the MVP.

**Independent Test**: For a product with several periods of sales history, confirm the dashboard shows an
expected-demand figure and trend, that products at risk of stock-out are surfaced as reorder candidates,
and that an offline estimate still appears when no server forecast is available.

**Acceptance Scenarios**:

1. **Given** a product has a meaningful sales history, **When** the owner opens its forecast, **Then** an
   expected-demand figure for the upcoming period and a short trend are shown.
2. **Given** a product has only a little history, **When** a forecast is requested, **Then** the system
   still returns a reasonable estimate using a simpler method and indicates lower confidence.
3. **Given** projected demand will exhaust available stock within the lead time, **When** the owner views
   inventory or the reorder list, **Then** that product is flagged as a reorder candidate.
4. **Given** the device is offline with no server forecast cached, **When** the owner opens a product's
   demand view, **Then** a locally computed estimate is shown so the screen is never empty.

---

### User Story 3 - Ask the business questions in plain language (Priority: P3)

The owner types or speaks a question — "how much did I sell this month?", "top 5 products", "how many
customers do I have?" — and gets an immediate answer, on-device, without building a report. Common
questions resolve to a one-tap dashboard tile; less common ones are answered from the local data. The
owner can also rearrange which tiles appear on their dashboard.

**Why this priority**: Natural-language access and personalization lower the skill barrier and increase
engagement, but the dashboard and forecast deliver value without them, so this is the last slice.

**Independent Test**: Ask a set of common business questions and confirm each returns a correct answer
from local data offline (mapped to a tile where one exists), and confirm the owner can add, remove and
reorder dashboard tiles and have that layout persist and follow them across devices.

**Acceptance Scenarios**:

1. **Given** the device is offline, **When** the owner asks "total sales this month", **Then** the
   correct figure is returned from local data, matching the dashboard's sales tile.
2. **Given** a question maps to a named metric (e.g. "top products"), **When** it is asked, **Then** the
   answer is produced deterministically as the corresponding tile rather than depending on a model.
3. **Given** the owner customizes the dashboard, **When** they add, remove or reorder tiles, **Then** the
   layout is saved as a workspace setting and is reflected on their other devices after sync.
4. **Given** a request needs historical depth beyond the device's data, **When** the owner is online,
   **Then** a server-side export covering the full period can be produced.

---

### Edge Cases

- **No data yet**: A brand-new workspace with no invoices/payments/inventory shows empty-state tiles with
  guidance, not errors or misleading zeros presented as insights.
- **Backdated or edited documents**: Editing or backdating an invoice/payment must be reflected in the
  affected period's figures, not only in the current day's.
- **Voided / cancelled / draft documents**: Only finalized documents count toward sales, GST and
  collections; drafts and cancelled documents are excluded.
- **Refunds / credit notes / partial payments**: Collections, outstanding and aging reflect partial
  payments and reversals rather than treating every invoice as fully paid or fully open.
- **Timezone boundary**: Transactions near midnight bucket to the correct business day per the workspace
  timezone, even across daylight-saving changes, and even when two devices are in different zones.
- **Sparse or seasonal history**: Forecasting degrades gracefully (simpler method, lower confidence)
  rather than producing wild numbers when history is short or highly seasonal.
- **Mixed intra-/inter-state tax**: The GST summary correctly separates the intra-state split from the
  inter-state amount per the place of supply recorded on each invoice.
- **Large history**: A workspace with years of data still renders period tiles quickly because each tile
  is bounded to its selected period.
- **Natural-language miss**: A question the assistant cannot safely answer returns a clear "couldn't
  answer that" message instead of a wrong or unsafe result.
- **Offline with no chat capability**: Free-form questions that require the on-device assistant are
  unavailable on low-resource devices, but the one-tap metric tiles still answer the common questions.

## Requirements *(mandatory)*

### Functional Requirements

#### Dashboard & KPIs (P1)

- **FR-001**: The system MUST present a business dashboard summarizing sales, collections & aging, top
  products, top customers, GST, and inventory health for a selected period.
- **FR-002**: Users MUST be able to select the reporting period as day, week, or month, and the system
  MUST recompute every metric for the selected period.
- **FR-003**: The system MUST compute and display **sales** metrics: gross and net sales, transaction
  count, and average order value, from finalized sales documents only.
- **FR-004**: The system MUST compute and display **collections & aging**: amount collected in the
  period, total outstanding, and outstanding split into aging buckets by overdue age.
- **FR-005**: The system MUST compute and display **top products** and **top customers** by revenue for
  the period, limited to a configurable top-N.
- **FR-006**: The system MUST compute and display a **GST summary** for the period, separating the
  intra-state tax split from the inter-state amount and grouping by tax rate, derived from the tax
  recorded on each finalized invoice.
- **FR-007**: The system MUST compute and display **inventory** metrics: total stock value, count of
  low-stock items, and inventory turns.
- **FR-008**: All period bucketing (day/week/month) MUST use the workspace's configured business
  timezone, never the device's or server's local timezone; stored data remains in coordinated universal
  time.
- **FR-009**: On mobile, every P1 metric MUST be computable and displayable with no network connection
  from data already on the device, and the dashboard MUST never block on connectivity.
- **FR-010**: The dashboard MUST show a freshness indicator (e.g. "last synced") so users understand how
  current the figures are.
- **FR-011**: For periods extending beyond the history available on the device, the system MUST fill the
  older portion from the server when online, and clearly indicate reduced coverage when offline.
- **FR-012**: Users MUST be able to export the displayed figures to a shareable document, with money
  formatted in the workspace currency and dates in the workspace format.
- **FR-013**: Metrics MUST exclude drafts, cancelled and voided documents, and MUST reflect refunds,
  credit notes and partial payments.
- **FR-014**: Editing or backdating a source document MUST be reflected in the figures for the affected
  period, not only the current day.

#### Demand Forecasting & Reorder Signal (P2)

- **FR-015**: The system MUST produce a per-product demand forecast (expected demand for an upcoming
  period plus a confidence indication) from historical sales.
- **FR-016**: The forecast MUST capture trend and seasonal patterns where enough history exists, and MUST
  fall back to a simpler estimate with lower stated confidence when history is sparse.
- **FR-017**: The system MUST surface products trending toward a stock-out within their lead time as
  reorder candidates.
- **FR-018**: The system MUST make the demand signal (expected demand and its variability) available to
  the replenishment and inventory capabilities without the analytics capability itself altering inventory
  levels or reorder settings.
- **FR-019**: When no server forecast is available offline, the system MUST display a locally computed
  demand estimate so the demand view is never empty.

#### Natural-Language Q&A, Configuration & Server Export (P3)

- **FR-020**: Users MUST be able to ask common business questions in natural language and receive
  answers computed from their data on-device.
- **FR-021**: Questions that correspond to a named metric MUST be answered deterministically via the
  matching dashboard tile, independent of any model.
- **FR-022**: Natural-language answering MUST operate within read-only, safety-constrained access to the
  user's own workspace data and MUST never modify data or read another workspace's data.
- **FR-023**: When a question cannot be answered safely, the system MUST return a clear inability message
  rather than an incorrect or unsafe answer.
- **FR-024**: Users MUST be able to customize their dashboard by adding, removing and reordering tiles,
  and that layout MUST persist as a workspace setting and synchronize across the user's devices.
- **FR-025**: The system MUST offer a server-side export for periods that exceed the device's local
  history.

#### Cross-cutting

- **FR-026**: All figures MUST be scoped strictly to the active workspace; no metric may include or leak
  another workspace's data.
- **FR-027**: The same metric definition MUST yield consistent results whether computed on-device or by
  the server for the same period and source data.

### Key Entities *(include if feature involves data)*

- **Period KPI Summary**: A pre-aggregated record of a workspace's metrics for one business date and
  metric group (sales, collections, aging, top product, top customer, GST, inventory), with optional
  dimension keys (product, customer, tax rate) and measures (gross / net / tax amounts, counts,
  quantities). Rebuilt incrementally from business activity and reconcilable from source records.
- **Metric Definition**: A declarative description of a single KPI — its identity, group, unit, how it is
  aggregated, which source it derives from, and which periods it supports — that drives the dashboard,
  the natural-language mapping, and consistency between device and server.
- **Demand Forecast**: A per-product, per-period projection of expected demand with a variability measure
  and the method used, generated centrally and made available read-only to clients.
- **Dashboard Layout (workspace setting)**: The ordered set of tiles a workspace has chosen to display,
  with each tile's metric, period preset and optional filter.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: With the device offline, every P1 dashboard tile renders correct figures for the selected
  period and each tile appears in under one second on typical small-business data volumes.
- **SC-002**: Switching the reporting period (day → week → month) recomputes all tiles in under one
  second.
- **SC-003**: For 100% of transactions recorded near a day boundary, the figure is attributed to the
  correct business day in the workspace timezone, including across daylight-saving transitions and across
  devices in different timezones.
- **SC-004**: Dashboard figures reconcile exactly with the underlying records for the same period — for
  example, the sum of per-invoice sales equals the sales tile, and on-device and server values agree to
  the last currency unit.
- **SC-005**: An owner can go from opening the app to reading their key numbers (sales, outstanding, top
  products) in under 10 seconds with no manual configuration.
- **SC-006**: The GST summary's intra-state and inter-state amounts reconcile to the tax recorded on the
  underlying finalized invoices with zero discrepancy.
- **SC-007**: For products with at least two seasonal cycles of history, the demand forecast's error
  (e.g. mean absolute percentage error on held-out periods) is materially lower than a naïve
  "same as last period" baseline.
- **SC-008**: For at least the most common business questions (e.g. period sales total, customer count,
  top products), natural-language queries return the correct answer offline, matching the corresponding
  dashboard tile.
- **SC-009**: A custom dashboard layout set on one device is reflected on the user's other devices after
  a normal sync, with no layout data loss.
- **SC-010**: Users can export the visible figures to a shareable document in under 5 seconds, with money
  and dates correctly localized to the workspace.

## Assumptions

- The platform already records the business activity these metrics derive from (finalized invoices,
  orders, payments/ledger, inventory movements) and already synchronizes that data to the device, so the
  dashboard reads existing data rather than introducing new data capture.
- "Business timezone", currency and date format come from the workspace's business profile already
  available to clients.
- Heavy forecasting computation runs centrally; devices perform only simple, deterministic statistics
  offline.
- Natural-language answering reuses the existing on-device assistant's safety-constrained data-query
  capability and therefore requires a device capable of running that assistant for free-form questions;
  one-tap metric tiles remain available regardless.
- The replenishment capability (separate feature) and inventory capability own reorder decisions and
  stock levels respectively; this feature only measures and signals.
- "Small-business data volumes" means up to roughly thousands of invoices/orders and thousands of
  products/customers per workspace for the stated performance targets.
- The web (browser) version of this dashboard is a tracked follow-up and is out of scope for this
  specification.

## Out of Scope

- A web/browser dashboard (tracked separately).
- Advanced ML forecasting methods beyond trend/seasonal smoothing (candidates for a later phase).
- Automatic creation of purchase orders or automatic changes to reorder levels/stock (owned by
  replenishment and inventory).
- Cross-workspace or organization-wide consolidated analytics.
