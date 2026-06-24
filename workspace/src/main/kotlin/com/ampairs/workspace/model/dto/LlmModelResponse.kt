package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.MasterLlmModel
import com.ampairs.workspace.model.enums.LlmModelRole

/**
 * Catalog entry returned to the mobile app. Field names serialize to snake_case (global Jackson
 * strategy) so they line up with the app's `RemoteModelDescriptor` — `id`, `display_name`,
 * `backend_id`, `download_url`, `estimated_peak_memory_bytes`, `top_k`, etc.
 */
data class LlmModelResponse(
    val id: String,
    val displayName: String,
    val role: LlmModelRole,
    val backendId: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val estimatedPeakMemoryBytes: Long,
    val sha256: String?,
    val temperature: Double,
    val topK: Int,
    val topP: Double,
    val maxTokens: Int,
)

fun MasterLlmModel.asLlmModelResponse(): LlmModelResponse = LlmModelResponse(
    id = modelId,
    displayName = displayName,
    role = role,
    backendId = backendId,
    fileName = fileName,
    downloadUrl = downloadUrl,
    sizeBytes = sizeBytes,
    estimatedPeakMemoryBytes = estimatedPeakMemoryBytes,
    sha256 = sha256,
    temperature = temperature,
    topK = topK,
    topP = topP,
    maxTokens = maxTokens,
)
