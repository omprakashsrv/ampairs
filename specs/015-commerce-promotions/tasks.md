---
description: "Task list for Commerce Promotions & Offers (015)"
---

# Tasks: Commerce Promotions & Offers

**Input**: `specs/015-commerce-promotions/` (spec.md, plan.md)
**Prerequisites**: feature **009 (Pricing)** merged — provides `SalesChannel`, `Money`,
`PricingResolutionService`, `Storefront.defaultChannel`, ecom Kafka projection infra.
**Build order**: 010 → 009 → **015**.
**Repos**: `ampairs` (backend), `ampairs-app` (KMP). Develop on `claude/wonderful-dirac-zl47z7`.

## Format: `[ID] [P?] [Story] Description`
- **[P]** = parallelizable · **[Story]** = US1–US5 from spec.md

---

## Phase 1: Setup

- [ ] T001 Create backend module `ampairs/promotion/` (Gradle, base package `com.ampairs.promotion`); register in `settings.gradle.kts` + `ampairs_service`.
- [ ] T002 Add `promotion` to `migrationModules`; create `promotion/src/main/resources/db/migration/{mysql,postgresql}/`.
- [ ] T003 [P] Create app module `ampairs-app/feature/promotion/` (KMP + composeResources + packageOfResClass pin); register in `settings.gradle.kts`.
- [ ] T004 [P] `./gradlew :ampairs_service:flywayInfo` for next `V1.0.x`.

---

## Phase 2: Foundational (BLOCKS all stories)

- [ ] T005 `PromotionType { CART_DISCOUNT, COUPON, BOGO, VOLUME_SCHEME, BUNDLE }`; `CouponRejectionReason` enum (BELOW_MIN_CART, INELIGIBLE_GROUP, INELIGIBLE_ZONE, EXPIRED, USAGE_LIMIT_REACHED, GLOBAL_LIMIT_REACHED, ...).
- [ ] T006 `Promotion` entity (`OwnableBaseDomain`): uid, name, type, channels(Set<SalesChannel>), status, priority, stackable, conflictPolicy, startsAt?, endsAt?, currency, fundingBrandId?, active.
- [ ] T007 [P] `PromotionEligibility` (embedded/JSON): customerGroupId?, customerType?, customerId?, brandId?, categoryId?, productGroupId?, productId?/variantSku?, geoZoneId?, minQty?, minCartValue?, attributePredicates(JSON)?. Reuse shared `GeoZone` + `AttributePredicate` from feature 009 (no new geo model).
- [ ] T008 [P] `PromotionEffect` (JSON, type-specific): CART_DISCOUNT {percent?/flat?, scope}; COUPON {code, effect, freeShipping?}; BOGO {triggerProduct/variantSku, triggerQty, freeProduct/variantSku, freeQty, perOrderCap?, freeGoodsTaxPolicy}; VOLUME_SCHEME {aggregateBy(BRAND|CATEGORY), basis(QTY|VALUE), slabs[{minThreshold, percent}]}.
- [ ] T009 `Coupon` (Promotion-of-type-COUPON or child): code (normalized unique-active per workspace), perCustomerLimit?, globalLimit?. `CouponRedemption` entity: uid, couponUid, customerId?, orderRef, redeemedAt.
- [ ] T010 Flyway `V1.0.x__create_promotion_tables.sql` (both vendors): promotion, coupon, coupon_redemption; **unique (coupon_uid, customer_id, order_ref)** + index for atomic global count; indexes on owner_id, type, channel, code.
- [ ] T011 [P] Define order-of-operations contract used everywhere: **009 resolve → 015 apply → tax → snapshot**. Document the `ResolvedLine`/`CartContext`/`PromotionResult` shapes (shared with app).
- [ ] T011a Shared **eligibility evaluator** used by all promotion types: matches structured dimensions (channel, group, customer-type, customer, brand, category, product-group, product/variant, geo-zone via pincode→zone, min-qty, min-cart) then optional `attributePredicates` (lowest precedence). Reuses feature 009's `GeoZone` + `AttributePredicate`. Mirrored pure-Kotlin in the app.

**Checkpoint**: promotion data model + enums + pipeline contract ready.

---

## Phase 3: User Story 1 — Line & cart discounts in store ordering/invoicing (P1) 🎯 MVP

**Goal**: workspace cart-discount offer auto-applies during offline order/invoice entry; manual line discounts retained + tagged `MANUAL`; snapshot + zero regression.

**Independent Test**: cart-discount (RETAIL, min-cart ₹2,000, 10%); order ₹1,800 (no apply) → ₹2,400 (−₹240 before tax); save offline + sync intact; no offer → totals identical to today.

### Implementation
- [ ] T012 [P] [US1] Repositories: `PromotionRepository` (+ `@EntityGraph`); DTOs + mappers `PromotionRequest/Response`, `PromotionApplicationResponse` (`promotion/domain/dto/`).
- [ ] T013 [US1] `PromotionEngine` PUBLIC interface + impl `apply(CartContext, lines: List<ResolvedLine>) : PromotionResult` (appliedPromotions ordered, discountAdjustments line/order, freeGoodsLines, rejections). CART_DISCOUNT path: eligibility (channel, min-cart) → order/line % or flat **before tax**; clamp ≥ 0 (FR-019).
- [ ] T014 [US1] `PromotionService` (CRUD/activate/deactivate/soft-delete) + `PromotionController` `/promotion/v1/promotions` (ApiResponse, tenant at controller).
- [ ] T015 [US1] Backend `order`/`invoice` `/sync`: **persist the client-applied offer snapshot verbatim — no server re-apply** (offline-first trust model). Merchant-side apply happens on the app (T019); manual line discounts are tagged `source = MANUAL` client-side. (Server-side apply is the ecom path only — see Phase 7.)
- [ ] T016 [US1] Snapshot fields (both repos): `Order/Invoice.appliedPromotions`(JSON) + `promotionDiscountTotalMinor`; `OrderItem/InvoiceItem.appliedPromotionUids`, `promotionDiscountMinor`, `isFreeGood`, `sourcePromotionUid`, `currency` (entities + Flyway/Room migrations).

### App (offline)
- [ ] T017 [US1] Backend promotion `/sync` controller `GET/POST /promotion/v1/promotions/sync`.
- [ ] T018 [P] [US1] App Room `PromotionEntity` (+ eligibility/effect JSON) in `feature/promotion`; DB factory `@SingleIn(WorkspaceScope::class)`; `PromotionSyncDelegate` (`@SyncEntityKey(PROMOTION)`) — **push + pull** (push owns admin-created promotions); add `PROMOTION` to `SyncEntity`.
- [ ] T019 [US1] App pure `PromotionEngine` mirroring backend CART_DISCOUNT apply over resolved lines (`Money`-typed); wire into `OrderViewModel`/`InvoiceViewModel` **after** `PriceResolver`, **before** tax calc; snapshot onto items.
- [ ] T020 [P] [US1] Compile all 3 app targets.

### Tests
- [ ] T021 [P] [US1] Apply tests: threshold on/off, before-tax math, clamp ≥ 0, snapshot round-trip; zero-regression test (SC-002).

**Checkpoint**: cart discounts auto-apply offline in store order/invoice; manual discounts preserved.

---

## Phase 3b: App admin UI — promotion/coupon/bundle management (US1, offline-first) 🎯 (C1: app admin only)

**Goal**: merchants create/edit/activate all promotion types (incl. coupons + BUNDLE), eligibility, geo-zones, predicates **in the KMP app**, offline-first; no web admin.

**Independent Test**: offline, create a RETAIL cart-discount + a coupon + a BUNDLE; save → rows `synced=false`; on reconnect they push and appear server-side.

- [ ] T021a [US1] App `PromotionRepository` (local-only): Room write `synced=false` + `markPendingPush(SyncEntity.PROMOTION, now)`; soft-delete = `active=false, synced=false`. No `Api` in repo.
- [ ] T021b [US1] App admin ViewModels + screens (`feature/promotion/.../ui`): promotion list, create/edit per type (CART_DISCOUNT, COUPON, BOGO, VOLUME_SCHEME, BUNDLE), eligibility editor (dims + geo-zone + predicates), coupon code/limits, bundle set/effectMode editor. MVI + `metroViewModel()`; `stringResource` only.
- [ ] T021c [US1] Wire `PromotionRoute`/entry providers + nav into app admin/settings area; client-side validation (e.g. tier/slab contiguity, bundle set non-empty) before save.
- [ ] T021d [P] [US1] Compile all 3 app targets.

**Checkpoint**: merchant can fully manage promotions/coupons/bundles from the app, offline; pushes on reconnect.

---

## Phase 4: User Story 2 — Coupon codes with eligibility (P1)

**Goal**: code-gated discount with eligibility + per-customer/global limits, atomic usage, clear rejection reasons.

**Independent Test**: `WELCOME50` (₹50, RETAIL, min-cart ₹500, group New, 1/customer, 1000 global) — rejects below-cart / wrong-group, accepts valid, rejects 2nd use + global cap.

### Implementation
- [ ] T022 [P] [US2] `CouponRepository`, `CouponRedemptionRepository`; `CouponApplyRequest/Response` DTOs.
- [ ] T023 [US2] `CouponService`: normalize code (upper/trim), validate eligibility + window, **atomic** redemption (unique constraint + transactional global count); return `CouponRejectionReason` on failure (FR-007, NFR-005).
- [ ] T024 [US2] Extend `PromotionEngine.apply` to accept an entered coupon code; merge coupon effect with stacking/conflictPolicy (FR-005); record redemption only on order placement (not on preview).
- [ ] T025 [US2] App: coupon entry in order/invoice + `feature/ecom` cart; offline preview validates eligibility from local read model; redemption recorded server-side at sync/checkout (avoid offline double-spend — reconcile on push).
- [ ] T026 [P] [US2] Compile 3 targets.

### Tests
- [ ] T027 [P] [US2] Eligibility + limit matrix; **concurrent redemption** test — no over-redemption (SC-004).

**Checkpoint**: coupons validated + atomically redeemed in store and (later) online.

---

## Phase 5: User Story 3 — BOGO / free-goods scheme (P1)

**Goal**: free-goods lines for "buy X get Y free" (same/different SKU), trigger ratios, per-order cap, deterministic free-goods tax policy, snapshot + brand attribution.

**Independent Test**: buy-10-get-1-free (Brand-X case, WHOLESALE) — 9→none, 10→1 free line, 21→2 free lines; different-SKU variant; tax per policy.

### Implementation
- [ ] T028 [US3] `PromotionEngine` BOGO path: compute floor(qty/triggerQty), emit `FreeGoodsLine` (qty, unitPriceMinor=0, isFreeGood, sourcePromotionUid), honor `perOrderCap`; different-SKU support; out-of-stock free SKU → flag not drop.
- [ ] T029 [US3] Free-goods tax policy (`ZERO_RATED` | `TAXABLE_AT_MRP`) fed deterministically into tax calc (both repos).
- [ ] T030 [US3] Persist/snapshot free-goods lines as order/invoice/cart line items flagged `isFreeGood` (+ `fundingBrandId`); ensure they render in PDF/checkout.
- [ ] T031 [US3] App `PromotionEngine` BOGO mirror + UI: free lines shown distinctly in order/invoice + ecom cart; compile 3 targets.

### Tests
- [ ] T032 [P] [US3] Trigger-ratio + cap + different-SKU + tax-policy tests; snapshot integrity.

**Checkpoint**: free-goods schemes work in store and reflect online.

---

## Phase 6: User Story 4 — Brand volume/value scheme QPS/TPR (P2)

**Goal**: qty/value slab on a brand/category aggregate within one order; apportion discount across scope lines before tax; brand attribution; reconcile to minor unit.

**Independent Test**: Brand-X (50–99→3%, 100+→5%, WHOLESALE) — 40→none, 60→3% apportioned, 120→5%; value variant; non-brand lines untouched.

### Implementation
- [ ] T033 [US4] `PromotionEngine` VOLUME_SCHEME path: aggregate qty/value over brand/category scope in the cart, select slab, apportion % across scope lines (largest-remainder rounding, reconcile to total), attribute to `fundingBrandId`.
- [ ] T034 [US4] App mirror + snapshot (`appliedSlabThreshold` on application); compile 3 targets.

### Tests
- [ ] T035 [P] [US4] Slab selection, apportionment reconciliation to minor unit (SC-005), scope isolation.

---

## Phase 6b: Combo / Bundle offers (FR-002 `BUNDLE`, SC-009) (P2)

**Goal**: `BUNDLE` type with `effectMode = FIXED_PRICE | DISCOUNT` — fixed combo price for a product
set, or % / flat off when ≥ `minItemsFromSet` qualifying items are present.

**Independent Test**: define "A+B+C for ₹499" (FIXED_PRICE) → set present charges ₹499; define "any 3
from set → 15% off" (DISCOUNT) → applies only at ≥3 qualifying items.

- [ ] T035a [US-BUNDLE] `PromotionEngine` BUNDLE path: detect the configured set + required qtys in the cart; FIXED_PRICE → reprice the set lines so their sum = `fixedPriceMinor` (apportioned, reconcile to minor unit, before tax); DISCOUNT → apply % / flat to qualifying lines when `minItemsFromSet` met; clamp ≥ 0.
- [ ] T035b [US-BUNDLE] App `PromotionEngine` BUNDLE mirror + UI (bundle shown as a grouped/repriced set in order/invoice + ecom cart); snapshot applied bundle; compile 3 targets.
- [ ] T035c [P] [US-BUNDLE] Tests: both modes, threshold gating, apportionment reconciliation, snapshot integrity (SC-009).

---

## Phase 7: User Story 5 — Same offers online (P2)

**Goal**: project promotions/coupons to ecom read model; public coupon-apply/offer resolution by channel/customer; snapshot at cart/checkout.

**Independent Test**: RETAIL coupon + WHOLESALE BOGO defined in store; anonymous can apply coupon, can't see BOGO; logged-in Distributor gets BOGO; edit-after-add keeps snapshot.

### Implementation
- [ ] T036 [US5] Kafka `PromotionChangedEvent` publisher in `promotion`; `EcomPromotionProjection` entity + repo + listener (reuse CatalogSyncService pattern); Flyway.
- [ ] T037 [US5] Public endpoints `POST /v1/store/{slug}/coupon/apply` + `GET /v1/store/{slug}/offers` (StorefrontTenantInterceptor context, defaultChannel, authed customer group; resolve from projection — FR-011/012/013/014).
- [ ] T038 [US5] `CartService`/`CheckoutService`: auto-apply eligible offers + entered coupon; snapshot applied offers + free-goods lines onto `EcomCart`/`EcomOrder`; record coupon redemption atomically at checkout.
- [ ] T039 [P] [US5] App `feature/ecom`: coupon entry + auto-offer display at cart; snapshot at checkout.

### Tests
- [ ] T040 [P] [US5] Anonymous-can't-get-wholesale-offer (SC-003); snapshot-after-edit (FR-010); resolution P95 < 60 ms (SC-008).

---

## Phase 8: Polish & Cross-Cutting

- [ ] T041 Parity test: offers+tax totals identical between the **merchant-app engine (offline)** and the **ecom server-side engine** for identical inputs (SC-006/NFR-007); plus a test that `order`/`invoice` `/sync` persists the pushed offer snapshot verbatim (no re-apply).
- [ ] T042 [P] `Money` contract test rejects bare-number money (SC-007); totals never < 0 (FR-019).
- [ ] T043 [P] `data-model.md`, `quickstart.md`, `contracts/` finalized; `docs/guides/offline-sync-contract.md` adds `promotion`/`coupon`.
- [ ] T044 Run `flywayInfo` + `buildAll`/`testAll`; app compile-3-targets.

---

## Dependencies & Execution Order

- **Setup (P1) → Foundational (P2)** block everything; **feature 009 must be merged first**.
- **US1 (P3)** = cart-discount spine + offline + snapshot — first MVP.
- **US2 (P4)** coupons reuse eligibility/limit subsystem (atomic usage).
- **US3 (P5)** BOGO/free-goods — the core wholesale value (cannot be expressed by legacy Discount).
- **US4 (P6)** volume schemes — depend on apportionment proven in US1/US3.
- **US5 (P7)** online projection — depends on US1–US4 engine + ecom infra.
- **Polish (P8)** last.

### Parallel opportunities
- T006/T007/T008 (entity + JSON shapes), T012/T022 (repos/DTOs) in parallel.
- App `[P]` mirrors after each backend engine path lands; all `[P]` tests parallel.

## Notes
- Engine runs **after** 009 resolution, **before** tax, then **snapshot** — same order everywhere.
- No raw `Double` money; snapshot fields additive; no promotion configured = today's totals.
- Coupon redemption is the one place needing strict atomicity (online + in-store concurrency).
- Commit per logical group; compile 3 app targets before any app phase is "done".
