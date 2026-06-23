# Implementation Plan: Commerce Pricing Engine (Retail + Wholesale + Distribution + Brands)

**Branch**: `claude/wonderful-dirac-zl47z7` (per session policy; spec id `009-commerce-pricing`)
**Date**: 2026-06-23 | **Spec**: `specs/009-commerce-pricing/spec.md`
**Input**: Feature specification from `specs/009-commerce-pricing/spec.md`

## Summary

Add a server-authoritative **price-resolution engine** so the same product sells at a retail price to
walk-in/B2C shoppers and at channel/group/brand/quantity-tiered wholesale prices to B2B buyers.
Master data + resolution live in a new monolith bounded context `com.ampairs.pricing`, exposed to
`order`/`invoice` via a public service interface and **projected into the ecom read model via Kafka**
(the pattern `CatalogSyncService` already uses for catalog). The KMP app gets a `feature/pricing`
read-model (offline-first `/sync`) so in-store order/invoice entry resolves prices offline. Resolution
**falls back to today's `sellingPrice`** when no list matches → zero regression. This feature is the
base-price layer; **offers/promotions are feature 015** and consume this feature's resolved output.

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Java 21 (Spring Boot 4.0); App Kotlin 2.4 KMP (Compose MP 1.11).
**Primary Dependencies**: Spring Data JPA, Flyway, Kafka (existing ecom catalog pipeline), Jackson
(SNAKE_CASE); App: Room KMP, Ktor, Metro DI, kotlinx-serialization, the existing
`feature/tax-api` `DocumentTotalsCalculator`.
**Storage**: Backend PostgreSQL primary (+ MySQL variant); App Room (per-workspace DB).
**Testing**: Backend JUnit5 + Testcontainers (`./gradlew testAll`); App compile-3-targets + unit tests
for the resolver.
**Target Platform**: Linux server; Android/iOS/Desktop app; (Angular web — display only, later).
**Project Type**: Mobile + API (two repos: `ampairs`, `ampairs-app`).
**Performance Goals**: Public storefront price resolution P95 < 50 ms (from projection, no cross-service call).
**Constraints**: Money is `{amount_minor, currency}` on the wire / `BigDecimal(19,4)`+`currency CHAR(3)`
in DB / `Money(minorUnits,currency)` in app — no raw `Double` money in new code. Offline-capable app resolution.
**Scale/Scope**: Per-workspace price lists (tens–hundreds of lists, thousands of items); multi-tenant.

## Constitution Check

*GATE: Must pass before Phase 0; re-check after Phase 1 design.*

- **Timestamps** — all new entities use `Instant`; columns `TIMESTAMPTZ`/`TIMESTAMP`. ✅
- **DTO isolation** — `PriceList`/`PriceListItem` never exposed raw; Request/Response DTOs in
  `pricing/domain/dto/` with extension-fn mapping. ✅
- **JSON SNAKE_CASE** — no `@JsonProperty` for standard fields; `Money` serializes as
  `{amount_minor, currency}` (custom — documented). ✅
- **ApiResponse<T>** — all controllers wrap; paginated `/sync` uses `PageResponse`. ✅
- **No try/catch in controllers**; exceptions bubble. ✅
- **Tenant context at controller level**; price lists are `OwnableBaseDomain`; public storefront uses
  `StorefrontTenantInterceptor` (no `X-Workspace-ID`). ✅
- **@EntityGraph** for `PriceList → items`. ✅
- **Derived queries** preferred; `@Query`/`nativeQuery` only where needed (resolution lookups). ✅
- **Offline-sync `/sync` contract** for `price_list` (and projection feed for app). ✅
- **Module boundaries** — new BC `com.ampairs.pricing`; cross-module via public service interface,
  no direct repo access; added to `migrationModules`. ✅
- **Flyway both vendors**; check `flywayInfo` for next version. ✅

No violations → Complexity Tracking empty.

## Project Structure

### Documentation (this feature)

```
specs/009-commerce-pricing/
├── spec.md          # done
├── plan.md          # this file
├── research.md      # Phase 0 (decisions D1/D5 already in program PLAN — summarized here)
├── data-model.md    # Phase 1 (entities, columns, indexes)
├── quickstart.md    # Phase 1 (end-to-end "configure a wholesale list" walkthrough)
├── contracts/       # Phase 1 (OpenAPI-ish: /pricing/v1/price-lists, /sync, public resolve)
└── tasks.md         # Phase 2 (/speckit.tasks)
```

### Source Code (both repos)

```
ampairs/ (backend)
└── pricing/                                   # NEW bounded context
    ├── src/main/kotlin/com/ampairs/pricing/
    │   ├── domain/model/        # PriceList, PriceListItem, PriceTier(JSON), SalesChannel
    │   ├── domain/dto/          # PriceList(Request|Response), PriceResolution(Response), Money
    │   ├── repository/          # PriceListRepository, PriceListItemRepository
    │   ├── service/             # PricingResolutionService (PUBLIC iface) + impl, PriceListService
    │   ├── projection/          # PriceListChangedEvent publisher (Kafka)
    │   └── controller/          # PriceListController (/pricing/v1/...), price-list /sync controller
    └── src/main/resources/db/migration/{mysql,postgresql}/   # V1.0.x__create_pricing_tables.sql

ampairs/ (touch existing — additive only)
├── ecom/  : EcomPriceListProjection entity + repo + Kafka listener (reuse CatalogSyncService pattern);
│            Storefront.defaultChannel; public price-resolve endpoint on StorefrontPublicController;
│            snapshot fields on EcomCartItem/EcomOrderLineItem
├── order/ : OrderItem snapshot fields (resolvedUnitPriceMinor,currency,priceSource,matchedPriceListUid);
│            wire PricingResolutionService at the 010 price-resolution seam
└── invoice/ : InvoiceItem snapshot fields; same wiring

ampairs-app/ (KMP)
└── feature/pricing/                           # NEW admin-CRUD + read-model module (offline-first)
    ├── commonMain/.../data/db          # PriceListEntity, PriceListItemEntity, PriceTier, GeoZoneEntity (Room)
    ├── commonMain/.../data/api         # pricing /sync API (push + pull)
    ├── commonMain/.../data/repository  # PriceListRepository (local write synced=false + markPendingPush)
    ├── commonMain/.../sync             # PricingSyncDelegate (@SyncEntityKey(PRICE_LIST)) — push + pull
    ├── commonMain/.../domain           # Money value class, PriceResolver (pure, offline)
    ├── commonMain/.../ui               # admin screens + ViewModels: price-list list/create/edit, tiers, geo-zones
    └── {android,ios,desktop}Main       # DB factory (@SingleIn(WorkspaceScope::class))

ampairs-app/ (touch existing — additive)
├── feature/order|invoice : ViewModel line-entry calls PriceResolver (replaces sellingPrice×multiplier);
│                            snapshot fields on OrderItem/InvoiceItem (+ entities)
└── feature/ecom          : catalog/cart price display + snapshot from projection
```

**Structure Decision**: Mobile + API. New backend BC `com.ampairs.pricing` (master + resolution),
new app `feature/pricing` (offline read-model + resolver). Existing modules touched additively only —
no behavior change when no price list exists.

## Phase 0 — Research (decisions already locked in program PLAN)

- **D1 Money** → `{amount_minor:Long, currency}` wire, `BigDecimal(19,4)`+`currency CHAR(3)` DB,
  `Money(minorUnits,currency)` app. Default INR.
- **D2 SalesChannel** → `enum {RETAIL, WHOLESALE}` (extensible). `Storefront.defaultChannel`.
- **D5 Pricing home** → monolith `com.ampairs.pricing` + Kafka projection to ecom (no hot-path call).
- **Resolution precedence** (spec FR-004 + 2026-06-23 clarification): per-customer special >
  customer-group/channel list > brand/category list > catalog fallback; ties → `priority` then
  most-recently-activated; variant match wins over base within a list.
- **Open for /speckit.clarify**: free-goods/MOQ block-vs-warn already delegated to cart layer; confirm
  whether brand/category lists ship in MVP or P2.

## Phase 1 — Design (data-model + contracts; see those files)

- **Entities**: `PriceList` (structured targeting dims incl. `productGroupId`, `geoZoneId`,
  `customerType` + `attributePredicates` JSON), `PriceListItem` (+ `PriceTier` JSON), `SalesChannel`,
  shared `GeoZone` (pincode/range/state membership), `AttributePredicate` (value); ecom
  `EcomPriceListProjection`; app `PriceListEntity`/`PriceListItemEntity`/`GeoZoneEntity`.
- **Targeting model (2026-06-23 clarify)**: hybrid — structured dimensions (hot path) + optional
  lowest-precedence attribute predicates. Precedence: per-customer special > customer/group+channel >
  product-group/brand/category > geo-zone/customer-type > attribute-predicate > catalog fallback.
- **Public service**: `PricingResolutionService.resolve(customerId?, channel, productId, variantSku?,
  qty, workspace) : PriceResolution` (effectiveUnitPrice, source, matchedPriceListUid, appliedTierMinQty, belowMoq).
- **Contracts**:
  - `GET/POST /pricing/v1/price-lists` (+ items) — merchant CRUD.
  - `GET/POST /pricing/v1/price-lists/sync` — offline-sync contract for the app.
  - `GET /v1/store/{slug}/price` (public, storefront context) — resolve for shopper/customer.
  - Kafka `PriceListChangedEvent` → ecom listener → `EcomPriceListProjection`.

## Phase 2 — Tasks

Generated in `tasks.md` (grouped by user story US1–US4), build order **010 → 009 → 015**.

## Complexity Tracking

*No constitution violations — section intentionally empty.*
