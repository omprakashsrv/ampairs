package com.ampairs.inventory.event

import com.ampairs.event.domain.events.BaseEntityEvent
import java.math.BigDecimal

/**
 * Event published when inventory stock is updated
 */
class InventoryStockUpdatedEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val inventoryItemId: String,
    val sku: String,
    val itemName: String,
    val warehouseId: String,
    val previousStock: BigDecimal,
    val newStock: BigDecimal,
    val changeAmount: BigDecimal,
    val transactionType: String,
    val reason: String
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "stock_updated",
            "inventory_item_id" to inventoryItemId,
            "sku" to sku,
            "item_name" to itemName,
            "warehouse_id" to warehouseId,
            "previous_stock" to previousStock.toString(),
            "new_stock" to newStock.toString(),
            "change_amount" to changeAmount.toString(),
            "transaction_type" to transactionType,
            "reason" to reason
        )
    }
}

/**
 * Event published when inventory reaches low stock level
 */
class InventoryLowStockEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val inventoryItemId: String,
    val sku: String,
    val itemName: String,
    val warehouseId: String,
    val currentStock: BigDecimal,
    val reorderLevel: BigDecimal
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "low_stock_alert",
            "inventory_item_id" to inventoryItemId,
            "sku" to sku,
            "item_name" to itemName,
            "warehouse_id" to warehouseId,
            "current_stock" to currentStock.toString(),
            "reorder_level" to reorderLevel.toString()
        )
    }
}

/**
 * Event published when inventory reaches zero or negative stock
 */
class InventoryOutOfStockEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val inventoryItemId: String,
    val sku: String,
    val itemName: String,
    val warehouseId: String,
    val currentStock: BigDecimal
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "out_of_stock_alert",
            "inventory_item_id" to inventoryItemId,
            "sku" to sku,
            "item_name" to itemName,
            "warehouse_id" to warehouseId,
            "current_stock" to currentStock.toString()
        )
    }
}

/**
 * Event published when a batch is about to expire
 */
class InventoryBatchExpiringEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val batchId: String,
    val batchNumber: String,
    val inventoryItemId: String,
    val itemName: String,
    val warehouseId: String,
    val availableQuantity: BigDecimal,
    val expiryDate: String,
    val daysUntilExpiry: Long
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "batch_expiring_alert",
            "batch_id" to batchId,
            "batch_number" to batchNumber,
            "inventory_item_id" to inventoryItemId,
            "item_name" to itemName,
            "warehouse_id" to warehouseId,
            "available_quantity" to availableQuantity.toString(),
            "expiry_date" to expiryDate,
            "days_until_expiry" to daysUntilExpiry
        )
    }
}

/**
 * Event published when a batch has expired
 */
class InventoryBatchExpiredEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val batchId: String,
    val batchNumber: String,
    val inventoryItemId: String,
    val itemName: String,
    val warehouseId: String,
    val quantity: BigDecimal,
    val expiryDate: String
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "batch_expired",
            "batch_id" to batchId,
            "batch_number" to batchNumber,
            "inventory_item_id" to inventoryItemId,
            "item_name" to itemName,
            "warehouse_id" to warehouseId,
            "quantity" to quantity.toString(),
            "expiry_date" to expiryDate
        )
    }
}

/**
 * Event published when inventory transfer is initiated
 */
class InventoryTransferInitiatedEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val transactionId: String,
    val transactionNumber: String,
    val inventoryItemId: String,
    val itemName: String,
    val fromWarehouseId: String,
    val toWarehouseId: String,
    val quantity: BigDecimal
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "transfer_initiated",
            "transaction_id" to transactionId,
            "transaction_number" to transactionNumber,
            "inventory_item_id" to inventoryItemId,
            "item_name" to itemName,
            "from_warehouse_id" to fromWarehouseId,
            "to_warehouse_id" to toWarehouseId,
            "quantity" to quantity.toString()
        )
    }
}

/**
 * Event published when inventory adjustment is made
 */
class InventoryAdjustmentEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val transactionId: String,
    val transactionNumber: String,
    val inventoryItemId: String,
    val itemName: String,
    val warehouseId: String,
    val adjustmentQuantity: BigDecimal,
    val reason: String,
    val notes: String?
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "inventory_adjusted",
            "transaction_id" to transactionId,
            "transaction_number" to transactionNumber,
            "inventory_item_id" to inventoryItemId,
            "item_name" to itemName,
            "warehouse_id" to warehouseId,
            "adjustment_quantity" to adjustmentQuantity.toString(),
            "reason" to reason,
            "notes" to (notes ?: "")
        )
    }
}
