package com.ampairs.customer.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.customer.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

/**
 * Customer image entity representing uploaded images for customers.
 * Images are stored in object storage (S3/MinIO) with workspace-aware paths.
 * Path format: bucket/{workspace-slug}/customer/{customer-uid}/{image-uid}.{extension}
 */
@Entity(name = "customer_image")
@Table(
    name = "customer_image",
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
     * Workspace slug for this image
     */
    @Column(name = "workspace_slug", length = 100)
    var workspaceSlug: String = ""

    /**
     * Original filename
     */
    @Column(name = "original_filename", length = 500)
    var originalFilename: String = ""

    /**
     * File extension (e.g., jpg, png)
     */
    @Column(name = "file_extension", length = 20)
    var fileExtension: String = ""

    /**
     * Content type/MIME type
     */
    @Column(name = "content_type", length = 100)
    var contentType: String = ""

    /**
     * File size in bytes
     */
    @Column(name = "file_size", nullable = false)
    var fileSize: Long = 0

    /**
     * Object storage path (relative path in bucket)
     */
    @Column(name = "storage_path", length = 1000)
    var storagePath: String = ""

    /**
     * Full object storage URL (for quick access)
     */
    @Column(name = "storage_url", length = 1000)
    var storageUrl: String? = null

    /**
     * Image metadata stored as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "json")
    var metadata: ImageMetadata = ImageMetadata()

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
     * Image description/caption
     */
    @Column(name = "description", length = 500)
    var description: String? = null

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

    override fun obtainSeqIdPrefix(): String {
        return Constants.CUSTOMER_IMAGE_PREFIX
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
     * Update storage metadata after upload
     */
    fun updateStorageMetadata(url: String?, etag: String?, lastModified: Instant?) {
        this.storageUrl = url
        this.metadata = this.metadata.copy(
            etag = etag,
            lastModified = lastModified
        )
    }

    /**
     * Update image dimensions
     */
    fun updateDimensions(width: Int, height: Int) {
        this.metadata = this.metadata.copy(
            width = width,
            height = height
        )
    }

    /**
     * Generate storage path for this image
     */
    fun generateStoragePath(workspaceSlug: String, customerUid: String): String {
        return "$workspaceSlug/customer/$customerUid/${this.uid}.${this.fileExtension}"
    }

    /**
     * Get formatted file size (e.g., "1.5 MB")
     */
    fun getFormattedFileSize(): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            fileSize >= gb -> "%.2f GB".format(fileSize / gb)
            fileSize >= mb -> "%.2f MB".format(fileSize / mb)
            fileSize >= kb -> "%.2f KB".format(fileSize / kb)
            else -> "$fileSize bytes"
        }
    }
}

/**
 * Image metadata stored as JSON
 */
data class ImageMetadata(
    val etag: String? = null,
    val lastModified: Instant? = null,
    val width: Int? = null,
    val height: Int? = null,
    val thumbnailsGenerated: Boolean = false,
    val additionalProperties: Map<String, String> = emptyMap()
)