package com.ampairs.form.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.form.domain.repository.FormSchemaRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the form module's sync checkpoint for the current workspace. The mobile `form`
 * SyncEntity covers the whole form configuration as an aggregate, so the checkpoint is the latest
 * `updatedAt` across the workspace's FormSchema aggregates (each aggregate's `updatedAt` already
 * reflects the max across its sections/fields). `@TenantId`-filtered (workspace-scoped).
 */
@Component
class FormCheckpointContributor(
    private val schemaRepository: FormSchemaRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> =
        mapOf("form" to schemaRepository.findMaxUpdatedAt())
}
