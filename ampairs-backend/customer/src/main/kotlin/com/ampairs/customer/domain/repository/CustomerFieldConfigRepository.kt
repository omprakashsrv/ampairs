package com.ampairs.customer.domain.repository

import com.ampairs.customer.domain.model.CustomerFieldConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for CustomerFieldConfig entity with tenant-aware operations
 */
@Repository
interface CustomerFieldConfigRepository : JpaRepository<CustomerFieldConfig, Long> {

    /**
     * Find all enabled field configurations ordered by display order
     */
    @Query("SELECT cfc FROM customer_field_config cfc WHERE cfc.enabled = true ORDER BY cfc.displayOrder ASC")
    fun findByEnabledTrueOrderByDisplayOrderAsc(): List<CustomerFieldConfig>

    /**
     * Find all visible field configurations
     */
    @Query("SELECT cfc FROM customer_field_config cfc WHERE cfc.visible = true AND cfc.enabled = true ORDER BY cfc.displayOrder ASC")
    fun findByVisibleTrueAndEnabledTrueOrderByDisplayOrderAsc(): List<CustomerFieldConfig>

    /**
     * Find all mandatory field configurations
     */
    @Query("SELECT cfc FROM customer_field_config cfc WHERE cfc.mandatory = true AND cfc.enabled = true")
    fun findByMandatoryTrueAndEnabledTrue(): List<CustomerFieldConfig>

    /**
     * Find field configuration by field name
     */
    @Query("SELECT cfc FROM customer_field_config cfc WHERE cfc.fieldName = :fieldName")
    fun findByFieldName(@Param("fieldName") fieldName: String): CustomerFieldConfig?

    /**
     * Find field configuration by UID
     */
    @Query("SELECT cfc FROM customer_field_config cfc WHERE cfc.uid = :uid")
    fun findByUid(@Param("uid") uid: String): CustomerFieldConfig?

    /**
     * Check if field configuration exists for a field name
     */
    @Query("SELECT CASE WHEN COUNT(cfc) > 0 THEN true ELSE false END FROM customer_field_config cfc WHERE cfc.fieldName = :fieldName")
    fun existsByFieldName(@Param("fieldName") fieldName: String): Boolean

    /**
     * Find all configurations ordered by display order
     */
    @Query("SELECT cfc FROM customer_field_config cfc ORDER BY cfc.displayOrder ASC, cfc.fieldName ASC")
    fun findAllOrderByDisplayOrderAscFieldNameAsc(): List<CustomerFieldConfig>

    /**
     * Count enabled configurations
     */
    @Query("SELECT COUNT(cfc) FROM customer_field_config cfc WHERE cfc.enabled = true")
    fun countByEnabledTrue(): Long

    /**
     * Count visible configurations
     */
    @Query("SELECT COUNT(cfc) FROM customer_field_config cfc WHERE cfc.visible = true AND cfc.enabled = true")
    fun countByVisibleTrueAndEnabledTrue(): Long

    /**
     * Get next display order
     */
    @Query("SELECT COALESCE(MAX(cfc.displayOrder), -1) + 1 FROM customer_field_config cfc")
    fun getNextDisplayOrder(): Int
}
