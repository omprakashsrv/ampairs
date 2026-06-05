package com.ampairs.form.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.form.domain.repository.AttributeDefinitionRepository
import com.ampairs.form.domain.repository.FieldConfigRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the form module's sync checkpoint for the current workspace. The mobile `form`
 * SyncEntity covers the dynamic form configuration, so the checkpoint is the latest `updatedAt`
 * across AttributeDefinition and FieldConfig. Queries are `@TenantId`-filtered (workspace-scoped).
 */
@Component
class FormCheckpointContributor(
    private val attributeDefinitionRepository: AttributeDefinitionRepository,
    private val fieldConfigRepository: FieldConfigRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> {
        val maxForm = listOfNotNull(
            attributeDefinitionRepository.findMaxUpdatedAt(),
            fieldConfigRepository.findMaxUpdatedAt(),
        ).maxOrNull()
        return mapOf("form" to maxForm)
    }
}
