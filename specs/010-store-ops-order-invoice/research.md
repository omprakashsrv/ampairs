# Research & Decisions: Store Operations — Order & Invoice with GST

**Spec**: `specs/010-store-ops-order-invoice/spec.md` · **Plan**: `plan.md`
Phase-0 decisions with rationale, grounded in the code audit of both repos.

## R1 — Client is the tax-calculation authority
**Finding**: order/invoice controllers store `taxInfos`/`totalTax` exactly as supplied; there is **no**
server-side tax calc service. `GstRuleTemplateService` only defines the INTRA/INTER `componentComposition`.
**Decision**: compute tax on the **app** (reusing `TaxCalculationEngine` + synced `TaxRule`s); server
validates consistency only. **Why**: matches the existing contract, keeps offline create fully
functional, avoids a round-trip, and lets one calc core serve order, invoice, and later the storefront.

## R2 — Reuse `TaxCalculationEngine`; wire it via a pure document calculator
**Finding**: `TaxCalculationEngine` (CGST/SGST/IGST by jurisdiction) exists but is never called;
`OrderItem.taxInfos` is always empty and `updateTaxInfos()` is a TODO; `TaxSpec` is **inverted**.
**Decision**: build a pure `DocumentTotalsCalculator` (in `feature/tax`) that orchestrates unit
scaling, rate resolution, discounts, and inclusive/exclusive into one tested unit; fix the scenario
logic. **Why**: collapses every recurring tax bug into one place with golden tests; keeps ViewModels thin.

## R3 — Tax inclusive vs exclusive = per-document toggle (C1)
**Decision**: `priceMode` on the document; inclusive back-calculates `taxable = price/(1+rate)`.
**Why**: store ops span wholesale (exclusive) and counter retail (MRP-inclusive); a per-document toggle
(defaulted from a business setting) covers both without per-line complexity. **Reconciliation**: extracted
components must sum back to the inclusive line amount (rounding rule in calc core).

## R4 — Overall discount mode = business-configurable, both supported (C2)
**Decision**: `overallDiscountMode` ∈ {`PRE_TAX_APPORTIONED`, `POST_TAX_REDUCTION`}.
**Why**: GST-correct invoices need the discount to reduce **taxable value** (pre-tax, apportioned per
HSN); but some businesses want a simple post-tax bill reduction. Supporting both, selected by setting,
satisfies compliance and convenience. **Default**: `POST_TAX_REDUCTION` only for legacy back-fill;
new-business default is configurable.

## R5 — Line price override allowed (C3)
**Decision**: editable unit price; entered value is the snapshot, GST recomputes on it; keep
`productPrice` as the original reference. **Why**: counter negotiation and ad-hoc pricing are routine;
the snapshot preserves auditability and is the natural seam where the 009 pricing engine later supplies
the default instead of `sellingPrice`.

## R6 — Client-assigned, per-series invoice numbering (C4/C5)
**Finding**: today the server assigns the number on push → offline invoices are unnumbered/unprintable.
**Decision**: client assigns `"{prefix}/{FY}/{seq}"` from a local per-series counter; each device/branch
has a distinct `prefix` (from a business setting); server enforces `UNIQUE(owner, series, sequence)` and
rejects (never silently renumbers) collisions. **Why**: a GST invoice must carry a sequential number at
issue time; per-series prefixes make multi-device offline numbering collision-free while staying
GST-legal (multiple consecutive series are permitted). **Rejected**: shared reserved blocks (needs
online to reserve), optimistic+renumber (changes a printed legal number).

## R7 — Sync endpoints: mirror the `product` module
**Finding**: order/invoice only expose single `POST` + non-paginated `GET ?last_updated=` (fixed 50, no
`hasNext`); `product` already has bulk `POST` + `GET /sync` `PageResponse`.
**Decision**: add `POST .../sync` (bulk upsert) + `GET .../sync` (`PageResponse` + `hasNext`) on both
modules. **Why**: the app's `SyncDelegate` pattern needs batched bulk push + cursor pull; reuse the
proven product contract for consistency.

## R8 — Offline-first rebuild (repositories local-only)
**Finding**: repositories call `orderApi`/`invoiceApi` directly; both SyncDelegates are no-op stubs.
**Decision**: repositories write Room + `markPendingPush`; **all** API moves into the delegates; move
`Order/InvoiceApi` to `WorkspaceScope`. **Why**: enforces the project's offline-first rule and fixes the
stale-API-on-workspace-switch risk.

## R9 — Units on line items (FR-014)
**Finding**: `Unit`/`UnitConversion`/`UnitConversionEngine` exist; line items have no unit field and
never convert. **Decision**: store `unitId` + derived `baseQuantity`; scale price by multiplier; respect
`decimalPlaces`. **Why**: quantity is meaningless without a unit, and `baseQuantity` is the field future
inventory deduction will consume (added now to avoid a later migration).

## R10 — Inline product create (FR-009)
**Decision**: the picker offers "new product" → product repo create (local + `markPendingPush(PRODUCT)`),
UID in ViewModel; pushes before the order/invoice (dependency order). **Why**: removes the "product not
in catalog" dead-end during sales without leaving the document.

## R11 — Money stays `Double` (deferred)
**Decision**: do not introduce `Money(minorUnits)` here. **Why**: the user mandated today's pricing
model; the money migration is sequenced with the pricing feature (009) to avoid a cross-cutting refactor
mid-feature. Calc core is written so swapping the numeric type later is localized.

## Open (resolve in `/speckit.plan`/tasks, low impact)
- Rounding policy: half-up, 2 dp, component-level (proposed).
- Series identity composition: device vs branch vs FY — pin before writing the UNIQUE constraint.
- Missing-buyer-GSTIN fallback: address state → seller state → INTRA (proposed).
