package com.ampairs.customer.domain.service

import com.ampairs.customer.domain.model.MasterCustomerType
import com.ampairs.customer.repository.MasterCustomerTypeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing master customer types
 */
@Service
@Transactional
class MasterCustomerTypeService(
    private val masterCustomerTypeRepository: MasterCustomerTypeRepository
) {

    private val logger = LoggerFactory.getLogger(MasterCustomerTypeService::class.java)

    /**
     * Get all active master customer types
     */
    @Transactional(readOnly = true)
    fun getAllActiveCustomerTypes(): List<MasterCustomerType> {
        return masterCustomerTypeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc()
    }

    /**
     * Find customer type by code
     */
    @Transactional(readOnly = true)
    fun findByTypeCode(typeCode: String): MasterCustomerType? {
        return masterCustomerTypeRepository.findByTypeCode(typeCode)
    }

    /**
     * Search customer types by keyword
     */
    @Transactional(readOnly = true)
    fun searchCustomerTypes(searchTerm: String): List<MasterCustomerType> {
        return masterCustomerTypeRepository.searchActiveCustomerTypes(searchTerm)
    }

    /**
     * Get customer types that allow credit
     */
    @Transactional(readOnly = true)
    fun getCustomerTypesWithCredit(): List<MasterCustomerType> {
        return masterCustomerTypeRepository.findByActiveTrueAndDefaultCreditLimitGreaterThanOrderByDisplayOrderAscNameAsc()
    }

    /**
     * Get master customer type statistics
     */
    @Transactional(readOnly = true)
    fun getMasterCustomerTypeStatistics(): Map<String, Any> {
        val totalCount = masterCustomerTypeRepository.count()
        val activeCount = masterCustomerTypeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().size
        val creditEnabledCount = masterCustomerTypeRepository.findByActiveTrueAndDefaultCreditLimitGreaterThanOrderByDisplayOrderAscNameAsc().size

        return mapOf(
            "total_count" to totalCount,
            "active_count" to activeCount,
            "inactive_count" to (totalCount - activeCount),
            "credit_enabled_count" to creditEnabledCount
        )
    }

    /**
     * Check if customer type exists
     */
    @Transactional(readOnly = true)
    fun existsByTypeCode(typeCode: String): Boolean {
        return masterCustomerTypeRepository.existsByTypeCode(typeCode)
    }
}