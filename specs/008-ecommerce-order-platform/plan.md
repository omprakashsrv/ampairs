# Implementation Plan: Ecommerce Order Platform

**Branch**: `008-ecommerce-order-platform` | **Date**: 2026-05-30 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `/specs/008-ecommerce-order-platform/spec.md`

---

## Summary

Build a merchant-configurable ecommerce storefront module (`ecom`) within the Ampairs monorepo. Merchants create and publish storefronts; products sync from management via Kafka catalog events; end customers browse, cart, and checkout; ecom orders flow back to management via Kafka for inventory deduction and fulfilment tracking. Single platform-wide customer identity reuses the existing `auth` module with a new `END_CUSTOMER` user type.

---

## Technical Context

**Language/Version**: Kotlin 2.3 / Java 21  
**Primary Dependencies**: Spring Boot 4.0, Spring Kafka 3.x, Spring Data JPA, Spring Security, Hibernate 6.x, Jackson (global snake_case config)  
**Storage**: PostgreSQL (primary persistence, `tsvector`/GIN full-text search for product discovery), Kafka (event bus — 3 dedicated topics)  
**Testing**: JUnit 5 + Testcontainers (PostgreSQL, Kafka), Mockito-Kotlin  
**Target Platform**: Linux server (JVM, runs as part of `ampairs_service`)  
**Project Type**: Single project — new `ecom` module assembled into existing `ampairs_service`  
**Performance Goals**: Product search < 1s p95 (SC-002), catalog sync < 5s p99 (SC-003), 500 concurrent shoppers (SC-004)  
**Constraints**: Payment out of scope; no Elasticsearch in v1 (PostgreSQL `tsvector`/GIN); no shipping integration  
**Scale/Scope**: 500 concurrent shoppers across all storefronts; ~10 Kafka topics total; ~350 new Kotlin source lines (estimated); 9 new Flyway migrations

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Timestamps — `Instant` everywhere | ✅ PASS | All entity/DTO timestamp fields use `Instant`; DB columns `TIMESTAMP`/`TIMESTAMPTZ` |
| II. DTO isolation | ✅ PASS | All controllers accept Request DTOs and return Response DTOs; entity ↔ DTO via extension functions |
| III. JSON conventions — no `@JsonProperty` | ✅ PASS | Global `SNAKE_CASE` strategy applies; no annotations added for standard fields |
| IV. Multi-tenancy — controller sets context | ✅ PASS | `Storefront`/`EcomListedProduct` extend `OwnableBaseDomain`; public storefront routes resolve slug → workspaceId in controller try/finally |
| V. API responses — `ApiResponse<T>` | ✅ PASS | All endpoints return `ApiResponse.success(...)` |
| VI. Centralized exception handling | ✅ PASS | No try/catch in controllers for business exceptions |
| VII. `@EntityGraph` for relationships | ✅ PASS | Named entity graphs defined on `EcomCart`, `EcomOrder` |
| VIII. Angular Material 3 | ✅ N/A | No frontend code in this feature |
| IX. Module boundaries | ✅ PASS | New `ecom` bounded context; cross-module via public service interfaces and Kafka only |
| X. Compose parity | ✅ N/A | No mobile code in this feature |
| XI. Security & secrets | ✅ PASS | Kafka bootstrap URL via env var; JWT signing reuses existing key infrastructure |

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
    │   │   │   └── EcomKafkaConfig.kt            # Kafka producer/consumer config (separate from WebSocket Kafka)
    │   │   ├── controller/
    │   │   │   ├── StorefrontPublicController.kt  # GET /api/v1/store/{slug}/...
    │   │   │   ├── CartController.kt
    │   │   │   ├── CheckoutController.kt
    │   │   │   ├── CustomerAccountController.kt   # /api/v1/ecom/account/...
    │   │   │   └── StorefrontManagementController.kt  # /api/v1/ecom/management/...
    │   │   ├── domain/
    │   │   │   ├── dto/
    │   │   │   │   ├── StorefrontRequest.kt
    │   │   │   │   ├── StorefrontUpdateRequest.kt
    │   │   │   │   ├── StorefrontResponse.kt
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
    │   │   │   │   ├── StockStatus.kt
    │   │   │   │   ├── CartStatus.kt
    │   │   │   │   ├── EcomOrderStatus.kt
    │   │   │   │   └── EcomLineItemStatus.kt
    │   │   │   └── model/
    │   │   │       ├── Storefront.kt
    │   │   │       ├── EcomListedProduct.kt
    │   │   │       ├── EcomCart.kt
    │   │   │       ├── EcomCartItem.kt
    │   │   │       ├── CustomerAddress.kt
    │   │   │       ├── EcomOrder.kt
    │   │   │       └── EcomOrderLineItem.kt
    │   │   ├── repository/
    │   │   │   ├── StorefrontRepository.kt
    │   │   │   ├── EcomListedProductRepository.kt
    │   │   │   ├── EcomCartRepository.kt
    │   │   │   ├── EcomCartItemRepository.kt
    │   │   │   ├── CustomerAddressRepository.kt
    │   │   │   ├── EcomOrderRepository.kt
    │   │   │   └── EcomOrderLineItemRepository.kt
    │   │   ├── service/
    │   │   │   ├── StorefrontService.kt           # also implements EcomStorefrontLookupService (core)
    │   │   │   ├── CatalogSyncService.kt          # Applies EcomCatalogEvents to EcomListedProduct
    │   │   │   ├── CartService.kt
    │   │   │   ├── CheckoutService.kt
    │   │   │   ├── EcomOrderService.kt
    │   │   │   └── CustomerAddressService.kt
    │   │   ├── kafka/
    │   │   │   ├── EcomCatalogKafkaConsumer.kt    # Consumes ecom-catalog-events
    │   │   │   ├── EcomOrderStatusKafkaConsumer.kt # Consumes ecom-order-status
    │   │   │   └── EcomOrderKafkaProducer.kt      # Produces ecom-order-placed
    │   │   └── exception/
    │   │       └── EcomExceptionHandler.kt
    │   └── resources/
    │       └── db/migration/postgresql/
    │           ├── V1.0.30__create_ecom_storefront.sql
    │           ├── V1.0.31__create_ecom_listed_product.sql
    │           ├── V1.0.32__create_ecom_cart_tables.sql
    │           ├── V1.0.33__create_ecom_order_tables.sql
    │           ├── V1.0.34__create_ecom_customer_address.sql
    │           └── V1.0.35__add_tsvector_search_index.sql
    └── test/kotlin/com/ampairs/ecom/
        ├── service/
        │   ├── StorefrontServiceTest.kt
        │   ├── CatalogSyncServiceTest.kt
        │   ├── CartServiceTest.kt
        │   ├── CheckoutServiceTest.kt
        │   └── EcomOrderServiceTest.kt
        └── integration/
            └── EcomIntegrationTest.kt
```

**Additions to existing modules**:
```
product/
└── src/main/kotlin/com/ampairs/product/
    ├── domain/model/Product.kt           # + isEcomListed: Boolean
    ├── controller/ProductEcomController.kt  # PUT /api/v1/products/{id}/ecom/list|unlist
    ├── service/ProductEcomService.kt     # list/unlist logic + Kafka publish
    └── kafka/EcomCatalogKafkaProducer.kt # Produces ecom-catalog-events
    resources/db/migration/postgresql/
    └── V1.0.28__add_ecom_listed_to_product.sql

order/
└── src/main/kotlin/com/ampairs/order/
    ├── domain/model/Order.kt             # + ecomOrderRef: String?
    ├── domain/enums/OrderStatus.kt       # + PENDING_MERCHANT_REVIEW
    └── kafka/EcomOrderPlacedConsumer.kt  # Consumes ecom-order-placed
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

**Structure Decision**: Single project (Option 1). Backend-only changes. The Angular web storefront (`store.ampairs.com/{slug}`) and Compose mobile client will consume these APIs in separate PRs against the `ampairs-web` and `ampairs-app` repos. All frontend integration is out of scope for this branch.

---

## Implementation Phases

### Phase A: Foundation (migrations, entities, module wiring)
1. Add `user_type` column migration + `UserType` enum to `user` module
2. Add `is_ecom_listed` column migration to `product` module
3. Add `ecom_order_ref` column + `PENDING_MERCHANT_REVIEW` status to `order` module
4. Add Kafka payload DTOs to `event` module (`EcomCatalogEvent`, `EcomOrderPlacedEvent`, `EcomOrderStatusEvent`)
5. Create `ecom` module with `build.gradle.kts` and wire into `ampairs_service`
6. Create all ecom entity migrations (V1.0.30–V1.0.35)
7. Create all ecom JPA entities and repositories

### Phase B: Catalog Sync (management → ecom)
8. Add `EcomCatalogKafkaProducer` to `product` module
9. Add `ProductEcomService` (list/unlist) + `ProductEcomController` to `product` module
10. Add `EcomKafkaConfig` to `ecom` module (consumer factory for catalog + order-status topics)
11. Add `EcomCatalogKafkaConsumer` + `CatalogSyncService` to `ecom` module

### Phase C: Storefront Management API
12. `StorefrontService` (create, publish, unpublish, get)
13. `StorefrontManagementController` (POST/GET/PUT/publish/unpublish)

### Phase D: Storefront Public API
14. `StorefrontPublicController` (GET slug, list products, search, get product)
15. `tsvector`/GIN full-text search `@Query` in `EcomListedProductRepository`

### Phase E: Cart
16. `CartService` (create, get, add item, update qty, remove item, merge on login)
17. `CartController`

### Phase F: Customer Auth Extension and Account
18. Extend existing `auth` module: add optional `userType` field to `UserRegistrationRequest`; add `user_type` claim to JWT generation; no new auth controller or service in `ecom`
19. `CustomerAddressService` + `CustomerAccountController`

### Phase G: Checkout and Order Flow
21. `CheckoutService` (validate cart, create EcomOrder, publish `EcomOrderPlacedEvent`)
22. `CheckoutController`
23. `EcomOrderKafkaProducer` (produces to `ecom-order-placed`)
24. `EcomOrderPlacedConsumer` in `order` module (inventory check, create Order, publish status event)
25. `EcomOrderStatusKafkaConsumer` + `EcomOrderService` (apply status updates from management)

### Phase H: Merchant Order Review
26. `EcomOrderManagementController` (list orders, get order, edit line items, confirm order)
27. Confirmation triggers `EcomOrderStatusEvent(CONFIRMED)` back to ecom module

### Phase I: Tests
28. Unit tests for all services (Mockito-Kotlin)
29. Integration test: full order flow with embedded Kafka + Testcontainers PostgreSQL

---

## Complexity Tracking

*No constitution violations.*

**Deferred items**:
- **SC-007 automated isolation tests**: Full per-workspace isolation test suite (verifying no cross-tenant data leakage in ecom tables) is deferred to a follow-up PR. The architecture supports this via `OwnableBaseDomain` and explicit `workspaceId` columns, but Testcontainers integration tests covering the isolation invariant are out of scope for this branch.
- **Cross-module OrderEcomService bridge**: `EcomOrderService.confirmOrder(...)` calls `OrderEcomService.confirmEcomOrder(...)` directly in the monolith (T046). This is a valid Spring bean injection across module boundaries. When `ecom` is extracted into its own service, this call is replaced with a Kafka event on a new `ecom-order-confirm` topic.
- **EcomStorefrontLookupService bridge**: `ProductEcomService` injects `EcomStorefrontLookupService` (defined in `core`, implemented by `ecom`). When extracted, replace with an internal REST call or Kafka catalog topic reverse lookup.
