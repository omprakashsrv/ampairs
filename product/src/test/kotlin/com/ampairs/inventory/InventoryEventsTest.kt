package com.ampairs.inventory

import com.ampairs.inventory.event.InventoryAdjustmentEvent
import com.ampairs.inventory.event.InventoryBatchExpiredEvent
import com.ampairs.inventory.event.InventoryBatchExpiringEvent
import com.ampairs.inventory.event.InventoryLowStockEvent
import com.ampairs.inventory.event.InventoryOutOfStockEvent
import com.ampairs.inventory.event.InventoryStockUpdatedEvent
import com.ampairs.inventory.event.InventoryTransferInitiatedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class InventoryEventsTest {

    private val src = "test-source"

    @Test
    fun `stock updated event exposes changes`() {
        val e = InventoryStockUpdatedEvent(
            src, "WS-1", "INV-1", "USR-1", "DEV-1",
            inventoryItemId = "INV-1", sku = "SKU-1", itemName = "Widget", warehouseId = "WHR-1",
            previousStock = BigDecimal("10"), newStock = BigDecimal("15"), changeAmount = BigDecimal("5"),
            transactionType = "STOCK_IN", reason = "PURCHASE"
        )
        val c = e.getChanges()
        assertEquals("stock_updated", c["action"])
        assertEquals("SKU-1", c["sku"])
        assertEquals("15", c["new_stock"])
        assertEquals("WS-1", e.workspaceId)
    }

    @Test
    fun `low stock event exposes changes`() {
        val e = InventoryLowStockEvent(
            src, "WS-1", "INV-1", "USR-1", "DEV-1",
            inventoryItemId = "INV-1", sku = "SKU-1", itemName = "Widget", warehouseId = "WHR-1",
            currentStock = BigDecimal("3"), reorderLevel = BigDecimal("10")
        )
        val c = e.getChanges()
        assertEquals("low_stock_alert", c["action"])
        assertEquals("3", c["current_stock"])
        assertEquals("10", c["reorder_level"])
    }

    @Test
    fun `out of stock event exposes changes`() {
        val e = InventoryOutOfStockEvent(
            src, "WS-1", "INV-1", "USR-1", "DEV-1",
            inventoryItemId = "INV-1", sku = "SKU-1", itemName = "Widget", warehouseId = "WHR-1",
            currentStock = BigDecimal.ZERO
        )
        assertEquals("out_of_stock_alert", e.getChanges()["action"])
    }

    @Test
    fun `batch expiring event exposes changes`() {
        val e = InventoryBatchExpiringEvent(
            src, "WS-1", "BCH-1", "USR-1", "DEV-1",
            batchId = "BCH-1", batchNumber = "B-1", inventoryItemId = "INV-1", itemName = "Widget",
            warehouseId = "WHR-1", availableQuantity = BigDecimal("5"), expiryDate = "2025-02-01", daysUntilExpiry = 7
        )
        val c = e.getChanges()
        assertEquals("batch_expiring_alert", c["action"])
        assertEquals(7L, c["days_until_expiry"])
    }

    @Test
    fun `batch expired event exposes changes`() {
        val e = InventoryBatchExpiredEvent(
            src, "WS-1", "BCH-1", "USR-1", "DEV-1",
            batchId = "BCH-1", batchNumber = "B-1", inventoryItemId = "INV-1", itemName = "Widget",
            warehouseId = "WHR-1", quantity = BigDecimal("2"), expiryDate = "2025-01-01"
        )
        assertEquals("batch_expired", e.getChanges()["action"])
    }

    @Test
    fun `transfer initiated event exposes changes`() {
        val e = InventoryTransferInitiatedEvent(
            src, "WS-1", "TXN-1", "USR-1", "DEV-1",
            transactionId = "TXN-1", transactionNumber = "TXN-20250101-0001", inventoryItemId = "INV-1",
            itemName = "Widget", fromWarehouseId = "WHR-1", toWarehouseId = "WHR-2", quantity = BigDecimal("4")
        )
        val c = e.getChanges()
        assertEquals("transfer_initiated", c["action"])
        assertEquals("WHR-2", c["to_warehouse_id"])
    }

    @Test
    fun `adjustment event handles null notes`() {
        val e = InventoryAdjustmentEvent(
            src, "WS-1", "TXN-1", "USR-1", "DEV-1",
            transactionId = "TXN-1", transactionNumber = "TXN-20250101-0002", inventoryItemId = "INV-1",
            itemName = "Widget", warehouseId = "WHR-1", adjustmentQuantity = BigDecimal("-3"),
            reason = "DAMAGE", notes = null
        )
        val c = e.getChanges()
        assertEquals("inventory_adjusted", c["action"])
        assertEquals("", c["notes"])
    }
}
