# product module

Product catalog, variants, category management, and a full multi-warehouse inventory system with batch/serial number tracking.

## Responsibilities

- Product catalog (create, categorize, search, media)
- Product variants (SKU, attributes)
- Category and unit of measure management
- Inventory management:
  - Multi-warehouse stock tracking
  - Batch number management with expiry
  - Serial number management
  - Real-time inventory ledger
  - FIFO / LIFO / FEFO valuation methods
  - Stock movement transactions
- AWS S3 media storage

## REST Endpoints

### Products (`/product/v1`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/product/v1` | Create product |
| GET | `/product/v1` | List with pagination and sync |
| GET | `/product/v1/{productId}` | Get product by ID |
| PUT | `/product/v1/{productId}` | Update product |
| DELETE | `/product/v1/{productId}` | Soft delete |
| GET | `/product/v1/search` | Full-text search |
| POST | `/product/v1/bulk` | Bulk create/update |
| GET | `/product/v1/{productId}/images` | Product images |
| POST | `/product/v1/{productId}/images` | Upload product image |
| DELETE | `/product/v1/{productId}/images/{imageId}` | Delete image |

### Variants (`/product/v1/{productId}/variants`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/product/v1/{productId}/variants` | List variants |
| POST | `/product/v1/{productId}/variants` | Create variant |
| PUT | `/product/v1/{productId}/variants/{variantId}` | Update variant |
| DELETE | `/product/v1/{productId}/variants/{variantId}` | Delete variant |

### Categories (`/product/v1/categories`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/product/v1/categories` | List categories |
| POST | `/product/v1/categories` | Create category |
| PUT | `/product/v1/categories/{categoryId}` | Update category |
| DELETE | `/product/v1/categories/{categoryId}` | Delete category |

### Inventory (`/product/v1/inventory`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/product/v1/inventory` | List inventory across warehouses |
| GET | `/product/v1/inventory/{productId}` | Get stock for a product |
| POST | `/product/v1/inventory/adjust` | Manual stock adjustment |
| POST | `/product/v1/inventory/transfer` | Transfer stock between warehouses |
| GET | `/product/v1/inventory/transactions` | Transaction history |
| GET | `/product/v1/inventory/ledger/{productId}` | Inventory ledger |
| GET | `/product/v1/inventory/batches/{productId}` | List batches |
| POST | `/product/v1/inventory/batches` | Create batch |
| GET | `/product/v1/inventory/serials/{productId}` | List serial numbers |
| POST | `/product/v1/inventory/serials` | Register serial numbers |

## Key Entities

### Product

```kotlin
class Product : OwnableBaseDomain() {
    val name: String
    val sku: String?               // stock keeping unit
    val barcode: String?
    val description: String?
    val categoryId: String?
    val unitId: String?
    val taxCodeId: String?
    val basePrice: BigDecimal
    val sellingPrice: BigDecimal
    val costPrice: BigDecimal?
    val active: Boolean
    val trackInventory: Boolean
    val inventoryMethod: InventoryMethod  // FIFO, LIFO, FEFO
    val lowStockThreshold: Int?
    val attributes: Map<String, Any>?    // custom JSON fields
}
```

### ProductVariant

```kotlin
class ProductVariant : OwnableBaseDomain() {
    val productId: String
    val name: String
    val sku: String?
    val barcode: String?
    val attributes: Map<String, Any>   // color, size, etc.
    val basePrice: BigDecimal?
    val sellingPrice: BigDecimal?
    val active: Boolean
}
```

### Inventory

```kotlin
class Inventory : OwnableBaseDomain() {
    val productId: String
    val variantId: String?
    val warehouseId: String
    val quantityOnHand: BigDecimal
    val quantityReserved: BigDecimal
    val quantityAvailable: BigDecimal  // onHand - reserved
    val lastUpdated: Instant
}
```

### InventoryTransaction

```kotlin
class InventoryTransaction : OwnableBaseDomain() {
    val productId: String
    val variantId: String?
    val warehouseId: String
    val transactionType: TransactionType  // PURCHASE, SALE, ADJUSTMENT, TRANSFER_IN, TRANSFER_OUT
    val quantity: BigDecimal
    val unitCost: BigDecimal?
    val referenceId: String?     // order/invoice UID
    val referenceType: String?   // ORDER, INVOICE, ADJUSTMENT
    val batchId: String?
    val notes: String?
    val transactedAt: Instant
}
```

### InventoryBatch

```kotlin
class InventoryBatch : OwnableBaseDomain() {
    val productId: String
    val warehouseId: String
    val batchNumber: String
    val quantity: BigDecimal
    val remainingQuantity: BigDecimal
    val manufacturingDate: Instant?
    val expiryDate: Instant?
    val unitCost: BigDecimal?
    val active: Boolean
}
```

### InventorySerial

```kotlin
class InventorySerial : OwnableBaseDomain() {
    val productId: String
    val warehouseId: String
    val serialNumber: String       // unique per workspace
    val status: SerialStatus       // AVAILABLE, SOLD, DEFECTIVE, RETURNED
    val purchasedAt: Instant?
    val soldAt: Instant?
    val referenceId: String?       // sale/order reference
}
```

### InventoryLedger

Append-only audit trail of all stock movements:

```kotlin
class InventoryLedger : OwnableBaseDomain() {
    val productId: String
    val warehouseId: String
    val transactionId: String
    val quantityBefore: BigDecimal
    val quantityChange: BigDecimal
    val quantityAfter: BigDecimal
    val transactionType: String
    val recordedAt: Instant
}
```

## Database Migrations

| File | Description |
|------|-------------|
| `V1.0.42__create_inventory_module_tables.sql` | inventory, warehouse tables |
| `V1.0.43__create_inventory_transaction_table.sql` | Transaction tracking |
| `V1.0.44__create_inventory_batch_table.sql` | Batch number management |
| `V1.0.45__create_inventory_serial_table.sql` | Serial number tracking |
| `V1.0.46__create_inventory_ledger_table.sql` | Append-only ledger |
| `V1.0.47__create_product_module_tables.sql` | product, category, variant tables |
| `V1.0.48__add_performance_indexes.sql` | Query performance indexes |
| `V1.0.49__add_product_variant_support.sql` | Variant schema additions |
