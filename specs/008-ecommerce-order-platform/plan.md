# Implementation Plan: Ecommerce Order Platform

**Branch**: `008-ecommerce-order-platform` | **Date**: 2026-05-31 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `/specs/008-ecommerce-order-platform/spec.md`

---

## Summary

Build a merchant-configurable ecommerce storefront module (`ecom`) within the Ampairs monorepo. Merchants create and publish storefronts with configurable access modes (PUBLIC or RESTRICTED); products sync from management via Kafka catalog events; end customers browse, cart, and checkout; ecom orders flow back to management via Kafka for inventory deduction and fulfilment tracking. A `StorefrontAccessFilter` (Spring `HandlerInterceptor`) enforces workspace scoping and access-list gating in a single pass before any controller executes. Single platform-wide customer identity reuses the existing `auth` module with a new `END_CUSTOMER` user type.

---

## Technical Context

**Language/Version**: Kotlin 2.3 / Java 21  
**Primary Dependencies**: Spring Boot 4.0, Spring Kafka 3.x, Spring Data JPA, Spring Security, Hibernate 6.x, Jackson (global snake_case config), Caffeine 3.x (in-process LRU cache)  
**Storage**: PostgreSQL (primary persistence, `tsvector`/GIN full-text search for product discovery), Kafka (event bus — 3 dedicated topics)  
**Testing**: JUnit 5 + Testcontainers (PostgreSQL, Kafka), Mockito-Kotlin  
**Target Platform**: Linux server (JVM, runs as part of `ampairs_service`)  
**Project Type**: Single project — new `ecom` module assembled into existing `ampairs_service`  
**Performance Goals**: Product search < 1s p95 (SC-002), catalog sync < 5s p99 (SC-003), 500 concurrent shoppers (SC-004), access-list LRU lookup adds < 1ms overhead (in-process Caffeine, 100-entry capacity)  
**Constraints**: Payment out of scope; no Elasticsearch in v1 (PostgreSQL `tsvector`/GIN); no shipping integration  
**Scale/Scope**: 500 concurrent shoppers across all storefronts; ~10 Kafka topics total; ~470 new Kotlin source lines (estimated); 11 new Flyway migrations

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Timestamps — `Instant` everywhere | ✅ PASS | All entity/DTO timestamp fields use `Instant`; DB columns `TIMESTAMPTZ` |
| II. DTO isolation | ✅ PASS | All controllers accept Request DTOs and return Response DTOs; entity ↔ DTO via extension functions; `StorefrontAccessEntry` has dedicated Request/Response DTOs |
| III. JSON conventions — no `@JsonProperty` | ✅ PASS | Global `SNAKE_CASE` strategy applies; no annotations added for standard fields |
| IV. Multi-tenancy — controller sets context | ✅ PASS | Tenant context set in `StorefrontAccessFilter.preHandle()` (before any controller executes); `StorefrontAccessEntry` extends `OwnableBaseDomain` (workspace-scoped) |
| V. API responses — `ApiResponse<T>` | ✅ PASS | All endpoints return `ApiResponse.success(...)` |
| VI. Centralized exception handling | ✅ PASS | `StoreAccessDeniedException` and `StoreUnauthenticatedException` added to `EcomExceptionHandler`; no try/catch in controllers |
| VII. `@EntityGraph` for relationships | ✅ PASS | Named entity graphs on `EcomCart`, `EcomOrder` |
| VIII. Angular Material 3 | ✅ N/A | No frontend code in this feature |
| IX. Module boundaries | ✅ PASS | `StorefrontAccessFilter` lives in `ecom` module; cross-module via public service interfaces and Kafka only; `EcomStorefrontLookupService` interface in `core` |
| X. Compose parity | ✅ N/A | No mobile code in this feature |
| XI. Security & secrets | ✅ PASS | Kafka bootstrap URL via env var; JWT signing reuses existing key infrastructure; no secrets in source |

**No violations. All gates pass.**

---

## Project Structure

### Documentation (this feature)

```
specs/008-ecommerce-order-platform/
├── plan.md              ← This file
├── spec.md
├── research.md          ← Phase 0 output
├── data-model.md        ← Phase 1 output
├── quickstart.md        ← Phase 1 output
└── contracts/
    ├── storefront-api.md
    ├── management-api.md
    └── kafka-contracts.md
```

### Source Code Layout

**New `ecom` module**:
```
ecom/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── kotlin/com/ampairs/ecom/
    │   │   ├── config/
    │   │   │   ├── Constants.kt
    │   │   │   ├── EcomKafkaConfig.kt              # Kafka producer/consumer config
    │   │   │   └── EcomWebMvcConfig.kt             # Registers StorefrontAccessFilter as HandlerInterceptor
    │   │   ├── filter/
    │   │   │   └── StorefrontAccessFilter.kt       # HandlerInterceptor: slug→storefront, TenantContext, access gate
    │   │   ├── controller/
    │   │   │   ├── StorefrontPublicController.kt   # GET /api/v1/store/{slug}/...  (reads pre-resolved storefront from request attr)
    │   │   │   ├── CartController.kt
    │   │   │   ├── CheckoutController.kt
    │   │   │   ├── CustomerAccountController.kt    # /api/v1/ecom/account/...
    │   │   │   ├── StorefrontManagementController.kt  # /api/v1/ecom/management/storefront/...
    │   │   │   ├── EcomOrderManagementController.kt   # /api/v1/ecom/management/orders/...
    │   │   │   └── StorefrontAccessController.kt   # /api/v1/ecom/management/storefront/access/...
    │   │   ├── domain/
    │   │   │   ├── dto/
    │   │   │   │   ├── StorefrontRequest.kt
    │   │   │   │   ├── StorefrontUpdateRequest.kt
    │   │   │   │   ├── StorefrontResponse.kt        # includes accessMode field
    │   │   │   │   ├── StorefrontAccessEntryRequest.kt
    │   │   │   │   ├── StorefrontAccessEntryResponse.kt
    │   │   │   │   ├── StorefrontAccessBulkImportResult.kt
    │   │   │   │   ├── ListedProductResponse.kt
    │   │   │   │   ├── CartResponse.kt
    │   │   │   │   ├── CartItemRequest.kt
    │   │   │   │   ├── CheckoutRequest.kt
    │   │   │   │   ├── EcomOrderResponse.kt
    │   │   │   │   ├── EcomOrderManagementResponse.kt
    │   │   │   │   ├── EcomOrderLineItemEditRequest.kt
    │   │   │   │   ├── CustomerAddressRequest.kt
    │   │   │   │   └── CustomerAddressResponse.kt
    │   │   │   ├── enums/
    │   │   │   │   ├── StorefrontStatus.kt
    │   │   │   │   ├── StorefrontAccessMode.kt      # PUBLIC | RESTRICTED
    │   │   │   │   ├── StorefrontAccessIdentifierType.kt  # USER_ID | PHONE | EMAIL | EXTERNAL_ID
    │   │   │   │   ├── StockStatus.kt
    │   │   │   │   ├── CartStatus.kt
    │   │   │   │   ├── EcomOrderStatus.kt
    │   │   │   │   └── EcomLineItemStatus.kt
    │   │   │   └── model/
    │   │   │       ├── Storefront.kt                # + accessMode: StorefrontAccessMode
    │   │   │       ├── StorefrontAccessEntry.kt     # extends OwnableBaseDomain
    │   │   │       ├── EcomListedProduct.kt
    │   │   │       ├── EcomCart.kt
    │   │   │       ├── EcomCartItem.kt
    │   │   │       ├── CustomerAddress.kt
    │   │   │       ├── EcomOrder.kt
    │   │   │       └── EcomOrderLineItem.kt
    │   │   ├── repository/
    │   │   │   ├── StorefrontRepository.kt
    │   │   │   ├── StorefrontAccessEntryRepository.kt
    │   │   │   ├── EcomListedProductRepository.kt
    │   │   │   ├── EcomCartRepository.kt
    │   │   │   ├── EcomCartItemRepository.kt
    │   │   │   ├── CustomerAddressRepository.kt
    │   │   │   ├── EcomOrderRepository.kt
    │   │   │   └── EcomOrderLineItemRepository.kt
    │   │   ├── service/
    │   │   │   ├── StorefrontService.kt             # implements EcomStorefrontLookupService (core)
    │   │   │   ├── StorefrontAccessService.kt       # access-list CRUD + Caffeine LRU cache + access check
    │   │   │   ├── CatalogSyncService.kt
    │   │   │   ├── CartService.kt
    │   │   │   ├── CheckoutService.kt
    │   │   │   ├── EcomOrderService.kt
    │   │   │   └── CustomerAddressService.kt
    │   │   ├── kafka/
    │   │   │   ├── EcomCatalogKafkaConsumer.kt
    │   │   │   ├── EcomOrderStatusKafkaConsumer.kt
    │   │   │   └── EcomOrderKafkaProducer.kt
    │   │   └── exception/
    │   │       └── EcomExceptionHandler.kt          # + StoreAccessDeniedException, StoreUnauthenticatedException
    │   └── resources/
    │       └── db/migration/postgresql/
    │           ├── V1.0.30__create_ecom_storefront.sql
    │           ├── V1.0.31__create_ecom_listed_product.sql
    │           ├── V1.0.32__create_ecom_cart_tables.sql
    │           ├── V1.0.33__create_ecom_order_tables.sql
    │           ├── V1.0.34__create_ecom_customer_address.sql
    │           ├── V1.0.35__add_tsvector_search_index.sql
    │           ├── V1.0.36__add_access_mode_to_storefront.sql    # NEW
    │           └── V1.0.37__create_ecom_storefront_access_entry.sql  # NEW
    └── test/kotlin/com/ampairs/ecom/
        ├── service/
        │   ├── StorefrontServiceTest.kt
        │   ├── StorefrontAccessServiceTest.kt       # NEW
        │   ├── CatalogSyncServiceTest.kt
        │   ├── CartServiceTest.kt
        │   ├── CheckoutServiceTest.kt
        │   └── EcomOrderServiceTest.kt
        ├── filter/
        │   └── StorefrontAccessFilterTest.kt        # NEW
        └── integration/
            └── EcomIntegrationTest.kt
```

**Additions to existing modules**:
```
product/
└── src/main/kotlin/com/ampairs/product/
    ├── domain/model/Product.kt           # + isEcomListed: Boolean
    ├── controller/ProductEcomController.kt
    ├── service/ProductEcomService.kt
    └── kafka/EcomCatalogKafkaProducer.kt
    resources/db/migration/postgresql/
    └── V1.0.28__add_ecom_listed_to_product.sql

order/
└── src/main/kotlin/com/ampairs/order/
    ├── domain/model/Order.kt             # + ecomOrderRef: String?
    ├── domain/enums/OrderStatus.kt       # + PENDING_MERCHANT_REVIEW
    └── kafka/EcomOrderPlacedConsumer.kt
    resources/db/migration/postgresql/
    └── V1.0.29__add_ecom_order_ref_to_order.sql

user/
└── src/main/kotlin/com/ampairs/user/
    └── model/User.kt                     # + userType: UserType
    resources/db/migration/postgresql/
    └── V1.0.27__add_user_type_to_app_user.sql

event/
└── src/main/kotlin/com/ampairs/event/domain/kafka/
    ├── EcomCatalogEvent.kt
    ├── EcomOrderPlacedEvent.kt
    └── EcomOrderStatusEvent.kt
```

**Structure Decision**: Single project (Option 1). Backend-only changes. The Angular web storefront and Compose mobile client consume these APIs in separate PRs. All frontend integration is out of scope for this branch.

---

## Implementation Phases

### Phase A: Foundation (migrations, entities, module wiring)

1. Add `user_type` column migration + `UserType` enum to `user` module
2. Add `is_ecom_listed` column migration to `product` module
3. Add `ecom_order_ref` column + `PENDING_MERCHANT_REVIEW` status to `order` module
4. Add Kafka payload DTOs to `event` module (`EcomCatalogEvent`, `EcomOrderPlacedEvent`, `EcomOrderStatusEvent`)
5. Create `ecom` module with `build.gradle.kts` and wire into `ampairs_service`
6. Create all ecom entity migrations (V1.0.30–V1.0.37)
7. Create all ecom JPA entities and repositories
8. Create `StorefrontAccessMode` and `StorefrontAccessIdentifierType` enums

### Phase B: Catalog Sync (management → ecom)

9. Add `EcomCatalogKafkaProducer` to `product` module
10. Add `ProductEcomService` (list/unlist) + `ProductEcomController` to `product` module
11. Add `EcomKafkaConfig` to `ecom` module (consumer factory for catalog + order-status topics)
12. Add `EcomCatalogKafkaConsumer` + `CatalogSyncService` to `ecom` module

### Phase B.5: Storefront Access Control (FR-030 – FR-041)

This phase is inserted between Foundation and Storefront Management because `StorefrontAccessFilter` must exist before any public or management endpoint is wired — it is the gateway that all store-scoped requests flow through.

13. Migrations V1.0.36 (`access_mode` column) and V1.0.37 (`ecom_storefront_access_entry` table)
14. `StorefrontAccessEntry` entity + `StorefrontAccessEntryRepository`
15. `StorefrontAccessService`: access-list CRUD, Caffeine LRU cache (max 100 storefront entries, LRU eviction), `checkAccess(storefront, principal): Boolean`, cache invalidation on every write
16. `StorefrontAccessFilter` (registered as `HandlerInterceptor` via `EcomWebMvcConfig`):
    - Applies to paths matching `/api/v1/store/{slug}/**`
    - Resolves slug → `Storefront` (throws `StorefrontNotFoundException` if not PUBLISHED)
    - Sets `TenantContextHolder.setCurrentTenant(storefront.ownerId)` in try/finally
    - Stores resolved `Storefront` as request attribute `"resolvedStorefront"`
    - If `accessMode = RESTRICTED`: extracts principal from `SecurityContextHolder`; if workspace member → bypass; if authenticated end customer → extract USER_ID, PHONE, EMAIL, EXTERNAL_ID claim and call `storefront AccessService.checkAccess()`; if no match → throw `StoreAccessDeniedException`; if unauthenticated → throw `StoreUnauthenticatedException`
    - If `accessMode = PUBLIC`: no access check
17. Update `EcomExceptionHandler` with `StoreAccessDeniedException → 403 STORE_ACCESS_DENIED` and `StoreUnauthenticatedException → 403 STORE_UNAUTHENTICATED`
18. `StorefrontAccessController` + DTOs: `GET /api/v1/ecom/management/storefront/access` (list entries, paginated), `POST /` (add entry), `DELETE /{uid}` (remove entry), `POST /bulk-import` (CSV: `identifier_type, identifier_value`; returns `StorefrontAccessBulkImportResult` with success count and per-row errors)
19. Update `StorefrontResponse` and `StorefrontUpdateRequest` DTOs to include `accessMode: StorefrontAccessMode`
20. Update `StorefrontService.updateStorefront()` to handle `accessMode` changes and invalidate the Caffeine cache entry for the affected storefront

### Phase C: Storefront Management API

21. `StorefrontService` (create, publish, unpublish, get, update with `accessMode`)
22. `StorefrontManagementController`

### Phase D: Storefront Public API

23. `StorefrontPublicController` — reads pre-resolved `Storefront` from request attribute `"resolvedStorefront"` set by `StorefrontAccessFilter`; does NOT re-resolve the slug inline
24. `tsvector`/GIN full-text search `@Query` in `EcomListedProductRepository`

### Phase E: Cart

25. `CartService` (create, get, add item, update qty, remove item, merge on login)
26. `CartController` — reads pre-resolved `Storefront` from request attribute

### Phase F: Customer Auth Extension and Account

27. Extend existing `auth` module: `userType` field + `user_type` JWT claim + `external_id` claim support in token generation; `JwtTokenValidator` extracts `user_type`, `external_id` as security attributes
28. `CustomerAddressService` + `CustomerAccountController`

### Phase G: Checkout and Order Flow

29. `CheckoutService` (validate cart, create EcomOrder, publish `EcomOrderPlacedEvent`)
30. `CheckoutController` — reads pre-resolved `Storefront` from request attribute
31. `EcomOrderKafkaProducer`
32. `EcomOrderPlacedConsumer` in `order` module
33. `EcomOrderStatusKafkaConsumer` + `EcomOrderService`

### Phase H: Merchant Order Review

34. `EcomOrderManagementController` (list, get, edit line items, confirm order)
35. `OrderEcomService` interface + implementation in `order` module

### Phase I: Tests

36. Unit tests for all services including `StorefrontAccessServiceTest` and `StorefrontAccessFilterTest`
37. Integration test: full order flow with embedded Kafka + Testcontainers PostgreSQL
38. Access-control test: verify 100% rejection for unlisted identities across all four identifier types (SC-009)

---

## Key Design Decisions

### StorefrontAccessFilter as HandlerInterceptor

The spec mandates (FR-031, FR-038) that access control runs in the storefront resolution filter, not in controllers. The original plan resolved the slug inline in each controller — this violated FR-038. The new approach:

- `StorefrontAccessFilter` implements `HandlerInterceptor` and is registered via `EcomWebMvcConfig.addInterceptors()` for the path pattern `/api/v1/store/**`
- `preHandle()` performs: slug extraction → storefront resolution → TenantContext setup → access gate → stores `Storefront` as `request.setAttribute("resolvedStorefront", storefront)`
- Controllers (`StorefrontPublicController`, `CartController`, `CheckoutController`) read the pre-resolved storefront from the request attribute, eliminating redundant slug resolution
- `afterCompletion()` calls `TenantContextHolder.clear()` — replaces the try/finally in each controller

### Access Check Logic (FR-033, FR-039)

```
StorefrontAccessFilter.preHandle():
  val storefront = storefrontService.getPublishedStorefrontBySlug(slug)
  TenantContextHolder.setCurrentTenant(storefront.ownerId)
  request.setAttribute("resolvedStorefront", storefront)
  
  if (storefront.accessMode == RESTRICTED) {
      val principal = SecurityContextHolder.getContext().authentication
      if (principal == null || !principal.isAuthenticated) throw StoreUnauthenticatedException()
      if (principal.hasWorkspaceRole(storefront.ownerId)) return true  // FR-039: workspace member bypass
      val identifiers = extractIdentifiers(principal)  // USER_ID, PHONE, EMAIL, EXTERNAL_ID claim
      if (!storefrontAccessService.checkAccess(storefront.uid, identifiers)) throw StoreAccessDeniedException()
  }
  return true
```

### Caffeine LRU Cache (FR-040)

`StorefrontAccessService` declares a `LoadingCache<String, List<StorefrontAccessEntry>>` (key = `storefrontId`, max 100 entries, LRU). The cache is populated on first access and explicitly evicted on any write (add, remove, bulk import, `accessMode` toggle). With a 100-entry cap, the worst case is 100 full access-list loads in memory simultaneously — acceptable for v1 scale.

### FR-036: Portable Entry Resolution

No registration hook is required. The filter extracts the authenticated principal's phone and email from their platform account at request time and matches against stored entries. A pre-seeded `PHONE` or `EMAIL` entry automatically grants access to any matching account regardless of when that account was created.

---

## Complexity Tracking

*No constitution violations.*

**Deferred items**:
- **SC-007 automated isolation tests**: Full per-workspace isolation test suite deferred to a follow-up PR. Architecture supports this via `OwnableBaseDomain` and explicit `workspaceId` columns.
- **SC-009 full rejection test suite**: Unit tests in `StorefrontAccessFilterTest` cover the four identifier types and workspace-member bypass. End-to-end Testcontainers integration test for the access gate is targeted for Phase I but deferred to follow-up if time-constrained.
- **Cross-module OrderEcomService bridge**: `EcomOrderService.confirmOrder()` calls `OrderEcomService.confirmEcomOrder()` directly in the monolith. Replace with Kafka on future extraction.
- **EcomStorefrontLookupService bridge**: `ProductEcomService` injects `EcomStorefrontLookupService` (defined in `core`, implemented by `ecom`). Replace with internal REST or Kafka reverse lookup on extraction.
- **Caffeine dependency**: Add `implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")` to `ecom/build.gradle.kts`. Caffeine is already a transitive dependency of Spring Boot Cache; explicit declaration pins the version.
