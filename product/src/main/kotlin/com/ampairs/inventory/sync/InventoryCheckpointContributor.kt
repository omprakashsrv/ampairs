package com.ampairs.inventory.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.inventory.repository.InventoryItemRepository
import com.ampairs.inventory.repository.InventoryTransactionRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the inventory bounded context's sync checkpoints (max `updatedAt` per entity) for
 * the current workspace. Without this, the mobile client's connect/reconnect/hourly bootstrap
 * never learns the server has inventory data and never pulls it. Queries are `@TenantId`-filtered,
 * so automatically workspace-scoped.
 */
@Component
class InventoryCheckpointContributor(
    private val inventoryItemRepository: InventoryItemRepository,
    private val inventoryTransactionRepository: InventoryTransactionRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "inventory" to inventoryItemRepository.findMaxUpdatedAt(),
        "inventory_transaction" to inventoryTransactionRepository.findMaxUpdatedAt(),
    )
}
