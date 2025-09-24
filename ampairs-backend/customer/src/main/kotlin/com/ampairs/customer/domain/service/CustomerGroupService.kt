package com.ampairs.customer.domain.service

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.customer.domain.model.CustomerGroup
import com.ampairs.customer.repository.CustomerGroupRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing workspace customer groups.
 * Uses @TenantId automatic filtering based on current workspace context.
 */
@Service
@Transactional
class CustomerGroupService(
    private val customerGroupRepository: CustomerGroupRepository
) {

    private val logger = LoggerFactory.getLogger(CustomerGroupService::class.java)

    /**
     * Get all active customer groups for current workspace
     * Note: @TenantId automatically filters by current workspace
     */
    @Transactional(readOnly = true)
    fun getAllActiveCustomerGroups(): List<CustomerGroup> {
        return customerGroupRepository.findAll().filter { it.active }
            .sortedWith(compareBy<CustomerGroup> { it.displayOrder }.thenBy { it.name })
    }

    /**
     * Get customer groups ordered by priority level within current workspace
     */
    @Transactional(readOnly = true)
    fun getCustomerGroupsByPriority(): List<CustomerGroup> {
        return customerGroupRepository.findAll().filter { it.active }
            .sortedWith(compareByDescending<CustomerGroup> { it.priorityLevel }.thenBy { it.name })
    }

    /**
     * Find customer group by code within current workspace
     */
    @Transactional(readOnly = true)
    fun findByGroupCode(groupCode: String): CustomerGroup? {
        return customerGroupRepository.findAll().find {
            it.groupCode.equals(groupCode, ignoreCase = true) && it.active
        }
    }

    /**
     * Search customer groups by keyword within current workspace
     */
    @Transactional(readOnly = true)
    fun searchCustomerGroups(searchTerm: String): List<CustomerGroup> {
        return customerGroupRepository.findAll().filter { customerGroup ->
            customerGroup.active && (
                customerGroup.name.contains(searchTerm, ignoreCase = true) ||
                customerGroup.description?.contains(searchTerm, ignoreCase = true) == true
            )
        }.sortedWith(compareBy<CustomerGroup> { it.displayOrder }.thenBy { it.name })
    }

    /**
     * Get customer groups that have discount benefits within current workspace
     */
    @Transactional(readOnly = true)
    fun getCustomerGroupsWithDiscount(): List<CustomerGroup> {
        return customerGroupRepository.findAll().filter {
            it.active && it.defaultDiscountPercentage > 0
        }.sortedWith(compareBy<CustomerGroup> { it.displayOrder }.thenBy { it.name })
    }

    /**
     * Create new customer group in current workspace
     */
    fun createCustomerGroup(customerGroup: CustomerGroup): CustomerGroup {
        return customerGroupRepository.save(customerGroup)
    }

    /**
     * Update existing customer group
     */
    fun updateCustomerGroup(groupCode: String, updates: CustomerGroup): CustomerGroup? {
        val existingGroup = findByGroupCode(groupCode) ?: return null

        existingGroup.name = updates.name
        existingGroup.description = updates.description
        existingGroup.displayOrder = updates.displayOrder
        existingGroup.active = updates.active
        existingGroup.defaultDiscountPercentage = updates.defaultDiscountPercentage
        existingGroup.priorityLevel = updates.priorityLevel
        existingGroup.metadata = updates.metadata

        return customerGroupRepository.save(existingGroup)
    }

    /**
     * Get customer group statistics for current workspace
     */
    @Transactional(readOnly = true)
    fun getCustomerGroupStatistics(): Map<String, Any> {
        val allGroups = customerGroupRepository.findAll()
        val activeGroups = allGroups.filter { it.active }
        val discountEnabledGroups = activeGroups.filter { it.defaultDiscountPercentage > 0 }

        return mapOf(
            "total_count" to allGroups.size,
            "active_count" to activeGroups.size,
            "discount_enabled_count" to discountEnabledGroups.size
        )
    }

    /**
     * Check if customer group exists within current workspace
     */
    @Transactional(readOnly = true)
    fun existsByGroupCode(groupCode: String): Boolean {
        return findByGroupCode(groupCode) != null
    }
}