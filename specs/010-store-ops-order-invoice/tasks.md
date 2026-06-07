# Tasks: Store Operations — Order & Invoice with GST

**Spec**: `spec.md` · **Plan**: `plan.md` · **Data model**: `data-model.md` · **Contracts**: `contracts/sync-api.md`
**Sequencing**: APP-FIRST. Phases **A1–A4 are the actionable batch** (do these now). **B1 (backend)**
and **I1 (integration)** are listed but **deferred** until the app side is complete.
`[P]` = parallelizable (different files / no ordering dependency). Paths are in `ampairs-app/` unless noted.

> Pre-flight (settle before A1 — they bake into schema/constraints): **(a)** invoice series identity for
> the `UNIQUE` key (device vs branch vs FY); **(b)** calc-core home (`feature/tax` assumed); **(c)** confirm
> `business` module scope. `contracts/sync-api.md` is the frozen seam — lock it before A1.

---

## PHASE A1 — App data layer  (entities → DAOs → repos → delegates)

### Entities & DB
- **T001** Add `unit_id`, `base_quantity`, `variant_sku` to `feature/order/.../order/db/entity/OrderItemEntity.kt`.
- **T002** Add `price_mode`, `overall_discount_mode` to `feature/order/.../order/db/entity/OrderEntity.kt`.
- **T003** [P] Add the same item fields to `feature/invoice/.../invoice/db/entity/InvoiceItemEntity.kt`.
- **T004** [P] Add `price_mode`, `overall_discount_mode`, `series`, `sequence_number` to `InvoiceEntity.kt`.
- **T005** New `InvoiceNumberSeriesEntity` (`series_id` PK, `prefix`, `financial_year`, `last_sequence`) +
  `InvoiceNumberSeriesDao` with atomic `nextSequence(seriesId)` in `feature/invoice/.../invoice/db/`.
- **T006** Bump Room schema version + add Room migrations for the order & invoice DBs (back-fill:
  `base_quantity=quantity`, `unit_id`=product base unit, `price_mode=TAX_EXCLUSIVE`,
  `overall_discount_mode=POST_TAX_REDUCTION`, invoice `series='DEFAULT'`, `sequence_number`=existing number).
- **T007** Register the new entity in the invoice Room `@Database` class + provide the DAO in the
  invoice `@ContributesTo(WorkspaceScope::class)` module.

### Domain models
- **T008** Update `feature/order/.../order/domain/Order.kt` + `OrderItem.kt`: add `unitId`, `baseQuantity`,
  `variantSku`, `priceMode`, `overallDiscountMode`; **remove UID generation from `init {}`** (moves to VM);
  delete the broken `updateTaxes()`/`updateDiscount()`/inverted `taxSpec` logic (calc core replaces it).
- **T009** [P] Mirror domain changes in `feature/invoice/.../invoice/domain/` (+ `series`, `sequenceNumber`).
- **T010** [P] Document the `Discount(percent,value)` convention (flat→percent=0) where it's defined;
  add a `DiscountInput` adapter if needed (no shape change).

### Repositories → LOCAL-ONLY (offline-first)
- **T011** Rewrite `feature/order/.../order/db/OrderRepository.kt`: inject `OrderDao` + `SyncStateDao`
  (NOT `OrderApi`); `saveOrder`/`delete` write Room (`synced=false`) + `markPendingPush(SyncEntity.ORDER)`;
  delete soft-deletes (`active=false, synced=false`). **Remove all `orderApi.*` calls.** Keep reactive
  `Flow` reads + paging source.
- **T012** [P] Same rewrite for `feature/invoice/.../invoice/.../InvoiceRepository.kt` (`SyncEntity.INVOICE`).

### Sync delegates + DI
- **T013** Implement real `feature/order/.../order/sync/OrderSyncDelegate.kt`
  (`@Inject @ContributesIntoMap(WorkspaceScope::class) @SyncEntityKey(SyncEntity.ORDER)`, `dependsOn=[CUSTOMER,PRODUCT]`):
  `pushPendingToServer` (read `synced=0`, batch 100 → `POST /order/v1/orders/sync`, mark synced) and
  `pullFromServer` (paginated `GET /order/v1/orders/sync` cursor + `hasNext`, local-unsynced-wins, soft-delete).
  Follow `/offline-sync` failure-propagation rules.
- **T014** [P] Implement `InvoiceSyncDelegate` (`SyncEntity.INVOICE`, `dependsOn=[CUSTOMER,PRODUCT,ORDER]`).
- **T015** Update `OrderApi`/`OrderApiImpl` + invoice equivalents: add `/sync` bulk + paginated calls per
  `contracts/sync-api.md`; **move the Api binding to `WorkspaceScope`** (it carries `X-Workspace-ID`).
- **T016** Add the new request/response DTO fields (`unit_id`, `base_quantity`, `variant_sku`, `price_mode`,
  `overall_discount_mode`, invoice `series`/`sequence_number`) to the app API models (`@SerialName` snake_case).

---

## PHASE A2 — App calculation core (feature/tax)  [parallel with A1]

- **T017** New models in `feature/tax/.../tax/calculation/`: `DiscountInput`, `ResolvedRate`,
  `LineCalcInput`, `DocumentCalcInput`, `LineCalcResult`, `DocumentCalcResult` (see data-model §4).
- **T018** `ScenarioResolver`: GSTIN state-code compare → `INTRA|INTER` — **fix the inverted logic**
  (different states ⇒ INTER); fallback missing buyer GSTIN → address state → seller state → INTRA.
- **T019** `RateResolver`: map a `TaxRule.componentComposition[scenario]` → `ResolvedRate` (components + totalRate).
- **T020** `DocumentTotalsCalculator` (pure): per-line gross → line discount → overall-discount
  apportionment (PRE_TAX mode) → inclusive/exclusive split (C1) → components → line/doc aggregation →
  POST_TAX reduction (C2); half-up 2dp + remainder reconciliation.
- **T021** Unit tests (`feature/tax` commonTest): intra vs inter; exclusive vs inclusive; line discount;
  overall discount in BOTH modes; multi-rate/multi-HSN; exempt (blank tax code); rounding/remainder;
  negative-floor. These are the golden tests guarding GST correctness.

---

## PHASE A3 — App UI rebuild (feature/order, then mirror feature/invoice)

### ViewModels
- **T022** Rebuild `feature/order/.../order/viewmodel/OrderViewModel.kt` to MVI: state (lines, customer,
  priceMode, overall discount, totals), intents `addProduct/removeLine/changeQty/changeUnit/overridePrice/
  setLineDiscount/setOverallDiscount/togglePriceMode/selectCustomer/save`. Generate UIDs via
  `UidGenerator` (`ORD`/`OIT`). Recompute reactively through `DocumentTotalsCalculator` (inject tax-rule
  lookup + `UnitConversionEngine`); derive scenario from customer/seller GSTIN. **Replace the
  `updateTaxInfos()` stub and the ignored picker callback.**
- **T023** [P] Mirror in `feature/invoice/.../InvoiceViewModel.kt`; on save, assign `series` +
  `sequenceNumber` via `InvoiceNumberSeriesDao.nextSequence` and format the display number.
- **T024** Order→Invoice conversion in the order VM (or a small mapper): build invoice from order
  (lines/tax/discount/unit/addresses), cross-link `invoiceRefId`/`orderRefId`, idempotent (reopen if
  already converted), assign invoice number, mark both `PENDING_PUSH`.

### Screens (Compose, MVI, `collectAsStateWithLifecycle`, `stringResource`)
- **T025** Rebuild line editor in `feature/order/.../order/ui/OrderScreen.kt`: per-line unit dropdown
  (base + derived units, `decimalPlaces`-constrained qty), editable unit price, line discount; wire the
  product-picker callback to `addProduct`.
- **T026** Add a **variant selector** when the picked product `hasVariants` (records `variantSku` + variant price).
- **T027** Add **inline product create** in the picker: minimal form (name, price, HSN tax code, base unit)
  → product repo create (local + `markPendingPush(PRODUCT)`, UID in VM) → add as line.
- **T028** Document controls + totals panel: overall discount entry, **tax-inclusive/exclusive toggle**,
  CGST/SGST/IGST breakdown + grand total reconciled from the calc result.
- **T029** Adaptive list-detail for desktop/tablet (extend `OrderPaneScreen.kt`); "Create Invoice" action.
- **T030** [P] Mirror screens for invoice (`feature/invoice/.../ui/`); show the assigned invoice number.
- **T031** Strings → each module's `composeResources/values/strings.xml`; no hardcoded UI text.

### Cleanup
- **T032** Delete the disabled duplicate `shared/.../kotlin-disabled/invoice/`; converge on the single
  `feature/invoice` flow.
- **T033** Business defaults: read synced business settings if present (priceMode, overallDiscountMode,
  series prefix), else a local fallback constant (TAX_EXCLUSIVE / PRE_TAX_APPORTIONED / device prefix).

---

## PHASE A4 — App validation (app side "complete")

- **T034** `./gradlew shared:compileKotlinIosSimulatorArm64` (iOS) — fix any commonMain platform leaks.
- **T035** `./gradlew androidApp:compileDebugKotlinAndroid` and `desktopApp:compileKotlin`.
- **T036** Run calc-core unit tests (T021) green.
- **T037** Offline E2E from `quickstart.md` A–I (skip live-server steps F.3/H-offline-push/I-push) on
  Android + Desktop (+ iOS sim where feasible).
- **T038** Regression: existing order list/search/paging + saved-order display unchanged; confirm **no
  repository makes a network call** (delegates own all order/invoice API traffic).

⇒ **Gate**: app feature-complete offline; sync wired to the contract but unverified live. Proceed to B1.

---

## PHASE B1 — Backend (ampairs)  [DEFERRED until A4 gate]

- **T039** order: `POST /order/v1/orders/sync` (bulk upsert, `List<OrderUpdateRequest>`, no recompute) +
  `GET /order/v1/orders/sync` (`PageResponse`, `hasNext`) — `order/.../controller/OrderController.kt`,
  `OrderService.kt`.
- **T040** order migration **V1.0.77** (mysql+postgresql): `order_item.{unit_id,base_quantity,variant_sku}`,
  `customer_order.{price_mode,overall_discount_mode}` + back-fill.
- **T041** [P] invoice: same `/sync` endpoints; columns `invoice_item.{unit_id,base_quantity,variant_sku}`,
  `invoice.{price_mode,overall_discount_mode,series,sequence_number}` + **UNIQUE(owner_id,series,sequence_number)**;
  migration **V1.0.78** + back-fill default series. Reject (not renumber) colliding pushes.
- **T042** [P] business: settings for default `price_mode`, `overall_discount_mode`, invoice series prefix
  (per device/branch); expose via the existing business-settings response.
- **T043** Backend DTO additions (per `contracts/sync-api.md`) + validation (taxInfos consistency,
  series uniqueness, `base_quantity>0`); `./gradlew :ampairs_service:flywayInfo && buildAll` + module tests.

---

## PHASE I1 — Integration  [DEFERRED, last]

- **T044** Point app at backend; verify order & invoice push (bulk) + pull (paginated) round-trip.
- **T045** Verify client invoice numbering survives round-trip; forced duplicate rejected (not renumbered).
- **T046** Swap app business-defaults from local fallback (T033) to synced business settings.
- **T047** Reconcile any contract drift found in integration (update `contracts/sync-api.md` if needed).
- **T048** Full `quickstart.md` A–J on all three targets.

---

## Dependencies (quick map)

- A1 entities (T001–T007) → repos (T011–T012) → delegates (T013–T016).
- A2 (T017–T021) parallel to A1; **A3 VMs (T022–T024) depend on A2 calc core + A1 repos/domain**.
- A3 screens (T025–T031) depend on their VMs; cleanup (T032–T033) anytime in A3.
- A4 (T034–T038) after A1–A3. **B1 after A4 gate. I1 after B1.**
- Order is the reference build; invoice mirrors it (`[P]` invoice tasks track their order counterparts).
- Never couple invoice→order; share only the calc core via `feature/tax`.
