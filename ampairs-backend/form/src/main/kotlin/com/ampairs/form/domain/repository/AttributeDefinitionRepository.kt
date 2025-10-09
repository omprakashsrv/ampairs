package com.ampairs.form.domain.repository

import com.ampairs.form.domain.model.AttributeDefinition
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

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
}
