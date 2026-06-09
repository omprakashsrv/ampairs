package com.ampairs.setting.domain.dto

import com.ampairs.core.setting.SettingDefinition
import com.ampairs.core.setting.SettingValueType
import com.ampairs.setting.domain.model.StoreSetting
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

/**
 * Bulk-sync push payload for a single setting override. UID is client-generated (offline-first) but
 * may be omitted, in which case the server resolves/creates by `(module, key)`.
 */
data class SettingRequest(
    val uid: String? = null,

    @field:NotBlank(message = "Setting module is required")
    @field:Size(max = 64, message = "Module must not exceed 64 characters")
    val module: String,

    @field:NotBlank(message = "Setting key is required")
    @field:Size(max = 128, message = "Key must not exceed 128 characters")
    val key: String,

    val value: String? = null,

    val active: Boolean = true,

    val refId: String? = null,
)

data class SettingResponse(
    val uid: String,
    val refId: String?,
    val module: String,
    val key: String,
    val value: String?,
    val valueType: SettingValueType,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun StoreSetting.asSettingResponse(): SettingResponse = SettingResponse(
    uid = uid,
    refId = refId,
    module = moduleCode,
    key = settingKey,
    value = value,
    valueType = valueType,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun List<StoreSetting>.asSettingResponses(): List<SettingResponse> = map { it.asSettingResponse() }

/**
 * Definition catalog entry returned to clients so they can render a generic settings UI (label,
 * type, default, allowed values) without hard-coding it. Filtered server-side to the workspace's
 * installed modules.
 */
data class SettingDefinitionResponse(
    val module: String,
    val key: String,
    val valueType: SettingValueType,
    val defaultValue: String,
    val allowedValues: List<String>,
    val label: String,
    val description: String?,
)

fun SettingDefinition.asDefinitionResponse(): SettingDefinitionResponse = SettingDefinitionResponse(
    module = module,
    key = key,
    valueType = valueType,
    defaultValue = defaultValue,
    allowedValues = allowedValues,
    label = label,
    description = description,
)

fun List<SettingDefinition>.asDefinitionResponses(): List<SettingDefinitionResponse> =
    map { it.asDefinitionResponse() }
