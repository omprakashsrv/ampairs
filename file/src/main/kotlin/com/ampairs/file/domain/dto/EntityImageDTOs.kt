package com.ampairs.file.domain.dto

import com.ampairs.file.domain.model.EntityImage
import jakarta.validation.constraints.*
import java.time.Instant

data class EntityImageUploadRequest(
    val entityType: String,
    val entityUid: String,
    @field:Size(min = 1, max = 36) val uid: String? = null,
    @field:Size(max = 500) val description: String? = null,
    val isPrimary: Boolean = false,
    @field:Min(0) val displayOrder: Int? = null
)

data class EntityImageResponse(
    val uid: String,
    val entityType: String,
    val entityUid: String,
    val originalFilename: String,
    val fileExtension: String,
    val contentType: String,
    val fileSize: Long,
    val formattedFileSize: String,
    val storagePath: String,
    val storageUrl: String?,
    val imageUrl: String,
    val thumbnailUrl: String,
    val isPrimary: Boolean,
    val displayOrder: Int,
    val description: String?,
    val width: Int?,
    val height: Int?,
    val checksum: String,
    val uploadedAt: Instant,
    val active: Boolean,
    val etag: String?,
    val lastModified: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class EntityImageUpdateRequest(
    @field:Size(max = 500) val description: String? = null,
    val isPrimary: Boolean? = null,
    @field:Min(0) val displayOrder: Int? = null
)

data class EntityImageListResponse(
    val images: List<EntityImageResponse>,
    val totalCount: Int,
    val primaryImage: EntityImageResponse?,
    val totalSize: Long,
    val formattedTotalSize: String
)

data class EntityImageStatsResponse(
    val entityType: String,
    val entityUid: String,
    val totalImages: Int,
    val primaryImages: Int,
    val totalSize: Long,
    val formattedTotalSize: String,
    val averageSize: Long,
    val largestImage: EntityImageResponse?,
    val oldestImage: EntityImageResponse?,
    val newestImage: EntityImageResponse?
)

data class EntityImageBulkDeleteRequest(
    @field:NotEmpty @field:Size(max = 50) val imageUids: List<@NotBlank String>
)

data class EntityImageBulkDeleteResponse(
    val successCount: Int,
    val failureCount: Int,
    val successfulImageUids: List<String>,
    val failedImageUids: List<String>,
    val errors: List<String> = emptyList()
)

data class EntityImageReorderRequest(
    @field:NotEmpty val imageOrders: List<ImageOrderItem>
) {
    data class ImageOrderItem(
        @field:NotBlank val imageUid: String,
        @field:Min(0) val displayOrder: Int
    )
}

fun EntityImage.asEntityImageResponse(baseUrl: String): EntityImageResponse = EntityImageResponse(
    uid = uid,
    entityType = entityType,
    entityUid = entityUid,
    originalFilename = originalFilename,
    fileExtension = fileExtension,
    contentType = contentType,
    fileSize = fileSize,
    formattedFileSize = getFormattedFileSize(),
    storagePath = storagePath,
    storageUrl = storageUrl,
    imageUrl = "$baseUrl/$uid/download",
    thumbnailUrl = "$baseUrl/$uid/thumbnail",
    isPrimary = isPrimary,
    displayOrder = displayOrder,
    description = description,
    width = metadata.width,
    height = metadata.height,
    checksum = checksum,
    uploadedAt = uploadedAt,
    active = active,
    etag = metadata.etag,
    lastModified = metadata.lastModified,
    createdAt = createdAt ?: Instant.now(),
    updatedAt = updatedAt ?: Instant.now()
)

fun List<EntityImage>.asEntityImageListResponse(baseUrl: String): EntityImageListResponse {
    val responses = map { it.asEntityImageResponse(baseUrl) }
    return EntityImageListResponse(
        images = responses,
        totalCount = size,
        primaryImage = responses.find { it.isPrimary },
        totalSize = sumOf { it.fileSize },
        formattedTotalSize = formatEntityFileSize(sumOf { it.fileSize })
    )
}

fun formatEntityFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> "${bytes / (1024 * 1024 * 1024)} GB"
}
