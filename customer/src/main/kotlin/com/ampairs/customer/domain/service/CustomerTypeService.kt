package com.ampairs.customer.domain.service

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.customer.domain.model.CustomerType
import com.ampairs.customer.repository.CustomerTypeRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing workspace customer types.
 * Uses @TenantId automatic filtering based on current workspace context.
 */
@Service
@Transactional
class CustomerTypeService(
    private val customerTypeRepository: CustomerTypeRepository
) {

    private val logger = LoggerFactory.getLogger(CustomerTypeService::class.java)

    /**
     * Get all active customer types for current workspace
     * Note: @TenantId automatically filters by current workspace
     */
    @Transactional(readOnly = true)
    fun getAllActiveCustomerTypes(): List<CustomerType> {
        return customerTypeRepository.findAll().filter { it.active }
            .sortedWith(compareBy<CustomerType> { it.displayOrder }.thenBy { it.name })
    }

    /**
     * Get all active customer types for current workspace with pagination
     * Note: @TenantId automatically filters by current workspace
     */
    @Transactional(readOnly = true)
    fun getAllActiveCustomerTypes(pageable: Pageable): Page<CustomerType> {
        return customerTypeRepository.findByActiveTrue(pageable)
    }

    /**
     * Find customer type by code within current workspace
     */
    @Transactional(readOnly = true)
    fun findByTypeCode(typeCode: String): CustomerType? {
        return customerTypeRepository.findAll().find {
            it.typeCode.equals(typeCode, ignoreCase = true) && it.active
        }
    }

    /**
     * Search customer types by keyword within current workspace
     */
    @Transactional(readOnly = true)
    fun searchCustomerTypes(searchTerm: String): List<CustomerType> {
        return customerTypeRepository.findAll().filter { customerType ->
            customerType.active && (
                customerType.name.contains(searchTerm, ignoreCase = true) ||
                customerType.description?.contains(searchTerm, ignoreCase = true) == true
            )
        }.sortedWith(compareBy<CustomerType> { it.displayOrder }.thenBy { it.name })
    }

    /**
     * Get customer types that allow credit within current workspace
     */
    @Transactional(readOnly = true)
    fun getCustomerTypesWithCredit(): List<CustomerType> {
        return customerTypeRepository.findAll().filter {
            it.active && it.defaultCreditLimit > 0
        }.sortedWith(compareBy<CustomerType> { it.displayOrder }.thenBy { it.name })
    }

    /**
     * Create new customer type in current workspace
     *
     * @throws IllegalArgumentException if UID already exists or typeCode is duplicate
     */
    fun createCustomerType(customerType: CustomerType): CustomerType {
        // Validate UID uniqueness if client provided one
        if (customerType.uid.isNotEmpty() && customerTypeRepository.existsByUid(customerType.uid)) {
            logger.error("Attempted to create customer type with duplicate UID: ${customerType.uid}")
            throw IllegalArgumentException("Customer type with UID '${customerType.uid}' already exists")
        }

        // Validate typeCode uniqueness within workspace
        if (customerTypeRepository.existsByTypeCode(customerType.typeCode)) {
            logger.error("Attempted to create customer type with duplicate code: ${customerType.typeCode}")
            throw IllegalArgumentException("Customer type with code '${customerType.typeCode}' already exists in this workspace")
        }

        return customerTypeRepository.save(customerType)
    }

    /**
     * Update existing customer type
     */
    fun updateCustomerType(typeCode: String, updates: CustomerType): CustomerType? {
        val existingType = findByTypeCode(typeCode) ?: return null

        existingType.name = updates.name
        existingType.description = updates.description
        existingType.displayOrder = updates.displayOrder
        existingType.active = updates.active
        existingType.defaultCreditLimit = updates.defaultCreditLimit
        existingType.defaultCreditDays = updates.defaultCreditDays
        existingType.metadata = updates.metadata

        return customerTypeRepository.save(existingType)
    }

    /**
     * Get customer type statistics for current workspace
     */
    @Transactional(readOnly = true)
    fun getCustomerTypeStatistics(): Map<String, Any> {
        val allTypes = customerTypeRepository.findAll()
        val activeTypes = allTypes.filter { it.active }
        val creditEnabledTypes = activeTypes.filter { it.defaultCreditLimit > 0 }

        return mapOf(
            "total_count" to allTypes.size,
            "active_count" to activeTypes.size,
            "credit_enabled_count" to creditEnabledTypes.size
        )
    }

    /**
     * Check if customer type exists within current workspace
     */
    @Transactional(readOnly = true)
    fun existsByTypeCode(typeCode: String): Boolean {
        return findByTypeCode(typeCode) != null
    }
}