package com.ampairs.inventory

import com.ampairs.inventory.config.Constants
import com.ampairs.inventory.domain.dto.BulkSerialRequest
import com.ampairs.inventory.domain.dto.SerialRequest
import com.ampairs.inventory.domain.dto.SerialReturnRequest
import com.ampairs.inventory.domain.dto.SerialSaleRequest
import com.ampairs.inventory.domain.dto.SerialUpdateRequest
import com.ampairs.inventory.domain.model.InventoryItem
import com.ampairs.inventory.domain.model.InventorySerial
import com.ampairs.inventory.domain.model.Warehouse
import com.ampairs.inventory.repository.InventorySerialRepository
import com.ampairs.inventory.service.InventoryItemService
import com.ampairs.inventory.service.InventorySerialService
import com.ampairs.inventory.service.WarehouseService
import org.junit.jupiter.api.Assertions.assertEquals
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
class InventorySerialServiceTest {

    @Mock
    private lateinit var serialRepository: InventorySerialRepository

    @Mock
    private lateinit var itemService: InventoryItemService

    @Mock
    private lateinit var warehouseService: WarehouseService

    private lateinit var service: InventorySerialService

    private fun serial(number: String = "SN-1", block: InventorySerial.() -> Unit = {}): InventorySerial =
        InventorySerial().apply {
            uid = "SRL-$number"
            serialNumber = number
            inventoryItemId = "INV-1"
            warehouseId = "WHR-1"
            status = Constants.SERIAL_AVAILABLE
            block()
        }

    @BeforeEach
    fun setUp() {
        service = InventorySerialService(serialRepository, itemService, warehouseService)
        whenever(serialRepository.save(any<InventorySerial>())).thenAnswer { it.arguments[0] }
        whenever(serialRepository.saveAll(any<List<InventorySerial>>())).thenAnswer { it.arguments[0] }
        whenever(itemService.getInventoryItemByUid(any())).thenReturn(InventoryItem().apply { uid = "INV-1" })
        whenever(warehouseService.getWarehouseByUid(any())).thenReturn(Warehouse().apply { uid = "WHR-1" })
    }

    @Test
    fun `getSerialByUid throws when missing`() {
        whenever(serialRepository.findByUid("X")).thenReturn(null)
        assertThrows(IllegalArgumentException::class.java) { service.getSerialByUid("X") }
    }

    @Test
    fun `getSerialByNumber throws when missing`() {
        whenever(serialRepository.findBySerialNumber("X")).thenReturn(null)
        assertThrows(IllegalArgumentException::class.java) { service.getSerialByNumber("X") }
    }

    @Test
    fun `createSerial saves new`() {
        val request = SerialRequest(
            serialNumber = "SN-NEW",
            inventoryItemId = "INV-1",
            warehouseId = "WHR-1",
            costPrice = BigDecimal("5")
        )
        whenever(serialRepository.existsBySerialNumber("SN-NEW")).thenReturn(false)

        val result = service.createSerial(request)

        assertEquals("SN-NEW", result.serialNumber)
        verify(serialRepository).save(any<InventorySerial>())
    }

    @Test
    fun `createSerial rejects duplicate`() {
        val request = SerialRequest(serialNumber = "SN-1", inventoryItemId = "INV-1", warehouseId = "WHR-1", costPrice = BigDecimal.ONE)
        whenever(serialRepository.existsBySerialNumber("SN-1")).thenReturn(true)
        assertThrows(IllegalArgumentException::class.java) { service.createSerial(request) }
        verify(serialRepository, never()).save(any<InventorySerial>())
    }

    @Test
    fun `createBulkSerials creates all when none exist`() {
        val request = BulkSerialRequest(
            inventoryItemId = "INV-1",
            warehouseId = "WHR-1",
            serialNumbers = listOf("A", "B", "C"),
            costPrice = BigDecimal("3")
        )
        whenever(serialRepository.findBySerialNumberIn(request.serialNumbers)).thenReturn(emptyList())

        val result = service.createBulkSerials(request)

        assertEquals(3, result.size)
        assertTrue(result.all { it.status == Constants.SERIAL_AVAILABLE })
    }

    @Test
    fun `createBulkSerials rejects when any exists`() {
        val request = BulkSerialRequest(
            inventoryItemId = "INV-1",
            warehouseId = "WHR-1",
            serialNumbers = listOf("A", "B"),
            costPrice = BigDecimal("3")
        )
        whenever(serialRepository.findBySerialNumberIn(request.serialNumbers)).thenReturn(listOf(serial("A")))
        assertThrows(IllegalArgumentException::class.java) { service.createBulkSerials(request) }
    }

    @Test
    fun `updateSerial applies provided fields only`() {
        val s = serial("SN-1") { costPrice = BigDecimal("1"); notes = "orig" }
        whenever(serialRepository.findByUid("SRL-SN-1")).thenReturn(s)

        val result = service.updateSerial("SRL-SN-1", SerialUpdateRequest(costPrice = BigDecimal("9")))

        assertEquals(BigDecimal("9"), result.costPrice)
        assertEquals("orig", result.notes) // unchanged
    }

    @Test
    fun `deleteSerial allows available`() {
        val s = serial("SN-1")
        whenever(serialRepository.findByUid("SRL-SN-1")).thenReturn(s)

        service.deleteSerial("SRL-SN-1")

        verify(serialRepository).delete(s)
    }

    @Test
    fun `deleteSerial blocks non-available`() {
        val s = serial("SN-1") { status = Constants.SERIAL_SOLD }
        whenever(serialRepository.findByUid("SRL-SN-1")).thenReturn(s)
        assertThrows(IllegalStateException::class.java) { service.deleteSerial("SRL-SN-1") }
    }

    @Test
    fun `allocateSerials returns FIFO subset`() {
        val list = listOf(serial("A"), serial("B"), serial("C"))
        whenever(serialRepository.findAvailableSerials("INV-1", "WHR-1")).thenReturn(list)

        val result = service.allocateSerials("INV-1", "WHR-1", 2)

        assertEquals(listOf("A", "B"), result)
    }

    @Test
    fun `allocateSerials throws when not enough`() {
        whenever(serialRepository.findAvailableSerials("INV-1", "WHR-1")).thenReturn(listOf(serial("A")))
        assertThrows(IllegalStateException::class.java) { service.allocateSerials("INV-1", "WHR-1", 5) }
    }

    @Test
    fun `reserveSerials transitions available to reserved`() {
        val a = serial("A")
        val b = serial("B")
        whenever(serialRepository.findBySerialNumberIn(listOf("A", "B"))).thenReturn(listOf(a, b))

        service.reserveSerials(listOf("A", "B"))

        assertEquals(Constants.SERIAL_RESERVED, a.status)
        assertEquals(Constants.SERIAL_RESERVED, b.status)
    }

    @Test
    fun `reserveSerials throws when one not found`() {
        whenever(serialRepository.findBySerialNumberIn(listOf("A", "B"))).thenReturn(listOf(serial("A")))
        assertThrows(IllegalArgumentException::class.java) { service.reserveSerials(listOf("A", "B")) }
    }

    @Test
    fun `releaseReservations only releases reserved ones`() {
        val reserved = serial("A") { status = Constants.SERIAL_RESERVED }
        val available = serial("B")
        whenever(serialRepository.findBySerialNumberIn(listOf("A", "B"))).thenReturn(listOf(reserved, available))

        service.releaseReservations(listOf("A", "B"))

        assertEquals(Constants.SERIAL_AVAILABLE, reserved.status)
        // available untouched, save called only for reserved one
        verify(serialRepository).save(reserved)
        verify(serialRepository, never()).save(available)
    }

    @Test
    fun `markSerialAsSold updates status and references`() {
        val s = serial("SN-1")
        whenever(serialRepository.findBySerialNumber("SN-1")).thenReturn(s)
        val request = SerialSaleRequest(
            serialNumber = "SN-1",
            referenceType = "INVOICE",
            referenceId = "INV-DOC-1",
            sellingPrice = BigDecimal("99")
        )

        val result = service.markSerialAsSold(request)

        assertEquals(Constants.SERIAL_SOLD, result.status)
        assertEquals("INVOICE", result.soldReferenceType)
        assertEquals(BigDecimal("99"), result.sellingPrice)
    }

    @Test
    fun `markSerialsAsSold marks all`() {
        val a = serial("A")
        val b = serial("B")
        whenever(serialRepository.findBySerialNumberIn(listOf("A", "B"))).thenReturn(listOf(a, b))

        val result = service.markSerialsAsSold(listOf("A", "B"), "ORDER", "ORD-1")

        assertEquals(2, result.size)
        assertTrue(result.all { it.isSold() })
    }

    @Test
    fun `markSerialsAsSold throws when missing`() {
        whenever(serialRepository.findBySerialNumberIn(listOf("A", "B"))).thenReturn(listOf(serial("A")))
        assertThrows(IllegalArgumentException::class.java) {
            service.markSerialsAsSold(listOf("A", "B"), "ORDER", "ORD-1")
        }
    }

    @Test
    fun `markSerialAsReturned requires sold first`() {
        val s = serial("SN-1") { status = Constants.SERIAL_AVAILABLE }
        whenever(serialRepository.findBySerialNumber("SN-1")).thenReturn(s)
        assertThrows(IllegalStateException::class.java) {
            service.markSerialAsReturned(SerialReturnRequest(serialNumber = "SN-1", referenceType = "RET", referenceId = "R-1"))
        }
    }

    @Test
    fun `markSerialAsReturned succeeds when sold`() {
        val s = serial("SN-1") { status = Constants.SERIAL_SOLD }
        whenever(serialRepository.findBySerialNumber("SN-1")).thenReturn(s)

        val result = service.markSerialAsReturned(SerialReturnRequest(serialNumber = "SN-1", referenceType = "RET", referenceId = "R-1"))

        assertTrue(result.isReturned())
    }

    @Test
    fun `markSerialAsDamaged sets damaged`() {
        val s = serial("SN-1")
        whenever(serialRepository.findBySerialNumber("SN-1")).thenReturn(s)

        val result = service.markSerialAsDamaged("SN-1", "broken")

        assertTrue(result.isDamaged())
        assertEquals("broken", result.notes)
    }

    @Test
    fun `makeSerialAvailable blocks sold`() {
        val s = serial("SN-1") { status = Constants.SERIAL_SOLD }
        whenever(serialRepository.findBySerialNumber("SN-1")).thenReturn(s)
        assertThrows(IllegalStateException::class.java) { service.makeSerialAvailable("SN-1") }
    }

    @Test
    fun `updateSerialWarehouse changes location`() {
        val s = serial("SN-1")
        whenever(serialRepository.findByUid("SRL-SN-1")).thenReturn(s)

        val result = service.updateSerialWarehouse("SRL-SN-1", "WHR-9")

        assertEquals("WHR-9", result.warehouseId)
    }

    @Test
    fun `getSerialStatusSummary aggregates counts`() {
        whenever(serialRepository.countByInventoryItemIdAndWarehouseId("INV-1", "WHR-1")).thenReturn(10L)
        whenever(serialRepository.countByInventoryItemIdAndWarehouseIdAndStatus("INV-1", "WHR-1", Constants.SERIAL_AVAILABLE)).thenReturn(4L)
        whenever(serialRepository.countByInventoryItemIdAndWarehouseIdAndStatus("INV-1", "WHR-1", Constants.SERIAL_RESERVED)).thenReturn(2L)
        whenever(serialRepository.countByInventoryItemIdAndWarehouseIdAndStatus("INV-1", "WHR-1", Constants.SERIAL_SOLD)).thenReturn(3L)
        whenever(serialRepository.countByInventoryItemIdAndWarehouseIdAndStatus("INV-1", "WHR-1", Constants.SERIAL_DAMAGED)).thenReturn(1L)
        whenever(serialRepository.countByInventoryItemIdAndWarehouseIdAndStatus("INV-1", "WHR-1", "RETURNED")).thenReturn(0L)

        val summary = service.getSerialStatusSummary("INV-1", "WHR-1")

        assertEquals(10L, summary.totalSerials)
        assertEquals(4L, summary.availableSerials)
        assertEquals(3L, summary.soldSerials)
    }
}
