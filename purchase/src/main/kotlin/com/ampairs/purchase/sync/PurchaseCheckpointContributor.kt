package com.ampairs.purchase.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.purchase.repository.PurchaseRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the purchase module's sync checkpoint (max `updatedAt`) for the current workspace.
 * The query is `@TenantId`-filtered, so it is automatically workspace-scoped.
 */
@Component
class PurchaseCheckpointContributor(
    private val purchaseRepository: PurchaseRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "purchase" to purchaseRepository.findMaxUpdatedAt(),
    )
}
