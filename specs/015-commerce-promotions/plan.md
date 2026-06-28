# Implementation Plan: Commerce Promotions & Offers

**Branch**: `claude/wonderful-dirac-zl47z7` (per session policy; spec id `015-commerce-promotions`)
**Date**: 2026-06-23 | **Spec**: `specs/015-commerce-promotions/spec.md`
**Input**: Feature specification from `specs/015-commerce-promotions/spec.md`

## Summary

Add an **offers/promotions engine** that adjusts an order on top of prices already resolved by feature
009. MVP types: cart/coupon discounts, BOGO/free-goods, and brand volume/value schemes (QPS/TPR).
Master data + the single-sourced apply engine live in a new monolith bounded context
`com.ampairs.promotion`, **projected to the ecom read model via Kafka** (the CatalogSyncService/price-
list pattern). **Trust model (mirrors 009):** merchant in-store order/invoice applies offers
**client-side** in the KMP app (offline) and pushes a snapshot the backend `/sync` stores verbatim;
**online** customer orders apply/validate offers **server-side** at ecom checkout. The KMP app gets a
full `feature/promotion` (offline-first `/sync`, admin CRUD + apply engine).
The engine runs **after price resolution (009) and before tax** (`DocumentTotalsCalculator`), emits
discount/free-goods lines, and the applied offer is **snapshotted** onto the order/cart so a later
promotion edit never changes a placed order. Zero regression when no promotion is configured.

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Java 21 (Spring Boot 4.0); App Kotlin 2.4 KMP.
**Primary Dependencies**: Spring Data JPA, Flyway, Kafka, Jackson; App Room KMP, Ktor, Metro DI,
kotlinx-serialization; consumes 009 `PricingResolutionService` + `feature/tax-api`.
**Storage**: Backend PostgreSQL primary (+ MySQL); App Room (per-workspace). Coupon usage counting
needs DB-level atomicity (unique constraint / atomic increment).
**Testing**: Backend JUnit5 + Testcontainers (incl. concurrent-redemption test); App resolver
property/parity tests + compile-3-targets.
**Target Platform**: Linux server; Android/iOS/Desktop app; (Angular web later).
**Project Type**: Mobile + API (two repos).
**Performance Goals**: Public storefront offer resolution P95 < 60 ms (from projection).
**Constraints**: `Money` everywhere (no `Double`); offline-capable app apply; deterministic stacking;
apportionment reconciles to the minor unit; totals never < 0.
**Scale/Scope**: Per-workspace promotions/coupons (hundreds), redemption records (high write volume).

## Constitution Check

- **Timestamps** `Instant`/`TIMESTAMPTZ`. ✅
- **DTO isolation** — `Promotion`/`Coupon`/`CouponRedemption` via DTOs in `promotion/domain/dto/`. ✅
- **JSON SNAKE_CASE**; `Money` as `{amount_minor,currency}`. ✅
- **ApiResponse<T>**; `/sync` uses `PageResponse`. ✅
- **No try/catch in controllers**. ✅
- **Tenant context at controller level**; entities `OwnableBaseDomain`; public coupon-apply via
  `StorefrontTenantInterceptor` (no `X-Workspace-ID`). ✅
- **@EntityGraph** for `Promotion → eligibility/effect` where relational. ✅
- **Derived queries**; `@Query`/native only for coupon lookup + atomic usage. ✅
- **Offline-sync `/sync`** for `promotion` + `coupon`; projection feed for app/ecom. ✅
- **Module boundaries** — new BC `com.ampairs.promotion`; public service iface to order/invoice; added
  to `migrationModules`. ✅
- **Flyway both vendors**; `flywayInfo` for next version. ✅
- **NFR-005 atomic usage** — DB unique `(coupon_uid, customer_id, order_ref)` + atomic counter. ✅

No violations → Complexity Tracking empty.

## Project Structure

### Documentation (this feature)

```
specs/015-commerce-promotions/
├── spec.md          # done
├── plan.md          # this file
├── research.md      # Phase 0 (stacking/conflictPolicy, free-goods tax policy, atomic usage)
├── data-model.md    # Phase 1 (entities, JSON effect shapes, indexes, usage table)
├── quickstart.md    # Phase 1 (configure each MVP offer + apply in store + online)
├── contracts/       # Phase 1 (/promotion/v1/..., /sync, public coupon-apply/resolve)
└── tasks.md         # Phase 2 (/speckit.tasks)
```

### Source Code (both repos)

```
ampairs/ (backend)
└── promotion/                                  # NEW bounded context
    ├── domain/model/    # Promotion, PromotionEligibility(JSON), PromotionEffect(JSON),
    │                    # Coupon, CouponRedemption, PromotionType, FreeGoodsLine(JSON)
    ├── domain/dto/      # Promotion(Request|Response), CouponApply(Request|Response),
    │                    # PromotionApplication(Response), Money
    ├── repository/      # PromotionRepository, CouponRepository, CouponRedemptionRepository
    ├── service/         # PromotionEngine (PUBLIC iface) + impl, PromotionService,
    │                    # CouponService (atomic redemption)
    ├── projection/      # PromotionChangedEvent publisher (Kafka)
    └── controller/      # PromotionController (/promotion/v1/...), promotion /sync controller
    └── src/main/resources/db/migration/{mysql,postgresql}/  # V1.0.x__create_promotion_tables.sql

ampairs/ (touch existing — additive)
├── ecom/    : EcomPromotionProjection + Kafka listener; public coupon-apply/offer-resolve endpoints
│             (StorefrontPublicController); snapshot fields + free-goods lines on EcomCart/EcomOrder
├── order/   : Order/OrderItem snapshot fields (appliedPromotionUids, promotionDiscountMinor,
│             isFreeGood, sourcePromotionUid); call PromotionEngine after 009 resolution, before tax
└── invoice/ : Invoice/InvoiceItem same

ampairs-app/ (KMP)
└── feature/promotion/                          # NEW admin-CRUD + engine module (offline-first)
    ├── commonMain/.../data/db          # PromotionEntity, CouponEntity (Room)
    ├── commonMain/.../data/api         # promotion /sync API (push + pull)
    ├── commonMain/.../data/repository  # PromotionRepository (local write synced=false + markPendingPush)
    ├── commonMain/.../sync             # PromotionSyncDelegate (@SyncEntityKey(PROMOTION)) — push + pull
    ├── commonMain/.../domain           # PromotionEngine (pure, offline): apply over resolved lines
    ├── commonMain/.../ui               # admin screens + ViewModels: promotion/coupon/bundle list/create/edit
    └── {android,ios,desktop}Main       # DB factory (@SingleIn(WorkspaceScope::class))

ampairs-app/ (touch existing — additive)
├── feature/order|invoice : ViewModel calls PromotionEngine after PriceResolver, before tax calc;
│                            snapshot fields + free-goods lines on OrderItem/InvoiceItem (+ entities)
└── feature/ecom          : coupon entry + auto-offers at cart; snapshot at checkout
```

**Structure Decision**: Mobile + API. New backend BC `com.ampairs.promotion` and app
`feature/promotion`. Order of operations everywhere: **009 resolve → 015 apply → tax → snapshot**.
Existing modules touched additively; no promotion configured = today's totals.

## Phase 0 — Research

- **Stacking / conflictPolicy** (resolved 2026-06-23): each `Promotion` has `stackable: Boolean` +
  `priority: Int` + workspace `conflictPolicy` (default `HIGHEST_PRIORITY`, tiebreak
  `BEST_FOR_CUSTOMER`). Non-stackable offers are mutually exclusive; coupons non-stackable with each
  other but may stack with one auto-promotion unless flagged. Engine returns the ordered applied set.
- **Free-goods tax policy** (resolved 2026-06-24): per-promotion `freeGoodsTaxPolicy` (`ZERO_RATED` |
  `TAXABLE_AT_MRP`); **default `ZERO_RATED` — no GST on free goods** (₹0 free-goods lines carry no tax).
- **Coupons online-only** (resolved 2026-06-24): the merchant app never applies/redeems a coupon
  offline — offline coupon entry returns `REQUIRES_CONNECTION`; the server validates + atomically
  redeems online. Auto-promotions (cart/BOGO/volume/bundle) still apply offline. Removes double-spend.
- **Atomic usage**: `CouponRedemption` unique `(coupon_uid, customer_id, order_ref)` + transactional
  global-count check; reject `GLOBAL_LIMIT_REACHED`/`USAGE_LIMIT_REACHED` deterministically.
- **Apportionment**: volume-scheme/BOGO discounts apportioned across scope lines, rounding absorbed by
  a deterministic line (largest-remainder), reconciling to the offer total to the minor unit.
- **Targeting (2026-06-23 clarify)**: hybrid eligibility — structured dimensions (incl.
  `productGroupId`, `geoZoneId`, `customerType`) + optional lowest-precedence `attributePredicates`;
  **reuses** the shared `GeoZone` master and `AttributePredicate` shape from feature 009 (no second
  model). Geo eligibility maps customer/delivery pincode → zone.
- **BUNDLE / combo (2026-06-23 clarify)**: new `PromotionType.BUNDLE` with
  `effectMode = FIXED_PRICE | DISCOUNT`; effect carries the product set + required qtys and either a
  fixed combo price or a `minItemsFromSet` threshold discount. Applied in the same engine pipeline.
- **Depends on 009**: requires `SalesChannel`, `Money`, `PricingResolutionService` output, the shared
  `GeoZone`/`AttributePredicate`, and the ecom Kafka projection infra to exist first → build after 009.

## Phase 1 — Design (data-model + contracts)

- **Public service**: `PromotionEngine.apply(context: CartContext, lines: List<ResolvedLine>) :
  PromotionResult` where `PromotionResult` = ordered `appliedPromotions`, per-line/order
  `discountAdjustments`, `freeGoodsLines`, and `rejections` (coupon reasons).
- **Contracts**:
  - `GET/POST /promotion/v1/promotions` (+ coupons) — merchant CRUD.
  - `GET/POST /promotion/v1/promotions/sync` — offline-sync for the app.
  - `POST /v1/store/{slug}/coupon/apply` + `GET /v1/store/{slug}/offers` (public, storefront context).
  - Kafka `PromotionChangedEvent` → ecom listener → `EcomPromotionProjection`.

## Phase 2 — Tasks

Generated in `tasks.md`, grouped by user story US1–US5. Prereq: feature 009 merged.

## Complexity Tracking

*No constitution violations — section intentionally empty.*
