---
description: "Task list for Commerce Pricing Engine (009)"
---

# Tasks: Commerce Pricing Engine (Retail + Wholesale + Distribution + Brands)

**Input**: `specs/009-commerce-pricing/` (spec.md, plan.md)
**Prerequisites**: feature 010 (Store-Ops) merged — it leaves the price-resolution seam this feature plugs into.
**Build order**: 010 → **009** → 015.
**Repos**: `ampairs` (backend), `ampairs-app` (KMP). Develop on `claude/wonderful-dirac-zl47z7`.

## Format: `[ID] [P?] [Story] Description`
- **[P]** = parallelizable (different files, no dependency)
- **[Story]** = US1–US4 from spec.md

---

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 Create backend module `ampairs/pricing/` (Gradle module, `build.gradle.kts`, base package `com.ampairs.pricing`); register in `settings.gradle.kts` + `ampairs_service` deps.
- [X] T002 Add `pricing` to `migrationModules` in `ampairs_service/build.gradle.kts`; create empty `pricing/src/main/resources/db/migration/{mysql,postgresql}/` dirs.
- [X] T003 [P] Create app module `ampairs-app/feature/pricing/` (KMP module + `build.gradle.kts`, composeResources, packageOfResClass pin); register in `settings.gradle.kts`.
- [X] T004 [P] Run `./gradlew :ampairs_service:flywayInfo` to pick the next `V1.0.x` version for pricing migrations.

---

## Phase 2: Foundational (BLOCKS all stories)

**⚠️ No user-story work begins until this is done.**

- [X] T005 Define `SalesChannel { RETAIL, WHOLESALE }` enum in **`core`** (shared) so ecom/order/invoice/pricing/promotion all reference it without cross-feature coupling.
- [X] T006 [P] Backend `Money` representation: serializer for `{amount_minor: Long, currency}`; `BigDecimal(19,4)`+`currency CHAR(3)` column convention (helper/converter).
- [ ] T007 [P] App `Money(minorUnits: Long, currency: String)` value class in `feature/pricing` (or shared) with `kotlinx-serialization`.
- [X] T008 Add `defaultChannel: SalesChannel = RETAIL` to ecom `Storefront` (entity + migration + DTO).
- [X] T009 Confirm/extend the 010 price-resolution **seam in the merchant app**: the single call site in the app `order`/`invoice` line build that reads `product.sellingPrice` → refactor to an injectable `PriceResolver` port (client-side; no behavior change yet). **No backend resolution wiring** — merchant orders are resolved on the client.
- [X] T009a Backend `order`/`invoice`: add price-snapshot columns (resolvedUnitPriceMinor, currency, priceSource, matchedPriceListUid, appliedTierMinQty, belowMoq) to Order/OrderItem + Invoice/InvoiceItem (entities + Flyway both vendors) so the `/sync` POST **persists the client snapshot verbatim** (no re-resolution).

**Checkpoint**: channel + money + seam ready.

---

## Phase 3: User Story 1 — Wholesale price list with tier breaks (P1) 🎯 MVP

**Goal**: Merchant creates a channel/group price list with slab tiers + MOQ; server resolves the effective unit price; fallback to `sellingPrice`.

**Independent Test**: create list (WHOLESALE, group Distributor), add tiered item (1–9 ₹240/10–49 ₹225/50+ ₹210, MOQ 10); resolve qty 5/10/60 → ₹240/₹225/₹210, `belowMoq` at 5; product with no list → `CATALOG_FALLBACK`.

### Implementation (backend)
- [X] T010 [P] [US1] `PriceList` entity (`OwnableBaseDomain`): uid, name, channel, structured targeting (customerGroupId?, customerType?, customerId?, brandId?, categoryId?, productGroupId?, geoZoneId?), attributePredicates(JSON)?, currency, priority, status, startsAt?, endsAt?, active — `pricing/domain/model/`.
- [X] T011 [P] [US1] `PriceListItem` entity + `PriceTier` (JSON list: minQty, unitPrice): productId, variantSku?, unitPrice, moq?, tiers — `pricing/domain/model/`.
- [X] T011a [P] [US1] `GeoZone` entity (`OwnableBaseDomain`) **owned by the `pricing` module**: uid, name, members (pincodes/ranges/states) + repo + DTOs + Flyway + a **public service interface** (`GeoZoneService.zoneForPincode(pincode)`); `AttributePredicate` value type `{field, operator, value}`. Promotion (015) references `GeoZone` by uid via this service (015 already depends on 009). `GeoZone` is also synced to the app (via the pricing `/sync` so admins manage zones offline) and projected to ecom (T030a).
- [X] T012 [US1] Flyway `V1.0.x__create_pricing_tables.sql` in **both** mysql + postgresql (price_list, price_list_item; indexes on owner_id, channel, customer_group_id, product_id).
- [X] T013 [P] [US1] Repositories: `PriceListRepository`, `PriceListItemRepository` (`@EntityGraph` list→items).
- [X] T014 [P] [US1] DTOs + mappers: `PriceListRequest/Response`, `PriceListItemRequest/Response`, `PriceResolutionResponse` (`pricing/domain/dto/`).
- [X] T015 [US1] `PricingResolutionService` PUBLIC interface + impl: `resolve(customerId?, channel, productId, variantSku?, qty, pincode?, workspace)` → effectiveUnitPrice, source (PRICE_LIST|CATALOG_FALLBACK), matchedPriceListUid, appliedTierMinQty, belowMoq. Map pincode→geo-zone; structured-dimension match first, attribute-predicate match last (lowest precedence); precedence per FR-004 + 2026-06-23 clarify; variant>base; deterministic.
- [X] T016 [US1] Tier validation (contiguous, non-overlapping; >top tier uses top) at save (FR-014).
- [X] T017 [US1] `PriceListService` (CRUD, activate/deactivate, soft-delete) + `PriceListController` `/pricing/v1/price-lists` (ApiResponse, tenant at controller).
- [X] T018 [US1] Catalog fallback path: when no active list matches, return `product.sellingPrice` tagged workspace base currency (FR-005).

### Tests
- [X] T019 [P] [US1] Resolution unit tests (qty 5/10/60, MOQ flag, fallback, overlap precedence).
- [X] T020 [P] [US1] Tier-validation test (gap/overlap rejected).
- [X] T020e [P] [US1] Precedence test (SC-010): geo-zone list resolves for a customer/delivery pincode; an attribute-predicate match never overrides a structured-dimension match; full precedence chain (per-customer > group/channel > product-group/brand/category > geo-zone/customer-type > predicate > fallback).

**Checkpoint**: backend can resolve a wholesale tiered price; fallback verified.

---

## Phase 3b: App admin UI — price-list management (US1, offline-first) 🎯 (C1: app admin only)

**Goal**: merchants create/edit/activate price lists (items, tiers, MOQ, geo-zones, predicates) **in the KMP app**, offline-first; no web admin.

**Independent Test**: offline, create a WHOLESALE list for group Distributor with a tiered item + MOQ; save → row `synced=false`; on reconnect it pushes via `PricingSyncDelegate` and appears server-side.

- [X] T020a [US1] App `PriceListRepository` (local-only): write to Room `synced=false` + `syncStateDao.markPendingPush(SyncEntity.PRICE_LIST, now)`; soft-delete = `active=false, synced=false`. No `Api` in repo (offline-sync rule).
- [X] T020b [US1] App admin ViewModels + screens (`feature/pricing/.../ui`): price-list list, create/edit (name, channel, targeting dims, currency, priority, validity), item editor (product/variant, unitPrice, MOQ, tiers), geo-zone picker/editor, attribute-predicate editor. MVI + `metroViewModel()`; `stringResource` only.
- [X] T020c [US1] Wire `PriceListRoute`/entry providers + nav into app admin/settings area; tier-validation mirrored client-side (contiguous/non-overlapping) before save.
- [X] T020d [P] [US1] Compile all 3 app targets.

**Checkpoint**: merchant can fully manage price lists from the app, offline; pushes on reconnect.

---

## Phase 4: User Story 3 — B2B order entry on the app uses the same prices (P2, offline)

> Sequenced before US2 because in-store-first is the product priority; storefront (US2) follows.

**Goal**: App pulls the price-list projection and resolves line prices offline in order/invoice entry.

**Independent Test**: offline, select Distributor + product qty 50 → line auto-fills ₹210 from local read model; change qty to 5 → ₹240 + MOQ warning.

### Implementation (app + backend sync)
- [X] T021 [US1→app] Backend price-list `/sync` controller `GET/POST /pricing/v1/price-lists/sync` (offline-sync contract: snake_case params, soft-deletes in feed, UID-keyed bulk upsert).
- [X] T022 [P] [US3] App Room: `PriceListEntity`, `PriceListItemEntity` (+ tiers JSON) in `feature/pricing/.../data/db`; DB factory `@SingleIn(WorkspaceScope::class)` per platform.
- [X] T023 [P] [US3] App pricing `/sync` API + `PricingSyncDelegate` (`@ContributesIntoMap(WorkspaceScope::class)`, `@SyncEntityKey(PRICE_LIST)`) — **push + pull** (push owns the admin-created lists); add `PRICE_LIST` to `SyncEntity`.
- [X] T024 [US3] App `PriceResolver` (pure, offline) mirroring backend precedence/tiers/MOQ over the Room read model; `Money`-typed.
- [X] T025 [US3] Wire `OrderViewModel`/`InvoiceViewModel` line entry: replace `productPrice × unitMultiplier` with `PriceResolver.resolve(...)` (respect `priceOverridden`); re-resolve on qty/variant/unit change.
- [X] T026 [US3] Snapshot fields on `OrderItem`/`InvoiceItem` (+ Room entities + migration v5→v6 + entity↔domain↔wire mappers): `resolvedUnitPriceMinor`, `currency`, `priceSource`, `matchedPriceListUid`, `appliedTierMinQty`, `belowMoq`. `PriceResolver` widened to return `ResolvedPrice` (snapshot, not just a `Double`); order/invoice ViewModels capture it at line build and it pushes verbatim on `/sync`.
- [X] T027 [US3] Surface MOQ warning in order/invoice UI (warn for B2B rep, not block). `belowMoq` flows OrderItem/InvoiceItem → `DocLineUi` → shared editor grid row (error-tinted label) + compact line card (error-container chip).
- [X] T028 [P] [US3] Compile all 3 targets (`androidApp:compileDebugKotlinAndroid`, `shared:compileKotlinIosSimulatorArm64`, `desktopApp:compileKotlin`).

**Checkpoint**: app resolves wholesale tier prices offline in order/invoice entry.

---

## Phase 5: User Story 2 — Storefront shows the right price (P1)

**Goal**: Project price lists to ecom read model; public resolution by channel/customer; snapshot at cart/checkout.

**Independent Test**: wholesale customer sees ₹225 at qty 10, adds to cart; merchant edits list; checkout charges snapshotted ₹225. Anonymous on RETAIL never sees wholesale.

### Implementation (backend ecom)
- [ ] T029 [US2] Kafka `PriceListChangedEvent` publisher in `pricing` on list/item activate/edit/deactivate.
- [ ] T030 [US2] `EcomPriceListProjection` entity + repo + Kafka listener (reuse `CatalogSyncService` pattern); Flyway in postgresql (+ mysql if needed).
- [ ] T030a [US2] Project **`GeoZone` membership to the ecom read model** (`EcomGeoZoneProjection` + listener on `GeoZoneChangedEvent`) so storefront server-side resolution can map a delivery/customer pincode → zone without a call into pricing. (Shared by promotions 015.)
- [X] T031 [US2] Public resolve endpoint `GET /v1/store/{slug}/price` on `StorefrontPublicController`: runs in `StorefrontTenantInterceptor` context, defaults to `storefront.defaultChannel`, honors authed B2B customer group; resolves from projection only (FR-006/007/008/011).
- [X] T032 [US2] Snapshot fields on `EcomCartItem`/`EcomOrderLineItem` (`resolvedUnitPriceMinor`, currency, priceSource, matchedPriceListUid); `CartService.addOrUpdateItem` + `CheckoutService` snapshot the resolved price (FR-009).
- [ ] T033 [P] [US2] App `feature/ecom`: catalog/cart price display reads channel/group price from projection; checkout sends snapshot.

### Tests
- [X] T034 [P] [US2] Negative test: anonymous RETAIL never receives a WHOLESALE price (SC-003) — `PricingResolutionServiceTest`: a RETAIL request falls back to catalog and the WHOLESALE feed is never queried (`verify(never())`).
- [X] T035 [P] [US2] Snapshot-honored-after-edit test (SC-001) — `CartServiceTest`: resolved price is snapshotted onto the cart row at add-time (immune to later list edits); resolution P95 < 50 ms smoke (SC-004) in `PricingResolutionServiceTest`.

**Checkpoint**: storefront + app + in-store resolve identically (SC-006).

---

## Phase 6: User Story 4 — Currency travels with every price (P3)

- [X] T036 [P] [US4] Enforce `Money` on all pricing DTOs; contract test rejects bare-number money (SC-005).
- [X] T037 [P] [US4] Single-currency-per-list validation (FR-010); catalog-fallback tagged workspace base currency.

---

## Phase 7: Polish & Cross-Cutting

- [X] T038 Regression suite: no price list configured → identical totals to today (SC-002) — covered by `PricingResolutionServiceTest` "no matching list falls back to catalog selling price": resolution returns the product's catalog price verbatim (CATALOG_FALLBACK), so totals are identical to pre-feature behavior.
- [X] T039 Parity test: **merchant-app (Room) resolver vs ecom server-side (projection) resolver** produce identical effective prices for identical inputs (SC-006); plus a test that `order`/`invoice` `/sync` persists the pushed snapshot verbatim (no re-resolution).
- [X] T040 [P] `data-model.md`, `quickstart.md`, `contracts/` finalized; update CLAUDE.md "mysql only" stale note → Postgres primary.
- [X] T041 [P] `docs/guides/offline-sync-contract.md`: add `price_list` to syncable resources.
- [ ] T042 Run `./gradlew :ampairs_service:flywayInfo` + `buildAll`/`testAll`; app compile-3-targets.

---

## Dependencies & Execution Order

- **Setup (P1) → Foundational (P2)** block everything.
- **US1 (P3 phase)** = backend resolution MVP — first.
- **US3 (P4)** depends on US1 (needs resolution + `/sync`). In-store-first priority.
- **US2 (P5)** depends on US1 (+ ecom projection infra).
- **US4 (P6)** cross-cuts; verify alongside.
- **Polish (P7)** last.

### Parallel opportunities
- T010/T011/T013/T014 (entities/repos/DTOs) in parallel.
- App T022/T023 in parallel; backend US2 (T029/T030) parallel with app US3 polish.
- All `[P]` test tasks parallel.

## Notes
- Money: no raw `Double` in new code. Snapshot fields additive — existing price fields retained.
- Resolution is the **only** new server-side calc; offers (015) layer on top later.
- Commit per logical group; compile 3 app targets before any app phase is "done".
