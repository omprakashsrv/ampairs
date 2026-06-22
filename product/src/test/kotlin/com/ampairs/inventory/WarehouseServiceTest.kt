package com.ampairs.inventory

import com.ampairs.inventory.domain.model.Warehouse
import com.ampairs.inventory.repository.WarehouseRepository
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

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WarehouseServiceTest {

    @Mock
    private lateinit var warehouseRepository: WarehouseRepository

    private lateinit var service: WarehouseService

    private fun warehouse(block: Warehouse.() -> Unit = {}): Warehouse =
        Warehouse().apply {
            uid = "WHR-1"
            name = "Main Warehouse"
            code = "WH-001"
            isActive = true
            isDefault = false
            block()
        }

    @BeforeEach
    fun setUp() {
        service = WarehouseService(warehouseRepository)
        whenever(warehouseRepository.save(any<Warehouse>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `createWarehouse persists when code is unique`() {
        val wh = warehouse()
        whenever(warehouseRepository.existsByCode("WH-001")).thenReturn(false)

        val result = service.createWarehouse(wh)

        assertSame(wh, result)
        verify(warehouseRepository).save(wh)
    }

    @Test
    fun `createWarehouse rejects duplicate code`() {
        whenever(warehouseRepository.existsByCode("WH-001")).thenReturn(true)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.createWarehouse(warehouse())
        }
        assertTrue(ex.message!!.contains("WH-001"))
        verify(warehouseRepository, never()).save(any<Warehouse>())
    }

    @Test
    fun `createWarehouse marked default unsets previous default`() {
        val existingDefault = warehouse { uid = "WHR-OLD"; code = "WH-OLD"; isDefault = true }
        whenever(warehouseRepository.existsByCode("WH-NEW")).thenReturn(false)
        whenever(warehouseRepository.findByIsDefaultTrue()).thenReturn(existingDefault)

        service.createWarehouse(warehouse { uid = "WHR-NEW"; code = "WH-NEW"; isDefault = true })

        assertFalse(existingDefault.isDefault)
        verify(warehouseRepository).save(existingDefault)
    }

    @Test
    fun `updateWarehouse updates fields on existing`() {
        val existing = warehouse()
        whenever(warehouseRepository.findByUid("WHR-1")).thenReturn(existing)
        val updates = warehouse {
            name = "Renamed"
            code = "WH-001"
            warehouseType = "STORE"
            isActive = false
            phone = "999"
            email = "a@b.com"
            managerName = "Bob"
            description = "desc"
        }

        val result = service.updateWarehouse("WHR-1", updates)

        assertEquals("Renamed", result.name)
        assertEquals("STORE", result.warehouseType)
        assertFalse(result.isActive)
        assertEquals("999", result.phone)
        assertEquals("Bob", result.managerName)
    }

    @Test
    fun `updateWarehouse not found throws`() {
        whenever(warehouseRepository.findByUid("MISSING")).thenReturn(null)

        assertThrows(IllegalArgumentException::class.java) {
            service.updateWarehouse("MISSING", warehouse())
        }
    }

    @Test
    fun `updateWarehouse rejects code change to an existing code`() {
        val existing = warehouse { code = "OLD" }
        whenever(warehouseRepository.findByUid("WHR-1")).thenReturn(existing)
        whenever(warehouseRepository.existsByCode("NEW")).thenReturn(true)

        assertThrows(IllegalArgumentException::class.java) {
            service.updateWarehouse("WHR-1", warehouse { code = "NEW" })
        }
    }

    @Test
    fun `updateWarehouse setting default unsets the previous default`() {
        val existing = warehouse { isDefault = false }
        val previousDefault = warehouse { uid = "WHR-OLD"; code = "WH-OLD"; isDefault = true }
        whenever(warehouseRepository.findByUid("WHR-1")).thenReturn(existing)
        whenever(warehouseRepository.findByIsDefaultTrue()).thenReturn(previousDefault)

        service.updateWarehouse("WHR-1", warehouse { isDefault = true })

        assertFalse(previousDefault.isDefault)
        assertTrue(existing.isDefault)
    }

    @Test
    fun `setDefaultWarehouse marks target as default`() {
        val target = warehouse { isDefault = false }
        whenever(warehouseRepository.findByUid("WHR-1")).thenReturn(target)
        whenever(warehouseRepository.findByIsDefaultTrue()).thenReturn(null)

        val result = service.setDefaultWarehouse("WHR-1")

        assertTrue(result.isDefault)
    }

    @Test
    fun `setDefaultWarehouse not found throws`() {
        whenever(warehouseRepository.findByUid("X")).thenReturn(null)
        assertThrows(IllegalArgumentException::class.java) { service.setDefaultWarehouse("X") }
    }

    @Test
    fun `deleteWarehouse performs soft delete and clears default`() {
        val wh = warehouse { isActive = true; isDefault = true }
        whenever(warehouseRepository.findByUid("WHR-1")).thenReturn(wh)

        service.deleteWarehouse("WHR-1")

        assertFalse(wh.isActive)
        assertFalse(wh.isDefault)
        verify(warehouseRepository).save(wh)
    }

    @Test
    fun `deleteWarehouse not found throws`() {
        whenever(warehouseRepository.findByUid("X")).thenReturn(null)
        assertThrows(IllegalArgumentException::class.java) { service.deleteWarehouse("X") }
    }

    @Test
    fun `getters delegate to repository`() {
        val wh = warehouse()
        whenever(warehouseRepository.findByUid("WHR-1")).thenReturn(wh)
        whenever(warehouseRepository.findByCode("WH-001")).thenReturn(wh)
        whenever(warehouseRepository.findByIsActiveTrue()).thenReturn(listOf(wh))
        whenever(warehouseRepository.findByIsDefaultTrue()).thenReturn(wh)
        whenever(warehouseRepository.findAll()).thenReturn(mutableListOf(wh))
        whenever(warehouseRepository.count()).thenReturn(3L)

        assertSame(wh, service.getWarehouseByUid("WHR-1"))
        assertSame(wh, service.getWarehouseByCode("WH-001"))
        assertEquals(1, service.getActiveWarehouses().size)
        assertSame(wh, service.getDefaultWarehouse())
        assertEquals(1, service.getAllWarehouses().size)
        assertEquals(3L, service.countWarehouses())
    }

    @Test
    fun `getWarehouseByUid returns null when absent`() {
        whenever(warehouseRepository.findByUid("none")).thenReturn(null)
        assertNull(service.getWarehouseByUid("none"))
    }
}
