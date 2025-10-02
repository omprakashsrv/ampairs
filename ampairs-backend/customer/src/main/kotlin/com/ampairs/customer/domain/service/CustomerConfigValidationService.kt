package com.ampairs.customer.domain.service

import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.CustomerAttributeDefinition
import com.ampairs.customer.domain.model.CustomerFieldConfig
import com.ampairs.customer.domain.model.ValidationResult
import com.ampairs.customer.domain.repository.CustomerAttributeDefinitionRepository
import com.ampairs.customer.domain.repository.CustomerFieldConfigRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * Service to validate customer data against field configurations and attribute definitions
 */
@Service
@Transactional(readOnly = true)
class CustomerConfigValidationService(
    private val fieldConfigRepository: CustomerFieldConfigRepository,
    private val attributeDefinitionRepository: CustomerAttributeDefinitionRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // Map of Customer entity field names to property accessors
    private val customerFieldMap: Map<String, KProperty1<Customer, *>> by lazy {
        Customer::class.memberProperties.associateBy { it.name }
    }

    /**
     * Validate customer against all field configurations and attribute definitions
     */
    fun validateCustomer(customer: Customer): CustomerValidationResult {
        val errors = mutableListOf<String>()

        // Validate standard fields
        val fieldErrors = validateFields(customer)
        errors.addAll(fieldErrors)

        // Validate custom attributes
        val attributeErrors = validateAttributes(customer.attributes ?: emptyMap())
        errors.addAll(attributeErrors)

        return CustomerValidationResult(
            valid = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Validate customer fields against field configurations
     */
    fun validateFields(customer: Customer): List<String> {
        val errors = mutableListOf<String>()

        // Get all enabled field configurations
        val fieldConfigs = fieldConfigRepository.findByEnabledTrueOrderByDisplayOrderAsc()

        for (config in fieldConfigs) {
            val fieldValue = getFieldValue(customer, config.fieldName)
            val result = config.validateValue(fieldValue)

            if (!result.valid) {
                result.message?.let { errors.add(it) }
            }
        }

        return errors
    }

    /**
     * Validate custom attributes against attribute definitions
     */
    fun validateAttributes(attributes: Map<String, Any>): List<String> {
        val errors = mutableListOf<String>()

        // Get all enabled attribute definitions
        val attributeDefinitions = attributeDefinitionRepository.findByEnabledTrueOrderByDisplayOrderAsc()

        for (definition in attributeDefinitions) {
            val attributeValue = attributes[definition.attributeKey]
            val result = definition.validateValue(attributeValue)

            if (!result.valid) {
                result.message?.let { errors.add(it) }
            }
        }

        // Check for undefined attributes (attributes not in definitions)
        val definedKeys = attributeDefinitions.map { it.attributeKey }.toSet()
        val undefinedKeys = attributes.keys - definedKeys

        if (undefinedKeys.isNotEmpty()) {
            logger.warn("Customer has undefined attributes: $undefinedKeys")
            // Optionally add error for undefined attributes
            // errors.add("Undefined attributes found: ${undefinedKeys.joinToString(", ")}")
        }

        return errors
    }

    /**
     * Get visible field configurations for frontend display
     */
    fun getVisibleFieldConfigs(): List<CustomerFieldConfig> {
        return fieldConfigRepository.findByVisibleTrueAndEnabledTrueOrderByDisplayOrderAsc()
    }

    /**
     * Get visible attribute definitions for frontend display
     */
    fun getVisibleAttributeDefinitions(): List<CustomerAttributeDefinition> {
        return attributeDefinitionRepository.findByVisibleTrueAndEnabledTrueOrderByDisplayOrderAsc()
    }

    /**
     * Get mandatory field configurations
     */
    fun getMandatoryFieldConfigs(): List<CustomerFieldConfig> {
        return fieldConfigRepository.findByMandatoryTrueAndEnabledTrue()
    }

    /**
     * Get mandatory attribute definitions
     */
    fun getMandatoryAttributeDefinitions(): List<CustomerAttributeDefinition> {
        return attributeDefinitionRepository.findByMandatoryTrueAndEnabledTrue()
    }

    /**
     * Get field configuration schema for frontend
     */
    fun getFieldConfigSchema(): CustomerConfigSchema {
        val fieldConfigs = fieldConfigRepository.findByEnabledTrueOrderByDisplayOrderAsc()
        val attributeDefinitions = attributeDefinitionRepository.findByEnabledTrueOrderByDisplayOrderAsc()

        return CustomerConfigSchema(
            fields = fieldConfigs,
            attributes = attributeDefinitions
        )
    }

    /**
     * Get field value from customer using reflection
     */
    private fun getFieldValue(customer: Customer, fieldName: String): Any? {
        return try {
            val property = customerFieldMap[fieldName]
            property?.get(customer)
        } catch (e: Exception) {
            logger.warn("Failed to get field value for $fieldName", e)
            null
        }
    }

    /**
     * Apply default values to customer attributes based on attribute definitions
     */
    fun applyDefaultAttributes(customer: Customer) {
        val attributeDefinitions = attributeDefinitionRepository.findByEnabledTrueOrderByDisplayOrderAsc()
        val currentAttributes = customer.attributes?.toMutableMap() ?: mutableMapOf()

        for (definition in attributeDefinitions) {
            // Only apply default if attribute is not already set
            if (!currentAttributes.containsKey(definition.attributeKey)) {
                val defaultValue = definition.getParsedDefaultValue()
                if (defaultValue != null) {
                    currentAttributes[definition.attributeKey] = defaultValue
                }
            }
        }

        customer.attributes = currentAttributes
    }

    /**
     * Filter customer attributes to only include defined attributes
     */
    fun filterDefinedAttributes(attributes: Map<String, Any>): Map<String, Any> {
        val attributeDefinitions = attributeDefinitionRepository.findByEnabledTrueOrderByDisplayOrderAsc()
        val definedKeys = attributeDefinitions.map { it.attributeKey }.toSet()

        return attributes.filterKeys { it in definedKeys }
    }

    /**
     * Get attribute definitions grouped by category
     */
    fun getAttributeDefinitionsByCategory(): Map<String, List<CustomerAttributeDefinition>> {
        val attributeDefinitions = attributeDefinitionRepository.findByEnabledTrueOrderByDisplayOrderAsc()
        return attributeDefinitions.groupBy { it.category ?: "General" }
    }

    /**
     * Check if a specific field is visible
     */
    fun isFieldVisible(fieldName: String): Boolean {
        val config = fieldConfigRepository.findByFieldName(fieldName)
        return config?.visible == true && config.enabled
    }

    /**
     * Check if a specific field is mandatory
     */
    fun isFieldMandatory(fieldName: String): Boolean {
        val config = fieldConfigRepository.findByFieldName(fieldName)
        return config?.mandatory == true && config.enabled
    }

    /**
     * Check if a specific attribute is visible
     */
    fun isAttributeVisible(attributeKey: String): Boolean {
        val definition = attributeDefinitionRepository.findByAttributeKey(attributeKey)
        return definition?.visible == true && definition.enabled
    }

    /**
     * Check if a specific attribute is mandatory
     */
    fun isAttributeMandatory(attributeKey: String): Boolean {
        val definition = attributeDefinitionRepository.findByAttributeKey(attributeKey)
        return definition?.mandatory == true && definition.enabled
    }
}

/**
 * Result of customer validation
 */
data class CustomerValidationResult(
    val valid: Boolean,
    val errors: List<String>
)

/**
 * Customer configuration schema for frontend
 */
data class CustomerConfigSchema(
    val fields: List<CustomerFieldConfig>,
    val attributes: List<CustomerAttributeDefinition>
)
