package com.ampairs.form.domain.dto

import com.ampairs.form.domain.model.FieldConfig
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Field configuration response DTO
 */
data class FieldConfigResponse(
    val uid: String,
    val entityType: String,
    val fieldName: String,
    val displayName: String,
    val visible: Boolean = true,
    val mandatory: Boolean = false,
    val enabled: Boolean = true,
    val displayOrder: Int = 0,
    val validationType: String? = null,
    val validationParams: Map<String, String>? = null,
    val placeholder: String? = null,
    val helpText: String? = null,
    val defaultValue: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * Field configuration create/update request DTO
 */
data class FieldConfigRequest(
    @JsonProperty("entity_type")
    val entityType: String,
    @JsonProperty("field_name")
    val fieldName: String,
    @JsonProperty("display_name")
    val displayName: String,
    val visible: Boolean = true,
    val mandatory: Boolean = false,
    val enabled: Boolean = true,
    @JsonProperty("display_order")
    val displayOrder: Int = 0,
    @JsonProperty("validation_type")
    val validationType: String? = null,
    @JsonProperty("validation_params")
    val validationParams: Map<String, String>? = null,
    val placeholder: String? = null,
    @JsonProperty("help_text")
    val helpText: String? = null,
    @JsonProperty("default_value")
    val defaultValue: String? = null
)

/**
 * Extension functions for FieldConfig entity
 */
private val objectMapper = jacksonObjectMapper()

fun FieldConfig.asFieldConfigResponse(): FieldConfigResponse {
    val validationParamsMap: Map<String, String>? = validationParams?.let {
        try {
            objectMapper.readValue(it)
        } catch (e: Exception) {
            null
        }
    }

    return FieldConfigResponse(
        uid = uid,
        entityType = entityType,
        fieldName = fieldName,
        displayName = displayName,
        visible = visible,
        mandatory = mandatory,
        enabled = enabled,
        displayOrder = displayOrder,
        validationType = validationType,
        validationParams = validationParamsMap,
        placeholder = placeholder,
        helpText = helpText,
        defaultValue = defaultValue,
        createdAt = createdAt?.toString(),
        updatedAt = updatedAt?.toString()
    )
}

fun FieldConfigRequest.toFieldConfig(): FieldConfig {
    return FieldConfig().apply {
        this.entityType = this@toFieldConfig.entityType
        this.fieldName = this@toFieldConfig.fieldName
        this.displayName = this@toFieldConfig.displayName
        this.visible = this@toFieldConfig.visible
        this.mandatory = this@toFieldConfig.mandatory
        this.enabled = this@toFieldConfig.enabled
        this.displayOrder = this@toFieldConfig.displayOrder
        this.validationType = this@toFieldConfig.validationType
        this.validationParams = this@toFieldConfig.validationParams?.let {
            objectMapper.writeValueAsString(it)
        }
        this.placeholder = this@toFieldConfig.placeholder
        this.helpText = this@toFieldConfig.helpText
        this.defaultValue = this@toFieldConfig.defaultValue
    }
}

fun List<FieldConfig>.asFieldConfigResponses(): List<FieldConfigResponse> {
    return this.map { it.asFieldConfigResponse() }
}
