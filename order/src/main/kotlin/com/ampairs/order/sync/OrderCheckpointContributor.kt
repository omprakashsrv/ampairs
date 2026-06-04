package com.ampairs.order.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.order.repository.OrderRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the order module's sync checkpoint (max `updatedAt`) for the current workspace.
 * `@TenantId`-filtered, so automatically workspace-scoped.
 */
@Component
class OrderCheckpointContributor(
    private val orderRepository: OrderRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "order" to orderRepository.findMaxUpdatedAt(),
    )
}
