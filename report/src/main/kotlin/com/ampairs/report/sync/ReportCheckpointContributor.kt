package com.ampairs.report.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.report.repository.ExportTemplateRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the report module's sync checkpoint (max `updatedAt`) for the current workspace.
 * `@TenantId`-filtered, so automatically workspace-scoped.
 */
@Component
class ReportCheckpointContributor(
    private val exportTemplateRepository: ExportTemplateRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "export_template" to exportTemplateRepository.findMaxUpdatedAt(),
    )
}
