package com.ampairs.product.connector

import com.ampairs.core.connector.WriteOutcome
import com.ampairs.inventory.domain.dto.PhysicalCountRequest
import com.ampairs.inventory.domain.model.InventoryItem
import com.ampairs.inventory.domain.model.Warehouse
import com.ampairs.inventory.repository.InventoryItemRepository
import com.ampairs.inventory.service.InventoryTransactionService
import com.ampairs.inventory.service.WarehouseService
import com.ampairs.product.domain.model.Product
import com.ampairs.product.repository.ProductRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.math.BigDecimal

/**
 * Unit tests for the Tally `stock_balance` connector writer (spec 013 §stock_balance re-map).
 * Verifies: opt-in tracking (no auto-create), default-warehouse gating, idempotent no-op, and a
 * ledger-consistent physical COUNT only on a real change.
 */
@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConnectorProductStockWriterTest {

    @Mock private lateinit var productRepository: ProductRepository
    @Mock private lateinit var warehouseService: WarehouseService
    @Mock private lateinit var inventoryItemRepository: InventoryItemRepository
    @Mock private lateinit var inventoryTransactionService: InventoryTransactionService

    private lateinit var writer: ConnectorProductStockWriter

    private val product = Product().apply { uid = "PRD1"; refId = "TALLY-GUID-1"; name = "Widget" }
    private val warehouse = Warehouse().apply { uid = "WH1"; isDefault = true }

    @BeforeEach
    fun setUp() {
        writer = ConnectorProductStockWriter(
            productRepository, warehouseService, inventoryItemRepository, inventoryTransactionService,
        )
    }

    private fun item(current: String) = InventoryItem().apply {
        uid = "ITM1"; productId = "PRD1"; warehouseId = "WH1"; currentStock = BigDecimal(current)
    }

    @Test
    fun `entityType is stock_balance`() {
        assertEquals("stock_balance", writer.entityType)
    }

    @Test
    fun `no matching product is skipped and never counts`() {
        whenever(productRepository.findByRefId(any())).thenReturn(null)
        whenever(productRepository.findByUid(any())).thenReturn(null)

        val r = writer.applySparse("MISSING", "MISSING", mapOf("stockQuantity" to 10))

        assertEquals(WriteOutcome.SKIPPED, r.outcome)
        verify(inventoryTransactionService, never()).physicalCount(any())
    }

    @Test
    fun `missing stockQuantity column is skipped`() {
        whenever(productRepository.findByRefId("TALLY-GUID-1")).thenReturn(product)

        val r = writer.applySparse("TALLY-GUID-1", null, mapOf("name" to "x"))

        assertEquals(WriteOutcome.SKIPPED, r.outcome)
        assertEquals("PRD1", r.ampairsUid)
        verify(inventoryTransactionService, never()).physicalCount(any())
    }

    @Test
    fun `non-numeric stockQuantity is skipped`() {
        whenever(productRepository.findByRefId("TALLY-GUID-1")).thenReturn(product)

        val r = writer.applySparse("TALLY-GUID-1", null, mapOf("stockQuantity" to "abc"))

        assertEquals(WriteOutcome.SKIPPED, r.outcome)
        verify(inventoryTransactionService, never()).physicalCount(any())
    }

    @Test
    fun `no default warehouse is skipped`() {
        whenever(productRepository.findByRefId("TALLY-GUID-1")).thenReturn(product)
        whenever(warehouseService.getDefaultWarehouse()).thenReturn(null)

        val r = writer.applySparse("TALLY-GUID-1", null, mapOf("stockQuantity" to 10))

        assertEquals(WriteOutcome.SKIPPED, r.outcome)
        verify(inventoryTransactionService, never()).physicalCount(any())
    }

    @Test
    fun `untracked product is skipped and never auto-creates an item`() {
        whenever(productRepository.findByRefId("TALLY-GUID-1")).thenReturn(product)
        whenever(warehouseService.getDefaultWarehouse()).thenReturn(warehouse)
        whenever(inventoryItemRepository.findByProductIdAndWarehouseId("PRD1", "WH1")).thenReturn(null)

        val r = writer.applySparse("TALLY-GUID-1", null, mapOf("stockQuantity" to 10))

        assertEquals(WriteOutcome.SKIPPED, r.outcome)
        verify(inventoryTransactionService, never()).physicalCount(any())
    }

    @Test
    fun `unchanged stock is an idempotent skip - no count`() {
        whenever(productRepository.findByRefId("TALLY-GUID-1")).thenReturn(product)
        whenever(warehouseService.getDefaultWarehouse()).thenReturn(warehouse)
        // current on-hand 10.000; incoming 10 → compareTo == 0 (scale-insensitive)
        whenever(inventoryItemRepository.findByProductIdAndWarehouseId("PRD1", "WH1")).thenReturn(item("10.000"))

        val r = writer.applySparse("TALLY-GUID-1", null, mapOf("stockQuantity" to "10"))

        assertEquals(WriteOutcome.SKIPPED, r.outcome)
        verify(inventoryTransactionService, never()).physicalCount(any())
    }

    @Test
    fun `changed stock reconciles via physical count and reports UPDATED`() {
        whenever(productRepository.findByRefId("TALLY-GUID-1")).thenReturn(product)
        whenever(warehouseService.getDefaultWarehouse()).thenReturn(warehouse)
        whenever(inventoryItemRepository.findByProductIdAndWarehouseId("PRD1", "WH1")).thenReturn(item("5"))

        val r = writer.applySparse("TALLY-GUID-1", null, mapOf("stockQuantity" to 12.5))

        assertEquals(WriteOutcome.UPDATED, r.outcome)
        assertEquals("PRD1", r.ampairsUid)
        assertEquals(listOf("stockQuantity"), r.appliedColumns)
        verify(inventoryTransactionService).physicalCount(check<PhysicalCountRequest> {
            assertEquals("ITM1", it.inventoryItemId)
            assertEquals("WH1", it.warehouseId)
            assertEquals(0, it.countedQuantity.compareTo(BigDecimal("12.5")))
        })
    }

    @Test
    fun `matches by uid when refId absent`() {
        whenever(productRepository.findByUid("PRD1")).thenReturn(product)
        whenever(warehouseService.getDefaultWarehouse()).thenReturn(warehouse)
        whenever(inventoryItemRepository.findByProductIdAndWarehouseId("PRD1", "WH1")).thenReturn(item("0"))

        val r = writer.applySparse(null, "PRD1", mapOf("stockQuantity" to 3))

        assertEquals(WriteOutcome.UPDATED, r.outcome)
        verify(inventoryTransactionService).physicalCount(any())
    }
}
