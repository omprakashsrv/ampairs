package com.ampairs.form.domain.dto

data class EntityConfigSchemaResponse(
    val fieldConfigs: List<FieldConfigResponse> = emptyList(),
    val attributeDefinitions: List<AttributeDefinitionResponse> = emptyList(),
    val lastUpdated: String? = null
)

data class EntityConfigSchemaRequest(
    val fieldConfigs: List<FieldConfigRequest> = emptyList(),
    val attributeDefinitions: List<AttributeDefinitionRequest> = emptyList()
)
