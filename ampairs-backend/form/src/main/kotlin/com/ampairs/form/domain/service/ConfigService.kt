package com.ampairs.form.domain.service

import com.ampairs.form.domain.dto.*
import com.ampairs.form.domain.model.AttributeDefinition
import com.ampairs.form.domain.model.FieldConfig
import com.ampairs.form.domain.repository.AttributeDefinitionRepository
import com.ampairs.form.domain.repository.FieldConfigRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Service for managing entity configuration schemas
 */
@Service
class ConfigService(
    private val fieldConfigRepository: FieldConfigRepository,
    private val attributeDefinitionRepository: AttributeDefinitionRepository
) {

    private val logger = LoggerFactory.getLogger(ConfigService::class.java)

    /**
     * Get complete configuration schema for an entity type
     * Auto-seeds default configuration on first access if none exists
     */
    @Transactional
    fun getConfigSchema(entityType: String): EntityConfigSchemaResponse {
        var fieldConfigs = fieldConfigRepository
            .findByEntityTypeOrderByDisplayOrderAsc(entityType)

        var attributeDefinitions = attributeDefinitionRepository
            .findByEntityTypeOrderByDisplayOrderAsc(entityType)

        // Auto-seed if empty (first time access for this entity type in this workspace)
        if (fieldConfigs.isEmpty() && attributeDefinitions.isEmpty()) {
            logger.info("No configuration found for entity type: {}, seeding defaults...", entityType)
            seedDefaultConfig(entityType)

            // Fetch again after seeding
            fieldConfigs = fieldConfigRepository.findByEntityTypeOrderByDisplayOrderAsc(entityType)
            attributeDefinitions = attributeDefinitionRepository.findByEntityTypeOrderByDisplayOrderAsc(entityType)
        }

        val fieldConfigResponses = fieldConfigs.asFieldConfigResponses()
        val attributeDefinitionResponses = attributeDefinitions.asAttributeDefinitionResponses()
        val lastUpdated = getLastUpdatedTimestamp(fieldConfigResponses, attributeDefinitionResponses)

        return EntityConfigSchemaResponse(
            fieldConfigs = fieldConfigResponses,
            attributeDefinitions = attributeDefinitionResponses,
            lastUpdated = lastUpdated
        )
    }

    /**
     * Get all configuration schemas for all entity types
     */
    @Transactional(readOnly = true)
    fun getAllConfigSchemas(): List<EntityConfigSchemaResponse> {
        // Get distinct entity types from both tables
        val entityTypes = mutableSetOf<String>()
        entityTypes.addAll(fieldConfigRepository.findAll().map { it.entityType }.distinct())
        entityTypes.addAll(attributeDefinitionRepository.findAll().map { it.entityType }.distinct())

        return entityTypes.map { getConfigSchema(it) }
    }

    /**
     * Create or update field configuration
     */
    @Transactional
    fun saveFieldConfig(request: FieldConfigRequest): FieldConfigResponse {
        val existing = fieldConfigRepository.findByEntityTypeAndFieldName(
            request.entityType,
            request.fieldName
        )

        val entity = if (existing != null) {
            existing.apply {
                displayName = request.displayName
                visible = request.visible
                mandatory = request.mandatory
                enabled = request.enabled
                displayOrder = request.displayOrder
                validationType = request.validationType
                validationParams = request.validationParams?.let {
                    com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().writeValueAsString(it)
                }
                placeholder = request.placeholder
                helpText = request.helpText
                defaultValue = request.defaultValue
            }
        } else {
            request.toFieldConfig()
        }

        return fieldConfigRepository.save(entity).asFieldConfigResponse()
    }

    /**
     * Create or update attribute definition
     */
    @Transactional
    fun saveAttributeDefinition(request: AttributeDefinitionRequest): AttributeDefinitionResponse {
        val existing = attributeDefinitionRepository.findByEntityTypeAndAttributeKey(
            request.entityType,
            request.attributeKey
        )

        val entity = if (existing != null) {
            existing.apply {
                displayName = request.displayName
                dataType = request.dataType
                visible = request.visible
                mandatory = request.mandatory
                enabled = request.enabled
                displayOrder = request.displayOrder
                category = request.category
                defaultValue = request.defaultValue
                validationType = request.validationType
                validationParams = request.validationParams?.let {
                    com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().writeValueAsString(it)
                }
                enumValues = request.enumValues?.let {
                    com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().writeValueAsString(it)
                }
                placeholder = request.placeholder
                helpText = request.helpText
            }
        } else {
            request.toAttributeDefinition()
        }

        return attributeDefinitionRepository.save(entity).asAttributeDefinitionResponse()
    }

    /**
     * Delete field configuration
     */
    @Transactional
    fun deleteFieldConfig(entityType: String, fieldName: String) {
        fieldConfigRepository.findByEntityTypeAndFieldName(entityType, fieldName)
            ?.let { fieldConfigRepository.delete(it) }
    }

    /**
     * Delete attribute definition
     */
    @Transactional
    fun deleteAttributeDefinition(entityType: String, attributeKey: String) {
        attributeDefinitionRepository.findByEntityTypeAndAttributeKey(entityType, attributeKey)
            ?.let { attributeDefinitionRepository.delete(it) }
    }

    /**
     * Bulk save entity configuration schema (field configs + attribute definitions)
     * Used for initializing defaults or bulk updates from frontend
     */
    @Transactional
    fun saveConfigSchema(request: EntityConfigSchemaRequest): EntityConfigSchemaResponse {
        // Extract entity type from the first available config
        val entityType = request.fieldConfigs.firstOrNull()?.entityType
            ?: request.attributeDefinitions.firstOrNull()?.entityType
            ?: throw IllegalArgumentException("Cannot determine entity type: no field configs or attribute definitions provided")

        logger.info("Bulk saving configuration for entity type: {}", entityType)

        // Save all field configs
        val savedFieldConfigs = request.fieldConfigs.map { fieldConfigRequest ->
            saveFieldConfig(fieldConfigRequest)
        }

        // Save all attribute definitions
        val savedAttributeDefinitions = request.attributeDefinitions.map { attributeRequest ->
            saveAttributeDefinition(attributeRequest)
        }

        val lastUpdated = getLastUpdatedTimestamp(savedFieldConfigs, savedAttributeDefinitions)

        logger.info("Saved {} field configs and {} attribute definitions for entity type: {}",
            savedFieldConfigs.size, savedAttributeDefinitions.size, entityType)

        return EntityConfigSchemaResponse(
            fieldConfigs = savedFieldConfigs,
            attributeDefinitions = savedAttributeDefinitions,
            lastUpdated = lastUpdated
        )
    }

    /**
     * Get the latest updated timestamp from configs
     */
    private fun getLastUpdatedTimestamp(
        fieldConfigs: List<FieldConfigResponse>,
        attributeDefinitions: List<AttributeDefinitionResponse>
    ): String {
        val fieldUpdates = fieldConfigs.mapNotNull { it.updatedAt }
        val attributeUpdates = attributeDefinitions.mapNotNull { it.updatedAt }
        val allUpdates = fieldUpdates + attributeUpdates

        return allUpdates.maxOrNull() ?: LocalDateTime.now().toString()
    }

    /**
     * Seed default configuration for an entity type
     */
    private fun seedDefaultConfig(entityType: String) {
        when (entityType.lowercase()) {
            "customer" -> seedCustomerDefaults()
            "product" -> seedProductDefaults()
            "order" -> seedOrderDefaults()
            "invoice" -> seedInvoiceDefaults()
            else -> {
                logger.warn("No default configuration defined for entity type: {}", entityType)
            }
        }
    }

    /**
     * Seed default customer form configuration
     */
    private fun seedCustomerDefaults() {
        val fields = listOf(
            createFieldConfig("customer", "name", "Customer Name", 1, visible = true, mandatory = true),
            createFieldConfig("customer", "phone", "Phone Number", 2, visible = true, mandatory = true, validationType = "PHONE"),
            createFieldConfig("customer", "email", "Email Address", 3, visible = true, mandatory = false, validationType = "EMAIL"),
            createFieldConfig("customer", "gstNumber", "GST Number", 4, visible = true, mandatory = false,
                placeholder = "Enter 15-digit GST number", helpText = "GST number format: 22AAAAA0000A1Z5"),
            createFieldConfig("customer", "customerType", "Customer Type", 5, visible = true, mandatory = false),
            createFieldConfig("customer", "customerGroup", "Customer Group", 6, visible = true, mandatory = false),
            createFieldConfig("customer", "address", "Address", 7, visible = true, mandatory = false),
            createFieldConfig("customer", "city", "City", 8, visible = true, mandatory = false),
            createFieldConfig("customer", "state", "State", 9, visible = true, mandatory = false),
            createFieldConfig("customer", "pincode", "Pincode", 10, visible = true, mandatory = false, validationType = "NUMBER"),
            createFieldConfig("customer", "creditLimit", "Credit Limit", 11, visible = true, mandatory = false, validationType = "NUMBER",
                placeholder = "0.00", helpText = "Maximum credit limit for this customer"),
            createFieldConfig("customer", "creditDays", "Credit Days", 12, visible = true, mandatory = false, validationType = "NUMBER",
                placeholder = "0", helpText = "Number of days credit allowed"),
        )

        fieldConfigRepository.saveAll(fields)
        logger.info("Seeded {} customer field configurations", fields.size)

        val attributes = listOf(
            createAttributeDefinition("customer", "industry", "Industry", "STRING", 1,
                helpText = "Customer's business industry"),
            createAttributeDefinition("customer", "company_size", "Company Size", "STRING", 2,
                helpText = "Size of customer's organization"),
            createAttributeDefinition("customer", "preferred_payment_method", "Preferred Payment Method", "STRING", 3,
                helpText = "Customer's preferred payment method"),
        )

        attributeDefinitionRepository.saveAll(attributes)
        logger.info("Seeded {} customer attribute definitions", attributes.size)
    }

    /**
     * Seed default product form configuration
     */
    private fun seedProductDefaults() {
        val fields = listOf(
            createFieldConfig("product", "name", "Product Name", 1, visible = true, mandatory = true),
            createFieldConfig("product", "sku", "SKU", 2, visible = true, mandatory = false),
            createFieldConfig("product", "description", "Description", 3, visible = true, mandatory = false),
            createFieldConfig("product", "category", "Category", 4, visible = true, mandatory = false),
            createFieldConfig("product", "price", "Price", 5, visible = true, mandatory = true, validationType = "NUMBER"),
            createFieldConfig("product", "taxCode", "Tax Code", 6, visible = true, mandatory = false),
            createFieldConfig("product", "unit", "Unit", 7, visible = true, mandatory = false),
        )

        fieldConfigRepository.saveAll(fields)
        logger.info("Seeded {} product field configurations", fields.size)
    }

    /**
     * Seed default order form configuration
     */
    private fun seedOrderDefaults() {
        val fields = listOf(
            createFieldConfig("order", "customer", "Customer", 1, visible = true, mandatory = true),
            createFieldConfig("order", "orderDate", "Order Date", 2, visible = true, mandatory = true),
            createFieldConfig("order", "deliveryDate", "Delivery Date", 3, visible = true, mandatory = false),
            createFieldConfig("order", "status", "Status", 4, visible = true, mandatory = false),
            createFieldConfig("order", "notes", "Notes", 5, visible = true, mandatory = false),
        )

        fieldConfigRepository.saveAll(fields)
        logger.info("Seeded {} order field configurations", fields.size)
    }

    /**
     * Seed default invoice form configuration
     */
    private fun seedInvoiceDefaults() {
        val fields = listOf(
            createFieldConfig("invoice", "customer", "Customer", 1, visible = true, mandatory = true),
            createFieldConfig("invoice", "invoiceDate", "Invoice Date", 2, visible = true, mandatory = true),
            createFieldConfig("invoice", "dueDate", "Due Date", 3, visible = true, mandatory = false),
            createFieldConfig("invoice", "status", "Status", 4, visible = true, mandatory = false),
            createFieldConfig("invoice", "notes", "Notes", 5, visible = true, mandatory = false),
        )

        fieldConfigRepository.saveAll(fields)
        logger.info("Seeded {} invoice field configurations", fields.size)
    }

    /**
     * Helper method to create a FieldConfig entity
     */
    private fun createFieldConfig(
        entityType: String,
        fieldName: String,
        displayName: String,
        displayOrder: Int,
        visible: Boolean,
        mandatory: Boolean,
        enabled: Boolean = true,
        placeholder: String? = null,
        helpText: String? = null,
        defaultValue: String? = null,
        validationType: String? = null
    ) = FieldConfig().apply {
        this.entityType = entityType
        this.fieldName = fieldName
        this.displayName = displayName
        this.displayOrder = displayOrder
        this.visible = visible
        this.mandatory = mandatory
        this.enabled = enabled
        this.placeholder = placeholder
        this.helpText = helpText
        this.defaultValue = defaultValue
        this.validationType = validationType
    }

    /**
     * Helper method to create an AttributeDefinition entity
     */
    private fun createAttributeDefinition(
        entityType: String,
        attributeKey: String,
        displayName: String,
        dataType: String,
        displayOrder: Int,
        visible: Boolean = true,
        mandatory: Boolean = false,
        enabled: Boolean = true,
        category: String? = null,
        placeholder: String? = null,
        helpText: String? = null,
        defaultValue: String? = null
    ) = AttributeDefinition().apply {
        this.entityType = entityType
        this.attributeKey = attributeKey
        this.displayName = displayName
        this.dataType = dataType
        this.displayOrder = displayOrder
        this.visible = visible
        this.mandatory = mandatory
        this.enabled = enabled
        this.category = category
        this.placeholder = placeholder
        this.helpText = helpText
        this.defaultValue = defaultValue
    }
}
