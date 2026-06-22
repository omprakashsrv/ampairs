package com.ampairs.inventory

import com.ampairs.inventory.domain.model.InventoryItem
import com.ampairs.inventory.domain.model.Warehouse
import com.ampairs.inventory.repository.InventoryItemRepository
import com.ampairs.inventory.service.InventoryItemService
import com.ampairs.inventory.service.WarehouseService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
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
class InventoryItemServiceTest {

    @Mock
    private lateinit var itemRepository: InventoryItemRepository

    @Mock
    private lateinit var warehouseService: WarehouseService

    private lateinit var service: InventoryItemService

    private fun item(block: InventoryItem.() -> Unit = {}): InventoryItem =
        InventoryItem().apply {
            uid = "INV-1"
            sku = "SKU-1"
            name = "Widget"
            warehouseId = "WHR-1"
            currentStock = BigDecimal("100")
            reservedStock = BigDecimal("10")
            availableStock = BigDecimal("90")
            reorderLevel = BigDecimal("20")
            maxStockLevel = BigDecimal("500")
            costPrice = BigDecimal("5")
            block()
        }

    private fun warehouse(active: Boolean = true): Warehouse =
        Warehouse().apply { uid = "WHR-1"; code = "WH-1"; isActive = active }

    @BeforeEach
    fun setUp() {
        service = InventoryItemService(itemRepository, warehouseService)
        whenever(itemRepository.save(any<InventoryItem>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `createInventoryItem saves and recalculates available stock`() {
        val it = item { currentStock = BigDecimal("50"); reservedStock = BigDecimal("5"); availableStock = BigDecimal.ZERO }
        whenever(itemRepository.existsBySku("SKU-1")).thenReturn(false)
        whenever(warehouseService.getWarehouseByUid("WHR-1")).thenReturn(warehouse())

        val result = service.createInventoryItem(it)

        assertEquals(BigDecimal("45"), result.availableStock)
        verify(itemRepository).save(it)
    }

    @Test
    fun `createInventoryItem rejects duplicate sku`() {
        whenever(itemRepository.existsBySku("SKU-1")).thenReturn(true)
        assertThrows(IllegalArgumentException::class.java) { service.createInventoryItem(item()) }
        verify(itemRepository, never()).save(any<InventoryItem>())
    }

    @Test
    fun `createInventoryItem rejects missing warehouse`() {
        whenever(itemRepository.existsBySku("SKU-1")).thenReturn(false)
        whenever(warehouseService.getWarehouseByUid("WHR-1")).thenReturn(null)
        assertThrows(IllegalArgumentException::class.java) { service.createInventoryItem(item()) }
    }

    @Test
    fun `createInventoryItem rejects inactive warehouse`() {
        whenever(itemRepository.existsBySku("SKU-1")).thenReturn(false)
        whenever(warehouseService.getWarehouseByUid("WHR-1")).thenReturn(warehouse(active = false))
        assertThrows(IllegalArgumentException::class.java) { service.createInventoryItem(item()) }
    }

    @Test
    fun `updateInventoryItem updates fields`() {
        val existing = item()
        whenever(itemRepository.findByUid("INV-1")).thenReturn(existing)
        val updates = item {
            sku = "SKU-1"
            name = "New Name"
            reorderLevel = BigDecimal("30")
            costPrice = BigDecimal("9")
            isActive = false
        }

        val result = service.updateInventoryItem("INV-1", updates)

        assertEquals("New Name", result.name)
        assertEquals(BigDecimal("30"), result.reorderLevel)
        assertEquals(BigDecimal("9"), result.costPrice)
        assertFalse(result.isActive)
    }

    @Test
    fun `updateInventoryItem rejects sku conflict`() {
        val existing = item { sku = "OLD" }
        whenever(itemRepository.findByUid("INV-1")).thenReturn(existing)
        whenever(itemRepository.existsBySku("NEW")).thenReturn(true)
        assertThrows(IllegalArgumentException::class.java) {
            service.updateInventoryItem("INV-1", item { sku = "NEW" })
        }
    }

    @Test
    fun `getInventoryItemByUid throws when missing`() {
        whenever(itemRepository.findByUid("X")).thenReturn(null)
        assertThrows(IllegalArgumentException::class.java) { service.getInventoryItemByUid("X") }
    }

    @Test
    fun `findByInventoryItemIdAndWarehouseId resolves by productId`() {
        val src = item { productId = "PRD-1"; productVariantId = null }
        val dest = item { uid = "INV-2"; warehouseId = "WHR-2" }
        whenever(itemRepository.findByUid("INV-1")).thenReturn(src)
        whenever(itemRepository.findByProductIdAndWarehouseId("PRD-1", "WHR-2")).thenReturn(dest)

        assertSame(dest, service.findByInventoryItemIdAndWarehouseId("INV-1", "WHR-2"))
    }

    @Test
    fun `findByInventoryItemIdAndWarehouseId resolves by variant`() {
        val src = item { productId = "PRD-1"; productVariantId = "VAR-1" }
        val dest = item { uid = "INV-2"; warehouseId = "WHR-2" }
        whenever(itemRepository.findByUid("INV-1")).thenReturn(src)
        whenever(itemRepository.findByProductVariantIdAndWarehouseId("VAR-1", "WHR-2")).thenReturn(dest)

        assertSame(dest, service.findByInventoryItemIdAndWarehouseId("INV-1", "WHR-2"))
    }

    @Test
    fun `findByInventoryItemIdAndWarehouseId standalone same warehouse returns item`() {
        val src = item { productId = null; warehouseId = "WHR-1" }
        whenever(itemRepository.findByUid("INV-1")).thenReturn(src)
        assertSame(src, service.findByInventoryItemIdAndWarehouseId("INV-1", "WHR-1"))
    }

    @Test
    fun `findByInventoryItemIdAndWarehouseId standalone different warehouse returns null`() {
        val src = item { productId = null; warehouseId = "WHR-1" }
        whenever(itemRepository.findByUid("INV-1")).thenReturn(src)
        assertNull(service.findByInventoryItemIdAndWarehouseId("INV-1", "WHR-OTHER"))
    }

    @Test
    fun `findByInventoryItemIdAndWarehouseId missing item returns null`() {
        whenever(itemRepository.findByUid("X")).thenReturn(null)
        assertNull(service.findByInventoryItemIdAndWarehouseId("X", "WHR-1"))
    }

    @Test
    fun `updateStockQuantities recalculates available`() {
        val it = item()
        whenever(itemRepository.findByUid("INV-1")).thenReturn(it)

        val result = service.updateStockQuantities("INV-1", BigDecimal("200"), BigDecimal("30"))

        assertEquals(BigDecimal("200"), result.currentStock)
        assertEquals(BigDecimal("170"), result.availableStock)
    }

    @Test
    fun `reserveStock increases reserved when enough available`() {
        val it = item { availableStock = BigDecimal("90"); reservedStock = BigDecimal("10") }
        whenever(itemRepository.findByUid("INV-1")).thenReturn(it)

        val result = service.reserveStock("INV-1", BigDecimal("40"))

        assertEquals(BigDecimal("50"), result.reservedStock)
        assertEquals(BigDecimal("50"), result.availableStock) // 100 - 50
    }

    @Test
    fun `reserveStock throws when insufficient available`() {
        val it = item { availableStock = BigDecimal("5") }
        whenever(itemRepository.findByUid("INV-1")).thenReturn(it)
        assertThrows(IllegalArgumentException::class.java) {
            service.reserveStock("INV-1", BigDecimal("10"))
        }
    }

    @Test
    fun `releaseReservedStock decreases reserved and floors at zero`() {
        val it = item { reservedStock = BigDecimal("10") }
        whenever(itemRepository.findByUid("INV-1")).thenReturn(it)

        val result = service.releaseReservedStock("INV-1", BigDecimal("25"))

        assertEquals(BigDecimal.ZERO, result.reservedStock)
        assertEquals(BigDecimal("100"), result.availableStock)
    }

    @Test
    fun `deleteInventoryItem soft deletes`() {
        val it = item { isActive = true }
        whenever(itemRepository.findByUid("INV-1")).thenReturn(it)

        service.deleteInventoryItem("INV-1")

        assertFalse(it.isActive)
        verify(itemRepository).save(it)
    }

    @Test
    fun `isLowStock compares current to reorder`() {
        whenever(itemRepository.findByUid("INV-1"))
            .thenReturn(item { currentStock = BigDecimal("15"); reorderLevel = BigDecimal("20") })
        assertTrue(service.isLowStock("INV-1"))

        whenever(itemRepository.findByUid("INV-1"))
            .thenReturn(item { currentStock = BigDecimal("100"); reorderLevel = BigDecimal("20") })
        assertFalse(service.isLowStock("INV-1"))
    }

    @Test
    fun `getLowStockItemsByWarehouse filters active and below reorder`() {
        val below = item { uid = "A"; isActive = true; currentStock = BigDecimal("5"); reorderLevel = BigDecimal("20") }
        val above = item { uid = "B"; isActive = true; currentStock = BigDecimal("100"); reorderLevel = BigDecimal("20") }
        val inactive = item { uid = "C"; isActive = false; currentStock = BigDecimal("1"); reorderLevel = BigDecimal("20") }
        whenever(itemRepository.findByWarehouseId("WHR-1")).thenReturn(listOf(below, above, inactive))

        val result = service.getLowStockItemsByWarehouse("WHR-1")

        assertEquals(1, result.size)
        assertEquals("A", result[0].uid)
    }

    @Test
    fun `getOutOfStockItems filters zero-or-negative active stock`() {
        val zero = item { uid = "Z"; isActive = true; currentStock = BigDecimal.ZERO }
        val negative = item { uid = "N"; isActive = true; currentStock = BigDecimal("-5") }
        val positive = item { uid = "P"; isActive = true; currentStock = BigDecimal("10") }
        val inactiveZero = item { uid = "I"; isActive = false; currentStock = BigDecimal.ZERO }
        whenever(itemRepository.findAll()).thenReturn(mutableListOf(zero, negative, positive, inactiveZero))

        val result = service.getOutOfStockItems()

        assertEquals(setOf("Z", "N"), result.map { it.uid }.toSet())
    }

    @Test
    fun `countLowStockItems returns size of low stock list`() {
        whenever(itemRepository.findLowStockItems()).thenReturn(listOf(item(), item { uid = "INV-2" }))
        assertEquals(2L, service.countLowStockItems())
    }

    @Test
    fun `query delegations pass through`() {
        whenever(itemRepository.findBySku("SKU-1")).thenReturn(item())
        whenever(itemRepository.countByWarehouseId("WHR-1")).thenReturn(7L)
        whenever(itemRepository.countByIsActive(true)).thenReturn(4L)
        whenever(itemRepository.findOverstockItems()).thenReturn(listOf(item()))
        whenever(itemRepository.findByProductId("PRD-1")).thenReturn(listOf(item()))

        assertEquals("SKU-1", service.getInventoryItemBySku("SKU-1")?.sku)
        assertEquals(7L, service.countByWarehouse("WHR-1"))
        assertEquals(4L, service.countActiveItems())
        assertEquals(1, service.getOverstockItems().size)
        assertEquals(1, service.getInventoryByProduct("PRD-1").size)
    }
}
