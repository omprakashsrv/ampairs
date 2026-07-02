package com.ampairs.inventory

import com.ampairs.core.domain.model.Address
import com.ampairs.inventory.config.Constants
import com.ampairs.inventory.domain.dto.InventoryBatchRequest
import com.ampairs.inventory.domain.dto.InventoryItemRequest
import com.ampairs.inventory.domain.dto.SerialRequest
import com.ampairs.inventory.domain.dto.StockTransferRequest
import com.ampairs.inventory.domain.dto.WarehouseRequest
import com.ampairs.inventory.domain.dto.asBatchSummaryResponse
import com.ampairs.inventory.domain.dto.asInventoryBatchResponse
import com.ampairs.inventory.domain.dto.asInventoryItemResponse
import com.ampairs.inventory.domain.dto.asInventorySerialResponse
import com.ampairs.inventory.domain.dto.asInventoryTransactionResponse
import com.ampairs.inventory.domain.dto.asStockSummary
import com.ampairs.inventory.domain.dto.asWarehouseResponse
import com.ampairs.inventory.domain.dto.toInventoryBatch
import com.ampairs.inventory.domain.dto.toInventoryItem
import com.ampairs.inventory.domain.dto.toInventorySerial
import com.ampairs.inventory.domain.dto.toWarehouse
import com.ampairs.inventory.domain.model.InventoryBatch
import com.ampairs.inventory.domain.model.InventoryItem
import com.ampairs.inventory.domain.model.InventorySerial
import com.ampairs.inventory.domain.model.InventoryTransaction
import com.ampairs.inventory.domain.model.Warehouse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class InventoryDtoMapperTest {

    @Test
    fun `WarehouseRequest toWarehouse normalizes code and trims`() {
        val request = WarehouseRequest(
            name = "  Main  ",
            code = "  wh-1  ",
            phone = " 999 ",
            description = " desc "
        )

        val wh = request.toWarehouse()

        assertEquals("Main", wh.name)
        assertEquals("WH-1", wh.code) // uppercased + trimmed
        assertEquals("999", wh.phone)
        assertEquals("desc", wh.description)
    }

    @Test
    fun `Warehouse asWarehouseResponse maps all fields`() {
        val wh = Warehouse().apply {
            uid = "WHR-1"
            name = "Main"
            code = "WH-1"
            warehouseType = Constants.WAREHOUSE_TYPE_STORE
            isActive = true
            isDefault = true
            address = Address()
            createdAt = Instant.parse("2025-01-01T00:00:00Z")
            updatedAt = Instant.parse("2025-01-02T00:00:00Z")
        }

        val resp = wh.asWarehouseResponse()

        assertEquals("WHR-1", resp.uid)
        assertEquals("WH-1", resp.code)
        assertEquals(Constants.WAREHOUSE_TYPE_STORE, resp.warehouseType)
        assertTrue(resp.isDefault)
        assertEquals(Instant.parse("2025-01-01T00:00:00Z"), resp.createdAt)
    }

    @Test
    fun `InventoryItemRequest toInventoryItem normalizes sku and recalculates`() {
        val request = InventoryItemRequest(
            sku = " abc ",
            name = " Widget ",
            warehouseId = "WHR-1",
            currentStock = BigDecimal("100"),
            reservedStock = BigDecimal("30")
        )

        val item = request.toInventoryItem()

        assertEquals("ABC", item.sku)
        assertEquals("Widget", item.name)
        assertEquals(BigDecimal("70"), item.availableStock)
    }

    @Test
    fun `InventoryItem asInventoryItemResponse computes stock value and flags`() {
        val item = InventoryItem().apply {
            uid = "INV-1"
            sku = "SKU-1"
            name = "Widget"
            warehouseId = "WHR-1"
            currentStock = BigDecimal("10")
            reservedStock = BigDecimal("2")
            availableStock = BigDecimal("8")
            reorderLevel = BigDecimal("20")
            maxStockLevel = BigDecimal("5")
            costPrice = BigDecimal("4")
        }

        val resp = item.asInventoryItemResponse()

        assertEquals(BigDecimal("40"), resp.stockValue) // 10 * 4
        assertTrue(resp.isLowStock)  // 10 <= 20
        assertTrue(resp.isOverStock) // 10 > 5
    }

    @Test
    fun `InventoryItem asStockSummary maps essentials`() {
        val item = InventoryItem().apply {
            uid = "INV-1"
            sku = "SKU-1"
            name = "Widget"
            warehouseId = "WHR-1"
            currentStock = BigDecimal("100")
            availableStock = BigDecimal("90")
            reservedStock = BigDecimal("10")
            reorderLevel = BigDecimal("0")
        }

        val summary = item.asStockSummary()

        assertEquals("INV-1", summary.itemUid)
        assertEquals(BigDecimal("90"), summary.availableStock)
        assertFalse(summary.isLowStock) // reorderLevel 0 -> not low
    }

    @Test
    fun `InventoryItem isLowStock and isOverStock edge cases`() {
        val noReorder = InventoryItem().apply { currentStock = BigDecimal("0"); reorderLevel = BigDecimal.ZERO }
        assertFalse(noReorder.isLowStock())

        val noMax = InventoryItem().apply { currentStock = BigDecimal("1000"); maxStockLevel = BigDecimal.ZERO }
        assertFalse(noMax.isOverStock())
    }

    @Test
    fun `SerialRequest toInventorySerial maps fields`() {
        val request = SerialRequest(
            serialNumber = "SN-1",
            inventoryItemId = "INV-1",
            warehouseId = "WHR-1",
            costPrice = BigDecimal("5"),
            sellingPrice = BigDecimal("8")
        )

        val serial = request.toInventorySerial()

        assertEquals("SN-1", serial.serialNumber)
        assertEquals(BigDecimal("5"), serial.costPrice)
        assertEquals(BigDecimal("8"), serial.sellingPrice)
    }

    @Test
    fun `InventorySerial asInventorySerialResponse maps status flags`() {
        val serial = InventorySerial().apply {
            uid = "SRL-1"
            serialNumber = "SN-1"
            inventoryItemId = "INV-1"
            warehouseId = "WHR-1"
            status = Constants.SERIAL_AVAILABLE
            receivedDate = Instant.parse("2025-01-01T00:00:00Z")
            warrantyExpiryDate = Instant.now().plusSeconds(60L * 60 * 24 * 30)
            createdAt = Instant.parse("2025-01-01T00:00:00Z")
            updatedAt = Instant.parse("2025-01-01T00:00:00Z")
        }

        val resp = serial.asInventorySerialResponse()

        assertTrue(resp.isAvailable)
        assertFalse(resp.isSold)
        assertFalse(resp.hasWarrantyExpired)
        assertTrue((resp.daysUntilWarrantyExpiry ?: 0) in 28..30)
    }

    @Test
    fun `InventoryTransaction asInventoryTransactionResponse maps amounts`() {
        val txn = InventoryTransaction().apply {
            uid = "TXN-1"
            transactionNumber = "TXN-20250101-0001"
            transactionType = Constants.TXN_TYPE_STOCK_IN
            transactionReason = Constants.REASON_PURCHASE
            inventoryItemId = "INV-1"
            warehouseId = "WHR-1"
            quantity = BigDecimal("10")
            unitCost = BigDecimal("4")
            calculateTotalCost()
            balanceAfter = BigDecimal("110")
        }

        val resp = txn.asInventoryTransactionResponse()

        assertEquals("TXN-20250101-0001", resp.transactionNumber)
        assertEquals(BigDecimal("40"), resp.totalCost)
        assertEquals(BigDecimal("110"), resp.balanceAfter)
        assertNull(resp.inventoryItemName) // lazy relation not loaded
    }

    @Test
    fun `InventoryBatchRequest toInventoryBatch initializes quantities`() {
        val request = InventoryBatchRequest(
            batchNumber = "B-1",
            inventoryItemId = "INV-1",
            warehouseId = "WHR-1",
            quantity = BigDecimal("100"),
            costPerUnit = BigDecimal("2")
        )

        val batch = request.toInventoryBatch()

        assertEquals(BigDecimal("100"), batch.totalQuantity)
        assertEquals(BigDecimal("100"), batch.availableQuantity)
        assertEquals(BigDecimal.ZERO, batch.reservedQuantity)
        assertTrue(batch.isActive)
        assertFalse(batch.isExpired)
    }

    @Test
    fun `InventoryBatch asInventoryBatchResponse computes value and consumed`() {
        val batch = InventoryBatch().apply {
            uid = "BCH-1"
            batchNumber = "B-1"
            inventoryItemId = "INV-1"
            warehouseId = "WHR-1"
            totalQuantity = BigDecimal("100")
            availableQuantity = BigDecimal("60")
            reservedQuantity = BigDecimal("10")
            costPerUnit = BigDecimal("2")
        }

        val resp = batch.asInventoryBatchResponse()

        assertEquals(BigDecimal("30"), resp.consumedQuantity) // 100 - 60 - 10
        assertEquals(BigDecimal("120"), resp.batchValue) // 60 * 2
        assertNull(resp.expiryDate)
        assertNull(resp.daysUntilExpiry)
    }

    @Test
    fun `InventoryBatch asBatchSummaryResponse maps with expiry`() {
        val batch = InventoryBatch().apply {
            uid = "BCH-1"
            batchNumber = "B-1"
            availableQuantity = BigDecimal("5")
            expiryDate = Instant.now().plusSeconds(60L * 60 * 24 * 10)
            costPerUnit = BigDecimal("3")
        }

        val summary = batch.asBatchSummaryResponse()

        assertEquals("B-1", summary.batchNumber)
        assertTrue((summary.daysUntilExpiry ?: 0) in 8..10)
    }

    @Test
    fun `InventoryBatch consume from available throws when insufficient`() {
        val batch = InventoryBatch().apply { availableQuantity = BigDecimal("5") }
        assertTrue(
            runCatching { batch.consume(BigDecimal("10"), fromReserved = false) }
                .exceptionOrNull() is IllegalStateException
        )
    }

    @Test
    fun `StockTransferRequest defaults are valid`() {
        val request = StockTransferRequest(
            inventoryItemId = "INV-1",
            fromWarehouseId = "WHR-1",
            toWarehouseId = "WHR-2",
            quantity = BigDecimal("5")
        )
        assertEquals("INV-1", request.inventoryItemId)
        assertEquals(BigDecimal("5"), request.quantity)
    }
}
