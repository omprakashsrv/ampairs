# Tasks: Ecommerce Order Platform

**Input**: Design documents from `/specs/008-ecommerce-order-platform/`  
**Branch**: `008-ecommerce-order-platform`  
**Stack**: Kotlin 2.3 / Java 21, Spring Boot 4.0, Spring Kafka, Spring Data JPA, PostgreSQL, Testcontainers

**Organization**: Tasks grouped by user story. Each story is an independently deliverable increment.

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel with other [P]-marked tasks in the same phase (different files)
- **[Story]**: US1–US6 maps to spec.md user stories; **F** = Foundation; **S** = Setup
- File paths are relative to repo root

---

## Phase 1: Setup

**Purpose**: Create the `ecom` module and wire it into the service. Blocking for all other phases.

- [X] T001 [S] Create `ecom/build.gradle.kts` — declare dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-kafka`, `jackson-module-kotlin`, `jackson-datatype-jsr310`, `org.postgresql:postgresql`, `kotlin-reflect`; add `api(project(":core"))`, `api(project(":auth"))`, `api(project(":event"))`, `api(project(":user"))`; disable `bootJar` (no main class)
- [X] T002 [S] Add `:ecom` to `ampairs_service/build.gradle.kts` `implementation(project(":ecom"))` dependency block and to `settings.gradle.kts` includes list
- [X] T003 [P] [S] Create `ecom/src/main/kotlin/com/ampairs/ecom/config/Constants.kt` — define `ECOM_ORDER_PREFIX = "ECO"`, `CART_SESSION_TTL_HOURS = 24L`, `AUTH_SESSION_TTL_DAYS = 30L`
- [X] T004 [P] [S] Create Kafka payload DTOs in `event/src/main/kotlin/com/ampairs/event/domain/kafka/`: `EcomCatalogEvent.kt` (fields: `eventType: CatalogEventType`, `workspaceId`, `storefrontId`, `managementProductId`, nullable `name`, `brand`, `category`, `subcategory`, `price: BigDecimal?`, `stockQuantity: Int?`, `imageUrls: List<String>?`, `description`, `publishedAt: Instant`) + `CatalogEventType` enum (`PRODUCT_LISTED`, `PRODUCT_UNLISTED`, `PRICE_UPDATED`, `STOCK_UPDATED`, `DETAILS_UPDATED`); `EcomOrderPlacedEvent.kt` (fields: `ecomOrderRef`, `workspaceId`, `storefrontId`, `customerId`, `customerName`, `customerEmail`, `customerPhone?`, `deliveryAddress: Address`, `lineItems: List<EcomOrderLineItemPayload>`, `subtotal: BigDecimal`, `totalAmount: BigDecimal`, `placedAt: Instant`) + `EcomOrderLineItemPayload` data class; `EcomOrderStatusEvent.kt` (fields: `ecomOrderRef`, `workspaceId`, `newStatus: String`, `managementOrderRef: String?`, `confirmedLineItems: List<ConfirmedLineItemPayload>?`, `updatedAt: Instant`) + `ConfirmedLineItemPayload` data class

**Checkpoint ✅ Phase 1 done**: `./gradlew :ecom:compileKotlin` succeeds (empty module compiles)

---

## Phase 2: Foundation

**Purpose**: All cross-module entity changes, all PostgreSQL migrations, all JPA entities and repositories. No user story implementation can begin until this phase is complete.

**⚠️ CRITICAL**: Migrations must be applied in order (V1.0.27 → V1.0.35). Entities depend on migrations. Repos depend on entities.

### Cross-Module Entity Additions

- [X] T005 [F] Add `UserType` enum (`MERCHANT_USER`, `END_CUSTOMER`) to `user/src/main/kotlin/com/ampairs/user/model/UserType.kt`; add field `@Column(name = "user_type", nullable = false, length = 20) @Enumerated(EnumType.STRING) var userType: UserType = UserType.MERCHANT_USER` to `user/src/main/kotlin/com/ampairs/user/model/User.kt`; create `user/src/main/resources/db/migration/postgresql/V1.0.27__add_user_type_to_app_user.sql` — `ALTER TABLE app_user ADD COLUMN user_type VARCHAR(20) NOT NULL DEFAULT 'MERCHANT_USER';`
- [X] T006 [P] [F] Add field `@Column(name = "is_ecom_listed", nullable = false) var isEcomListed: Boolean = false` to `product/src/main/kotlin/com/ampairs/product/domain/model/Product.kt`; create `product/src/main/resources/db/migration/postgresql/V1.0.28__add_ecom_listed_to_product.sql` — `ALTER TABLE product ADD COLUMN is_ecom_listed BOOLEAN NOT NULL DEFAULT FALSE;`
- [X] T007 [P] [F] Add field `@Column(name = "ecom_order_ref", length = 50) var ecomOrderRef: String? = null` to `order/src/main/kotlin/com/ampairs/order/domain/model/Order.kt`; add `PENDING_MERCHANT_REVIEW` value to `order/src/main/kotlin/com/ampairs/order/domain/enums/OrderStatus.kt`; add `findByEcomOrderRef(ref: String): Order?` to `order/src/main/kotlin/com/ampairs/order/repository/OrderRepository.kt`; create `order/src/main/resources/db/migration/postgresql/V1.0.29__add_ecom_order_ref_to_order.sql` — `ALTER TABLE customer_order ADD COLUMN ecom_order_ref VARCHAR(50) NULL; CREATE UNIQUE INDEX idx_order_ecom_ref ON customer_order(ecom_order_ref) WHERE ecom_order_ref IS NOT NULL;`

### Ecom Module Migrations

- [X] T008 [F] Create `ecom/src/main/resources/db/migration/postgresql/V1.0.30__create_ecom_storefront.sql` — `CREATE TABLE ecom_storefront (id BIGSERIAL PRIMARY KEY, uid VARCHAR(200) NOT NULL UNIQUE, owner_id VARCHAR(200), ref_id VARCHAR(255), created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), name VARCHAR(100) NOT NULL, slug VARCHAR(50) NOT NULL UNIQUE, description TEXT, logo_url VARCHAR(500), banner_url VARCHAR(500), status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', published_at TIMESTAMPTZ, unpublished_at TIMESTAMPTZ); CREATE INDEX idx_ecom_storefront_owner ON ecom_storefront(owner_id); CREATE INDEX idx_ecom_storefront_status ON ecom_storefront(status);`
- [X] T009 [P] [F] Create `ecom/src/main/resources/db/migration/postgresql/V1.0.31__create_ecom_listed_product.sql` — table `ecom_listed_product` with columns: `id BIGSERIAL PK`, `uid VARCHAR(200) UNIQUE`, `owner_id VARCHAR(200)`, `ref_id VARCHAR(255)`, `created_at`/`updated_at TIMESTAMPTZ`, `storefront_id VARCHAR(200) NOT NULL`, `management_product_id VARCHAR(200) NOT NULL`, `name VARCHAR(255) NOT NULL`, `description TEXT`, `image_urls JSONB NOT NULL DEFAULT '[]'`, `brand VARCHAR(255)`, `category VARCHAR(255)`, `subcategory VARCHAR(255)`, `price NUMERIC(19,4) NOT NULL`, `stock_quantity INT NOT NULL DEFAULT 0`, `stock_status VARCHAR(20) NOT NULL DEFAULT 'OUT_OF_STOCK'`, `is_visible BOOLEAN NOT NULL DEFAULT TRUE`, `last_synced_at TIMESTAMPTZ NOT NULL`; indexes: `(storefront_id)`, `(management_product_id)`, `(storefront_id, is_visible)`, `(storefront_id, stock_status)`, unique `(storefront_id, management_product_id)`
- [X] T010 [P] [F] Create `ecom/src/main/resources/db/migration/postgresql/V1.0.32__create_ecom_cart_tables.sql` — table `ecom_cart`: `id BIGSERIAL PK`, `uid VARCHAR(200) UNIQUE`, `created_at`/`updated_at TIMESTAMPTZ`, `storefront_id VARCHAR(200) NOT NULL`, `customer_id VARCHAR(200)`, `session_token VARCHAR(200) NOT NULL UNIQUE`, `expires_at TIMESTAMPTZ NOT NULL`, `status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'`; indexes: `(session_token)`, `(customer_id)`, `(storefront_id)`, `(expires_at)`; table `ecom_cart_item`: `id BIGSERIAL PK`, `uid VARCHAR(200) UNIQUE`, `created_at`/`updated_at TIMESTAMPTZ`, `cart_id VARCHAR(200) NOT NULL`, `listed_product_id VARCHAR(200) NOT NULL`, `management_product_id VARCHAR(200) NOT NULL`, `product_name VARCHAR(255) NOT NULL`, `unit_price NUMERIC(19,4) NOT NULL`, `quantity INT NOT NULL`, `primary_image_url VARCHAR(500)`; index `(cart_id)`
- [X] T011 [P] [F] Create `ecom/src/main/resources/db/migration/postgresql/V1.0.33__create_ecom_order_tables.sql` — table `ecom_order`: `id BIGSERIAL PK`, `uid VARCHAR(200) UNIQUE`, `created_at`/`updated_at TIMESTAMPTZ`, `ecom_order_ref VARCHAR(50) NOT NULL UNIQUE`, `storefront_id VARCHAR(200) NOT NULL`, `workspace_id VARCHAR(200) NOT NULL`, `customer_id VARCHAR(200) NOT NULL`, `customer_name VARCHAR(255) NOT NULL`, `customer_email VARCHAR(320) NOT NULL`, `customer_phone VARCHAR(20)`, `delivery_address JSONB NOT NULL`, `status VARCHAR(30) NOT NULL DEFAULT 'PLACED'`, `management_order_ref VARCHAR(255)`, `subtotal NUMERIC(19,4) NOT NULL`, `total_amount NUMERIC(19,4) NOT NULL`, `notes TEXT`, `placed_at TIMESTAMPTZ NOT NULL`, `confirmed_at TIMESTAMPTZ`, `merchant_reviewed_at TIMESTAMPTZ`; indexes: `(ecom_order_ref)`, `(storefront_id)`, `(customer_id)`, `(workspace_id)`, `(status)`; table `ecom_order_line_item`: `id BIGSERIAL PK`, `uid VARCHAR(200) UNIQUE`, `created_at`/`updated_at TIMESTAMPTZ`, `ecom_order_id VARCHAR(200) NOT NULL`, `listed_product_id VARCHAR(200) NOT NULL`, `management_product_id VARCHAR(200) NOT NULL`, `product_name VARCHAR(255) NOT NULL`, `unit_price NUMERIC(19,4) NOT NULL`, `quantity_ordered INT NOT NULL`, `quantity_confirmed INT`, `line_total NUMERIC(19,4) NOT NULL`, `status VARCHAR(20) NOT NULL DEFAULT 'ORDERED'`, `shipment_group VARCHAR(100)`; index `(ecom_order_id)`
- [X] T012 [P] [F] Create `ecom/src/main/resources/db/migration/postgresql/V1.0.34__create_ecom_customer_address.sql` — table `ecom_customer_address`: `id BIGSERIAL PK`, `uid VARCHAR(200) UNIQUE`, `created_at`/`updated_at TIMESTAMPTZ`, `customer_id VARCHAR(200) NOT NULL`, `label VARCHAR(50)`, `address_line1 VARCHAR(255) NOT NULL`, `address_line2 VARCHAR(255)`, `city VARCHAR(100) NOT NULL`, `state VARCHAR(100) NOT NULL`, `pin_code VARCHAR(20) NOT NULL`, `country VARCHAR(10) NOT NULL DEFAULT 'IN'`, `phone VARCHAR(20)`, `is_default BOOLEAN NOT NULL DEFAULT FALSE`; index `(customer_id)`
- [X] T013 [P] [F] Create `ecom/src/main/resources/db/migration/postgresql/V1.0.35__add_tsvector_search_index.sql` — `ALTER TABLE ecom_listed_product ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (to_tsvector('english', coalesce(name,'') || ' ' || coalesce(brand,'') || ' ' || coalesce(category,'') || ' ' || coalesce(subcategory,''))) STORED; CREATE INDEX idx_ecom_product_search ON ecom_listed_product USING GIN(search_vector);`

### Ecom JPA Entities + Repositories

- [X] T014 [F] Create `ecom/src/main/kotlin/com/ampairs/ecom/domain/enums/StorefrontStatus.kt` (`DRAFT`, `PUBLISHED`, `UNPUBLISHED`); create entity `ecom/src/main/kotlin/com/ampairs/ecom/domain/model/Storefront.kt` extending `OwnableBaseDomain` with all fields from data-model.md; create `ecom/src/main/kotlin/com/ampairs/ecom/repository/StorefrontRepository.kt` with: `findBySlug(slug: String): Storefront?`, `findBySlugAndStatus(slug: String, status: StorefrontStatus): Storefront?`, `existsBySlug(slug: String): Boolean`, `findByOwnerId(ownerId: String): Storefront?`
- [X] T015 [P] [F] Create `ecom/src/main/kotlin/com/ampairs/ecom/domain/enums/StockStatus.kt` (`IN_STOCK`, `LIMITED`, `OUT_OF_STOCK`); create entity `ecom/src/main/kotlin/com/ampairs/ecom/domain/model/EcomListedProduct.kt` extending `OwnableBaseDomain` with all fields from data-model.md — note `imageUrls: List<String>` uses `@JdbcTypeCode(SqlTypes.JSON)` + `@Column(columnDefinition = "jsonb")`; create `ecom/src/main/kotlin/com/ampairs/ecom/repository/EcomListedProductRepository.kt` with: `findByStorefrontIdAndManagementProductId`, `findByStorefrontIdAndIsVisible`, paginated `findByStorefrontIdAndIsVisibleTrue`, `@Query` for `tsvector` full-text search using `@@ plainto_tsquery`, `@Query` for filter by category/brand/subcategory
- [X] T016 [P] [F] Create `ecom/src/main/kotlin/com/ampairs/ecom/domain/enums/CartStatus.kt` (`ACTIVE`, `CONVERTED`, `MERGED`, `ABANDONED`); create entity `ecom/src/main/kotlin/com/ampairs/ecom/domain/model/EcomCart.kt` extending `BaseDomain` with `@NamedEntityGraph("EcomCart.withItems")`; create entity `ecom/src/main/kotlin/com/ampairs/ecom/domain/model/EcomCartItem.kt` extending `BaseDomain` — field is `primaryImageUrl: String?` (single thumbnail URL, first entry of the listed product's `imageUrls`); create repos `EcomCartRepository.kt` (`findBySessionToken`, `findByCustomerIdAndStorefrontIdAndStatus`, `findByExpiresAtBefore`) and `EcomCartItemRepository.kt` (`findByCartId`, `findByCartIdAndListedProductId`, `deleteByCartId`)
- [X] T017 [P] [F] Create `ecom/src/main/kotlin/com/ampairs/ecom/domain/enums/EcomOrderStatus.kt` (`PLACED`, `PENDING_MERCHANT_REVIEW`, `CONFIRMED`, `PROCESSING`, `DISPATCHED`, `DELIVERED`, `CANCELLED`) and `EcomLineItemStatus.kt` (`ORDERED`, `CONFIRMED`, `CANCELLED`); create entity `ecom/src/main/kotlin/com/ampairs/ecom/domain/model/EcomOrder.kt` extending `BaseDomain` with `@NamedEntityGraph("EcomOrder.withItems")` and `obtainSeqIdPrefix() = "ECO"`; create entity `ecom/src/main/kotlin/com/ampairs/ecom/domain/model/EcomOrderLineItem.kt` extending `BaseDomain`; create repos `EcomOrderRepository.kt` (`findByEcomOrderRef`, `findByCustomerIdAndStorefrontId` paginated, `findByWorkspaceId` paginated, `findByWorkspaceIdAndStatus`) and `EcomOrderLineItemRepository.kt` (`findByEcomOrderId`)
- [X] T018 [P] [F] Create entity `ecom/src/main/kotlin/com/ampairs/ecom/domain/model/CustomerAddress.kt` extending `BaseDomain`; create `ecom/src/main/kotlin/com/ampairs/ecom/repository/CustomerAddressRepository.kt` (`findByCustomerId`, `findByCustomerIdAndUid`, `findByCustomerIdAndIsDefaultTrue`, `countByCustomerId`)

### Ecom Infrastructure Beans

- [X] T019 [F] Create `ecom/src/main/kotlin/com/ampairs/ecom/config/EcomKafkaConfig.kt` — define two `ConcurrentKafkaListenerContainerFactory` beans: `ecomCatalogListenerContainerFactory` (group `ecom-catalog-consumer`, bootstrap from `${ecom.kafka.bootstrap-servers:localhost:9092}`, `AUTO_OFFSET_RESET=earliest`, manual ack) and `ecomOrderStatusListenerContainerFactory` (group `ecom-order-status-consumer`); add `${ecom.kafka.bootstrap-servers}` property to `ampairs_service/src/main/resources/application.yml` under a new `ecom.kafka` section
- [X] T020 [P] [F] Create `ecom/src/main/kotlin/com/ampairs/ecom/exception/EcomExceptionHandler.kt` — `@RestControllerAdvice` in `com.ampairs.ecom` package; handle `StorefrontNotFoundException` (404), `StorefrontSlugConflictException` (409), `ProductUnavailableException` (409), `InsufficientStockException` (422 with `available_quantity`), `CartExpiredException` (404), `EmptyCartException` (400), `EcomOrderNotFoundException` (404); all return `ApiResponse` error bodies

**Checkpoint ✅ Phase 2 done**: `./gradlew :ampairs_service:flywayMigrate` applies V1.0.27–V1.0.35 cleanly; `./gradlew :ecom:compileKotlin` and `:user:compileKotlin` and `:product:compileKotlin` and `:order:compileKotlin` all pass

---

## Phase 3: User Story 2 — Merchant Creates and Configures Their Storefront (Priority: P1)

**Goal**: A merchant can create, configure, publish, and unpublish their storefront from within the Ampairs management dashboard.

**Independent Test**: POST to create a storefront → returns 201 with `status: DRAFT`; PUT publish → returns `status: PUBLISHED`; GET `store.ampairs.com/green-mart` returns storefront info; duplicate slug → 409.

- [X] T021 [US2] Create DTO files: `ecom/src/main/kotlin/com/ampairs/ecom/domain/dto/StorefrontRequest.kt` (`@field:NotBlank name`, `@field:Pattern(regexp="[a-z0-9-]+") @field:Size(min=3, max=50) slug`, nullable `description`, `logoUrl`, `bannerUrl`) and `ecom/src/main/kotlin/com/ampairs/ecom/domain/dto/StorefrontResponse.kt` (all public fields); create `ecom/src/main/kotlin/com/ampairs/ecom/domain/dto/StorefrontUpdateRequest.kt` (no `slug` field — immutable after creation; all other fields nullable for partial update: `name: String?`, `description: String?`, `logoUrl: String?`, `bannerUrl: String?`) with extension function `StorefrontUpdateRequest.applyTo(storefront: Storefront): Storefront` that only sets non-null fields; add extension functions `Storefront.asStorefrontResponse(): StorefrontResponse` and `StorefrontRequest.toStorefront(ownerId: String): Storefront`
- [X] T022 [US2] Create `ecom/src/main/kotlin/com/ampairs/ecom/service/StorefrontService.kt` — implement: `createStorefront(request: StorefrontRequest, workspaceId: String): Storefront` (check uniqueness, throw `StorefrontSlugConflictException` on duplicate, throw conflict if workspace already has storefront); `updateStorefront(request: StorefrontUpdateRequest, workspaceId: String): Storefront`; `getStorefront(workspaceId: String): Storefront`; `getPublishedStorefrontBySlug(slug: String): Storefront` (throws `StorefrontNotFoundException` if not PUBLISHED); `publishStorefront(workspaceId: String): Storefront`; `unpublishStorefront(workspaceId: String): Storefront`
- [X] T023 [US2] Create `ecom/src/main/kotlin/com/ampairs/ecom/controller/StorefrontManagementController.kt` — `@RequestMapping("/api/v1/ecom/management")`, `@PreAuthorize` workspace member; set `TenantContextHolder.setCurrentTenant(workspaceId)` in try/finally; implement: `POST /storefront` → `ApiResponse.success(service.createStorefront(...), HttpStatus.CREATED)`; `GET /storefront` → `ApiResponse.success(...)`; `PUT /storefront` → `ApiResponse.success(...)`; `PUT /storefront/publish` → `ApiResponse.success(...)`; `PUT /storefront/unpublish` → `ApiResponse.success(...)`

**Checkpoint ✅ US2 done**: Merchant can create storefront (DRAFT), publish it, unpublish it, update config. Duplicate slug rejected.

---

## Phase 4: User Story 3 — Merchant Publishes Products to Their Storefront (Priority: P1)

**Goal**: Products marked as "listed" in management appear on the storefront within 5 seconds. Price, stock, and detail changes propagate automatically.

**Independent Test**: List a product → within 5s `GET /api/v1/store/{slug}/products` returns it; update price in management → within 5s storefront shows new price; unlist → product disappears from storefront.

- [X] T024 [US3] Create `product/src/main/kotlin/com/ampairs/product/kafka/EcomCatalogKafkaConfig.kt` — define `KafkaTemplate<String, String>` bean (`ecomCatalogKafkaTemplate`) using `${ecom.kafka.bootstrap-servers:localhost:9092}`, acks=1, 3 retries; create `product/src/main/kotlin/com/ampairs/product/kafka/EcomCatalogKafkaProducer.kt` — `fun publish(event: EcomCatalogEvent)` serializes to JSON with `jacksonObjectMapper()` and sends to `"ecom-catalog-events"` topic with key = `event.workspaceId`
- [X] T025 [US3] Define `EcomStorefrontLookupService` interface in `core/src/main/kotlin/com/ampairs/core/service/EcomStorefrontLookupService.kt` with method `findStorefrontIdByWorkspaceId(workspaceId: String): String?`; implement this interface in `ecom` module's `StorefrontService` — this avoids direct cross-module repository injection; create `product/src/main/kotlin/com/ampairs/product/service/ProductEcomService.kt` — inject `EcomCatalogKafkaProducer` and `EcomStorefrontLookupService` (from `core`); implement `listProductOnEcom(productId: String, workspaceId: String)`: set `product.isEcomListed = true`, look up `storefrontId` via `ecomStorefrontLookupService.findStorefrontIdByWorkspaceId(workspaceId)`, publish `EcomCatalogEvent(PRODUCT_LISTED, ...)` with full product snapshot (name, brand, category, subcategory, price = `sellingPrice`, stockQuantity from inventory, `imageUrls: List<String>` built from `product.images.map { it.url }`); implement `unlistProductFromEcom(productId: String, workspaceId: String)`: set `isEcomListed = false`, publish `EcomCatalogEvent(PRODUCT_UNLISTED, ...)`; modify existing `ProductService.updateProduct(...)` to: after save, if `isEcomListed = true`, publish the appropriate `PRICE_UPDATED`, `STOCK_UPDATED`, or `DETAILS_UPDATED` event based on which fields changed
- [X] T026 [P] [US3] Create `product/src/main/kotlin/com/ampairs/product/controller/ProductEcomController.kt` — `@RequestMapping("/api/v1/products/{productId}/ecom")`; `PUT /list` → `ApiResponse.success(productEcomService.listProductOnEcom(productId, workspaceId))`; `PUT /unlist` → `ApiResponse.success(productEcomService.unlistProductFromEcom(productId, workspaceId))`; set `TenantContextHolder` in try/finally from `X-Workspace-ID` header
- [X] T027 [US3] Create `ecom/src/main/kotlin/com/ampairs/ecom/kafka/EcomCatalogKafkaConsumer.kt` — `@KafkaListener(topics = ["ecom-catalog-events"], containerFactory = "ecomCatalogListenerContainerFactory", groupId = "ecom-catalog-consumer")`; deserialize JSON payload to `EcomCatalogEvent`; delegate to `catalogSyncService` based on `eventType`; log and acknowledge; on deserialization error, log and skip (do not poison queue)
- [X] T028 [US3] Create `ecom/src/main/kotlin/com/ampairs/ecom/service/CatalogSyncService.kt` — implement: `handleProductListed(event)`: upsert `EcomListedProduct` (find by `storefrontId + managementProductId`, create if absent, update all snapshot fields, set `isVisible = true`, set `stockStatus` from `stockQuantity`); `handleProductUnlisted(event)`: find by key, set `isVisible = false`; `handlePriceUpdated(event)`: update `price`, recalculate nothing else; `handleStockUpdated(event)`: update `stockQuantity`, recalculate `stockStatus` (`>10` → `IN_STOCK`, `1–10` → `LIMITED`, `≤0` → `OUT_OF_STOCK`); `handleDetailsUpdated(event)`: update only non-null fields; helper `resolveStockStatus(qty: Int): StockStatus`

**Checkpoint ✅ US3 done**: Product listed in management → `EcomCatalogEvent(PRODUCT_LISTED)` on Kafka → `EcomListedProduct` row created → visible on storefront product list.

---

## Phase 5: User Story 1 — End Customer Browses and Places an Order (Priority: P1) 🎯 MVP

**Goal**: A shopper visits the published storefront, searches/browses products, adds to cart (no login), and confirms an order (login required at checkout). Receives an order reference number.

**Independent Test**: Full flow from `GET /api/v1/store/green-mart` → search → create cart → add item → POST checkout (with JWT) → `ecom_order_ref` returned with `status: PLACED`.

- [X] T029 [US1] Create `ecom/src/main/kotlin/com/ampairs/ecom/domain/dto/ListedProductResponse.kt` (uid, name, brand, category, subcategory, price, stockStatus, stockQuantity, `imageUrls: List<String>`, description) and extension function `EcomListedProduct.asListedProductResponse()`; create `ecom/src/main/kotlin/com/ampairs/ecom/controller/StorefrontPublicController.kt` — `@RequestMapping("/api/v1/store/{slug}")`; resolve slug → `Storefront` via `storefrontService.getPublishedStorefrontBySlug(slug)` then set `TenantContextHolder` in try/finally; implement: `GET /` → storefront info; `GET /products` → `ApiResponse.success(PageResponse.from(listedProductRepo.findByStorefrontIdAndIsVisibleTrue(..., pageable).map { it.asListedProductResponse() }))`; `GET /products/search?q=` → `@Query` tsvector search with `ts_rank` ordering; `GET /products/{productId}` → product detail; all endpoints public (no auth)
- [X] T030 [P] [US1] Create DTOs `ecom/src/main/kotlin/com/ampairs/ecom/domain/dto/CartResponse.kt` (uid, sessionToken, status, expiresAt, items list with `primaryImageUrl: String?`, subtotal) and `CartItemRequest.kt` (`@field:NotNull listedProductId`, `@field:Min(1) quantity`); create `ecom/src/main/kotlin/com/ampairs/ecom/service/CartService.kt` — `createCart(storefrontId: String, customerId: String?): EcomCart` (generate UUID `sessionToken`, set `expiresAt` based on whether authenticated); `getCart(sessionToken: String): EcomCart` (throw `CartExpiredException` if expired or not ACTIVE); `addOrUpdateItem(sessionToken: String, request: CartItemRequest): EcomCart` (validate `isVisible = true`, validate `quantity ≤ stockQuantity`, throw `ProductUnavailableException`/`InsufficientStockException`; upsert `EcomCartItem` with price snapshot from `EcomListedProduct`); `removeItem(sessionToken: String, itemId: String): EcomCart`; `clearCart(sessionToken: String): EcomCart`; `claimGuestCart(sessionToken: String, customerId: String, storefrontId: String): EcomCart` (implementation body moved to T036 — stub the signature here so T036 can fill it); create `ecom/src/main/kotlin/com/ampairs/ecom/controller/CartController.kt` — `@RequestMapping("/api/v1/store/{slug}/cart")`; resolve slug → `Storefront` via `storefrontService.getPublishedStorefrontBySlug(slug)` then set `TenantContextHolder.setCurrentTenant(storefront.ownerId)` in try/finally before calling `CartService` (Constitution IV — `EcomListedProduct` is OwnableBaseDomain and requires tenant context for stock validation); no auth required except the claim endpoint; return `ApiResponse.success(cart.asCartResponse())`
- [X] T031 [P] [US1] Extend the existing `auth` module to support end-customer registration — no new auth controller or service created; modify the existing `UserRegistrationRequest` (or equivalent DTO in `auth/src/main/kotlin/com/ampairs/auth/`) to accept an optional `userType: UserType = UserType.MERCHANT_USER` field; in the registration handler: if `userType = END_CUSTOMER`, skip workspace role assignment (no `X-Workspace-ID` required, user is not attached to any workspace); update JWT token generation in the auth module to include a `user_type` claim in the access token (e.g., `claims["user_type"] = user.userType.name`); update `JwtTokenValidator` / principal extraction so `user_type` is available as a security attribute (needed for `@PreAuthorize` in `CheckoutController`); existing `/api/v1/auth/register` and `/api/v1/auth/login` endpoints serve both MERCHANT_USER and END_CUSTOMER — no separate endpoint needed
- [X] T032 [US1] Create `ecom/src/main/kotlin/com/ampairs/ecom/domain/dto/EcomOrderResponse.kt` (ecomOrderRef, status, storefrontId, customerName, deliveryAddress, lineItems, subtotal, totalAmount, placedAt) + `EcomOrderLineItemResponse` + extension functions `EcomOrder.asEcomOrderResponse()`; create `ecom/src/main/kotlin/com/ampairs/ecom/domain/dto/CheckoutRequest.kt` (`deliveryAddressId: String?`, `deliveryAddress: DeliveryAddressDto?`, `saveAddress: Boolean = false`, `notes: String?`; validate at least one of `deliveryAddressId`/`deliveryAddress` is set); create `ecom/src/main/kotlin/com/ampairs/ecom/service/CheckoutService.kt` — `checkout(sessionToken: String, request: CheckoutRequest, customerId: String, storefront: Storefront): EcomOrder` — validate cart not empty (throw `EmptyCartException`); resolve delivery address; create `EcomOrder` + `EcomOrderLineItem` rows from cart snapshot; set `status = PLACED`; call `cartService` to mark cart `CONVERTED`; optionally save address if `saveAddress = true`; publish `EcomOrderPlacedEvent` via `EcomOrderKafkaProducer`; create `ecom/src/main/kotlin/com/ampairs/ecom/controller/CheckoutController.kt` — `POST /api/v1/store/{slug}/cart/{sessionToken}/checkout` requires `END_CUSTOMER` JWT (`@PreAuthorize`); resolve slug → storefront; delegate to `checkoutService`; return 201 + `EcomOrderResponse`
- [X] T033 [US1] Create `ecom/src/main/kotlin/com/ampairs/ecom/kafka/EcomOrderKafkaProducer.kt` — define `ecomOrderKafkaTemplate: KafkaTemplate<String, String>` via `EcomKafkaConfig` producer bean (add producer factory bean to `EcomKafkaConfig.kt`); `fun publishOrderPlaced(order: EcomOrder)` — build `EcomOrderPlacedEvent` from `EcomOrder` + line items, serialize to JSON, send to `"ecom-order-placed"` topic with key = `order.workspaceId`; on failure log error (do not swallow — let `CheckoutService` propagate)
- [X] T036 [P] [US1] Implement cart claim endpoint in `CartController`: add `POST /api/v1/store/{slug}/cart/{sessionToken}/claim` — requires `END_CUSTOMER` JWT (no anonymous access); add `CartService.claimGuestCart(sessionToken: String, customerId: String, storefrontId: String): EcomCart` — find guest ACTIVE cart by `sessionToken` (throw `CartExpiredException` if not found or expired); find or create customer's ACTIVE cart for same storefront; merge items (for product already in customer cart, take higher quantity capped to stock; copy remaining guest items); mark guest cart `MERGED`; extend customer cart `expiresAt` to 30 days from now; return updated customer cart; client calls this immediately after login if it held a guest `sessionToken` to transfer the anonymous session

**Checkpoint ✅ US1 done**: Guest can browse products, add to cart, log in as END_CUSTOMER via standard `/api/v1/auth/login`, claim guest cart, confirm checkout → `ecom_order_ref` returned, `EcomOrderPlaced` event on Kafka topic.

---

## Phase 6: User Story 4 — End Customer Manages Their Account Across Storefronts (Priority: P2)

**Goal**: Customer registered at any storefront can log in at any other storefront. Views order history scoped per storefront. Manages saved delivery addresses.

**Independent Test**: Register at Merchant A → log in at Merchant B (same credentials) → address saved at A is available; order history at B shows only B's orders.

- [X] T034 [US4] Create `ecom/src/main/kotlin/com/ampairs/ecom/domain/dto/CustomerAddressRequest.kt` (label?, @field:NotBlank addressLine1, addressLine2?, @field:NotBlank city, state, pinCode, country = "IN", phone?, isDefault) and `CustomerAddressResponse.kt` + extension functions; create `ecom/src/main/kotlin/com/ampairs/ecom/service/CustomerAddressService.kt` — `getAddresses(customerId: String): List<CustomerAddress>`; `addAddress(customerId: String, request: CustomerAddressRequest): CustomerAddress` (if `isDefault = true`, clear `isDefault` on existing default first); `updateAddress(customerId: String, addressId: String, request: CustomerAddressRequest): CustomerAddress` (verify ownership); `deleteAddress(customerId: String, addressId: String)` (verify ownership); create `ecom/src/main/kotlin/com/ampairs/ecom/controller/CustomerAccountController.kt` — `@RequestMapping("/api/v1/ecom/account")`; `@PreAuthorize` requires `END_CUSTOMER` JWT; `GET /addresses`, `POST /addresses` (201), `PUT /addresses/{id}`, `DELETE /addresses/{id}` (204)
- [X] T035 [P] [US4] Create `ecom/src/main/kotlin/com/ampairs/ecom/service/EcomOrderService.kt` — `getCustomerOrders(customerId: String, storefrontId: String, pageable: Pageable): Page<EcomOrder>` (query by `customerId + storefrontId` — never cross-merchant); `getCustomerOrder(customerId: String, ecomOrderRef: String): EcomOrder` (verify `customerId` matches, throw 403 `AccessDeniedException` if not); add order history endpoints to `CustomerAccountController`: `GET /orders?storefront_id=` → `ApiResponse.success(PageResponse.from(page))`; `GET /orders/{ecomOrderRef}` → full order detail with line items

**Checkpoint ✅ US4 done**: END_CUSTOMER can log in at any storefront using standard `/api/v1/auth/login`; addresses saved/updated; order history strictly storefront-scoped; guest cart claim via T036 already delivered in US1.

---

## Phase 7: User Story 5 — Merchant Receives Ecom Orders in Management (Priority: P2)

**Goal**: When a customer places an order, the merchant sees it in their Ampairs dashboard. Inventory is deducted. Merchant can edit and confirm orders that can't be fully fulfilled.

**Independent Test**: Place a test order on storefront → within 30s `GET /api/v1/ecom/management/orders` shows the order; merchant can view line items, edit quantities, confirm; confirmed order → ecom module reflects `CONFIRMED` status.

- [X] T037 [US5] Create `order/src/main/kotlin/com/ampairs/order/kafka/EcomOrderKafkaConfig.kt` — define `ConcurrentKafkaListenerContainerFactory` bean (`ecomOrderPlacedListenerContainerFactory`) with group `management-ecom-order-consumer`, bootstrap from `${ecom.kafka.bootstrap-servers:localhost:9092}`; create `order/src/main/kotlin/com/ampairs/order/kafka/EcomOrderPlacedConsumer.kt` — `@KafkaListener(topics = ["ecom-order-placed"], containerFactory = "ecomOrderPlacedListenerContainerFactory")`; deserialize `EcomOrderPlacedEvent`; idempotency guard first: `if (orderRepository.findByEcomOrderRef(event.ecomOrderRef) != null) return`; then wrap the entire processing block in `TenantContextHolder.setCurrentTenant(event.workspaceId)` try/finally `{ TenantContextHolder.clear() }` (Constitution IV — consumer listener is not a controller but must follow the same pattern); check inventory for each line item via `WarehouseService`/`InventoryService`; if all fulfillable: create `Order` (orderType = "ECOM", status = CONFIRMED, ecomOrderRef set, customer fields populated, delivery address mapped) + `OrderItem` rows; deduct inventory; publish `EcomOrderStatusEvent(CONFIRMED)`; if partial/none fulfillable: create `Order` with `status = PENDING_MERCHANT_REVIEW`; publish `EcomOrderStatusEvent(PENDING_MERCHANT_REVIEW)`
- [X] T038 [P] [US5] Create `order/src/main/kotlin/com/ampairs/order/kafka/EcomOrderStatusProducer.kt` — `KafkaTemplate<String, String>` bean (`ecomOrderStatusKafkaTemplate`) in `EcomOrderKafkaConfig`; `fun publishStatusUpdate(event: EcomOrderStatusEvent)` — serialize to JSON, send to `"ecom-order-status"` topic, key = `event.workspaceId`
- [X] T039 [US5] Create `ecom/src/main/kotlin/com/ampairs/ecom/domain/dto/EcomOrderManagementResponse.kt` (adds `managementOrderRef`, `merchantReviewedAt` to the customer-facing response) and `EcomOrderLineItemEditRequest.kt` (`uid`, `quantityConfirmed: Int`, `status: EcomLineItemStatus`); add management methods to `EcomOrderService`: `getManagementOrders(workspaceId: String, status: EcomOrderStatus?, pageable: Pageable): Page<EcomOrder>`; `getManagementOrder(workspaceId: String, ecomOrderRef: String): EcomOrder`; `editLineItems(workspaceId: String, ecomOrderRef: String, items: List<EcomOrderLineItemEditRequest>): EcomOrder` (order must be PENDING_MERCHANT_REVIEW; at least one item CONFIRMED); `confirmOrder(workspaceId: String, ecomOrderRef: String): EcomOrder` — validate order is `PENDING_MERCHANT_REVIEW`; set `status = CONFIRMED`, `merchantReviewedAt = now()`; call `orderEcomService.confirmEcomOrder(ecomOrderRef, confirmedItems)` — the `OrderEcomService` implementation (T046, in `order` module) advances the management `Order` to `CONFIRMED` **and** publishes `EcomOrderStatusEvent(CONFIRMED)` via `EcomOrderStatusProducer` (T038) as part of the same call; the ecom module MUST NOT directly reference `EcomOrderStatusProducer` (it lives in the `order` module — cross-module producer injection would violate Constitution IX); the status event round-trips back through `ecom-order-status` → `EcomOrderStatusKafkaConsumer` → `applyStatusUpdate()` to update line-item quantities; create `ecom/src/main/kotlin/com/ampairs/ecom/controller/EcomOrderManagementController.kt` — `@RequestMapping("/api/v1/ecom/management/orders")`; workspace JWT + `X-Workspace-ID`; `GET /`, `GET /{ref}`, `PUT /{ref}/line-items`, `POST /{ref}/confirm`, `PUT /{ref}/status`
- [X] T040 [US5] Create `ecom/src/main/kotlin/com/ampairs/ecom/kafka/EcomOrderStatusKafkaConsumer.kt` — `@KafkaListener(topics = ["ecom-order-status"], containerFactory = "ecomOrderStatusListenerContainerFactory")`; deserialize `EcomOrderStatusEvent`; delegate to `ecomOrderService.applyStatusUpdate(event)` — find `EcomOrder` by `ecomOrderRef`; update `status`; if `managementOrderRef` is non-null, set it; if `confirmedLineItems` present, update each `EcomOrderLineItem.quantityConfirmed` and `status`; save; add `applyStatusUpdate(event: EcomOrderStatusEvent)` to `EcomOrderService`

**Checkpoint ✅ US5 done**: Place order → Kafka consumed by order module → Order row created in management with `ecomOrderRef` → `EcomOrderStatusEvent` consumed by ecom module → `EcomOrder.status` updated. Merchant can review and confirm partial orders.

---

## Phase 8: User Story 6 — Customer Tracks Their Order Status (Priority: P3)

**Goal**: Customer sees real-time order status (Confirmed, Processing, Dispatched, Delivered) on their account page as the merchant advances fulfilment.

**Independent Test**: Place order → merchant advances status to DISPATCHED in management → `GET /api/v1/ecom/account/orders/{ref}` returns `status: DISPATCHED` within 5 seconds.

- [X] T041 [US6] Ensure `EcomOrderStatusKafkaConsumer` (T040) handles all status transitions — verify `PROCESSING`, `DISPATCHED`, `DELIVERED`, `CANCELLED` are correctly applied to `EcomOrder.status`; update `EcomOrderResponse` DTO to include `confirmedAt`, `managementOrderRef`, and per-line `quantityConfirmed` and `status` fields so customer sees confirmed quantities; add `PUT /api/v1/ecom/management/orders/{ref}/status` endpoint validation to `EcomOrderManagementController` — only CONFIRMED → PROCESSING → DISPATCHED → DELIVERED transitions are allowed (reject invalid transitions with 409)

**Checkpoint ✅ US6 done**: Merchant status advancement in management propagates to ecom within 5 seconds. Customer order detail page reflects current fulfilment status and confirmed quantities.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Operational reliability, expired cart cleanup, and DLQ monitoring. No new user-facing features.

- [X] T042 [P] Add `@Scheduled(cron = "0 */30 * * * *")` cleanup task in `CartService` (or a dedicated `CartCleanupTask.kt` in `ecom/service/`) that calls `ecomCartRepository.findByExpiresAtBefore(Instant.now())` and sets `status = ABANDONED` on expired ACTIVE carts; enable `@EnableScheduling` in `ecom` module config
- [X] T043 [P] Add dead-letter queue routing to `EcomKafkaConfig.kt` — configure `DefaultErrorHandler` with `SeekToCurrentErrorHandler`, max 3 retries with 500ms backoff, `DeadLetterPublishingRecoverer` routing to `{topic}.dlq` topic; apply to both `ecomCatalogListenerContainerFactory` and `ecomOrderStatusListenerContainerFactory`; apply equivalent DLQ config to `EcomOrderKafkaConfig` in `order` module
- [X] T044 [P] Add `ecom.kafka.bootstrap-servers` configuration to `ampairs_service/src/main/resources/application-production.yml` and `application-sandbox.yml` with appropriate env variable references (e.g., `${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}`); verify `ecom-catalog-events`, `ecom-order-placed`, `ecom-order-status` topics are listed in startup validation or auto-create config
- [X] T045 [P] Add customer notification on order status change: in `EcomOrderService.applyStatusUpdate(event: EcomOrderStatusEvent)`, after updating the `EcomOrder` status to `CONFIRMED`, `DISPATCHED`, `DELIVERED`, or `CANCELLED`, call the existing `notification` module's public service interface to enqueue an order status notification to the customer's email/phone; inject `NotificationService` (from `notification` module via module boundary — no direct repository access); do not block the Kafka consumer on notification failure (catch and log, do not rethrow — notification failure must not roll back the order status update)
- [X] T046 [P] Define `OrderEcomService` interface in `order/src/main/kotlin/com/ampairs/order/service/OrderEcomService.kt` — method `confirmEcomOrder(ecomOrderRef: String, confirmedItems: List<ConfirmedLineItemPayload>)`: find `Order` by `ecomOrderRef` (using `findByEcomOrderRef` added in T007), advance status from `PENDING_MERCHANT_REVIEW` to `CONFIRMED`, update each `OrderItem`'s confirmed quantity from `confirmedItems`, set `confirmedAt = Instant.now()`; **then publish `EcomOrderStatusEvent(CONFIRMED, managementOrderRef, confirmedItems)` via `EcomOrderStatusProducer` (T038)** — publishing belongs here in the order module, not in the ecom module's service (the ecom module only calls this interface); expose as a Spring `@Service` bean so `ecom` module's `EcomOrderService` can inject it (valid monolith cross-module service call; replace with Kafka on future extraction)
- [X] T047 [P] Configure HikariCP connection pool for ecom workload: in `ampairs_service/src/main/resources/application.yml` (or `application-production.yml`), set `spring.datasource.hikari.maximum-pool-size` to accommodate 500 concurrent shoppers (SC-004 target); recommended: `maximum-pool-size: 20`, `minimum-idle: 5`, `connection-timeout: 30000`, `idle-timeout: 600000`, `max-lifetime: 1800000`; add a comment referencing SC-004 so future tuning is traceable; verify the pool size is not overriding an existing config in the aggregator

**Checkpoint ✅ Phase 9 done**: Expired carts cleaned up on schedule; failed Kafka messages routed to DLQ after 3 retries; all environments have Kafka bootstrap config; customer notifications fire on order status changes; `OrderEcomService` bridge defined; HikariCP pool sized for 500 concurrent shoppers.

---

## Dependencies Graph

```
Phase 1 (Setup)
    └── Phase 2 (Foundation)
            ├── Phase 3 (US2 Storefront) ────────────────────────────────────┐
            │                                                                  │
            ├── Phase 4 (US3 Catalog Sync) ──── depends on US2 (storefront)  │
            │                                                                  │
            ├── Phase 5 (US1 Browse+Cart+Checkout) ─ depends on US2, US3 ────┤
            │                                                                  │
            ├── Phase 6 (US4 Account) ─────────── depends on US1 (auth)      │
            │                                                                  │
            ├── Phase 7 (US5 Management Orders) ── depends on US1 (checkout) │
            │                                                                  │
            └── Phase 8 (US6 Order Tracking) ───── depends on US5            │
                                                                               │
Phase 9 (Polish) ────────────────────────────────────────────────────────────┘
```

**Stories that can be implemented concurrently after Phase 2**:
- US2 (Storefront management) and the entity/infrastructure work are independent of each other
- T031 (auth module user_type extension) can be built while US2 is in progress (different modules)
- US5 order consumer (T037) and US3 catalog producer (T024) are independent modules
- T036 (cart claim endpoint) and T030 (CartService) target different methods in the same service — sequential within CartService, but T036 can be done after T030 is merged

---

## Parallel Execution Examples

### Phase 2 parallelism (after T005–T008 are started)
```
Developer A: T005 (user userType) → T014 (Storefront entity)
Developer B: T006 (product isEcomListed) → T015 (EcomListedProduct entity)
Developer C: T007 (order ecomOrderRef) → T016 (Cart entities)
Developer D: T009–T013 (migrations) in parallel
```

### Phase 3 + 4 + 5 concurrency (after Phase 2)
```
Developer A: T021 → T022 → T023 (US2 storefront management API)
Developer B: T025 (EcomStorefrontLookupService in core) → T024 → T026 (US3 catalog producer)
Developer C: T031 (auth module user_type + JWT claim) — independent of both
Developer D: T027 → T028 (US3 catalog consumer) — after T019
Developer E: T036 (cart claim endpoint) — parallel with T030
```

---

## Implementation Strategy

**MVP scope** (ship US1 end-to-end): Phases 1–5

This delivers the complete shopper journey:
1. Merchant creates and publishes storefront (US2)
2. Merchant lists products that sync to storefront (US3)
3. Customer browses, carts, and checks out (US1)

US4 (account management), US5 (management order intake), and US6 (status tracking) are incremental improvements on top of the working core.

---

## Summary

| Phase | User Story | Tasks | Parallel Opportunities |
|-------|------------|-------|----------------------|
| 1 | Setup | T001–T004 | T003, T004 |
| 2 | Foundation | T005–T020 | T006, T007, T009–T013, T015–T018, T020 |
| 3 | US2 Storefront | T021–T023 | — |
| 4 | US3 Catalog Sync | T024–T028 | T025 (EcomStorefrontLookupService), T026 |
| 5 | US1 Browse+Order | T029–T033, T036 | T030, T031, T036 |
| 6 | US4 Account | T034–T035 | T035 |
| 7 | US5 Mgmt Orders | T037–T040 | T038 |
| 8 | US6 Tracking | T041 | — |
| 9 | Polish | T042–T047 | T042, T043, T044, T045, T046, T047 |

**Total**: 47 tasks | **MVP** (Phases 1–5): 36 tasks | **Parallel pairs**: 22 tasks
