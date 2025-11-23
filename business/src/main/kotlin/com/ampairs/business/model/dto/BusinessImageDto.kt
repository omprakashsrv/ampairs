package com.ampairs.business.model.dto

import com.ampairs.business.model.BusinessImage
import java.time.Instant

/**
 * Response DTO for business image.
 */
data class BusinessImageResponse(
    val uid: String,
    val businessId: String,
    val imageType: String,
    val imageUrl: String,
    val thumbnailUrl: String?,
    val title: String?,
    val description: String?,
    val altText: String?,
    val displayOrder: Int,
    val isPrimary: Boolean,
    val active: Boolean,
    val originalFilename: String?,
    val fileSize: Long?,
    val width: Int?,
    val height: Int?,
    val contentType: String?,
    val uploadedBy: String?,
    val uploadedAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Request DTO for updating business image metadata.
 */
data class UpdateBusinessImageRequest(
    val title: String? = null,
    val description: String? = null,
    val altText: String? = null,
    val imageType: String? = null
)

/**
 * Request DTO for reordering images.
 */
data class ReorderImagesRequest(
    val imageUids: List<String>
)

/**
 * Extension function to convert BusinessImage entity to BusinessImageResponse DTO.
 */
fun BusinessImage.asBusinessImageResponse(): BusinessImageResponse {
    return BusinessImageResponse(
        uid = this.uid,
        businessId = this.businessId,
        imageType = this.imageType.name,
        imageUrl = "/api/v1/business/images/${this.uid}/file",
        thumbnailUrl = this.thumbnailUrl?.let { "/api/v1/business/images/${this.uid}/thumbnail" },
        title = this.title,
        description = this.description,
        altText = this.altText,
        displayOrder = this.displayOrder,
        isPrimary = this.isPrimary,
        active = this.active,
        originalFilename = this.originalFilename,
        fileSize = this.fileSize,
        width = this.width,
        height = this.height,
        contentType = this.contentType,
        uploadedBy = this.uploadedBy,
        uploadedAt = this.uploadedAt,
        createdAt = this.createdAt ?: Instant.now(),
        updatedAt = this.updatedAt ?: Instant.now()
    )
}

/**
 * Extension function to convert list of BusinessImage entities to list of responses.
 */
fun List<BusinessImage>.asBusinessImageResponses(): List<BusinessImageResponse> {
    return this.map { it.asBusinessImageResponse() }
}
