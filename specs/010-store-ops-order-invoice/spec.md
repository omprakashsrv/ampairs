# Feature Specification: Store Operations — Order & Invoice with GST

**Feature Branch**: `010-store-ops-order-invoice`
**Created**: 2026-06-05
**Status**: Planned (clarified C1–C5; see `plan.md`, `data-model.md`, `research.md`, `contracts/`, `quickstart.md`)
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
  place-of-supply (buyer vs seller state), using the workspace's subscribed HSN/SAC tax codes. Price
  is **tax-exclusive or tax-inclusive per a document-level toggle** (see Clarifications C1).
- **Products**: line-item entry can **create a new product inline** (not only pick existing).
- **Units (UoM)**: each line item records the **unit it is transacted in**. The staffer can transact a
  product in its **base unit or any defined derived unit** (e.g. BOX where 1 BOX = 12 PCS); the line
  stores the chosen `unitId`, the entered quantity, and the derived **base-unit quantity** (for
  stock/inventory), with price scaled by the conversion. Uses the existing `UnitConversionEngine`.
- **Discounts**: support **both** a **per-line discount** and an **overall (document-level)
  discount**, each as a **percentage or a flat amount**. The overall discount's interaction with GST
  (pre-tax apportioned vs post-tax reduction) is **a business-level configuration; support both**
  (see Clarifications C2). The line discount is always pre-tax on its own line.
- **Rebuild posture**: **clean rebuild** of the create flow, sync layer, and tax wiring — keep the
  reusable parts (entity schema, `TaxCalculationEngine`, list/search/paging) per the audit.
- **Money**: keep the **existing `Double`** price/tax fields for this feature (today's pricing model);
  the `Money(minorUnits,currency)` migration rides with the pricing feature, not this one.

## Clarifications

### Session 2026-06-05

- **C1 — Tax basis (inclusive vs exclusive): toggle per document.** Each order/invoice carries a
  `priceMode` (`TAX_EXCLUSIVE` | `TAX_INCLUSIVE`). Exclusive → `sellingPrice` is the taxable base and
  GST is added on top. Inclusive → `sellingPrice` already contains GST and the taxable base + tax are
  back-calculated (`taxable = inclusivePrice / (1 + rate)`). The default mode comes from a business
  setting; staff may switch it on a document. Drives FR-003.
- **C2 — Overall-discount vs GST: business-configurable, support both.** A business setting
  (`overallDiscountMode = PRE_TAX_APPORTIONED` | `POST_TAX_REDUCTION`) selects behavior.
  `PRE_TAX_APPORTIONED` (GST-compliant) apportions the document discount across lines proportional to
  taxable value and recomputes per-line GST; `POST_TAX_REDUCTION` computes full GST then subtracts the
  discount from the grand total (per-HSN tax unaffected). Drives FR-015. Line-level discount is always
  pre-tax on its own line regardless of this setting.
- **C3 — Line unit-price override: allowed.** Staff may edit the auto-filled (unit-scaled)
  `sellingPrice` on a line; the entered price becomes the snapshot and GST recomputes on it. Drives
  FR-016.
- **C4 — Invoice numbering: client-assigned sequential, offline-capable.** The client assigns a GST
  invoice number at save time (so an offline invoice is legally numbered and immediately printable),
  not the server-on-push behavior of today. Drives FR-017.
- **C5 — Numbering collisions across offline devices: per-series prefix.** Each device/counter (and,
  where configured, branch/financial-year) owns its **own invoice series with a distinct prefix**,
  each strictly sequential (GST permits multiple series provided each is consecutive and unique).
  This eliminates offline collisions without requiring online block reservation; the server validates
  series+number uniqueness on push. Drives FR-017.

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
- **Discount model exists, untyped**: `Discount(percent: Double, value: Double)` is stored as a
  `List<Discount>` JSON on **both** `Order`/`OrderItem` (and invoice equivalents), and the old code
  aggregates them — but it carries no percent-vs-flat flag and its interaction with the (previously
  dead) tax math is unspecified. The rebuild must define discount→taxable→GST ordering precisely and
  apportion the overall discount across lines pre-tax.
- **Units exist but are unused on lines**: `Unit` (with `decimalPlaces`), product-scoped
  `UnitConversion` (`baseUnitId`/`derivedUnitId`/`multiplier`), the app `UnitConversionEngine`
  (direct + inverse) and backend `UnitConversionService` (+ `POST /unit/v1/conversions/convert`) all
  exist, and `Product` carries `baseUnitId` + `unitConversions` — but **`OrderItem`/`InvoiceItem`
  (Room and backend) have NO unit field**: `quantity` is a bare `Double` assumed to be the base unit,
  and the conversion engine is **never called** in the order/invoice path. Product prices
  (`sellingPrice`/`mrp`/`dp`) are expressed **per base unit**.

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

### User Story 5 — Staff transacts a line in any unit, with conversion (Priority: P1)

A staffer adds a product whose base unit is PCS but sells it as a BOX (1 BOX = 12 PCS). They pick
"BOX" as the line's unit and enter quantity 5; the line records 5 BOX, derives 60 PCS as the
base-unit quantity, scales the unit price from the per-PCS price, and computes GST on the resulting
taxable amount. Quantity input respects the unit's allowed decimal places.

**Why this priority**: Quantity without a unit is ambiguous and breaks both pricing and stock. Most
real store catalogs transact in multiple units, so this is integral to a correct line — not an add-on.

**Independent Test**: For a product priced ₹10/PCS with a 1 BOX = 12 PCS conversion, add a line of
5 BOX → unit price shows ₹120/BOX, taxable ₹600, base-unit quantity stored = 60 PCS; switch the unit
to PCS at qty 60 → identical taxable ₹600.

**Acceptance Scenarios**:

1. **Given** a product with a base unit and at least one derived-unit conversion, **When** the staffer
   opens the line's unit selector, **Then** they see the base unit plus all active derived units for
   that product.
2. **Given** the staffer picks a derived unit (BOX) and quantity 5 (1 BOX = 12 PCS), **When** the line
   computes, **Then** it stores `unitId = BOX`, `quantity = 5`, `baseQuantity = 60` (via
   `UnitConversionEngine`), and the unit price = per-base-unit price × 12.
3. **Given** a product with **no** conversions defined, **When** a line is added, **Then** it
   transacts in the base unit (multiplier 1, `baseQuantity = quantity`) with no error.
4. **Given** a unit whose `decimalPlaces = 0`, **When** the staffer tries to enter a fractional
   quantity, **Then** input is constrained to whole numbers; a `decimalPlaces = 3` unit allows up to
   3 decimals.
5. **Given** a line in a derived unit, **When** the order/invoice is saved and synced, **Then** the
   pushed line carries `unitId` and `baseQuantity` and they round-trip on pull.

---

### User Story 6 — Staff applies line and overall discounts, GST stays correct (Priority: P1)

A staffer gives one line a 10% discount and the whole bill a flat ₹100 off. Each line's taxable value
drops by its share of the discounts, GST is computed on the reduced taxable values, and the displayed
CGST/SGST/IGST and grand total all reconcile.

**Why this priority**: Discounts are routine in store sales and they directly change the taxable value
— getting the discount→tax ordering wrong produces non-compliant GST. This is core, not optional.

**Independent Test**: Two lines (₹500 @ 18%, ₹500 @ 18%), apply a flat ₹200 overall discount → each
line's taxable drops to ₹400, GST ₹72/line (CGST ₹36 + SGST ₹36 intra-state), grand total ₹944;
totals reconcile to taxable + tax.

**Acceptance Scenarios**:

1. **Given** a line of qty 2 @ ₹100 (18%), **When** a 10% line discount is applied, **Then** taxable =
   ₹180, GST ₹32.40, line total ₹212.40.
2. **Given** a multi-line bill, **When** an overall discount (percent or flat) is applied, **Then** it
   is **apportioned across lines proportional to each line's taxable value before tax**, and each
   line's GST is recomputed on its reduced taxable value.
3. **Given** both a percent and a flat discount option, **When** the staffer enters either, **Then**
   the `Discount` records which form was used (percent vs flat) and the resolved value, so the breakdown
   is reproducible.
4. **Given** discounts that would drive a line's taxable below zero, **When** computed, **Then** the
   line taxable floors at zero (no negative tax) and a warning is shown.
5. **Given** discounts applied, **When** the document is saved and synced, **Then** line and overall
   `discount` JSON round-trip and the server-stored totals match the client.

---

### Edge Cases

- **Discount + tax ordering** → line discount, then overall-discount apportionment, **then** tax —
  always on the net taxable value (never compute tax then discount).
- **Overall discount apportionment** (PRE_TAX_APPORTIONED mode) → distribute proportional to each
  line's pre-discount taxable value; assign any rounding remainder to the largest line so the sum of
  line discounts equals the entered overall discount exactly. (POST_TAX_REDUCTION mode skips
  apportionment and subtracts from the grand total.)
- **Tax-inclusive rounding** → when back-calculating taxable from an inclusive price, round the
  extracted taxable and each component so the components + taxable sum back to the inclusive line
  amount exactly.
- **Invoice series rollover** → a new financial year (or a new device) starts a fresh series; the
  sequence resets per series and the prefix disambiguates, so numbers never collide or appear to go
  backwards.
- **Percent vs flat** → both supported at each level; a percent is resolved to a value against the
  applicable base and the resolved value is stored alongside the percent for reproducibility.
- **Inverse conversion** (entering base when only a derived→base multiplier exists) → engine divides
  by the multiplier; guard against divide-by-zero.
- **Unit changed after quantity entered** → re-derive `baseQuantity`, rescale unit price, and re-run
  the line's GST reactively.
- **Inventory deduction** (out of scope to implement here) → must eventually read `baseQuantity`, not
  the transacted `quantity`; the field is added now so stock work later has the data.
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
  Honor the document `priceMode` (C1): in `TAX_EXCLUSIVE` add GST onto the price; in `TAX_INCLUSIVE`
  back-calculate taxable + GST out of the price (`taxable = price / (1 + Σrate)`). Persist `priceMode`
  on the order/invoice; its default comes from the business setting.
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
- **FR-014**: **Unit-of-measure on line items.** Add `unitId` (transacted unit) and `baseQuantity`
  (quantity in the product's base unit) to `OrderItem`/`InvoiceItem` (Room). On line entry, let the
  staffer pick the base unit or any active derived unit for that product; compute `baseQuantity` via
  `UnitConversionEngine.convertQuantity(...)`; scale the per-base-unit price to the chosen unit; and
  compute GST on the resulting taxable amount. Constrain quantity input to the unit's `decimalPlaces`.
  Default to the product's base unit (multiplier 1) when no conversion exists.
- **FR-015**: **Discounts at line and document level.** Support a per-line discount and an overall
  discount, each as **percent or flat**. The line discount always reduces its own line's taxable base
  pre-tax. The overall discount follows the business `overallDiscountMode` (C2): in
  `PRE_TAX_APPORTIONED`, apportion it across lines proportional to pre-discount line taxable value,
  then run GST on each line's net taxable value; in `POST_TAX_REDUCTION`, compute GST on line values
  then subtract the discount from the grand total (per-HSN tax unaffected). Floor line taxable at
  zero. Persist each `Discount` with both the entered form (percent or flat) and the resolved value,
  in the existing line/document `discount` JSON; persist the effective `overallDiscountMode` on the
  document so the breakdown is reproducible.
- **FR-016**: **Line unit-price override (C3).** Allow editing the auto-filled (unit-scaled)
  `sellingPrice` on a line; the entered value becomes the line's price snapshot and GST/discount
  recompute on it. Keep the original product price snapshot (`productPrice`) for reference.
- **FR-017**: **Client-assigned sequential invoice numbering (C4/C5).** Assign the GST invoice number
  on the client at save time from a **per-series sequence** (series = device/counter, optionally
  branch + financial-year, with a distinct prefix), strictly consecutive per series. An offline
  invoice is fully numbered and printable. Persist the series id + sequence number; the
  `InvoiceSyncDelegate` pushes them and the backend validates series+number uniqueness (rejecting
  duplicates rather than silently renumbering). Order numbers may remain server-assigned (orders are
  not the legal tax document); only invoices require client sequential numbering.

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
- **FR-B07**: Add `unitId` + `baseQuantity` columns to `order_item`/`invoice_item` and expose them in
  the request/response DTOs (`OrderItemResponse` etc.) so the transacted unit and base-unit quantity
  round-trip through bulk upsert and paginated pull. Server stores them as supplied (no recompute);
  `baseQuantity` is the field future inventory-deduction work reads.
- **FR-B08**: Add `priceMode` (C1) and `overallDiscountMode` (C2) columns to `customer_order`/`invoice`
  and their DTOs; store as supplied (client is the calc authority, no server recompute).
- **FR-B09**: Add invoice **series** + **sequence number** columns to `invoice` and DTOs; enforce a
  **unique (owner, series, number)** constraint and **reject** a bulk-upsert row that collides with an
  existing different invoice (return a conflict the client surfaces), rather than renumbering — so a
  printed offline number is never silently changed. Back-fill existing invoices into a default series.
- **FR-B10**: Expose the per-document defaults (`priceMode`, `overallDiscountMode`) and the device's
  invoice **series prefix** config as a **business setting** the app can read/sync (business module);
  these seed new documents and the numbering series.

### Non-Functional / Constraints

- **NFR-001**: App offline-first per `/offline-sync`; Metro DI per `/metro-di`
  (`@SingleIn(WorkspaceScope::class)` DBs, `@Inject` repos, `@ContributesIntoMap` VMs); Compose per
  `/cmp-practices` (MVI, `collectAsStateWithLifecycle`, `stringResource` only). Compile **all three
  targets** before done.
- **NFR-002**: Tax math is deterministic and reconciles to the displayed totals; covered by unit tests
  for intra/inter, multi-rate, discount-before-tax, and exempt cases.
- **NFR-003**: Backend `Instant` timestamps, DTO isolation, `ApiResponse<T>`, no try/catch in
  controllers, tenant context at controller level; Flyway in **both** `mysql/` and `postgresql/`.
  Schema migrations are required — next versions **order V1.0.23**, **invoice V1.0.12** (and a follow
  on version each if needed): `order_item`/`invoice_item` gain `unit_id` + `base_quantity` (back-fill
  `base_quantity = quantity`, `unit_id = product base unit`); `customer_order`/`invoice` gain
  `price_mode` + `overall_discount_mode` (back-fill `TAX_EXCLUSIVE` / `POST_TAX_REDUCTION` to match
  legacy behavior); `invoice` gains `series` + `sequence_number` with a **unique (owner_id, series,
  sequence_number)** index (back-fill existing rows into a default series). Pure endpoint additions
  need no migration — record in `NO_MIGRATION_NEEDED.md`.
- **NFR-004**: Money stays `Double` for this feature (today's model); do not introduce the `Money`
  type here.

### Key Entities (reuse existing; no schema redesign)

- **Order / OrderItem** (`feature/order` + backend `customer_order`/`order_item`): reuse current
  fields (`taxInfos` JSON, `totalTax`, `placeOfSupply`, `fromCustomerGst`, `toCustomerGst`,
  `invoiceRefId`, line price snapshots). **Add** to the line: `unitId` + `baseQuantity` (FR-014/B07)
  and **variant reference** fields (`variantSku`/attributes) if not present.
- **Unit / UnitConversion** (existing, `unit` module, synced to app): the line's `unitId` references
  a `Unit`; `UnitConversionEngine`/`UnitConversionService` supply the `multiplier` to derive
  `baseQuantity`. No change to these entities.
- **Discount** (existing `Discount(percent, value)` JSON on line + document): reused as-is; the
  feature defines its semantics (percent-or-flat, pre-tax, apportioned) rather than changing its
  shape. `value` holds the resolved amount; `percent` is 0 for a flat discount.
- **Invoice / InvoiceItem** (`feature/invoice` + backend `invoice`/`invoice_item`): mirror order;
  `orderRefId` link. **Add** `series` + `sequenceNumber` (client-assigned, unique per series — C4/C5).
- **Document settings** (new fields on Order/Invoice): `priceMode` (`TAX_EXCLUSIVE`|`TAX_INCLUSIVE` —
  C1) and `overallDiscountMode` (`PRE_TAX_APPORTIONED`|`POST_TAX_REDUCTION` — C2), defaulted from a
  **business setting** (business module) that also holds the device's invoice series prefix.
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
- **SC-008**: A line transacted in a derived unit stores the correct `unitId` + `baseQuantity`, scales
  the price and GST correctly, respects the unit's decimal places, and round-trips through sync —
  while products without conversions transact in the base unit with no error.
- **SC-009**: Line and overall discounts (percent or flat) behave per the business
  `overallDiscountMode` — pre-tax apportionment keeps per-line CGST/SGST/IGST correct, post-tax reduces
  the grand total — totals reconcile in both modes, and the discount breakdown round-trips through sync.
- **SC-010**: The same line produces correct GST in both `TAX_EXCLUSIVE` (GST added on top) and
  `TAX_INCLUSIVE` (GST extracted) modes, with components reconciling to the line amount.
- **SC-011**: An invoice created offline gets a complete, legally-sequential number from its series and
  is printable immediately; two offline devices in one workspace never produce the same invoice
  number, and the backend rejects (does not silently renumber) a colliding push.

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
  `sellingPrice`/`dp`/`mrp`/`taxCode`/variants/`baseUnitId`/`unitConversions`).
- Assumes the `unit` module + `UnitConversionEngine` (app) / `UnitConversionService` (backend) are
  available and that unit/conversion data syncs to the app; prices are per the product's base unit.
- Assumes the `business` module can hold the new settings (default `priceMode`, `overallDiscountMode`,
  invoice series prefix per device/branch) and sync them to the app (FR-B10).
- Assumes backend order/invoice modules' single-record + conversion endpoints remain; this feature
  **adds** bulk/paginated sync endpoints alongside them.

---

*Next steps*: `/speckit.clarify` is **complete** (C1–C5 above). Remaining minor defaults to confirm
during `/speckit.plan` if needed: rounding policy (half-up) and the intra-state fallback for a missing
buyer GSTIN. Proceed to `/speckit.plan`, `/speckit.tasks`, `/speckit.analyze`, and stop for
review before `/speckit.implement`. Develop on `claude/blissful-goldberg-iuzI6` in both repos; no PR
unless requested.
