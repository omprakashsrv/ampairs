package com.ampairs.connector.domain.dto

/** One field-mapping rule: external field → Ampairs column, with optional transform. */
data class FieldMappingRuleDto(
    val externalField: String = "",
    val ampairsField: String = "",
    val unmapped: Boolean = false,
    val transform: String? = null,
)

/** Full mapping for one entity type under an installation. */
data class FieldMappingDto(
    val installationUid: String = "",
    val entityType: String = "",
    val version: Int = 1,
    val rules: List<FieldMappingRuleDto> = emptyList(),
)
