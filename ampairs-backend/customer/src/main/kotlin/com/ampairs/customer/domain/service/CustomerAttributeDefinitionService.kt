package com.ampairs.customer.domain.service

import com.ampairs.customer.domain.dto.*
import com.ampairs.customer.domain.repository.CustomerAttributeDefinitionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing customer attribute definitions
 */
@Service
@Transactional(readOnly = true)
class CustomerAttributeDefinitionService(
    private val attributeDefinitionRepository: CustomerAttributeDefinitionRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Get all attribute definitions
     */
    fun getAllDefinitions(): List<CustomerAttributeDefinitionResponse> {
        return attributeDefinitionRepository.findAllOrderByDisplayOrderAscAttributeKeyAsc()
            .asCustomerAttributeDefinitionResponses()
    }

    /**
     * Get all enabled attribute definitions
     */
    fun getEnabledDefinitions(): List<CustomerAttributeDefinitionResponse> {
        return attributeDefinitionRepository.findByEnabledTrueOrderByDisplayOrderAsc()
            .asCustomerAttributeDefinitionResponses()
    }

    /**
     * Get all visible attribute definitions
     */
    fun getVisibleDefinitions(): List<CustomerAttributeDefinitionResponse> {
        return attributeDefinitionRepository.findByVisibleTrueAndEnabledTrueOrderByDisplayOrderAsc()
            .asCustomerAttributeDefinitionResponses()
    }

    /**
     * Get attribute definition by UID
     */
    fun getDefinitionByUid(uid: String): CustomerAttributeDefinitionResponse {
        val definition = attributeDefinitionRepository.findByUid(uid)
            ?: throw NoSuchElementException("Attribute definition not found with UID: $uid")
        return definition.asCustomerAttributeDefinitionResponse()
    }

    /**
     * Get attribute definition by attribute key
     */
    fun getDefinitionByAttributeKey(attributeKey: String): CustomerAttributeDefinitionResponse? {
        return attributeDefinitionRepository.findByAttributeKey(attributeKey)
            ?.asCustomerAttributeDefinitionResponse()
    }

    /**
     * Get definitions by category
     */
    fun getDefinitionsByCategory(category: String): List<CustomerAttributeDefinitionResponse> {
        return attributeDefinitionRepository.findByCategoryAndEnabledTrueOrderByDisplayOrderAsc(category)
            .asCustomerAttributeDefinitionResponses()
    }

    /**
     * Get definitions by data type
     */
    fun getDefinitionsByDataType(dataType: String): List<CustomerAttributeDefinitionResponse> {
        return attributeDefinitionRepository.findByDataTypeAndEnabledTrueOrderByDisplayOrderAsc(dataType)
            .asCustomerAttributeDefinitionResponses()
    }

    /**
     * Get all categories
     */
    fun getAllCategories(): List<String> {
        return attributeDefinitionRepository.findAllDistinctCategories()
    }

    /**
     * Get definitions grouped by category
     */
    fun getDefinitionsByCategories(): Map<String, List<CustomerAttributeDefinitionResponse>> {
        val definitions = attributeDefinitionRepository.findByEnabledTrueOrderByDisplayOrderAsc()
        return definitions.groupBy { it.category ?: "General" }
            .mapValues { (_, defs) -> defs.asCustomerAttributeDefinitionResponses() }
    }

    /**
     * Create new attribute definition
     */
    @Transactional
    fun createDefinition(request: CustomerAttributeDefinitionCreateRequest): CustomerAttributeDefinitionResponse {
        // Check if definition already exists for this attribute key
        if (attributeDefinitionRepository.existsByAttributeKey(request.attributeKey)) {
            throw IllegalArgumentException("Attribute definition already exists for key: ${request.attributeKey}")
        }

        // Validate enum values if data type is ENUM
        if (request.dataType == "ENUM" && request.enumValues.isNullOrEmpty()) {
            throw IllegalArgumentException("Enum values are required for ENUM data type")
        }

        val definition = request.toCustomerAttributeDefinition()

        // Set display order if not provided
        if (definition.displayOrder == 0) {
            definition.displayOrder = attributeDefinitionRepository.getNextDisplayOrder()
        }

        val saved = attributeDefinitionRepository.save(definition)
        logger.info("Created attribute definition for key: ${saved.attributeKey}")

        return saved.asCustomerAttributeDefinitionResponse()
    }

    /**
     * Update attribute definition
     */
    @Transactional
    fun updateDefinition(uid: String, request: CustomerAttributeDefinitionUpdateRequest): CustomerAttributeDefinitionResponse {
        val definition = attributeDefinitionRepository.findByUid(uid)
            ?: throw NoSuchElementException("Attribute definition not found with UID: $uid")

        // Validate enum values if data type is changing to ENUM
        if (request.dataType == "ENUM" && request.enumValues.isNullOrEmpty() && definition.enumValues.isNullOrEmpty()) {
            throw IllegalArgumentException("Enum values are required for ENUM data type")
        }

        // Apply updates
        request.displayName?.let { definition.displayName = it }
        request.dataType?.let { definition.dataType = it }
        request.visible?.let { definition.visible = it }
        request.mandatory?.let { definition.mandatory = it }
        request.enabled?.let { definition.enabled = it }
        request.displayOrder?.let { definition.displayOrder = it }
        request.category?.let { definition.category = it }
        request.defaultValue?.let { definition.defaultValue = it }
        request.validationType?.let { definition.validationType = it }
        request.validationParams?.let { definition.validationParams = it }
        request.enumValues?.let { definition.enumValues = it }
        request.placeholder?.let { definition.placeholder = it }
        request.helpText?.let { definition.helpText = it }

        val saved = attributeDefinitionRepository.save(definition)
        logger.info("Updated attribute definition for key: ${saved.attributeKey}")

        return saved.asCustomerAttributeDefinitionResponse()
    }

    /**
     * Delete attribute definition
     */
    @Transactional
    fun deleteDefinition(uid: String) {
        val definition = attributeDefinitionRepository.findByUid(uid)
            ?: throw NoSuchElementException("Attribute definition not found with UID: $uid")

        attributeDefinitionRepository.delete(definition)
        logger.info("Deleted attribute definition for key: ${definition.attributeKey}")
    }

    /**
     * Batch update attribute definitions
     */
    @Transactional
    fun batchUpdateDefinitions(request: CustomerAttributeDefinitionBatchUpdateRequest): List<CustomerAttributeDefinitionResponse> {
        val results = mutableListOf<CustomerAttributeDefinitionResponse>()

        for (defRequest in request.definitions) {
            val existing = attributeDefinitionRepository.findByAttributeKey(defRequest.attributeKey)

            if (existing != null) {
                // Update existing
                defRequest.displayName.let { existing.displayName = it }
                defRequest.dataType.let { existing.dataType = it }
                defRequest.visible.let { existing.visible = it }
                defRequest.mandatory.let { existing.mandatory = it }
                defRequest.enabled.let { existing.enabled = it }
                defRequest.displayOrder.let { existing.displayOrder = it }
                defRequest.category?.let { existing.category = it }
                defRequest.defaultValue?.let { existing.defaultValue = it }
                defRequest.validationType?.let { existing.validationType = it }
                defRequest.validationParams?.let { existing.validationParams = it }
                defRequest.enumValues?.let { existing.enumValues = it }
                defRequest.placeholder?.let { existing.placeholder = it }
                defRequest.helpText?.let { existing.helpText = it }

                results.add(attributeDefinitionRepository.save(existing).asCustomerAttributeDefinitionResponse())
            } else {
                // Create new
                val newDefinition = defRequest.toCustomerAttributeDefinition()
                results.add(attributeDefinitionRepository.save(newDefinition).asCustomerAttributeDefinitionResponse())
            }
        }

        logger.info("Batch updated ${results.size} attribute definitions")
        return results
    }

    /**
     * Get statistics
     */
    fun getStats(): AttributeDefinitionStats {
        return AttributeDefinitionStats(
            totalDefinitions = attributeDefinitionRepository.count(),
            enabledDefinitions = attributeDefinitionRepository.countByEnabledTrue(),
            visibleDefinitions = attributeDefinitionRepository.countByVisibleTrueAndEnabledTrue(),
            categories = attributeDefinitionRepository.findAllDistinctCategories().size,
            dataTypes = attributeDefinitionRepository.findAllDistinctDataTypes()
        )
    }
}

/**
 * Attribute definition statistics
 */
data class AttributeDefinitionStats(
    val totalDefinitions: Long,
    val enabledDefinitions: Long,
    val visibleDefinitions: Long,
    val categories: Int,
    val dataTypes: List<String>
)
