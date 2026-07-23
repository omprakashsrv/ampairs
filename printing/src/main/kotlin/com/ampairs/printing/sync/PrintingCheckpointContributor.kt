package com.ampairs.printing.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.printing.repository.PrintTemplateRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the printing module's sync checkpoint (max `updatedAt` of print templates) for the
 * current workspace. `@TenantId`-filtered, so automatically workspace-scoped.
 */
@Component
class PrintingCheckpointContributor(
    private val printTemplateRepository: PrintTemplateRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "print_template" to printTemplateRepository.findMaxUpdatedAt(),
    )
}
