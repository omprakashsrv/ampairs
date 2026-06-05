package com.ampairs.business.sync

import com.ampairs.business.repository.BusinessRepository
import com.ampairs.core.sync.SyncCheckpointContributor
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the business module's sync checkpoint (max `updatedAt`) for the current workspace.
 * `@TenantId`-filtered, so automatically workspace-scoped.
 */
@Component
class BusinessCheckpointContributor(
    private val businessRepository: BusinessRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "business" to businessRepository.findMaxUpdatedAt(),
    )
}
