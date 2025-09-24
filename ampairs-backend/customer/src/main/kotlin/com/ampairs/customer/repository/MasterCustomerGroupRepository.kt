package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.MasterCustomerGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for managing master customer group registry.
 * Provides methods for querying and managing the central customer group catalog.
 */
@Repository
interface MasterCustomerGroupRepository : JpaRepository<MasterCustomerGroup, String>, JpaSpecificationExecutor<MasterCustomerGroup> {

    /**
     * Find customer group by unique code
     */
    fun findByGroupCode(groupCode: String): MasterCustomerGroup?

    /**
     * Find all active customer groups
     */
    fun findByActiveTrueOrderByDisplayOrderAscNameAsc(): List<MasterCustomerGroup>

    /**
     * Find all active customer groups by name
     */
    fun findByActiveTrueOrderByNameAsc(): List<MasterCustomerGroup>

    /**
     * Find all active customer groups by priority level
     */
    fun findByActiveTrueOrderByPriorityLevelDescNameAsc(): List<MasterCustomerGroup>

    /**
     * Search active customer groups by name or description
     */
    @Query("""
        SELECT mcg FROM MasterCustomerGroup mcg
        WHERE mcg.active = true
        AND (LOWER(mcg.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
             OR LOWER(mcg.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        ORDER BY mcg.displayOrder ASC, mcg.name ASC
    """)
    fun searchActiveCustomerGroups(@Param("searchTerm") searchTerm: String): List<MasterCustomerGroup>

    /**
     * Find customer groups that have discount benefits
     */
    fun findByActiveTrueAndDefaultDiscountPercentageGreaterThanOrderByDisplayOrderAscNameAsc(discountPercentage: Double = 0.0): List<MasterCustomerGroup>

    /**
     * Check if customer group code exists
     */
    fun existsByGroupCode(groupCode: String): Boolean
}