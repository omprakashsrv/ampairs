package com.ampairs.inventory

import com.ampairs.inventory.config.Constants
import com.ampairs.inventory.domain.dto.BatchUpdateRequest
import com.ampairs.inventory.domain.dto.InventoryBatchRequest
import com.ampairs.inventory.domain.model.InventoryBatch
import com.ampairs.inventory.domain.model.InventoryItem
import com.ampairs.inventory.domain.model.Warehouse
import com.ampairs.inventory.repository.InventoryBatchRepository
import com.ampairs.inventory.service.InventoryBatchService
import com.ampairs.inventory.service.InventoryItemService
import com.ampairs.inventory.service.WarehouseService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryBatchServiceTest {

    @Mock
    private lateinit var batchRepository: InventoryBatchRepository

    @Mock
    private lateinit var itemService: InventoryItemService

    @Mock
    private lateinit var warehouseService: WarehouseService

    private lateinit var service: InventoryBatchService

    private fun batch(uid: String = "BCH-1", available: BigDecimal = BigDecimal("100"), block: InventoryBatch.() -> Unit = {}): InventoryBatch =
        InventoryBatch().apply {
            this.uid = uid
            batchNumber = "B-$uid"
            inventoryItemId = "INV-1"
            warehouseId = "WHR-1"
            totalQuantity = available
            availableQuantity = available
            reservedQuantity = BigDecimal.ZERO
            isActive = true
            isExpired = false
            block()
        }

    @BeforeEach
    fun setUp() {
        service = InventoryBatchService(batchRepository, itemService, warehouseService)
        whenever(batchRepository.save(any<InventoryBatch>())).thenAnswer { it.arguments[0] }
        whenever(itemService.getInventoryItemByUid(any())).thenReturn(InventoryItem().apply { uid = "INV-1" })
        whenever(warehouseService.getWarehouseByUid(any())).thenReturn(Warehouse().apply { uid = "WHR-1" })
    }

    @Test
    fun `getBatchByUid throws when missing`() {
        whenever(batchRepository.findByUid("X")).thenReturn(null)
        assertThrows(IllegalArgumentException::class.java) { service.getBatchByUid("X") }
    }

    @Test
    fun `createBatch saves new batch`() {
        val request = InventoryBatchRequest(
            batchNumber = "B-001",
            inventoryItemId = "INV-1",
            warehouseId = "WHR-1",
            quantity = BigDecimal("50"),
            costPerUnit = BigDecimal("3")
        )
        whenever(batchRepository.existsByBatchNumberAndInventoryItemIdAndWarehouseId("B-001", "INV-1", "WHR-1"))
            .thenReturn(false)

        val result = service.createBatch(request)

        assertEquals("B-001", result.batchNumber)
        assertEquals(BigDecimal("50"), result.totalQuantity)
        assertEquals(BigDecimal("50"), result.availableQuantity)
        assertTrue(result.isActive)
    }

    @Test
    fun `createBatch rejects duplicate batch number`() {
        val request = InventoryBatchRequest(
            batchNumber = "B-001",
            inventoryItemId = "INV-1",
            warehouseId = "WHR-1",
            quantity = BigDecimal("50"),
            costPerUnit = BigDecimal("3")
        )
        whenever(batchRepository.existsByBatchNumberAndInventoryItemIdAndWarehouseId("B-001", "INV-1", "WHR-1"))
            .thenReturn(true)

        assertThrows(IllegalArgumentException::class.java) { service.createBatch(request) }
        verify(batchRepository, never()).save(any<InventoryBatch>())
    }

    @Test
    fun `updateBatch applies provided fields`() {
        val b = batch()
        whenever(batchRepository.findByUid("BCH-1")).thenReturn(b)

        val result = service.updateBatch("BCH-1", BatchUpdateRequest(lotNumber = "LOT-9", costPerUnit = BigDecimal("7"), isActive = false))

        assertEquals("LOT-9", result.lotNumber)
        assertEquals(BigDecimal("7"), result.costPerUnit)
        assertFalse(result.isActive)
    }

    @Test
    fun `deleteBatch blocks when stock present`() {
        val b = batch(available = BigDecimal("10"))
        whenever(batchRepository.findByUid("BCH-1")).thenReturn(b)
        assertThrows(IllegalStateException::class.java) { service.deleteBatch("BCH-1") }
    }

    @Test
    fun `deleteBatch soft deletes when empty`() {
        val b = batch(available = BigDecimal.ZERO)
        whenever(batchRepository.findByUid("BCH-1")).thenReturn(b)

        service.deleteBatch("BCH-1")

        assertFalse(b.isActive)
        verify(batchRepository).save(b)
    }

    @Test
    fun `allocateBatches consumes across multiple batches FIFO`() {
        val b1 = batch("BCH-1", BigDecimal("30"))
        val b2 = batch("BCH-2", BigDecimal("50"))
        whenever(batchRepository.findAvailableBatchesFIFO("INV-1", "WHR-1")).thenReturn(listOf(b1, b2))

        val allocations = service.allocateBatches("INV-1", "WHR-1", BigDecimal("60"))

        assertEquals(2, allocations.size)
        assertEquals(BigDecimal("30"), allocations[0].second)
        assertEquals(BigDecimal("30"), allocations[1].second)
        assertEquals(BigDecimal.ZERO, b1.availableQuantity)
        assertEquals(BigDecimal("20"), b2.availableQuantity)
    }

    @Test
    fun `allocateBatches throws when no batches`() {
        whenever(batchRepository.findAvailableBatchesFIFO("INV-1", "WHR-1")).thenReturn(emptyList())
        assertThrows(IllegalStateException::class.java) {
            service.allocateBatches("INV-1", "WHR-1", BigDecimal("10"))
        }
    }

    @Test
    fun `allocateBatches throws when insufficient total`() {
        val b1 = batch("BCH-1", BigDecimal("10"))
        whenever(batchRepository.findAvailableBatchesFIFO("INV-1", "WHR-1")).thenReturn(listOf(b1))
        assertThrows(IllegalStateException::class.java) {
            service.allocateBatches("INV-1", "WHR-1", BigDecimal("100"))
        }
    }

    @Test
    fun `allocateBatches honors FEFO strategy override`() {
        val b1 = batch("BCH-1", BigDecimal("100"))
        whenever(batchRepository.findAvailableBatchesFEFO("INV-1", "WHR-1")).thenReturn(listOf(b1))

        val allocations = service.allocateBatches("INV-1", "WHR-1", BigDecimal("40"), strategy = Constants.STRATEGY_FEFO)

        assertEquals(BigDecimal("40"), allocations[0].second)
    }

    @Test
    fun `reserveBatches reserves available stock`() {
        val b1 = batch("BCH-1", BigDecimal("100"))
        whenever(batchRepository.findAvailableBatchesLIFO("INV-1", "WHR-1")).thenReturn(listOf(b1))

        val reservations = service.reserveBatches("INV-1", "WHR-1", BigDecimal("40"), strategy = Constants.STRATEGY_LIFO)

        assertEquals(BigDecimal("40"), reservations[0].second)
        assertEquals(BigDecimal("60"), b1.availableQuantity)
        assertEquals(BigDecimal("40"), b1.reservedQuantity)
    }

    @Test
    fun `reserveBatches throws when insufficient`() {
        val b1 = batch("BCH-1", BigDecimal("10"))
        whenever(batchRepository.findAvailableBatchesFIFO("INV-1", "WHR-1")).thenReturn(listOf(b1))
        assertThrows(IllegalStateException::class.java) {
            service.reserveBatches("INV-1", "WHR-1", BigDecimal("100"))
        }
    }

    @Test
    fun `releaseReservations releases each batch`() {
        val b1 = batch("BCH-1", BigDecimal("60")) { reservedQuantity = BigDecimal("40") }
        whenever(batchRepository.findByUid("BCH-1")).thenReturn(b1)

        service.releaseReservations(listOf(Pair("BCH-1", BigDecimal("40"))))

        assertEquals(BigDecimal.ZERO, b1.reservedQuantity)
        assertEquals(BigDecimal("100"), b1.availableQuantity)
    }

    @Test
    fun `markExpiredBatches marks all returned`() {
        val b1 = batch("BCH-1")
        val b2 = batch("BCH-2")
        whenever(batchRepository.findExpiredBatches()).thenReturn(listOf(b1, b2))

        val count = service.markExpiredBatches()

        assertEquals(2, count)
        assertTrue(b1.isExpired)
        assertTrue(b2.isExpired)
    }
}
