# Data Model: Ecommerce Order Platform

**Branch**: `008-ecommerce-order-platform`  
**Module**: `ecom/src/main/kotlin/com/ampairs/ecom/`

---

## Entity Overview

| Entity | Table | Extends | Tenant-Scoped |
|--------|-------|---------|---------------|
| `Storefront` | `ecom_storefront` | `OwnableBaseDomain` | Yes (workspace) |
| `EcomListedProduct` | `ecom_listed_product` | `OwnableBaseDomain` | Yes (workspace) |
| `EcomCart` | `ecom_cart` | `BaseDomain` | No (explicit `storefront_id`) |
| `EcomCartItem` | `ecom_cart_item` | `BaseDomain` | No |
| `CustomerAddress` | `ecom_customer_address` | `BaseDomain` | No (customer-scoped) |
| `EcomOrder` | `ecom_order` | `BaseDomain` | No (explicit `workspace_id` + `storefront_id`) |
| `EcomOrderLineItem` | `ecom_order_line_item` | `BaseDomain` | No |

**Cross-module changes** (not in `ecom` module):

| Entity | Module | Change |
|--------|--------|--------|
| `User` | `user` | Add `user_type: UserType` enum column |
| `Product` | `product` | Add `is_ecom_listed: Boolean` column |
| `Order` | `order` | Add `ecom_order_ref: String?` column; add `PENDING_MERCHANT_REVIEW` to `OrderStatus` |

---

## ecom Module Entities

### Storefront

```kotlin
@Entity("ecom_storefront")
@Table(
    indexes = [
        Index(name = "idx_ecom_storefront_uid", columnList = "uid", unique = true),
        Index(name = "idx_ecom_storefront_slug", columnList = "slug", unique = true),
        Index(name = "idx_ecom_storefront_owner", columnList = "owner_id")
    ]
)
class Storefront : OwnableBaseDomain() {
    var name: String                     // Display name of the store
    var slug: String                     // URL slug (unique, immutable after creation)
    var description: String?
    var logoUrl: String?
    var bannerUrl: String?
    var status: StorefrontStatus         // DRAFT | PUBLISHED | UNPUBLISHED
    var publishedAt: Instant?
    var unpublishedAt: Instant?
}

enum class StorefrontStatus { DRAFT, PUBLISHED, UNPUBLISHED }
```

**State transitions**: DRAFT → PUBLISHED → UNPUBLISHED → PUBLISHED (re-publish allowed)  
**Constraints**: `slug` is unique globally; immutable after first creation. Validated against workspace slug on creation.

---

### EcomListedProduct

Denormalized snapshot of a management product as it appears on the storefront. Updated via Kafka catalog events.

```kotlin
@Entity("ecom_listed_product")
@Table(
    indexes = [
        Index(name = "idx_ecom_product_uid", columnList = "uid", unique = true),
        Index(name = "idx_ecom_product_storefront", columnList = "storefront_id"),
        Index(name = "idx_ecom_product_mgmt_ref", columnList = "management_product_id"),
        Index(name = "idx_ecom_product_visible", columnList = "storefront_id, is_visible"),
        Index(name = "idx_ecom_product_stock", columnList = "storefront_id, stock_status")
    ]
)
@NamedEntityGraph(name = "EcomListedProduct.full", attributeNodes = [])
class EcomListedProduct : OwnableBaseDomain() {
    var storefrontId: String             // FK to Storefront.uid
    var managementProductId: String      // FK to product.uid in management (cross-module reference, no JPA join)
    var name: String
    var description: String?
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_urls", columnDefinition = "jsonb")
    var imageUrls: List<String> = emptyList()   // Denormalized snapshot of all product images; first entry is primary
    var brand: String?
    var category: String?
    var subcategory: String?
    var price: BigDecimal                // Selling price snapshot from management
    var stockQuantity: Int               // Current stock count (updated via events)
    var stockStatus: StockStatus         // IN_STOCK | OUT_OF_STOCK | LIMITED
    var isVisible: Boolean = true        // false = unlisted (product removed from storefront)
    var lastSyncedAt: Instant            // When the last catalog event was applied
}

enum class StockStatus { IN_STOCK, OUT_OF_STOCK, LIMITED }
```

**Note on `price` type**: `BigDecimal` matches financial precision requirements. PostgreSQL column: `NUMERIC(19,4)`.

**Stock status rules**:
- `stockQuantity > 10` → `IN_STOCK`
- `1 ≤ stockQuantity ≤ 10` → `LIMITED`
- `stockQuantity ≤ 0` → `OUT_OF_STOCK`

**Full-text search** (separate migration — PostgreSQL):
```sql
ALTER TABLE ecom_listed_product
  ADD COLUMN search_vector tsvector
  GENERATED ALWAYS AS (
    to_tsvector('english',
      coalesce(name, '') || ' ' ||
      coalesce(brand, '') || ' ' ||
      coalesce(category, '') || ' ' ||
      coalesce(subcategory, '')
    )
  ) STORED;

CREATE INDEX idx_ecom_product_search ON ecom_listed_product USING GIN (search_vector);
```

---

### EcomCart

```kotlin
@Entity("ecom_cart")
@Table(
    indexes = [
        Index(name = "idx_ecom_cart_uid", columnList = "uid", unique = true),
        Index(name = "idx_ecom_cart_session", columnList = "session_token", unique = true),
        Index(name = "idx_ecom_cart_customer", columnList = "customer_id"),
        Index(name = "idx_ecom_cart_storefront", columnList = "storefront_id"),
        Index(name = "idx_ecom_cart_expires", columnList = "expires_at")
    ]
)
@NamedEntityGraph(name = "EcomCart.withItems", attributeNodes = [NamedAttributeNode("items")])
class EcomCart : BaseDomain() {
    var storefrontId: String             // Which storefront this cart belongs to
    var customerId: String?              // null = guest cart; set on login/merge
    var sessionToken: String             // Opaque UUID, returned to client at cart creation
    var expiresAt: Instant               // 24hr from creation for guests; 30 days for authenticated
    var status: CartStatus               // ACTIVE | CONVERTED | MERGED | ABANDONED

    @OneToMany
    @JoinColumn(name = "cart_id", referencedColumnName = "uid", insertable = false, updatable = false)
    @BatchSize(size = 30)
    var items: MutableList<EcomCartItem> = mutableListOf()
}

enum class CartStatus { ACTIVE, CONVERTED, MERGED, ABANDONED }
```

---

### EcomCartItem

```kotlin
@Entity("ecom_cart_item")
@Table(
    indexes = [
        Index(name = "idx_ecom_cart_item_uid", columnList = "uid", unique = true),
        Index(name = "idx_ecom_cart_item_cart", columnList = "cart_id"),
        Index(name = "idx_ecom_cart_item_product", columnList = "listed_product_id")
    ]
)
class EcomCartItem : BaseDomain() {
    var cartId: String                   // FK to EcomCart.uid
    var listedProductId: String          // FK to EcomListedProduct.uid
    var managementProductId: String      // Snapshot of management product ref
    var productName: String              // Snapshot at time of addition
    var unitPrice: BigDecimal            // Snapshot at time of addition
    var quantity: Int                    // Capped to available stock at add time
    var primaryImageUrl: String?         // First image from EcomListedProduct.imageUrls; sufficient for cart thumbnail
}
```

---

### CustomerAddress

```kotlin
@Entity("ecom_customer_address")
@Table(
    indexes = [
        Index(name = "idx_ecom_addr_uid", columnList = "uid", unique = true),
        Index(name = "idx_ecom_addr_customer", columnList = "customer_id")
    ]
)
class CustomerAddress : BaseDomain() {
    var customerId: String               // FK to app_user.uid
    var label: String?                   // "Home", "Office", etc.
    var addressLine1: String
    var addressLine2: String?
    var city: String
    var state: String
    var pinCode: String
    var country: String = "IN"
    var phone: String?
    var isDefault: Boolean = false
}
```

---

### EcomOrder

```kotlin
@Entity("ecom_order")
@Table(
    indexes = [
        Index(name = "idx_ecom_order_uid", columnList = "uid", unique = true),
        Index(name = "idx_ecom_order_ref", columnList = "ecom_order_ref", unique = true),
        Index(name = "idx_ecom_order_storefront", columnList = "storefront_id"),
        Index(name = "idx_ecom_order_customer", columnList = "customer_id"),
        Index(name = "idx_ecom_order_workspace", columnList = "workspace_id"),
        Index(name = "idx_ecom_order_status", columnList = "status")
    ]
)
@NamedEntityGraph(name = "EcomOrder.withItems", attributeNodes = [NamedAttributeNode("lineItems")])
class EcomOrder : BaseDomain() {
    var ecomOrderRef: String             // Unique human-readable ref, e.g. "ECO-A1B2C3D4"
    var storefrontId: String
    var workspaceId: String              // Explicit (not @TenantId — this is not OwnableBaseDomain)
    var customerId: String
    var customerName: String
    var customerEmail: String
    var customerPhone: String?

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "delivery_address", nullable = false)
    var deliveryAddress: Address = Address()

    var status: EcomOrderStatus
    var managementOrderRef: String?      // Set after management processes the event
    var subtotal: BigDecimal
    var totalAmount: BigDecimal
    var notes: String?
    var placedAt: Instant = Instant.now()
    var confirmedAt: Instant?
    var merchantReviewedAt: Instant?

    @OneToMany
    @JoinColumn(name = "ecom_order_id", referencedColumnName = "uid", insertable = false, updatable = false)
    @BatchSize(size = 30)
    var lineItems: MutableList<EcomOrderLineItem> = mutableListOf()
}

enum class EcomOrderStatus {
    PLACED,                    // Customer confirmed checkout; event published
    PENDING_MERCHANT_REVIEW,   // Management couldn't fully fulfil; merchant must review
    CONFIRMED,                 // Management confirmed fulfilment (possibly edited)
    PROCESSING,
    DISPATCHED,
    DELIVERED,
    CANCELLED
}
```

---

### EcomOrderLineItem

Designed for future partial fulfilment and split shipments (FR-029). `shipment_group` is nullable in v1.

```kotlin
@Entity("ecom_order_line_item")
@Table(
    indexes = [
        Index(name = "idx_ecom_line_uid", columnList = "uid", unique = true),
        Index(name = "idx_ecom_line_order", columnList = "ecom_order_id"),
        Index(name = "idx_ecom_line_product", columnList = "listed_product_id")
    ]
)
class EcomOrderLineItem : BaseDomain() {
    var ecomOrderId: String
    var listedProductId: String          // Snapshot reference
    var managementProductId: String      // For management-side linkage
    var productName: String              // Snapshot
    var unitPrice: BigDecimal            // Snapshot at time of order
    var quantityOrdered: Int
    var quantityConfirmed: Int?          // Set by merchant during review; null until reviewed
    var lineTotal: BigDecimal            // quantityOrdered * unitPrice
    var status: EcomLineItemStatus       // ORDERED | CONFIRMED | CANCELLED
    var shipmentGroup: String?           // v1: null; future: shipment group reference
}

enum class EcomLineItemStatus { ORDERED, CONFIRMED, CANCELLED }
```

---

## Cross-Module Changes

### user module: UserType column

```kotlin
// user/src/main/kotlin/com/ampairs/user/model/User.kt
// Add:
@Column(name = "user_type", nullable = false, length = 20)
@Enumerated(EnumType.STRING)
var userType: UserType = UserType.MERCHANT_USER

enum class UserType { MERCHANT_USER, END_CUSTOMER }
```

Migration: `user/src/main/resources/db/migration/postgresql/V1.0.27__add_user_type_to_app_user.sql`

---

### product module: isEcomListed column

```kotlin
// product/src/main/kotlin/com/ampairs/product/domain/model/Product.kt
// Add:
@Column(name = "is_ecom_listed", nullable = false)
var isEcomListed: Boolean = false
```

Migration: `product/src/main/resources/db/migration/postgresql/V1.0.28__add_ecom_listed_to_product.sql`

---

### order module: ecomOrderRef + PENDING_MERCHANT_REVIEW

```kotlin
// order/src/main/kotlin/com/ampairs/order/domain/model/Order.kt
// Add:
@Column(name = "ecom_order_ref", length = 50)
var ecomOrderRef: String? = null        // ECO-xxxx from ecom module

// order/src/main/kotlin/com/ampairs/order/domain/enums/OrderStatus.kt
// Add PENDING_MERCHANT_REVIEW to enum:
enum class OrderStatus {
    DRAFT, NEW, ORDERED, CONFIRMED, PENDING_MERCHANT_REVIEW, PROCESSING, SHIPPED, DELIVERED, CANCELLED, REFUNDED
}
```

Migration: `order/src/main/resources/db/migration/postgresql/V1.0.29__add_ecom_order_ref_to_order.sql`

---

## Kafka Event Payload DTOs

These live in the `event` module under `com.ampairs.event.domain.kafka`:

### EcomCatalogEvent (management → ecom, topic: `ecom-catalog-events`)

```kotlin
data class EcomCatalogEvent(
    val eventType: CatalogEventType,    // PRODUCT_LISTED | PRODUCT_UNLISTED | PRICE_UPDATED | STOCK_UPDATED | DETAILS_UPDATED
    val workspaceId: String,
    val managementProductId: String,
    val storefrontId: String,
    // Nullable — only set for relevant event types:
    val name: String?,
    val brand: String?,
    val category: String?,
    val subcategory: String?,
    val price: BigDecimal?,
    val stockQuantity: Int?,
    val imageUrls: List<String>?,           // All product images; first entry is primary
    val description: String?,
    val publishedAt: Instant
)

enum class CatalogEventType {
    PRODUCT_LISTED, PRODUCT_UNLISTED, PRICE_UPDATED, STOCK_UPDATED, DETAILS_UPDATED
}
```

### EcomOrderPlacedEvent (ecom → management, topic: `ecom-order-placed`)

```kotlin
data class EcomOrderPlacedEvent(
    val ecomOrderRef: String,
    val workspaceId: String,
    val storefrontId: String,
    val customerId: String,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String?,
    val deliveryAddress: Address,
    val lineItems: List<EcomOrderLineItemPayload>,
    val subtotal: BigDecimal,
    val totalAmount: BigDecimal,
    val placedAt: Instant
)

data class EcomOrderLineItemPayload(
    val listedProductId: String,
    val managementProductId: String,
    val productName: String,
    val unitPrice: BigDecimal,
    val quantityOrdered: Int,
    val lineTotal: BigDecimal
)
```

### EcomOrderStatusEvent (management → ecom, topic: `ecom-order-status`)

```kotlin
data class EcomOrderStatusEvent(
    val ecomOrderRef: String,
    val workspaceId: String,
    val newStatus: String,           // Mirrors EcomOrderStatus values
    val managementOrderRef: String?,
    val confirmedLineItems: List<ConfirmedLineItemPayload>?,  // Set on CONFIRMED if edited
    val updatedAt: Instant
)

data class ConfirmedLineItemPayload(
    val managementProductId: String,
    val quantityConfirmed: Int,
    val status: String               // CONFIRMED | CANCELLED
)
```

---

## Relationship Diagram

```
Workspace (workspace module)
    └── Storefront (ecom, OwnableBaseDomain)
            └── EcomListedProduct (ecom, OwnableBaseDomain)
                    ← synced via EcomCatalogEvent from Product (product module)

app_user (user module, UserType = END_CUSTOMER)
    ├── EcomCart (ecom, BaseDomain)
    │       └── EcomCartItem
    ├── CustomerAddress (ecom, BaseDomain)
    └── EcomOrder (ecom, BaseDomain)
            ├── EcomOrderLineItem
            └── → publishes EcomOrderPlacedEvent → Order (order module)
                        → publishes EcomOrderStatusEvent → EcomOrder (status update)
```

---

## Migration Sequence

| Version | Module | File |
|---------|--------|------|
| V1.0.27 | user | `add_user_type_to_app_user.sql` |
| V1.0.28 | product | `add_ecom_listed_to_product.sql` |
| V1.0.29 | order | `add_ecom_order_ref_to_order.sql` |
| V1.0.30 | ecom | `create_ecom_storefront.sql` |
| V1.0.31 | ecom | `create_ecom_listed_product.sql` |
| V1.0.32 | ecom | `create_ecom_cart_tables.sql` |
| V1.0.33 | ecom | `create_ecom_order_tables.sql` |
| V1.0.34 | ecom | `create_ecom_customer_address.sql` |
| V1.0.35 | ecom | `add_tsvector_search_index.sql` |
