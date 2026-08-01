package com.ampairs.sequence.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.sequence.repository.SequenceDefinitionRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the sequence module's sync checkpoint (max `updatedAt` of definitions) for the
 * current workspace. The mobile pull is definitions-driven (allocations are pushed via consumption
 * reports, not pulled), so the definition table is the right checkpoint source. `@TenantId`-filtered,
 * so automatically workspace-scoped.
 */
@Component
class SequenceCheckpointContributor(
    private val sequenceDefinitionRepository: SequenceDefinitionRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "sequence" to sequenceDefinitionRepository.findMaxUpdatedAt(),
    )
}
