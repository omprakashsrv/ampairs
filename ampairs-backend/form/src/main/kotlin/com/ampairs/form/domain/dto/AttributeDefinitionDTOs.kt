package com.ampairs.form.domain.dto

import com.ampairs.form.domain.model.AttributeDefinition
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Attribute definition response DTO
 */
data class AttributeDefinitionResponse(
    val uid: String,
    val entityType: String,
    val attributeKey: String,
    val displayName: String,
    val dataType: String = "STRING",
    val visible: Boolean = true,
    val mandatory: Boolean = false,
    val enabled: Boolean = true,
    val displayOrder: Int = 0,
    val category: String? = null,
    val defaultValue: String? = null,
    val validationType: String? = null,
    val validationParams: Map<String, String>? = null,
    val enumValues: List<String>? = null,
    val placeholder: String? = null,
    val helpText: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * Attribute definition create/update request DTO
 */
data class AttributeDefinitionRequest(
    @JsonProperty("entity_type")
    val entityType: String,
    @JsonProperty("attribute_key")
    val attributeKey: String,
    @JsonProperty("display_name")
    val displayName: String,
    @JsonProperty("data_type")
    val dataType: String = "STRING",
    val visible: Boolean = true,
    val mandatory: Boolean = false,
    val enabled: Boolean = true,
    @JsonProperty("display_order")
    val displayOrder: Int = 0,
    val category: String? = null,
    @JsonProperty("default_value")
    val defaultValue: String? = null,
    @JsonProperty("validation_type")
    val validationType: String? = null,
    @JsonProperty("validation_params")
    val validationParams: Map<String, String>? = null,
    @JsonProperty("enum_values")
    val enumValues: List<String>? = null,
    val placeholder: String? = null,
    @JsonProperty("help_text")
    val helpText: String? = null
)

/**
 * Extension functions for AttributeDefinition entity
 */
private val objectMapper = jacksonObjectMapper()

fun AttributeDefinition.asAttributeDefinitionResponse(): AttributeDefinitionResponse {
    val validationParamsMap: Map<String, String>? = validationParams?.let {
        try {
            objectMapper.readValue(it)
        } catch (e: Exception) {
            null
        }
    }

    val enumValuesList: List<String>? = enumValues?.let {
        try {
            objectMapper.readValue(it)
        } catch (e: Exception) {
            null
        }
    }

    return AttributeDefinitionResponse(
        uid = uid,
        entityType = entityType,
        attributeKey = attributeKey,
        displayName = displayName,
        dataType = dataType,
        visible = visible,
        mandatory = mandatory,
        enabled = enabled,
        displayOrder = displayOrder,
        category = category,
        defaultValue = defaultValue,
        validationType = validationType,
        validationParams = validationParamsMap,
        enumValues = enumValuesList,
        placeholder = placeholder,
        helpText = helpText,
        createdAt = createdAt?.toString(),
        updatedAt = updatedAt?.toString()
    )
}

fun AttributeDefinitionRequest.toAttributeDefinition(): AttributeDefinition {
    return AttributeDefinition().apply {
        this.entityType = this@toAttributeDefinition.entityType
        this.attributeKey = this@toAttributeDefinition.attributeKey
        this.displayName = this@toAttributeDefinition.displayName
        this.dataType = this@toAttributeDefinition.dataType
        this.visible = this@toAttributeDefinition.visible
        this.mandatory = this@toAttributeDefinition.mandatory
        this.enabled = this@toAttributeDefinition.enabled
        this.displayOrder = this@toAttributeDefinition.displayOrder
        this.category = this@toAttributeDefinition.category
        this.defaultValue = this@toAttributeDefinition.defaultValue
        this.validationType = this@toAttributeDefinition.validationType
        this.validationParams = this@toAttributeDefinition.validationParams?.let {
            objectMapper.writeValueAsString(it)
        }
        this.enumValues = this@toAttributeDefinition.enumValues?.let {
            objectMapper.writeValueAsString(it)
        }
        this.placeholder = this@toAttributeDefinition.placeholder
        this.helpText = this@toAttributeDefinition.helpText
    }
}

fun List<AttributeDefinition>.asAttributeDefinitionResponses(): List<AttributeDefinitionResponse> {
    return this.map { it.asAttributeDefinitionResponse() }
}
