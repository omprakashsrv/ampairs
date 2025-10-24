package com.ampairs.customer.domain.dto

import com.ampairs.customer.domain.model.CustomerImage
import jakarta.validation.constraints.*
import java.time.Instant

/**
 * Request DTO for uploading customer image
 */
data class CustomerImageUploadRequest(
    @field:NotBlank(message = "Customer UID is required")
    @field:Size(min = 1, max = 36, message = "Customer UID must be between 1 and 36 characters")
    val customerUid: String,

    @field:Size(min = 1, max = 36, message = "Image UID must be between 1 and 36 characters")
    val uid: String? = null,

    @field:Size(max = 500, message = "Description cannot exceed 500 characters")
    val description: String? = null,

    @field:NotNull(message = "Primary status is required")
    val isPrimary: Boolean = false,

    @field:Min(value = 0, message = "Display order cannot be negative")
    val displayOrder: Int? = null
)

/**
 * Response DTO for customer image
 */
data class CustomerImageResponse(
    val uid: String,
    val customerUid: String,
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
    val uploadedAt: Instant,
    val active: Boolean,
    val etag: String?,
    val lastModified: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Request DTO for updating customer image metadata
 */
data class CustomerImageUpdateRequest(

    @field:Size(max = 500, message = "Description cannot exceed 500 characters")
    val description: String? = null,

    val isPrimary: Boolean? = null,

    @field:Min(value = 0, message = "Display order cannot be negative")
    val displayOrder: Int? = null
)

/**
 * Request DTO for bulk image operations
 */
data class CustomerImageBulkRequest(
    @field:NotEmpty(message = "Image UIDs list cannot be empty")
    @field:Size(max = 50, message = "Cannot process more than 50 images at once")
    val imageUids: List<@NotBlank String>
)

/**
 * Request DTO for reordering images
 */
data class CustomerImageReorderRequest(
    @field:NotEmpty(message = "Image order list cannot be empty")
    val imageOrders: List<ImageOrderItem>
) {
    data class ImageOrderItem(
        @field:NotBlank(message = "Image UID is required")
        val imageUid: String,

        @field:Min(value = 0, message = "Display order cannot be negative")
        val displayOrder: Int
    )
}

/**
 * Response DTO for image upload operation
 */
data class CustomerImageUploadResponse(
    val image: CustomerImageResponse,
    val uploadedAt: Instant,
    val processingTime: Long // in milliseconds
)

/**
 * Response DTO for bulk operations
 */
data class CustomerImageBulkResponse(
    val successCount: Int,
    val failureCount: Int,
    val successfulImageUids: List<String>,
    val failedImageUids: List<String>,
    val errors: List<String> = emptyList()
)

/**
 * Response DTO for customer image list
 */
data class CustomerImageListResponse(
    val images: List<CustomerImageResponse>,
    val totalCount: Int,
    val primaryImage: CustomerImageResponse?,
    val totalSize: Long,
    val formattedTotalSize: String
)

/**
 * Response DTO for image statistics
 */
data class CustomerImageStatsResponse(
    val customerUid: String,
    val totalImages: Int,
    val primaryImages: Int,
    val totalSize: Long,
    val formattedTotalSize: String,
    val averageSize: Long,
    val largestImage: CustomerImageResponse?,
    val oldestImage: CustomerImageResponse?,
    val newestImage: CustomerImageResponse?
)

/**
 * Response DTO for workspace image statistics
 */
data class WorkspaceImageStatsResponse(
    val workspaceSlug: String,
    val totalImages: Long,
    val totalSize: Long,
    val formattedTotalSize: String,
    val totalCustomersWithImages: Long,
    val averageImagesPerCustomer: Double,
    val contentTypeBreakdown: Map<String, Long>,
    val sizeBreakdown: SizeBreakdown
) {
    data class SizeBreakdown(
        val small: Long,  // < 1MB
        val medium: Long, // 1MB - 5MB
        val large: Long   // > 5MB
    )
}

/**
 * Extension functions to convert between entity and DTOs
 */
fun CustomerImage.asCustomerImageResponse(): CustomerImageResponse = CustomerImageResponse(
    uid = uid,
    customerUid = customerUid,
    originalFilename = originalFilename,
    fileExtension = fileExtension,
    contentType = contentType,
    fileSize = fileSize,
    formattedFileSize = getFormattedFileSize(),
    storagePath = storagePath,
    storageUrl = storageUrl,
    imageUrl = "/customer/v1/images/$customerUid/$uid/download",
    thumbnailUrl = "/customer/v1/images/$customerUid/$uid/thumbnail",
    isPrimary = isPrimary,
    displayOrder = displayOrder,
    description = description,
    width = metadata.width,
    height = metadata.height,
    uploadedAt = uploadedAt,
    active = active,
    etag = metadata.etag,
    lastModified = metadata.lastModified,
    createdAt = createdAt ?: Instant.now(),
    updatedAt = updatedAt ?: Instant.now()
)

fun List<CustomerImage>.asCustomerImageResponses(): List<CustomerImageResponse> =
    map { it.asCustomerImageResponse() }

/**
 * Create customer image list response with statistics
 */
fun List<CustomerImage>.asCustomerImageListResponse(): CustomerImageListResponse {
    val responses = asCustomerImageResponses()
    val primaryImage = responses.find { it.isPrimary }
    val totalSize = sumOf { it.fileSize }

    return CustomerImageListResponse(
        images = responses,
        totalCount = size,
        primaryImage = primaryImage,
        totalSize = totalSize,
        formattedTotalSize = formatFileSize(totalSize)
    )
}

/**
 * Request DTO for thumbnail operations
 */
data class ThumbnailRequest(
    @field:Min(value = 50, message = "Thumbnail size must be at least 50 pixels")
    @field:Max(value = 1000, message = "Thumbnail size cannot exceed 1000 pixels")
    val size: Int? = null,

    @field:Pattern(regexp = "^(jpg|jpeg|png|webp)$", message = "Format must be jpg, jpeg, png, or webp")
    val format: String? = null
)

/**
 * Response DTO for thumbnail metadata
 */
data class ThumbnailResponse(
    val size: Int,
    val width: Int,
    val height: Int,
    val format: String,
    val contentType: String,
    val contentLength: Long,
    val formattedFileSize: String,
    val url: String?,
    val cached: Boolean,
    val lastModified: Instant?,
    val etag: String?
)

/**
 * Response DTO for available thumbnails
 */
data class ThumbnailSizesResponse(
    val availableSizes: List<Int>,
    val defaultSize: Int,
    val supportedFormats: List<String>,
    val thumbnails: List<ThumbnailResponse>
)

/**
 * Request DTO for bulk thumbnail generation
 */
data class BulkThumbnailGenerationRequest(
    @field:NotEmpty(message = "Image UIDs list cannot be empty")
    @field:Size(max = 20, message = "Cannot process more than 20 images at once")
    val imageUids: List<@NotBlank String>,

    @field:NotEmpty(message = "At least one size must be specified")
    val sizes: List<@Min(50) @Max(1000) Int> = listOf(150, 300, 500),

    @field:Pattern(regexp = "^(jpg|jpeg|png|webp)$", message = "Format must be jpg, jpeg, png, or webp")
    val format: String = "jpg"
)

/**
 * Response DTO for bulk thumbnail generation
 */
data class BulkThumbnailGenerationResponse(
    val totalImages: Int,
    val successfulImages: Int,
    val failedImages: Int,
    val totalThumbnailsGenerated: Int,
    val results: List<ImageThumbnailResult>
)

/**
 * Individual image thumbnail generation result
 */
data class ImageThumbnailResult(
    val imageUid: String,
    val success: Boolean,
    val thumbnailsGenerated: Int,
    val error: String? = null,
    val thumbnails: List<ThumbnailResponse> = emptyList()
)

/**
 * Response DTO for thumbnail cleanup operations
 */
data class ThumbnailCleanupResponse(
    val deletedThumbnails: Int,
    val totalSizeFreed: Long,
    val formattedSizeFreed: String,
    val deletedPaths: List<String>
)

/**
 * Helper function to format file size
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}