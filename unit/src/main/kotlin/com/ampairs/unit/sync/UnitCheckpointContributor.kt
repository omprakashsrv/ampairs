package com.ampairs.unit.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.unit.repository.UnitRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the unit module's sync checkpoint (max `updatedAt`) for the current workspace.
 * `@TenantId`-filtered, so automatically workspace-scoped.
 */
@Component
class UnitCheckpointContributor(
    private val unitRepository: UnitRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "unit" to unitRepository.findMaxUpdatedAt(),
    )
}
