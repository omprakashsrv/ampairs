package com.ampairs.core.auth.domain

import java.time.Instant

/**
 * Request DTO for creating a new API key.
 */
data class CreateApiKeyRequest(
    val name: String,
    val description: String? = null,
    val scope: ApiKeyScope = ApiKeyScope.APP_UPDATES,
    val expiresInDays: Int? = null  // null = never expires
)

/**
 * Response DTO returned when creating an API key.
 * WARNING: The plain API key is only returned once!
 */
data class ApiKeyCreationResponse(
    val uid: String,
    val name: String,
    val apiKey: String,  // ONLY RETURNED ONCE!
    val keyPrefix: String,
    val scope: String,
    val expiresAt: Instant?,
    val createdAt: Instant,
    val warning: String = "⚠️ Store this API key securely. It will not be shown again!"
)

/**
 * Response DTO for listing API keys.
 * Does not include the actual key (only prefix for identification).
 */
data class ApiKeyResponse(
    val uid: String,
    val name: String,
    val description: String?,
    val keyPrefix: String,  // e.g., "amp_1a2b3c4d"
    val scope: String,
    val isActive: Boolean,
    val expiresAt: Instant?,
    val lastUsedAt: Instant?,
    val usageCount: Long,
    val createdAt: Instant,
    val createdByUserId: String?,
    val revokedAt: Instant?,
    val revokedBy: String?,
    val revokedReason: String?
)

/**
 * Extension functions for entity to DTO conversion.
 */
fun ApiKey.asApiKeyResponse(): ApiKeyResponse = ApiKeyResponse(
    uid = this.uid,
    name = this.name,
    description = this.description,
    keyPrefix = this.keyPrefix,
    scope = this.scope.name,
    isActive = this.isActive,
    expiresAt = this.expiresAt,
    lastUsedAt = this.lastUsedAt,
    usageCount = this.usageCount,
    createdAt = this.createdAt!!,
    createdByUserId = this.createdByUserId,
    revokedAt = this.revokedAt,
    revokedBy = this.revokedBy,
    revokedReason = this.revokedReason
)

fun List<ApiKey>.asApiKeyResponses(): List<ApiKeyResponse> = map { it.asApiKeyResponse() }
