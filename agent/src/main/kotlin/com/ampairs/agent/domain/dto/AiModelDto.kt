package com.ampairs.agent.domain.dto

import com.ampairs.agent.domain.model.AiModelDescriptor
import com.ampairs.agent.domain.model.ModelPlatform
import com.ampairs.agent.domain.model.ModelRole

/**
 * Public manifest entry for one downloadable on-device model. Mirrors [AiModelDescriptor] but
 * deliberately omits the upstream `sourceUrl` — the app fetches bytes through the backend proxy
 * (`GET /agent/v1/models/{id}/download`), keeping the source opaque. Global Jackson SNAKE_CASE maps
 * the camelCase fields (e.g. `parameterLabel` → `parameter_label`).
 */
data class AiModelResponse(
    val id: String,
    val name: String,
    val family: String,
    val parameterLabel: String,
    val role: ModelRole,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String?,
    val requiredRamMb: Int,
    val backendId: String,
    val platforms: Set<ModelPlatform>,
    val recommended: Boolean,
)

fun AiModelDescriptor.asResponse(): AiModelResponse = AiModelResponse(
    id = id,
    name = name,
    family = family,
    parameterLabel = parameterLabel,
    role = role,
    fileName = fileName,
    sizeBytes = sizeBytes,
    sha256 = sha256,
    requiredRamMb = requiredRamMb,
    backendId = backendId,
    platforms = platforms,
    recommended = recommended,
)

fun List<AiModelDescriptor>.asResponses(): List<AiModelResponse> = map { it.asResponse() }
