package com.ampairs.ecom.domain.dto

import com.ampairs.ecom.domain.enums.StorefrontAccessIdentifierType
import com.ampairs.ecom.domain.model.StorefrontAccessEntry
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

data class StorefrontAccessEntryRequest(
    @field:NotNull(message = "Identifier type is required")
    val identifierType: StorefrontAccessIdentifierType,

    @field:NotBlank(message = "Identifier value is required")
    val identifierValue: String,
)

data class StorefrontAccessEntryResponse(
    val uid: String,
    val storefrontId: String,
    val identifierType: StorefrontAccessIdentifierType,
    val identifierValue: String,
    val createdAt: Instant?,
)

data class StorefrontAccessBulkImportResult(
    val imported: Int,
    val skipped: Int,
    val failed: List<String>,
)

fun StorefrontAccessEntry.asResponse() = StorefrontAccessEntryResponse(
    uid = uid,
    storefrontId = storefrontId,
    identifierType = identifierType,
    identifierValue = identifierValue,
    createdAt = createdAt,
)
