package com.ampairs.form.domain.repository

import com.ampairs.form.domain.model.FieldConfig
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Repository for FieldConfig entities
 * Automatically filtered by @TenantId (workspace_id)
 */
@Repository
interface FieldConfigRepository : JpaRepository<FieldConfig, String> {

    /**
     * Find all field configs for a specific entity type
     * @param entityType: "customer", "product", "inventory", etc.
     */
    fun findByEntityTypeOrderByDisplayOrderAsc(entityType: String): List<FieldConfig>

    /**
     * Find field config by entity type and field name
     */
    fun findByEntityTypeAndFieldName(entityType: String, fieldName: String): FieldConfig?

    /**
     * Find all visible and enabled field configs for an entity type
     */
    fun findByEntityTypeAndVisibleTrueAndEnabledTrueOrderByDisplayOrderAsc(
        entityType: String
    ): List<FieldConfig>

    /**
     * Find all mandatory field configs for an entity type
     */
    fun findByEntityTypeAndMandatoryTrueAndEnabledTrue(
        entityType: String
    ): List<FieldConfig>

    /**
     * Delete all field configs for an entity type
     */
    fun deleteByEntityType(entityType: String)

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(f.updatedAt) FROM FieldConfig f")
    fun findMaxUpdatedAt(): Instant?

    /**
     * Incremental sync feed (pull): field configs updated at/after [lastSync], paginated.
     * @TenantId-filtered (workspace-scoped). NOTE: FieldConfig has no soft-delete column, so this
     * feed never carries DELETED rows — physical deletions do not propagate to clients.
     */
    @Query("SELECT f FROM FieldConfig f WHERE f.updatedAt >= :lastSync")
    fun findFieldConfigsUpdatedAfter(lastSync: Instant, pageable: Pageable): Page<FieldConfig>

    fun findByUid(uid: String): FieldConfig?
}
