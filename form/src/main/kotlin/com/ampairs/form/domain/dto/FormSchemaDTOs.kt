package com.ampairs.form.domain.dto

import com.ampairs.form.domain.model.validation.ValidationRule

/**
 * The form schema aggregate as transferred over `/config/schema/sync` and `/config/schema`.
 * `sections` + `fields` are the aggregate members. JSON is global SNAKE_CASE
 * (entityType → entity_type, displayOrder → display_order, …).
 */
data class FormSchemaResponse(
    val uid: String,
    val entityType: String,
    val version: Long,
    val sections: List<FormSectionDto> = emptyList(),
    val fields: List<FormFieldDto> = emptyList(),
    val lastUpdated: String? = null,
)

/**
 * Push request for an aggregate. The server replaces the aggregate (upsert present members, delete
 * absent) under optimistic concurrency: [baseVersion] must match the server's current version.
 */
data class FormSchemaRequest(
    val entityType: String,
    val baseVersion: Long? = null,
    val sections: List<FormSectionDto> = emptyList(),
    val fields: List<FormFieldDto> = emptyList(),
)

data class FormSectionDto(
    val uid: String? = null,
    val name: String,
    val displayOrder: Int = 0,
    val visible: Boolean = true,
)

data class FormFieldDto(
    val uid: String? = null,
    val source: String,
    val fieldKey: String,
    val displayName: String,
    val dataType: String,
    val widgetKey: String? = null,
    val sectionUid: String,
    val visible: Boolean = true,
    val mandatory: Boolean = false,
    val enabled: Boolean = true,
    val displayOrder: Int = 0,
    val defaultValue: String? = null,
    val optionSource: String? = null,
    val enumValues: List<String>? = null,
    val dynamicSourceKey: String? = null,
    val validationRules: List<ValidationRule>? = null,
    val placeholder: String? = null,
    val helpText: String? = null,
)
