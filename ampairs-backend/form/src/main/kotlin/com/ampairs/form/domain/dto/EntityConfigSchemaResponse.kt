package com.ampairs.form.domain.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Complete entity configuration schema response
 * Combines field configurations and attribute definitions for a specific entity type
 */
data class EntityConfigSchemaResponse(
    val fieldConfigs: List<FieldConfigResponse> = emptyList(),
    val attributeDefinitions: List<AttributeDefinitionResponse> = emptyList(),
    val lastUpdated: String? = null
)

/**
 * Complete entity configuration schema request
 * Combines field configurations and attribute definitions for bulk create/update
 * Entity type is extracted from the field configs/attribute definitions
 */
data class EntityConfigSchemaRequest(
    @JsonProperty("field_configs")
    val fieldConfigs: List<FieldConfigRequest> = emptyList(),
    @JsonProperty("attribute_definitions")
    val attributeDefinitions: List<AttributeDefinitionRequest> = emptyList()
)
