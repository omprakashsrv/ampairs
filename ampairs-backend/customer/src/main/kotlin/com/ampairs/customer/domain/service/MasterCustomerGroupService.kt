package com.ampairs.customer.domain.service

import com.ampairs.customer.domain.model.MasterCustomerGroup
import com.ampairs.customer.repository.MasterCustomerGroupRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing master customer groups
 */
@Service
@Transactional
class MasterCustomerGroupService(
    private val masterCustomerGroupRepository: MasterCustomerGroupRepository
) {

    private val logger = LoggerFactory.getLogger(MasterCustomerGroupService::class.java)

    /**
     * Get all active master customer groups
     */
    @Transactional(readOnly = true)
    fun getAllActiveCustomerGroups(): List<MasterCustomerGroup> {
        return masterCustomerGroupRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc()
    }

    /**
     * Get customer groups ordered by priority level
     */
    @Transactional(readOnly = true)
    fun getCustomerGroupsByPriority(): List<MasterCustomerGroup> {
        return masterCustomerGroupRepository.findByActiveTrueOrderByPriorityLevelDescNameAsc()
    }

    /**
     * Find customer group by code
     */
    @Transactional(readOnly = true)
    fun findByGroupCode(groupCode: String): MasterCustomerGroup? {
        return masterCustomerGroupRepository.findByGroupCode(groupCode)
    }

    /**
     * Search customer groups by keyword
     */
    @Transactional(readOnly = true)
    fun searchCustomerGroups(searchTerm: String): List<MasterCustomerGroup> {
        return masterCustomerGroupRepository.searchActiveCustomerGroups(searchTerm)
    }

    /**
     * Get customer groups that have discount benefits
     */
    @Transactional(readOnly = true)
    fun getCustomerGroupsWithDiscount(): List<MasterCustomerGroup> {
        return masterCustomerGroupRepository.findByActiveTrueAndDefaultDiscountPercentageGreaterThanOrderByDisplayOrderAscNameAsc()
    }

    /**
     * Get master customer group statistics
     */
    @Transactional(readOnly = true)
    fun getMasterCustomerGroupStatistics(): Map<String, Any> {
        val totalCount = masterCustomerGroupRepository.count()
        val activeCount = masterCustomerGroupRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().size
        val discountEnabledCount = masterCustomerGroupRepository.findByActiveTrueAndDefaultDiscountPercentageGreaterThanOrderByDisplayOrderAscNameAsc().size

        return mapOf(
            "total_count" to totalCount,
            "active_count" to activeCount,
            "inactive_count" to (totalCount - activeCount),
            "discount_enabled_count" to discountEnabledCount
        )
    }

    /**
     * Check if customer group exists
     */
    @Transactional(readOnly = true)
    fun existsByGroupCode(groupCode: String): Boolean {
        return masterCustomerGroupRepository.existsByGroupCode(groupCode)
    }
}