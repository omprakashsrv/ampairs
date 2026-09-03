package com.ampairs.cb_maintenance.domain.dto

import com.ampairs.cb_maintenance.domain.model.AssetCategoryAlias
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class AssetCategoryAliasRequest(
    val uid: String? = null,
    @field:NotBlank(message = "Canonical value is required")
    val canonical: String,
    @field:NotBlank(message = "Alias is required")
    val alias: String,
    val active: Boolean = true,
    val refId: String? = null,
)

data class AssetCategoryAliasResponse(
    val uid: String,
    val refId: String?,
    val canonical: String,
    val alias: String,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun AssetCategoryAlias.applyRequest(request: AssetCategoryAliasRequest): AssetCategoryAlias = apply {
    request.uid?.let { uid = it }
    canonical = request.canonical.trim()
    alias = request.alias.trim()
    aliasLower = request.alias.trim().lowercase()
    active = request.active
    request.refId?.takeIf { it.isNotBlank() }?.let { refId = it }
}

fun AssetCategoryAlias.asAssetCategoryAliasResponse(): AssetCategoryAliasResponse = AssetCategoryAliasResponse(
    uid = uid,
    refId = refId,
    canonical = canonical,
    alias = alias,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
