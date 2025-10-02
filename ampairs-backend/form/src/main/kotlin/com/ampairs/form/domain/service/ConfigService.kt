package com.ampairs.form.domain.service

import com.ampairs.form.domain.dto.*
import com.ampairs.form.domain.model.AttributeDefinition
import com.ampairs.form.domain.model.FieldConfig
import com.ampairs.form.domain.repository.AttributeDefinitionRepository
import com.ampairs.form.domain.repository.FieldConfigRepository
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

    /**
     * Get complete configuration schema for an entity type
     */
    @Transactional(readOnly = true)
    fun getConfigSchema(entityType: String): EntityConfigSchemaResponse {
        val fieldConfigs = fieldConfigRepository
            .findByEntityTypeOrderByDisplayOrderAsc(entityType)
            .asFieldConfigResponses()

        val attributeDefinitions = attributeDefinitionRepository
            .findByEntityTypeOrderByDisplayOrderAsc(entityType)
            .asAttributeDefinitionResponses()

        val lastUpdated = getLastUpdatedTimestamp(fieldConfigs, attributeDefinitions)

        return EntityConfigSchemaResponse(
            fieldConfigs = fieldConfigs,
            attributeDefinitions = attributeDefinitions,
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
}
