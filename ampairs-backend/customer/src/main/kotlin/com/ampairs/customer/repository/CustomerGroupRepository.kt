package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.CustomerGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

/**
 * Repository for managing workspace customer groups.
 * Uses @TenantId automatic filtering based on current workspace context.
 */
@Repository
interface CustomerGroupRepository : JpaRepository<CustomerGroup, String>, JpaSpecificationExecutor<CustomerGroup> {

    /**
     * Find customer group by unique code
     * Note: @TenantId automatically filters by current workspace
     */
    fun findByGroupCode(groupCode: String): CustomerGroup?

    /**
     * Find all active customer groups
     * Note: @TenantId automatically filters by current workspace
     */
    fun findByActiveTrue(): List<CustomerGroup>

    /**
     * Check if customer group code exists
     * Note: @TenantId automatically filters by current workspace
     */
    fun existsByGroupCode(groupCode: String): Boolean
}