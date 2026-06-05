# Implementation Plan: Store Operations — Order & Invoice with GST

**Spec**: `specs/010-store-ops-order-invoice/spec.md` (Status: Clarified, C1–C5)
**Created**: 2026-06-05
**Branch**: `claude/blissful-goldberg-iuzI6` (both repos)
**Repos**: `ampairs-app` (primary — clean rebuild), `ampairs` (backend — sync endpoints + columns)

## 1. Summary

Cleanly rebuild the store-staff order/invoice create flow on the KMP app (Android, iOS, Desktop) so
it: computes correct Indian GST (CGST/SGST vs IGST by place-of-supply) via the **existing but unwired**
`TaxCalculationEngine`; transacts line items in any **unit** (base or derived) with conversion; applies
**line + overall discounts** (business-configurable pre-/post-tax); supports a **per-document
tax-inclusive/exclusive toggle**; lets staff **override line price** and **create products inline**;
assigns **client-sequential GST invoice numbers** (per-series, offline-capable); and is **offline-first**
(local-only repositories + real `Order/Invoice SyncDelegate`s). The backend gains **bulk-upsert +
paginated sync** endpoints (mirroring `product`), the new columns, an invoice numbering uniqueness
constraint, and business-setting defaults.

The client is the **tax/calculation authority** (the backend stores `taxInfos`/totals as supplied — confirmed by audit), so the central new abstraction is a **pure, testable calculation core** on the app.

## 2. Technical Context

| Aspect | Decision |
|---|---|
| App stack | KMP/Compose, Metro DI, Room, Ktor, offline-sync (CentralSyncService + SyncDelegate) |
| Backend stack | Spring Boot 4, Kotlin, JPA, Flyway (mysql + postgresql), `OwnableBaseDomain`, `ApiResponse<T>` |
| Money | **Stay `Double`** for this feature (today's model); `Money(minorUnits)` rides with pricing (009) |
| Tax authority | **Client computes**, server stores as-supplied; reuse `TaxCalculationEngine` + `TaxRule.componentComposition` |
| Reused infra | `TaxCalculationEngine` (feature/tax), `UnitConversionEngine` (feature/unit), product picker, list/paging |
| New backend migrations | order **V1.0.23**, invoice **V1.0.12** (+ follow-ons as needed) |
| Modules touched | app: `feature/order`, `feature/invoice`, `feature/tax`, `feature/product`, `feature/unit`, `feature/business`; backend: `order`, `invoice`, `business` (settings), reuse `tax`/`unit` |

## 3. Constitution / Project-Rules Check

| Rule | How this plan complies |
|---|---|
| Offline-first (repo local-only; API in SyncDelegate) | Rebuild `Order/InvoiceRepository` to write Room + `markPendingPush`; **all** API in `Order/InvoiceSyncDelegate`. Removes the current direct-API violation. |
| Metro DI | `Order/InvoiceApi` → `WorkspaceScope`; DBs `@SingleIn(WorkspaceScope::class)`; repos `@Inject`; VMs `@ContributesIntoMap`; UIDs in ViewModel via `UidGenerator`. |
| KMP purity | Calculation core in `commonMain`, pure Kotlin (no `java.*`); `Clock.System.now()`; no `String.format`. |
| Backend rules | `Instant`, DTO isolation, `ApiResponse<T>`, no try/catch in controllers, tenant at controller, Flyway both dialects, `@EntityGraph`. |
| GST correctness | Calc core unit-tested for intra/inter, inclusive/exclusive, line+overall discount, exempt, rounding. |
| Compile gate | All three app targets compiled before "done". |

## 4. The Calculation Core (new — the heart of the feature)

A **pure** component, `DocumentTotalsCalculator`, in `feature/tax` (`com.ampairs.tax.calculation`,
`commonMain`) — both order and invoice already depend on tax. It takes fully-resolved inputs the
ViewModel assembles (so it stays free of repositories/DI) and returns a computed document.

**Inputs** (`DocumentCalcInput`):
- `lines`: each `{ productId, taxCode, unitId, quantity, baseQuantity, unitPrice (resolved or overridden), lineDiscount: DiscountInput? }`
- `priceMode`: `TAX_EXCLUSIVE | TAX_INCLUSIVE` (C1)
- `overallDiscount`: `DiscountInput?` + `overallDiscountMode: PRE_TAX_APPORTIONED | POST_TAX_REDUCTION` (C2)
- `scenario`: `INTRA | INTER` (derived from place-of-supply — see §5)
- `rates`: `Map<taxCode, ResolvedRate>` where `ResolvedRate = { components: [{name, percentage}], totalRate }` (from `TaxRule.componentComposition` for the scenario)

**Algorithm (per line, then document):**
1. `gross = unitPrice × quantity`
2. apply **line discount** (percent→value or flat) → `netAfterLineDiscount` (floor ≥ 0)
3. if `PRE_TAX_APPORTIONED`: apportion the overall discount across lines ∝ `netAfterLineDiscount`; remainder to the largest line → `netTaxableInput`
4. **inclusive vs exclusive** (C1):
   - `TAX_EXCLUSIVE`: `taxable = netTaxableInput`; components = `taxable × rate`
   - `TAX_INCLUSIVE`: `taxable = netTaxableInput / (1 + totalRate)`; components = `taxable × rate`; reconcile so components + taxable == `netTaxableInput`
5. `lineTax = Σ components`; `lineTotal = taxable + lineTax`
6. **document**: aggregate `taxInfos` by component, `totalTax`, `basePrice` (Σ taxable), `totalAmount`; if `POST_TAX_REDUCTION`, subtract the overall discount from `totalAmount` at the end
7. **rounding**: half-up 2 dp at the component level; remainder reconciliation so displayed lines sum to the document totals

**Output** (`DocumentCalcResult`): per-line `{ taxable, taxInfos[], totalTax, lineTotal, appliedDiscountValue }` + document `{ basePrice, totalTax, taxInfos[], totalAmount }`. The ViewModel maps this onto `OrderItem`/`Order` (and invoice equivalents) for persistence.

> This isolates every recurring bug from the audit (empty `taxInfos`, dead `updateTaxInfos()`,
> inverted `TaxSpec`) into one tested unit, and serves order, invoice, and (later) the storefront.

## 5. Place-of-Supply → Scenario (fixes the inverted bug)

`scenario = if (sellerStateCode == buyerStateCode) INTRA else INTER` — **corrected** from the current
inverted logic. State code = first 2 digits of GSTIN (`fromCustomerGst` seller, `toCustomerGst`/
`placeOfSupply` buyer). Fallback order for a missing buyer GSTIN: customer address state → seller
state → INTRA (per spec edge case). Computed in the ViewModel, passed to the calc core.

## 6. Invoice Numbering (C4/C5)

- New local Room table `invoice_number_series` (per workspace): `{ seriesId, prefix, financialYear, lastSequence }`.
- The device's series `prefix` comes from a **business setting** (FR-B10); each device/branch gets a distinct prefix so offline sequences never collide.
- On invoice save the ViewModel atomically increments `lastSequence` for the active series and formats `"{prefix}/{FY}/{seq}"` → `invoice.series` + `invoice.sequenceNumber` + display number.
- `InvoiceSyncDelegate` pushes `series`+`sequenceNumber`; backend enforces unique `(owner_id, series, sequence_number)` and **rejects** a colliding push (surfaced to the user) rather than renumbering.

## 7. Project Structure / Work Breakdown

```
PHASE 1 — Backend (ampairs)  [unblocks app sync]
  order/    + bulk-upsert POST /order/v1/orders/sync  (List<OrderUpdateRequest>)
            + paginated  GET  /order/v1/orders/sync?last_sync=&page=&size= (PageResponse, hasNext)
            + columns: order_item.unit_id, base_quantity; customer_order.price_mode, overall_discount_mode
            migration V1.0.23 (+ back-fill)
  invoice/  + same sync endpoints
            + columns: invoice_item.unit_id, base_quantity; invoice.price_mode, overall_discount_mode,
              series, sequence_number + UNIQUE(owner_id, series, sequence_number)
            migration V1.0.12 (+ back-fill default series)
  business/ + settings: default price_mode, overall_discount_mode, invoice series prefix (per device/branch)
            + expose via existing business settings sync
  DTOs: add fields to OrderUpdateRequest/OrderItem(Request|Response), Invoice equivalents

PHASE 2 — App data layer (ampairs-app)  [DTO migration order: entities→repos]
  feature/order, feature/invoice:
    - Room entities: + unit_id, base_quantity (item); + price_mode, overall_discount_mode (doc);
      invoice: + series, sequence_number; Room schema version bump + migration
    - Repositories → LOCAL-ONLY: write Room (synced=false) + syncStateDao.markPendingPush(ORDER|INVOICE).
      Remove ALL direct orderApi/invoiceApi calls.
    - Real OrderSyncDelegate / InvoiceSyncDelegate (@ContributesIntoMap(WorkspaceScope) + @SyncEntityKey):
      bulk push unsynced, batched pull w/ updatedAt cursor + hasNext, soft-delete, dependsOn=[CUSTOMER,PRODUCT]
      (invoice also dependsOn ORDER for conversion ordering)
    - Move Order/InvoiceApi binding → WorkspaceScope
    - invoice_number_series Room table + DAO

PHASE 3 — App calculation core (feature/tax)
    - DocumentTotalsCalculator (pure) + DiscountInput/ResolvedRate models
    - Scenario resolver (fix inverted INTRA/INTER); rate resolver from TaxRule.componentComposition
    - Unit tests: intra/inter, inclusive/exclusive, line+overall discount (both modes), exempt, rounding

PHASE 4 — App UI rebuild (feature/order, feature/invoice)
    - OrderViewModel/InvoiceViewModel: addProduct(), changeQty/unit/price, line/overall discount,
      priceMode toggle, reactive recompute via calc core, UID generation
    - Functional product picker: select existing + variant selector + INLINE create (product repo create)
    - Order screen / Invoice screen: line editor (unit dropdown w/ decimalPlaces, price override,
      discount), document discount + tax-mode toggle, totals panel; adaptive list-detail (desktop)
    - Order→Invoice conversion (client-side, idempotent, cross-link, assign invoice number)
    - Retire feature/.../kotlin-disabled/invoice; converge single invoice flow

PHASE 5 — Validation
    - ./gradlew androidApp:compileDebugKotlinAndroid ; shared:compileKotlinIosSimulatorArm64 ; desktopApp:compileKotlin
    - Backend: ./gradlew :ampairs_service:flywayInfo ; buildAll ; targeted module tests
    - Regression: existing list/search/paging + saved-order display unchanged
```

## 8. Phasing rationale & parallelism

- **Backend (P1) first** — the app's SyncDelegates need real endpoints to push/pull against.
- **P2 → P3 → P4** follow the project's DTO migration order (entities → repos → VMs → UI); compile after each.
- P3 (calc core) can proceed in parallel with P2 (data layer) since the calc core is pure and dependency-free.
- Order and invoice are near-identical; build order fully, then mirror to invoice (do **not** couple invoice→order — share only the calc core via `feature/tax`).

## 9. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Tax rounding mismatches client vs displayed totals | Single calc core with explicit half-up + remainder reconciliation; golden unit tests |
| Inclusive-mode back-calculation drift | Reconcile components to the inclusive line amount; test |
| Offline invoice number collisions | Per-series prefix per device (C5) + server unique constraint as backstop |
| Re-introducing the API-in-repository violation | Code-review gate against `/offline-sync`; repos inject `SyncStateDao`, never the Api |
| Invoice/order duplication drift | Share the calc core; mirror UI deliberately; delete the disabled module |
| Scope creep into pricing/inventory | Price = `sellingPrice` only (seam left for 009); `baseQuantity` stored but inventory deduction out of scope |

## 10. Out of scope (carried from spec)

Pricing engine (009), `Money` migration, payments/shipping/promotions, tax-inclusive *pricing tables*,
e-invoicing/IRN/e-way-bill, PDF generation, inventory deduction (field added, logic deferred),
storefront changes.

---

*Next*: `/speckit.tasks` to expand Phases 1–5 into ordered, testable tasks; then `/speckit.analyze`,
then `/speckit.implement`. See `data-model.md`, `research.md`, `contracts/sync-api.md`, `quickstart.md`.
