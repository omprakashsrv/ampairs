package com.ampairs.customer.domain.repository

import com.ampairs.customer.domain.model.CustomerAttributeDefinition
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for CustomerAttributeDefinition entity with tenant-aware operations
 */
@Repository
interface CustomerAttributeDefinitionRepository : JpaRepository<CustomerAttributeDefinition, Long> {

    /**
     * Find all enabled attribute definitions ordered by display order
     */
    @Query("SELECT cad FROM customer_attribute_definition cad WHERE cad.enabled = true ORDER BY cad.displayOrder ASC")
    fun findByEnabledTrueOrderByDisplayOrderAsc(): List<CustomerAttributeDefinition>

    /**
     * Find all visible attribute definitions
     */
    @Query("SELECT cad FROM customer_attribute_definition cad WHERE cad.visible = true AND cad.enabled = true ORDER BY cad.displayOrder ASC")
    fun findByVisibleTrueAndEnabledTrueOrderByDisplayOrderAsc(): List<CustomerAttributeDefinition>

    /**
     * Find all mandatory attribute definitions
     */
    @Query("SELECT cad FROM customer_attribute_definition cad WHERE cad.mandatory = true AND cad.enabled = true")
    fun findByMandatoryTrueAndEnabledTrue(): List<CustomerAttributeDefinition>

    /**
     * Find attribute definition by attribute key
     */
    @Query("SELECT cad FROM customer_attribute_definition cad WHERE cad.attributeKey = :attributeKey")
    fun findByAttributeKey(@Param("attributeKey") attributeKey: String): CustomerAttributeDefinition?

    /**
     * Find attribute definition by UID
     */
    @Query("SELECT cad FROM customer_attribute_definition cad WHERE cad.uid = :uid")
    fun findByUid(@Param("uid") uid: String): CustomerAttributeDefinition?

    /**
     * Check if attribute definition exists for an attribute key
     */
    @Query("SELECT CASE WHEN COUNT(cad) > 0 THEN true ELSE false END FROM customer_attribute_definition cad WHERE cad.attributeKey = :attributeKey")
    fun existsByAttributeKey(@Param("attributeKey") attributeKey: String): Boolean

    /**
     * Find all attribute definitions by category
     */
    @Query("SELECT cad FROM customer_attribute_definition cad WHERE cad.category = :category AND cad.enabled = true ORDER BY cad.displayOrder ASC")
    fun findByCategoryAndEnabledTrueOrderByDisplayOrderAsc(@Param("category") category: String): List<CustomerAttributeDefinition>

    /**
     * Find all attribute definitions by data type
     */
    @Query("SELECT cad FROM customer_attribute_definition cad WHERE cad.dataType = :dataType AND cad.enabled = true ORDER BY cad.displayOrder ASC")
    fun findByDataTypeAndEnabledTrueOrderByDisplayOrderAsc(@Param("dataType") dataType: String): List<CustomerAttributeDefinition>

    /**
     * Find all definitions ordered by display order
     */
    @Query("SELECT cad FROM customer_attribute_definition cad ORDER BY cad.displayOrder ASC, cad.attributeKey ASC")
    fun findAllOrderByDisplayOrderAscAttributeKeyAsc(): List<CustomerAttributeDefinition>

    /**
     * Count enabled definitions
     */
    @Query("SELECT COUNT(cad) FROM customer_attribute_definition cad WHERE cad.enabled = true")
    fun countByEnabledTrue(): Long

    /**
     * Count visible definitions
     */
    @Query("SELECT COUNT(cad) FROM customer_attribute_definition cad WHERE cad.visible = true AND cad.enabled = true")
    fun countByVisibleTrueAndEnabledTrue(): Long

    /**
     * Get next display order
     */
    @Query("SELECT COALESCE(MAX(cad.displayOrder), -1) + 1 FROM customer_attribute_definition cad")
    fun getNextDisplayOrder(): Int

    /**
     * Get all categories
     */
    @Query("SELECT DISTINCT cad.category FROM customer_attribute_definition cad WHERE cad.category IS NOT NULL AND cad.enabled = true ORDER BY cad.category ASC")
    fun findAllDistinctCategories(): List<String>

    /**
     * Get all data types in use
     */
    @Query("SELECT DISTINCT cad.dataType FROM customer_attribute_definition cad WHERE cad.enabled = true ORDER BY cad.dataType ASC")
    fun findAllDistinctDataTypes(): List<String>
}
