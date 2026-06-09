package com.ampairs.form.domain.repository

import com.ampairs.form.domain.model.AttributeDefinition
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Repository for AttributeDefinition entities
 * Automatically filtered by @TenantId (workspace_id)
 */
@Repository
interface AttributeDefinitionRepository : JpaRepository<AttributeDefinition, String> {

    /**
     * Find all attribute definitions for a specific entity type
     * @param entityType: "customer", "product", "inventory", etc.
     */
    fun findByEntityTypeOrderByDisplayOrderAsc(entityType: String): List<AttributeDefinition>

    /**
     * Find attribute definition by entity type and attribute key
     */
    fun findByEntityTypeAndAttributeKey(
        entityType: String,
        attributeKey: String
    ): AttributeDefinition?

    /**
     * Find all visible and enabled attribute definitions for an entity type
     */
    fun findByEntityTypeAndVisibleTrueAndEnabledTrueOrderByDisplayOrderAsc(
        entityType: String
    ): List<AttributeDefinition>

    /**
     * Find all mandatory attribute definitions for an entity type
     */
    fun findByEntityTypeAndMandatoryTrueAndEnabledTrue(
        entityType: String
    ): List<AttributeDefinition>

    /**
     * Find attribute definitions by entity type and category
     */
    fun findByEntityTypeAndCategoryOrderByDisplayOrderAsc(
        entityType: String,
        category: String
    ): List<AttributeDefinition>

    /**
     * Delete all attribute definitions for an entity type
     */
    fun deleteByEntityType(entityType: String)

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(a.updatedAt) FROM AttributeDefinition a")
    fun findMaxUpdatedAt(): Instant?

    /**
     * Incremental sync feed (pull): attribute definitions updated at/after [lastSync], paginated.
     * @TenantId-filtered (workspace-scoped). NOTE: AttributeDefinition has no soft-delete column, so
     * this feed never carries DELETED rows — physical deletions do not propagate to clients.
     */
    @Query("SELECT a FROM AttributeDefinition a WHERE a.updatedAt >= :lastSync")
    fun findAttributeDefinitionsUpdatedAfter(lastSync: Instant, pageable: Pageable): Page<AttributeDefinition>

    fun findByUid(uid: String): AttributeDefinition?
}
