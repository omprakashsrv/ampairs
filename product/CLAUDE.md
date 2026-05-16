# product module

Product catalog + variants + full multi-warehouse inventory system.

## Two bounded contexts in one module
- `com.ampairs.product` — catalog, variants, categories, brands, groups
- `com.ampairs.inventory` — warehouses, stock, transactions, ledger, batches, serials

## Key entities
- `Product` — name, sku, barcode, basePrice, sellingPrice, taxCodeId, unitId, hasVariants
- `ProductVariant` — productId, sku, attributes (JSON), pricing
- `Inventory` — productId, warehouseId, quantityOnHand, quantityReserved, quantityAvailable
- `InventoryTransaction` — transactionType (PURCHASE/SALE/ADJUSTMENT/TRANSFER), quantity, referenceId
- `InventoryBatch` — batchNumber, quantity, expiryDate
- `InventorySerial` — serialNumber, status (AVAILABLE/SOLD/DEFECTIVE)
- `InventoryLedger` — append-only audit trail of all stock movements
- `Warehouse` — name, address, warehouseType, capacity

## Base paths
`/product/v1/**`, `/inventory/v1/**`

## Migrations
`V1.0.42–V1.0.49` (inventory tables, product tables, indexes, variant support)

## Full docs
`docs/modules/product.md`
