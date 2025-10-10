package com.ampairs.customer.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.customer.config.Constants
import jakarta.persistence.*
import java.time.Instant

/**
 * Customer image entity representing uploaded images for customers.
 * Images are stored in object storage (S3/MinIO) with workspace-aware paths.
 * Path format: bucket/{workspace-slug}/customer/{customer-uid}/{image-uid}.{extension}
 */
@Entity(name = "customer_image")
@Table(
    name = "customer_images",
    indexes = [
        Index(name = "idx_customer_image_customer_uid", columnList = "customer_uid"),
        Index(name = "idx_customer_image_workspace", columnList = "owner_id"),
        Index(name = "idx_customer_image_primary", columnList = "is_primary"),
        Index(name = "idx_customer_image_created", columnList = "created_at")
    ]
)
class CustomerImage : OwnableBaseDomain() {

    /**
     * Reference to the customer this image belongs to
     */
    @Column(name = "customer_uid", nullable = false, length = 36)
    var customerUid: String = ""

    /**
     * Workspace slug for object storage path construction
     */
    @Column(name = "workspace_slug", nullable = false, length = 50)
    var workspaceSlug: String = ""

    /**
     * Original filename of the uploaded image
     */
    @Column(name = "original_filename", nullable = false, length = 255)
    var originalFilename: String = ""

    /**
     * File extension (jpg, png, etc.)
     */
    @Column(name = "file_extension", nullable = false, length = 10)
    var fileExtension: String = ""

    /**
     * Content type (MIME type) of the image
     */
    @Column(name = "content_type", nullable = false, length = 50)
    var contentType: String = ""

    /**
     * File size in bytes
     */
    @Column(name = "file_size", nullable = false)
    var fileSize: Long = 0

    /**
     * Object storage path
     * Format: {workspace-slug}/customer/{customer-uid}/{image-uid}.{extension}
     */
    @Column(name = "storage_path", nullable = false, length = 500)
    var storagePath: String = ""

    /**
     * Full object storage URL (for quick access)
     */
    @Column(name = "storage_url", length = 1000)
    var storageUrl: String? = null

    /**
     * Whether this is the primary image for the customer
     */
    @Column(name = "is_primary", nullable = false)
    var isPrimary: Boolean = false

    /**
     * Display order for multiple images
     */
    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    /**
     * Alt text for accessibility
     */
    @Column(name = "alt_text", length = 255)
    var altText: String? = null

    /**
     * Image description/caption
     */
    @Column(name = "description", length = 500)
    var description: String? = null

    /**
     * Image width in pixels (for optimization)
     */
    @Column(name = "width")
    var width: Int? = null

    /**
     * Image height in pixels (for optimization)
     */
    @Column(name = "height")
    var height: Int? = null

    /**
     * Upload timestamp
     */
    @Column(name = "uploaded_at", nullable = false)
    var uploadedAt: Instant = Instant.now()

    /**
     * Whether the image is active/visible
     */
    @Column(name = "active", nullable = false)
    var active: Boolean = true

    /**
     * ETag from object storage (for caching)
     */
    @Column(name = "etag", length = 100)
    var etag: String? = null

    /**
     * Last modified timestamp from object storage
     */
    @Column(name = "last_modified")
    var lastModified: Instant? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.CUSTOMER_IMAGE_PREFIX
    }

    /**
     * Generate storage path for this image
     * Format: {workspace-slug}/customer/{customer-uid}/{image-uid}.{extension}
     */
    fun generateStoragePath(): String {
        return "${workspaceSlug}/customer/${customerUid}/${uid}.${fileExtension}"
    }

    /**
     * Get file name for object storage
     */
    fun getStorageFileName(): String {
        return "${uid}.${fileExtension}"
    }

    /**
     * Check if this is an image file based on content type
     */
    fun isImageFile(): Boolean {
        return contentType.startsWith("image/")
    }

    /**
     * Get human-readable file size
     */
    fun getFormattedFileSize(): String {
        return when {
            fileSize < 1024 -> "${fileSize} B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            fileSize < 1024 * 1024 * 1024 -> "${fileSize / (1024 * 1024)} MB"
            else -> "${fileSize / (1024 * 1024 * 1024)} GB"
        }
    }

    /**
     * Set as primary image (should be called through service to ensure only one primary)
     */
    fun setAsPrimary() {
        isPrimary = true
        displayOrder = 0
    }

    /**
     * Set as secondary image
     */
    fun setAsSecondary(order: Int) {
        isPrimary = false
        displayOrder = order
    }

    /**
     * Mark image as deleted (soft delete)
     */
    fun markAsDeleted() {
        active = false
    }

    /**
     * Update storage metadata
     */
    fun updateStorageMetadata(url: String?, etag: String?, lastModified: Instant?) {
        this.storageUrl = url
        this.etag = etag
        this.lastModified = lastModified
    }

    /**
     * Update image dimensions
     */
    fun updateDimensions(width: Int?, height: Int?) {
        this.width = width
        this.height = height
    }
}