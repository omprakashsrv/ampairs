package com.ampairs.customer.domain.service

import com.ampairs.customer.domain.dto.*
import com.ampairs.customer.domain.model.CustomerFieldConfig
import com.ampairs.customer.domain.repository.CustomerFieldConfigRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing customer field configurations
 */
@Service
@Transactional(readOnly = true)
class CustomerFieldConfigService(
    private val fieldConfigRepository: CustomerFieldConfigRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Get all field configurations
     */
    fun getAllConfigs(): List<CustomerFieldConfigResponse> {
        return fieldConfigRepository.findAllOrderByDisplayOrderAscFieldNameAsc()
            .asCustomerFieldConfigResponses()
    }

    /**
     * Get all enabled field configurations
     */
    fun getEnabledConfigs(): List<CustomerFieldConfigResponse> {
        return fieldConfigRepository.findByEnabledTrueOrderByDisplayOrderAsc()
            .asCustomerFieldConfigResponses()
    }

    /**
     * Get all visible field configurations
     */
    fun getVisibleConfigs(): List<CustomerFieldConfigResponse> {
        return fieldConfigRepository.findByVisibleTrueAndEnabledTrueOrderByDisplayOrderAsc()
            .asCustomerFieldConfigResponses()
    }

    /**
     * Get field configuration by UID
     */
    fun getConfigByUid(uid: String): CustomerFieldConfigResponse {
        val config = fieldConfigRepository.findByUid(uid)
            ?: throw NoSuchElementException("Field configuration not found with UID: $uid")
        return config.asCustomerFieldConfigResponse()
    }

    /**
     * Get field configuration by field name
     */
    fun getConfigByFieldName(fieldName: String): CustomerFieldConfigResponse? {
        return fieldConfigRepository.findByFieldName(fieldName)?.asCustomerFieldConfigResponse()
    }

    /**
     * Create new field configuration
     */
    @Transactional
    fun createConfig(request: CustomerFieldConfigCreateRequest): CustomerFieldConfigResponse {
        // Check if configuration already exists for this field
        if (fieldConfigRepository.existsByFieldName(request.fieldName)) {
            throw IllegalArgumentException("Field configuration already exists for field: ${request.fieldName}")
        }

        val config = request.toCustomerFieldConfig()

        // Set display order if not provided
        if (config.displayOrder == 0) {
            config.displayOrder = fieldConfigRepository.getNextDisplayOrder()
        }

        val saved = fieldConfigRepository.save(config)
        logger.info("Created field configuration for field: ${saved.fieldName}")

        return saved.asCustomerFieldConfigResponse()
    }

    /**
     * Update field configuration
     */
    @Transactional
    fun updateConfig(uid: String, request: CustomerFieldConfigUpdateRequest): CustomerFieldConfigResponse {
        val config = fieldConfigRepository.findByUid(uid)
            ?: throw NoSuchElementException("Field configuration not found with UID: $uid")

        // Apply updates
        request.displayName?.let { config.displayName = it }
        request.visible?.let { config.visible = it }
        request.mandatory?.let { config.mandatory = it }
        request.enabled?.let { config.enabled = it }
        request.displayOrder?.let { config.displayOrder = it }
        request.validationType?.let { config.validationType = it }
        request.validationParams?.let { config.validationParams = it }
        request.placeholder?.let { config.placeholder = it }
        request.helpText?.let { config.helpText = it }
        request.defaultValue?.let { config.defaultValue = it }

        val saved = fieldConfigRepository.save(config)
        logger.info("Updated field configuration for field: ${saved.fieldName}")

        return saved.asCustomerFieldConfigResponse()
    }

    /**
     * Delete field configuration
     */
    @Transactional
    fun deleteConfig(uid: String) {
        val config = fieldConfigRepository.findByUid(uid)
            ?: throw NoSuchElementException("Field configuration not found with UID: $uid")

        fieldConfigRepository.delete(config)
        logger.info("Deleted field configuration for field: ${config.fieldName}")
    }

    /**
     * Batch update field configurations
     */
    @Transactional
    fun batchUpdateConfigs(request: CustomerFieldConfigBatchUpdateRequest): List<CustomerFieldConfigResponse> {
        val results = mutableListOf<CustomerFieldConfigResponse>()

        for (configRequest in request.configs) {
            val existing = fieldConfigRepository.findByFieldName(configRequest.fieldName)

            if (existing != null) {
                // Update existing
                configRequest.displayName.let { existing.displayName = it }
                configRequest.visible.let { existing.visible = it }
                configRequest.mandatory.let { existing.mandatory = it }
                configRequest.enabled.let { existing.enabled = it }
                configRequest.displayOrder.let { existing.displayOrder = it }
                configRequest.validationType?.let { existing.validationType = it }
                configRequest.validationParams?.let { existing.validationParams = it }
                configRequest.placeholder?.let { existing.placeholder = it }
                configRequest.helpText?.let { existing.helpText = it }
                configRequest.defaultValue?.let { existing.defaultValue = it }

                results.add(fieldConfigRepository.save(existing).asCustomerFieldConfigResponse())
            } else {
                // Create new
                val newConfig = configRequest.toCustomerFieldConfig()
                results.add(fieldConfigRepository.save(newConfig).asCustomerFieldConfigResponse())
            }
        }

        logger.info("Batch updated ${results.size} field configurations")
        return results
    }

    /**
     * Get statistics
     */
    fun getStats(): FieldConfigStats {
        return FieldConfigStats(
            totalConfigs = fieldConfigRepository.count(),
            enabledConfigs = fieldConfigRepository.countByEnabledTrue(),
            visibleConfigs = fieldConfigRepository.countByVisibleTrueAndEnabledTrue()
        )
    }
}

/**
 * Field configuration statistics
 */
data class FieldConfigStats(
    val totalConfigs: Long,
    val enabledConfigs: Long,
    val visibleConfigs: Long
)
