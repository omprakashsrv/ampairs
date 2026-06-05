# Feature Specification: Store Operations — Order & Invoice with GST

**Feature Branch**: `010-store-ops-order-invoice`
**Created**: 2026-06-05
**Status**: Draft
**Input**: Build a proper store-operations flow for **store staff** on the KMP app (Android, iOS,
**and Desktop**) to create **orders and invoices** with correct **Indian GST** tax calculation,
using **today's product pricing** (`sellingPrice`/`dp`/`mrp` — the pricing layer comes later). Staff
must be able to add line items from the catalog, **create a new product inline**, and generate an
order or an invoice **independently**, or **convert an order into an invoice**. The existing
order/invoice implementation is unsound and is being **cleanly rebuilt** to the project's
offline-sync + Metro DI + MVI rules.
**Program context**: This is the **immediate first feature** of the commerce program
(`specs/000-commerce-program/PLAN.md`); the pricing engine (`specs/009-commerce-pricing`) is
**re-slotted to after** this and plugs into the price-resolution seam defined here.
**Repos**: `ampairs-app` (primary — app rebuild), `ampairs` (backend — add bulk sync endpoints).

## Scope decisions (confirmed 2026-06-05)

- **Documents**: support **both** Order and Invoice. Each can be created **independently**, and an
  Invoice can also be **generated from an existing Order**.
- **Tax**: **Full Indian GST** — CGST+SGST for intra-state, IGST for inter-state, decided by
  place-of-supply (buyer vs seller state), using the workspace's subscribed HSN/SAC tax codes.
- **Products**: line-item entry can **create a new product inline** (not only pick existing).
- **Rebuild posture**: **clean rebuild** of the create flow, sync layer, and tax wiring — keep the
  reusable parts (entity schema, `TaxCalculationEngine`, list/search/paging) per the audit.
- **Money**: keep the **existing `Double`** price/tax fields for this feature (today's pricing model);
  the `Money(minorUnits,currency)` migration rides with the pricing feature, not this one.

## Current-state findings that shape this spec (from the code audit)

- **Tax engine exists but is never wired**: `feature/tax/.../TaxCalculationEngine` does CGST/SGST/IGST
  splits by jurisdiction, but `OrderItem.taxInfos` is always empty, `OrderViewModel.updateTaxInfos()`
  is a `TODO` stub, and orders save with `totalTax = 0.0`.
- **`TaxSpec` logic is inverted** (`states different → INTRA`); must be fixed to `→ INTER`.
- **Products can't actually be added**: `OrderScreen`'s `productPickerSlot` callback is ignored; no
  `addProduct()` on the ViewModel.
- **Offline-first violated**: `OrderRepository`/`InvoiceRepository` call the API directly;
  `OrderSyncDelegate`/`InvoiceSyncDelegate` are no-op stubs (`SyncEntity.ORDER`/`INVOICE` already
  exist in the enum but aren't wired with `@SyncEntityKey`).
- **Backend tax is client-side by contract**: order/invoice controllers store `taxInfos`/`totalTax`
  as supplied — so the app is the tax-calculation authority. `GstRuleTemplateService` already encodes
  the INTRA/INTER `componentComposition`.
- **Backend sync endpoints missing**: only single-record `POST /order/v1/orders` and a
  non-paginated `GET ?last_updated=` (fixed 50, no `hasNext`). The `product` module's `/sync`
  (bulk-upsert `POST` + `PageResponse` `GET`) is the pattern to mirror.
- **Order→Invoice conversion exists server-side**: `order.toInvoice()`,
  `POST /order/v1/orders/create-invoice`, with `Invoice.orderRefId` ↔ `Order.invoiceRefId`.

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Staff creates an order with correct GST (Priority: P1)

A store staffer opens the order screen on a tablet, picks a buyer customer, adds three catalog
products with quantities, and the screen shows per-line and order-level GST broken into CGST+SGST
(buyer in the same state) or IGST (different state), with a correct grand total. They save; it works
fully offline and syncs when online.

**Why this priority**: This is the core of store operations. Without correct, automatic GST on a
saved order, the feature delivers nothing usable.

**Independent Test**: Offline, create an order with a buyer in the seller's state, add a product with
HSN tax code at 18%, qty 2 @ ₹100 → line shows CGST ₹9 + SGST ₹9 (₹18 tax), line total ₹218, order
total reflects it; save succeeds offline; on reconnect the order pushes and returns an order number.

**Acceptance Scenarios**:

1. **Given** a buyer whose GST state code equals the seller's, **When** a line with an 18% HSN code is
   added at qty 2 @ ₹100, **Then** the line shows CGST 9% (₹9) + SGST 9% (₹9), taxable ₹200, line
   total ₹218.
2. **Given** a buyer in a **different** state, **When** the same line is added, **Then** the line shows
   IGST 18% (₹18) — not CGST/SGST — proving the INTRA/INTER decision is correct (the previously
   inverted logic is fixed).
3. **Given** several lines, **When** the order is viewed, **Then** order-level `taxInfos` aggregate the
   component values across lines and `totalTax`/`totalAmount` match the sum of lines.
4. **Given** no connectivity, **When** the order is saved, **Then** it persists locally
   (`synced = false`), is flagged `PENDING_PUSH`, and appears in the list immediately.
5. **Given** the saved order and restored connectivity, **When** sync runs, **Then** the order is
   bulk-pushed via the delegate, gets a server `orderNumber`, and is marked `synced = true` with no
   duplicate created on retry.

---

### User Story 2 — Staff creates an invoice independently (POS-style) (Priority: P1)

A staffer makes a counter sale directly as an invoice (no prior order): pick/identify the customer (or
walk-in), add line items, GST computes the same way, save → an invoice with an invoice number. Works
offline.

**Why this priority**: Many store sales are direct invoices with no order step; this must stand alone,
not be a side effect of orders.

**Independent Test**: Offline, create an invoice for a walk-in customer with two GST lines; save; the
invoice persists locally and later pushes to `/invoice/v1/...` returning an invoice number.

**Acceptance Scenarios**:

1. **Given** the invoice screen, **When** a staffer adds lines and saves with no existing order,
   **Then** an independent invoice is created (no `orderRefId`) with correct GST and an invoice number.
2. **Given** a walk-in (no customer record), **When** an invoice is saved, **Then** it stores
   `isWalkIn = true` with captured name/phone and still computes intra-state GST against the seller
   state by default.
3. **Given** an invoice created offline, **When** sync runs, **Then** it bulk-pushes and reconciles
   its server number without duplication.

---

### User Story 3 — Staff converts an order into an invoice (Priority: P2)

From a saved order, a staffer taps "Create Invoice". An invoice is generated from the order's lines,
tax breakdown, discounts, and customer/GST details; the order is linked to the invoice and vice versa.

**Why this priority**: A common follow-on, but it depends on Stories 1–2 existing first.

**Independent Test**: Save an order, tap "Create Invoice", and verify a new invoice with the same
lines/tax exists, `Invoice.orderRefId` = order uid, `Order.invoiceRefId` = invoice uid, and converting
the same order again does not create a second invoice.

**Acceptance Scenarios**:

1. **Given** a saved order with GST lines, **When** "Create Invoice" is tapped, **Then** an invoice is
   built locally from the order (lines, `taxInfos`, discounts, addresses, GST fields copied) and both
   records cross-link.
2. **Given** an order already converted (`invoiceRefId` set), **When** "Create Invoice" is tapped
   again, **Then** the existing invoice opens — no duplicate is created.
3. **Given** the conversion happened offline, **When** sync runs, **Then** both order and invoice push
   with their cross-links intact.

---

### User Story 4 — Staff adds a new product inline during entry (Priority: P2)

While building an order/invoice, a staffer hits a product not yet in the catalog. From the line-item
picker they create it inline (name, price, HSN tax code, unit), and it is immediately added as a line
and saved to the catalog.

**Why this priority**: The user explicitly needs "add products and do the ordering"; reduces friction,
but the order/invoice flow can ship without it first.

**Independent Test**: In the product picker, choose "New product", enter name + ₹ price + HSN code,
confirm → the product is created in the catalog (offline, `PENDING_PUSH`) and added as a line with GST
computed from its HSN code.

**Acceptance Scenarios**:

1. **Given** the picker, **When** the staffer creates a new product inline, **Then** a product UID is
   generated in the ViewModel (`UidGenerator.generateUid("PRD")`), saved via the product repository
   (local + `markPendingPush(PRODUCT)`), and added as a line item.
2. **Given** a product with **variants**, **When** the staffer picks it, **Then** they select a
   specific variant and the line records the variant (sku/attributes) and the variant's price.
3. **Given** an inline-created product, **When** sync runs, **Then** both the new product and the
   order/invoice referencing it push in dependency order (product before order).

---

### Edge Cases

- **Missing GST number / unregistered buyer** → default to **intra-state** (CGST+SGST) against the
  seller's state; surface a hint that place-of-supply was assumed.
- **Product with no/blank tax code** → line is treated as 0% (exempt), flagged visibly; never silently
  drops tax.
- **Tax-inclusive vs exclusive pricing** → MVP treats catalog `sellingPrice` as **tax-exclusive**
  (taxable base); inclusive pricing is out of scope (note for later).
- **Quantity/price edits** re-run tax calc reactively for that line and re-aggregate the totals.
- **Rounding** → round each component to 2 decimals; document half-up; line total = taxable + Σ
  components so displayed totals always reconcile.
- **Discount + tax order** → apply line discount to the taxable base **before** computing tax.
- **Place-of-supply state** derived from the first 2 digits of the buyer/seller GSTIN; if absent, fall
  back to the customer's address state, then seller state.
- **Sync conflict** → server `updatedAt` authoritative for synced rows; local unsynced edits win on
  pull (per `/offline-sync`).

## Requirements *(mandatory)*

### Functional Requirements — App (`ampairs-app`)

- **FR-001**: Provide a clean order create/edit flow (mobile + desktop adaptive list-detail) to select
  a customer, add/edit/remove line items, set quantity and (overridable) unit price from
  `product.sellingPrice`, and save.
- **FR-002**: Provide an equivalent independent invoice create/edit flow (walk-in supported).
- **FR-003**: **Wire `TaxCalculationEngine` into both flows** — on every line add/qty/price/customer
  change, load the product's HSN tax code, resolve the workspace `TaxRule.componentComposition`, pick
  the INTRA/INTER scenario from place-of-supply, and populate `OrderItem.taxInfos`/`totalTax`; then
  aggregate order/invoice `taxInfos`/`totalTax`/`totalAmount`. **Replace the `updateTaxInfos()` stub.**
- **FR-004**: **Fix the inverted `TaxSpec`** — different states ⇒ `INTER` (IGST), same state ⇒ `INTRA`
  (CGST+SGST).
- **FR-005**: Make the product picker functional — wire the picker callback to an `addProduct()`
  intent; support **variant selection**; allow **inline new-product creation** (FR-009).
- **FR-006**: Rebuild repositories to be **local-only**: write to Room (`synced = false`) and
  `syncStateDao.markPendingPush(ORDER|INVOICE, now)`. **Remove all direct API calls from
  repositories.**
- **FR-007**: Implement real `OrderSyncDelegate` and `InvoiceSyncDelegate`
  (`@ContributesIntoMap(WorkspaceScope::class)` + `@SyncEntityKey`): bulk push of unsynced rows,
  batched incremental pull with `updatedAt` cursor + `hasNext`, soft-delete handling, and
  `dependsOn`/`pushDependencies` = `[CUSTOMER, PRODUCT]` (and `ORDER` for invoice conversion ordering).
- **FR-008**: Order→Invoice conversion is a **client-side** operation that builds an Invoice from the
  Order locally, cross-links `orderRefId`/`invoiceRefId`, marks both `PENDING_PUSH`, and is idempotent
  (no duplicate if already converted).
- **FR-009**: Inline product create generates the UID in the ViewModel, persists via the product
  repository (local + `markPendingPush(PRODUCT)`), then adds the line.
- **FR-010**: Generate order/invoice/item UIDs in the **ViewModel** via `UidGenerator.generateUid`
  (`ORD`/`INV`/`OIT`/etc.) — not in entity `init {}`.
- **FR-011**: Move `OrderApi`/`InvoiceApi` bindings to **`WorkspaceScope`** (they carry
  `X-Workspace-ID`).
- **FR-012**: Remove/retire the duplicate `kotlin-disabled/invoice/` and converge on a single
  `feature/invoice` aligned 1:1 with the rebuilt order flow.
- **FR-013**: All flows function on **Desktop (JVM)** and **mobile**; reuse/extend the adaptive
  list-detail pane.

### Functional Requirements — Backend (`ampairs`)

- **FR-B01**: Add a **bulk-upsert** endpoint for orders accepting `List<OrderUpdateRequest>`
  (mirror `product`'s batch `POST`), preserving client UIDs and per-line `taxInfos`/`totalTax` as
  supplied (no server recompute).
- **FR-B02**: Add a **paginated incremental sync** GET (e.g. `/order/v1/orders/sync?last_sync=&page=&size=`)
  returning `PageResponse` with `hasNext` — mirroring `ProductController.getProductsSync`.
- **FR-B03**: Add the equivalent **bulk-upsert** + **paginated sync** endpoints for invoices.
- **FR-B04**: Ensure soft-deleted/cancelled rows are included in pull with a status the client can act
  on; keep `OrderCheckpointContributor`/`InvoiceCheckpointContributor` accurate.
- **FR-B05**: Keep order↔invoice cross-links (`orderRefId`/`invoiceRefId`) intact on bulk upsert so a
  client-converted pair reconciles correctly.
- **FR-B06**: No tax recomputation server-side (client is authority); validate `taxInfos` totals are
  internally consistent and reject malformed payloads.

### Non-Functional / Constraints

- **NFR-001**: App offline-first per `/offline-sync`; Metro DI per `/metro-di`
  (`@SingleIn(WorkspaceScope::class)` DBs, `@Inject` repos, `@ContributesIntoMap` VMs); Compose per
  `/cmp-practices` (MVI, `collectAsStateWithLifecycle`, `stringResource` only). Compile **all three
  targets** before done.
- **NFR-002**: Tax math is deterministic and reconciles to the displayed totals; covered by unit tests
  for intra/inter, multi-rate, discount-before-tax, and exempt cases.
- **NFR-003**: Backend `Instant` timestamps, DTO isolation, `ApiResponse<T>`, no try/catch in
  controllers, tenant context at controller level; Flyway in **both** `mysql/` and `postgresql/` —
  next versions **order V1.0.23**, **invoice V1.0.12** (only if schema changes; endpoint additions may
  need none — record in `NO_MIGRATION_NEEDED.md`).
- **NFR-004**: Money stays `Double` for this feature (today's model); do not introduce the `Money`
  type here.

### Key Entities (reuse existing; no schema redesign)

- **Order / OrderItem** (`feature/order` + backend `customer_order`/`order_item`): reuse current
  fields (`taxInfos` JSON, `totalTax`, `placeOfSupply`, `fromCustomerGst`, `toCustomerGst`,
  `invoiceRefId`, line price snapshots). Add **variant reference** fields to the line
  (`variantSku`/attributes) if not present.
- **Invoice / InvoiceItem** (`feature/invoice` + backend `invoice`/`invoice_item`): mirror order;
  `orderRefId` link.
- **TaxInfo** (existing, both repos): `{ id, name, percentage, taxSpec, value, formattedName }` — the
  per-component breakdown stored as JSON.
- **TaxRule / componentComposition** (tax module, synced to app): source of the INTRA/INTER component
  rates the engine reads.
- **SyncEntity.ORDER / SyncEntity.INVOICE**: already present; wire the delegates.

## Success Criteria *(mandatory)*

- **SC-001**: A staffer can create, GST-calculate, and save an **order** fully offline on mobile and
  desktop, and it syncs (bulk push + pull) without duplicates.
- **SC-002**: A staffer can create an independent **invoice** the same way.
- **SC-003**: Intra-state lines produce CGST+SGST and inter-state lines produce IGST, both reconciling
  to the grand total (regression-proven against the previously-inverted logic).
- **SC-004**: Converting an order to an invoice copies lines/tax/discounts, cross-links both, and is
  idempotent.
- **SC-005**: A new product created inline is added as a line and both it and the document sync in
  dependency order.
- **SC-006**: No repository makes a network call; `OrderSyncDelegate`/`InvoiceSyncDelegate` own all
  order/invoice API traffic; all three app targets compile.
- **SC-007**: Existing list/search/paging and saved-order display continue to work (no regression).

## Out of Scope (this feature)

- The **pricing engine** (price lists, channels, MOQ, tiers) — feature 009; this uses
  `product.sellingPrice` directly and leaves a clean seam where price resolution will plug in.
- `Money(minorUnits,currency)` migration and multi-currency — rides with pricing/go-global.
- Payments, shipping, promotions (features 011–013).
- Tax-**inclusive** pricing, e-invoicing/IRN/e-way-bill, and PDF generation (note for a follow-up).
- Storefront/ecom changes — this is **store-staff (internal)** operations, not the B2C storefront.

## Dependencies & Assumptions

- Assumes `feature/tax` `TaxCalculationEngine` and the workspace's subscribed `TaxRule`s (with
  `componentComposition`) are available/synced on the app.
- Assumes `customer` and `product` modules as they exist (customer GST fields; product
  `sellingPrice`/`dp`/`mrp`/`taxCode`/variants).
- Assumes backend order/invoice modules' single-record + conversion endpoints remain; this feature
  **adds** bulk/paginated sync endpoints alongside them.

---

*Next steps*: run `/speckit.clarify` (confirm edge-case defaults: tax-exclusive pricing, intra-state
fallback, rounding half-up), then `/speckit.plan`, `/speckit.tasks`, `/speckit.analyze`, and stop for
review before `/speckit.implement`. Develop on `claude/blissful-goldberg-iuzI6` in both repos; no PR
unless requested.
