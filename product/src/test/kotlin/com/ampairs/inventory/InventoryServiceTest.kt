package com.ampairs.inventory

import com.ampairs.inventory.domain.dto.InventoryRequest
import com.ampairs.inventory.domain.model.Inventory
import com.ampairs.inventory.repository.InventoryRepository
import com.ampairs.inventory.service.InventoryService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryServiceTest {

    @Mock
    private lateinit var repository: InventoryRepository

    private lateinit var service: InventoryService

    private fun request(
        id: String? = null,
        refId: String? = null,
        productId: String? = null,
        stock: Double = 5.0
    ) = InventoryRequest(
        id = id,
        refId = refId,
        productId = productId,
        productRefId = null,
        description = "desc",
        code = null,
        taxCode = null,
        baseUnitId = "UNT-1",
        active = true,
        mrp = 100.0,
        dp = 90.0,
        stock = stock,
        sellingPrice = 80.0,
        buyingPrice = 70.0,
        lastUpdated = null,
        createdAt = null,
        updatedAt = null
    )

    @BeforeEach
    fun setUp() {
        service = InventoryService(repository)
        whenever(repository.save(any<Inventory>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `updateInventories creates new when no identifiers`() {
        val result = service.updateInventories(listOf(request(stock = 12.0)))

        assertEquals(1, result.size)
        assertEquals(12.0, result[0].stock)
        assertEquals(0L, result[0].id) // new entity
    }

    @Test
    fun `updateInventories merges by uid (id)`() {
        val existing = Inventory().apply { id = 42; uid = "INV-1"; refId = "REF-EXIST" }
        whenever(repository.findByUid("INV-1")).thenReturn(existing)

        val result = service.updateInventories(listOf(request(id = "INV-1")))

        assertEquals(42L, result[0].id)
        // refId falls back to existing when request has none
        assertEquals("REF-EXIST", result[0].refId)
    }

    @Test
    fun `updateInventories merges by refId`() {
        val existing = Inventory().apply { id = 7; uid = "INV-7" }
        whenever(repository.findByRefId("REF-7")).thenReturn(existing)

        val result = service.updateInventories(listOf(request(refId = "REF-7")))

        assertEquals(7L, result[0].id)
        assertEquals("INV-7", result[0].uid)
    }

    @Test
    fun `updateInventories merges by productId`() {
        val existing = Inventory().apply { id = 9; uid = "INV-9" }
        whenever(repository.findByProductId("PRD-9")).thenReturn(existing)

        val result = service.updateInventories(listOf(request(productId = "PRD-9")))

        assertEquals(9L, result[0].id)
        assertEquals("INV-9", result[0].uid)
    }

    @Test
    fun `updateInventories handles multiple requests`() {
        val result = service.updateInventories(listOf(request(stock = 1.0), request(stock = 2.0)))
        assertEquals(2, result.size)
    }
}
