package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.CustomerGroup
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
     * Find all active customer groups with pagination
     * Note: @TenantId automatically filters by current workspace
     */
    fun findByActiveTrue(pageable: Pageable): Page<CustomerGroup>

    /**
     * Check if customer group code exists
     * Note: @TenantId automatically filters by current workspace
     */
    fun existsByGroupCode(groupCode: String): Boolean

    /**
     * Find customer group by UID
     * Note: UID uniqueness is global (not workspace-specific)
     */
    fun findByUid(uid: String): CustomerGroup?

    /**
     * Check if UID exists
     * Note: UID uniqueness is global (not workspace-specific)
     */
    fun existsByUid(uid: String): Boolean
}