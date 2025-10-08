package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.CustomerType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

/**
 * Repository for managing workspace customer types.
 * Uses @TenantId automatic filtering based on current workspace context.
 */
@Repository
interface CustomerTypeRepository : JpaRepository<CustomerType, String>, JpaSpecificationExecutor<CustomerType> {

    /**
     * Find customer type by unique code
     * Note: @TenantId automatically filters by current workspace
     */
    fun findByTypeCode(typeCode: String): CustomerType?

    /**
     * Find all active customer types
     * Note: @TenantId automatically filters by current workspace
     */
    fun findByActiveTrue(): List<CustomerType>

    /**
     * Find all active customer types with pagination
     * Note: @TenantId automatically filters by current workspace
     */
    fun findByActiveTrue(pageable: Pageable): Page<CustomerType>

    /**
     * Check if customer type code exists
     * Note: @TenantId automatically filters by current workspace
     */
    fun existsByTypeCode(typeCode: String): Boolean
}