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
| New backend migrations | order **V1.0.77**, invoice **V1.0.78** (+ follow-ons as needed) |
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

> **Sequencing (per direction): complete ALL app-side work first (Phases A1–A4), then do the backend
> (Phase B1), then end-to-end integration (Phase I1).** The app is built against the agreed contract in
> `contracts/sync-api.md`; the SyncDelegates compile and run, but a live server round-trip only succeeds
> after Phase B1. Everything else on the app — offline create/edit, the calc core, UI, client invoice
> numbering, all-target compilation — is fully buildable and verifiable **without** the backend.

```
═══ APP SIDE (ampairs-app) — done first ═══

PHASE A1 — App data layer   [DTO migration order: entities → repos]
  feature/order, feature/invoice:
    - Room entities: + unit_id, base_quantity, variant_sku (item); + price_mode, overall_discount_mode
      (doc); invoice: + series, sequence_number; Room schema version bump + Room migration (back-fill)
    - Repositories → LOCAL-ONLY: write Room (synced=false) + syncStateDao.markPendingPush(ORDER|INVOICE).
      Remove ALL direct orderApi/invoiceApi calls.
    - Real OrderSyncDelegate / InvoiceSyncDelegate (@ContributesIntoMap(WorkspaceScope) + @SyncEntityKey),
      written to contracts/sync-api.md: bulk push unsynced + batched pull (updatedAt cursor + hasNext) +
      soft-delete; dependsOn=[CUSTOMER,PRODUCT] (invoice also dependsOn ORDER)
    - Move Order/InvoiceApi binding → WorkspaceScope; Api targets the /sync URLs from the contract
    - invoice_number_series Room table + DAO (atomic nextSequence)

PHASE A2 — App calculation core (feature/tax)   [parallel with A1 — pure, dependency-free]
    - DocumentTotalsCalculator (pure) + DiscountInput/ResolvedRate models
    - Scenario resolver (FIX inverted INTRA/INTER); rate resolver from TaxRule.componentComposition
    - Unit tests: intra/inter, inclusive/exclusive, line+overall discount (both modes), exempt, rounding

PHASE A3 — App UI rebuild (feature/order, feature/invoice)
    - OrderViewModel/InvoiceViewModel: addProduct(), changeQty/unit/price, line/overall discount,
      priceMode toggle, reactive recompute via calc core, UID generation, place-of-supply scenario
    - Functional product picker: select existing + variant selector + INLINE create (product repo create)
    - Order/Invoice screens: line editor (unit dropdown w/ decimalPlaces, price override, discount),
      document discount + tax-mode toggle, totals panel; adaptive list-detail (desktop)
    - Order→Invoice conversion (client-side, idempotent, cross-link, assign invoice number)
    - Business defaults (price_mode, overall_discount_mode, series prefix): read synced business settings
      if present, else a sensible LOCAL default (TAX_EXCLUSIVE / PRE_TAX_APPORTIONED / device prefix) —
      swap to fully server-driven in Phase I1
    - Retire feature/.../kotlin-disabled/invoice; converge single invoice flow

PHASE A4 — App validation (app side "complete")
    - ./gradlew androidApp:compileDebugKotlinAndroid ; shared:compileKotlinIosSimulatorArm64 ; desktopApp:compileKotlin
    - Calc-core unit tests green
    - Offline end-to-end on all targets (quickstart A–J except the live-server steps)
    - Regression: existing list/search/paging + saved-order display unchanged
    - Repos make NO network calls (delegates own all API traffic)
    ⇒ At this point the app is feature-complete offline; sync is wired to the contract but unverified live.

═══ BACKEND SIDE (ampairs) — done second ═══

PHASE B1 — Backend
  order/    + bulk-upsert POST /order/v1/orders/sync (List<OrderUpdateRequest>)
            + paginated  GET  /order/v1/orders/sync?last_sync=&page=&size= (PageResponse, hasNext)
            + columns: order_item.unit_id, base_quantity, variant_sku; customer_order.price_mode,
              overall_discount_mode ; migration V1.0.77 (+ back-fill)
  invoice/  + same sync endpoints
            + columns: invoice_item.{unit_id,base_quantity,variant_sku}; invoice.{price_mode,
              overall_discount_mode, series, sequence_number} + UNIQUE(owner_id, series, sequence_number)
            migration V1.0.78 (+ back-fill default series)
  business/ + settings: default price_mode, overall_discount_mode, invoice series prefix (per device/branch)
            + expose via existing business settings sync
  DTOs: add fields to OrderUpdateRequest/OrderItem(Request|Response) + Invoice equivalents (per contracts/)
  Validate: ./gradlew :ampairs_service:flywayInfo ; buildAll ; order/invoice module tests

═══ INTEGRATION — done last ═══

PHASE I1 — End-to-end
    - Point the app at the backend; verify push (bulk) + pull (paginated) for order & invoice
    - Verify client invoice numbering survives round-trip; forced duplicate rejected (not renumbered)
    - Switch app business-defaults from local fallback to synced business settings
    - Reconcile any contract drift discovered during integration (update contracts/sync-api.md if so)
    - Full quickstart A–J on all three targets
```

## 8. Phasing rationale & parallelism

- **App-first (A1–A4), backend second (B1), integration last (I1)** — per direction. The app is built
  to the **contract** (`contracts/sync-api.md`), so it compiles and runs offline without the backend;
  only the live round-trip waits for B1.
- **Dependency caveat (made explicit):** SyncDelegate push/pull will not succeed against the server, and
  business-driven defaults fall back to local constants, until B1 + I1. This is acceptable because the
  feature's core (offline create, GST/discount/unit calc, numbering, UI) is independently verifiable.
- **A2 (calc core) runs parallel to A1** — it's pure and dependency-free.
- **Within the app**, follow the DTO migration order (entities → repos → VMs → UI); compile after each.
- Order and invoice are near-identical; build order fully, then mirror to invoice (do **not** couple
  invoice→order — share only the calc core via `feature/tax`).
- **Contract is the seam** between the two sides: lock `contracts/sync-api.md` before A1 so the backend
  in B1 implements exactly what the app already calls; drift is reconciled in I1.

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
