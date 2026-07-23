package com.ampairs.inventory.sync

import com.ampairs.inventory.repository.InventoryItemRepository
import com.ampairs.inventory.repository.InventoryTransactionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.time.Instant

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryCheckpointContributorTest {

    @Mock private lateinit var inventoryItemRepository: InventoryItemRepository
    @Mock private lateinit var inventoryTransactionRepository: InventoryTransactionRepository

    @Test
    fun `checkpoints reports both inventory entity codes`() {
        val itemAt = Instant.parse("2026-06-01T10:00:00Z")
        whenever(inventoryItemRepository.findMaxUpdatedAt()).thenReturn(itemAt)
        whenever(inventoryTransactionRepository.findMaxUpdatedAt()).thenReturn(null)

        val checkpoints = InventoryCheckpointContributor(
            inventoryItemRepository, inventoryTransactionRepository,
        ).checkpoints()

        assertEquals(setOf("inventory", "inventory_transaction"), checkpoints.keys)
        assertEquals(itemAt, checkpoints["inventory"])
        assertNull(checkpoints["inventory_transaction"])
    }
}
