package com.ampairs.business.model

import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.*
import java.time.Instant

/**
 * Entity representing business images (gallery).
 *
 * **Features**:
 * - Multiple images per business
 * - Support for different image types (GALLERY, STOREFRONT, PRODUCT_SHOWCASE, etc.)
 * - Ordering support via displayOrder field
 * - Primary image designation
 *
 * **Multi-Tenancy**:
 * - Inherits ownerId from OwnableBaseDomain (with @TenantId for automatic filtering)
 * - ownerId represents the workspace this image belongs to
 */
@Entity
@Table(
    name = "business_images",
    indexes = [
        Index(name = "idx_business_image_business_id", columnList = "business_id"),
        Index(name = "idx_business_image_type", columnList = "image_type"),
        Index(name = "idx_business_image_display_order", columnList = "display_order"),
        Index(name = "idx_business_image_is_primary", columnList = "is_primary")
    ]
)
class BusinessImage : OwnableBaseDomain() {

    /**
     * Reference to the parent business
     */
    @Column(name = "business_id", nullable = false, length = 36)
    var businessId: String = ""

    /**
     * Type/category of the image
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 30)
    var imageType: BusinessImageType = BusinessImageType.GALLERY

    /**
     * S3 object key for the full-size image
     */
    @Column(name = "image_url", nullable = false, length = 500)
    var imageUrl: String = ""

    /**
     * S3 object key for the thumbnail image
     */
    @Column(name = "thumbnail_url", length = 500)
    var thumbnailUrl: String? = null

    /**
     * Optional title/caption for the image
     */
    @Column(name = "title", length = 255)
    var title: String? = null

    /**
     * Optional description for the image
     */
    @Column(name = "description", length = 1000)
    var description: String? = null

    /**
     * Alt text for accessibility
     */
    @Column(name = "alt_text", length = 255)
    var altText: String? = null

    /**
     * Display order for sorting (lower numbers first)
     */
    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    /**
     * Whether this is the primary/featured image
     */
    @Column(name = "is_primary", nullable = false)
    var isPrimary: Boolean = false

    /**
     * Whether the image is active/visible
     */
    @Column(name = "active", nullable = false)
    var active: Boolean = true

    /**
     * Original filename (for reference)
     */
    @Column(name = "original_filename", length = 255)
    var originalFilename: String? = null

    /**
     * File size in bytes
     */
    @Column(name = "file_size")
    var fileSize: Long? = null

    /**
     * Image width in pixels
     */
    @Column(name = "width")
    var width: Int? = null

    /**
     * Image height in pixels
     */
    @Column(name = "height")
    var height: Int? = null

    /**
     * Content type (e.g., image/jpeg, image/png)
     */
    @Column(name = "content_type", length = 50)
    var contentType: String? = null

    /**
     * User who uploaded this image
     */
    @Column(name = "uploaded_by", length = 36)
    var uploadedBy: String? = null

    /**
     * Timestamp when image was uploaded
     */
    @Column(name = "uploaded_at")
    var uploadedAt: Instant = Instant.now()

    override fun obtainSeqIdPrefix(): String {
        return Constants.BUSINESS_IMAGE_PREFIX
    }

    override fun toString(): String {
        return "BusinessImage(uid='$uid', businessId='$businessId', type=${imageType.name}, isPrimary=$isPrimary)"
    }
}

/**
 * Types of business images
 */
enum class BusinessImageType(val description: String) {
    GALLERY("General gallery image"),
    STOREFRONT("Storefront or exterior image"),
    INTERIOR("Interior or workspace image"),
    PRODUCT_SHOWCASE("Product showcase image"),
    TEAM("Team or staff image"),
    BANNER("Banner or promotional image"),
    CERTIFICATE("Certificate or award image"),
    OTHER("Other image type");

    companion object {
        fun fromString(value: String): BusinessImageType {
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown business image type: $value")
        }
    }
}
