package com.ampairs.form.domain.dto

/**
 * Complete entity configuration schema response
 * Combines field configurations and attribute definitions for a specific entity type
 */
data class EntityConfigSchemaResponse(
    val fieldConfigs: List<FieldConfigResponse> = emptyList(),
    val attributeDefinitions: List<AttributeDefinitionResponse> = emptyList(),
    val lastUpdated: String? = null
)
