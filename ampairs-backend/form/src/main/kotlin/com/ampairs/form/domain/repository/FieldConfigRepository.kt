package com.ampairs.form.domain.repository

import com.ampairs.form.domain.model.FieldConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

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
}
