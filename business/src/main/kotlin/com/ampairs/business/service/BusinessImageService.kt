package com.ampairs.business.service

import com.ampairs.business.exception.BusinessImageNotFoundException
import com.ampairs.business.exception.BusinessImageValidationException
import com.ampairs.business.model.Business
import com.ampairs.business.model.BusinessImage
import com.ampairs.business.model.BusinessImageType
import com.ampairs.business.repository.BusinessImageRepository
import com.ampairs.business.repository.BusinessRepository
import com.ampairs.file.config.StorageProperties
import com.ampairs.file.service.ImageResizingService
import com.ampairs.file.storage.ObjectStorageService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.time.Instant

/**
 * Service for handling business logo and gallery image operations.
 * Handles validation, resizing, thumbnail generation, and S3 storage.
 */
@Service
class BusinessImageService(
    private val objectStorageService: ObjectStorageService,
    private val imageResizingService: ImageResizingService,
    private val storageProperties: StorageProperties,
    private val businessRepository: BusinessRepository,
    private val businessImageRepository: BusinessImageRepository,
) {

    private val logger = LoggerFactory.getLogger(BusinessImageService::class.java)

    companion object {
        private const val LOGO_FOLDER = "business-logos"
        private const val IMAGES_FOLDER = "business-images"
        private const val LOGO_FULL_SIZE_MAX = 512
        private const val LOGO_THUMBNAIL_SIZE = 256
        private const val IMAGE_FULL_SIZE_MAX = 1920
        private const val IMAGE_THUMBNAIL_SIZE = 400
        private const val MAX_FILE_SIZE = 10L * 1024 * 1024 // 10MB
        private const val MAX_IMAGES_PER_BUSINESS = 20

        private val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp"
        )
    }

    // ==================== Logo Operations ====================

    /**
     * Upload and process a logo for a business.
     * Generates both a full-size image (512x512 max) and a thumbnail (256x256).
     */
    @Transactional
    fun uploadLogo(business: Business, file: MultipartFile): Business {
        validateImage(file)

        val fileBytes = file.bytes
        val contentType = file.contentType ?: "image/jpeg"
        val extension = getExtensionFromContentType(contentType)

        val timestamp = System.currentTimeMillis()
        val fullSizeKey = "$LOGO_FOLDER/${business.uid}/logo_$timestamp.$extension"
        val thumbnailKey = "$LOGO_FOLDER/${business.uid}/logo_thumb_$timestamp.$extension"

        // Resize full-size image if needed
        val resizedBytes = resizeIfNeeded(fileBytes, LOGO_FULL_SIZE_MAX)

        // Generate thumbnail
        val thumbnailBytes = generateThumbnail(fileBytes, LOGO_THUMBNAIL_SIZE)

        // Delete old logos if they exist
        deleteOldLogo(business)

        // Upload both images
        uploadImage(fullSizeKey, resizedBytes, contentType)
        uploadImage(thumbnailKey, thumbnailBytes, contentType)

        // Store object keys
        business.logoUrl = fullSizeKey
        business.logoThumbnailUrl = thumbnailKey
        business.logoUpdatedAt = Instant.now()

        val savedBusiness = businessRepository.save(business)

        logger.info("Logo uploaded for business {}: full={}, thumbnail={}",
            business.uid, business.logoUrl, business.logoThumbnailUrl)

        return savedBusiness
    }

    /**
     * Get the logo bytes for a business.
     */
    fun getLogo(objectKey: String): ByteArray {
        return try {
            objectStorageService.downloadFile(
                bucketName = storageProperties.defaultBucket,
                objectKey = objectKey
            ).readBytes()
        } catch (e: Exception) {
            logger.error("Failed to download logo: {}", objectKey, e)
            throw BusinessImageNotFoundException("Logo not found")
        }
    }

    /**
     * Delete a business's logo.
     */
    @Transactional
    fun deleteLogo(business: Business): Business {
        deleteOldLogo(business)

        business.logoUrl = null
        business.logoThumbnailUrl = null
        business.logoUpdatedAt = Instant.now()

        return businessRepository.save(business)
    }

    // ==================== Gallery Image Operations ====================

    /**
     * Upload a gallery image for a business.
     */
    @Transactional
    fun uploadImage(
        business: Business,
        file: MultipartFile,
        imageType: BusinessImageType = BusinessImageType.GALLERY,
        title: String? = null,
        description: String? = null,
        altText: String? = null,
        isPrimary: Boolean = false,
        uploadedBy: String? = null
    ): BusinessImage {
        validateImage(file)

        // Check max images limit
        val currentCount = businessImageRepository.countByBusinessIdAndActive(business.uid, true)
        if (currentCount >= MAX_IMAGES_PER_BUSINESS) {
            throw BusinessImageValidationException("Maximum $MAX_IMAGES_PER_BUSINESS images allowed per business")
        }

        val fileBytes = file.bytes
        val contentType = file.contentType ?: "image/jpeg"
        val extension = getExtensionFromContentType(contentType)

        val timestamp = System.currentTimeMillis()
        val fullSizeKey = "$IMAGES_FOLDER/${business.uid}/img_$timestamp.$extension"
        val thumbnailKey = "$IMAGES_FOLDER/${business.uid}/img_thumb_$timestamp.$extension"

        // Resize full-size image if needed
        val resizedBytes = resizeIfNeeded(fileBytes, IMAGE_FULL_SIZE_MAX)

        // Generate thumbnail
        val thumbnailBytes = generateThumbnail(fileBytes, IMAGE_THUMBNAIL_SIZE)

        // Get image dimensions
        val dimensions = getImageDimensions(fileBytes)

        // Upload both images
        uploadImage(fullSizeKey, resizedBytes, contentType)
        uploadImage(thumbnailKey, thumbnailBytes, contentType)

        // Get next display order
        val displayOrder = businessImageRepository.getMaxDisplayOrder(business.uid) + 1

        // If setting as primary, clear other primary flags
        if (isPrimary) {
            businessImageRepository.clearAllPrimaryFlags(business.uid)
        }

        // Create image entity
        val businessImage = BusinessImage().apply {
            this.ownerId = business.ownerId
            this.businessId = business.uid
            this.imageType = imageType
            this.imageUrl = fullSizeKey
            this.thumbnailUrl = thumbnailKey
            this.title = title
            this.description = description
            this.altText = altText ?: title
            this.displayOrder = displayOrder
            this.isPrimary = isPrimary
            this.active = true
            this.originalFilename = file.originalFilename
            this.fileSize = file.size
            this.width = dimensions?.first
            this.height = dimensions?.second
            this.contentType = contentType
            this.uploadedBy = uploadedBy
            this.uploadedAt = Instant.now()
        }

        val savedImage = businessImageRepository.save(businessImage)

        logger.info("Image uploaded for business {}: uid={}, type={}",
            business.uid, savedImage.uid, imageType)

        return savedImage
    }

    /**
     * Get all images for a business.
     */
    fun getBusinessImages(businessId: String, active: Boolean = true): List<BusinessImage> {
        return businessImageRepository.findByBusinessIdAndActiveOrderByDisplayOrderAsc(businessId, active)
    }

    /**
     * Get images for a business with pagination.
     */
    fun getBusinessImages(businessId: String, active: Boolean = true, pageable: Pageable): Page<BusinessImage> {
        return businessImageRepository.findByBusinessIdAndActive(businessId, active, pageable)
    }

    /**
     * Get images by type.
     */
    fun getBusinessImagesByType(businessId: String, imageType: BusinessImageType): List<BusinessImage> {
        return businessImageRepository.findByBusinessIdAndImageTypeAndActiveOrderByDisplayOrderAsc(
            businessId, imageType, true)
    }

    /**
     * Get the primary image for a business.
     */
    fun getPrimaryImage(businessId: String): BusinessImage? {
        return businessImageRepository.findByBusinessIdAndIsPrimaryTrueAndActiveTrue(businessId)
    }

    /**
     * Get image by UID.
     */
    fun getImageByUid(uid: String): BusinessImage {
        return businessImageRepository.findByUid(uid)
            ?: throw BusinessImageNotFoundException("Image not found: $uid")
    }

    /**
     * Get image bytes.
     */
    fun getImageBytes(objectKey: String): ByteArray {
        return try {
            objectStorageService.downloadFile(
                bucketName = storageProperties.defaultBucket,
                objectKey = objectKey
            ).readBytes()
        } catch (e: Exception) {
            logger.error("Failed to download image: {}", objectKey, e)
            throw BusinessImageNotFoundException("Image not found")
        }
    }

    /**
     * Update image metadata.
     */
    @Transactional
    fun updateImage(
        imageUid: String,
        title: String? = null,
        description: String? = null,
        altText: String? = null,
        imageType: BusinessImageType? = null
    ): BusinessImage {
        val image = getImageByUid(imageUid)

        title?.let { image.title = it }
        description?.let { image.description = it }
        altText?.let { image.altText = it }
        imageType?.let { image.imageType = it }

        return businessImageRepository.save(image)
    }

    /**
     * Set an image as primary.
     */
    @Transactional
    fun setAsPrimary(imageUid: String): BusinessImage {
        val image = getImageByUid(imageUid)

        // Clear other primary flags
        businessImageRepository.clearPrimaryFlagExcept(image.businessId, imageUid)

        // Set this image as primary
        image.isPrimary = true

        return businessImageRepository.save(image)
    }

    /**
     * Reorder images.
     */
    @Transactional
    fun reorderImages(businessId: String, imageUids: List<String>) {
        imageUids.forEachIndexed { index, uid ->
            val image = businessImageRepository.findByUid(uid)
            if (image != null && image.businessId == businessId) {
                image.displayOrder = index
                businessImageRepository.save(image)
            }
        }
    }

    /**
     * Delete an image (soft delete).
     */
    @Transactional
    fun deleteImage(imageUid: String): String {
        val image = getImageByUid(imageUid)

        // Delete from S3
        deleteFromStorage(image.imageUrl)
        image.thumbnailUrl?.let { deleteFromStorage(it) }

        // Hard delete from database
        businessImageRepository.delete(image)

        logger.info("Image deleted: {}", imageUid)
        return "Image deleted successfully"
    }

    /**
     * Delete all images for a business.
     */
    @Transactional
    fun deleteAllImages(businessId: String) {
        val images = businessImageRepository.findByBusinessIdAndActiveOrderByDisplayOrderAsc(businessId, true)

        images.forEach { image ->
            deleteFromStorage(image.imageUrl)
            image.thumbnailUrl?.let { deleteFromStorage(it) }
        }

        businessImageRepository.deleteByBusinessId(businessId)
        logger.info("All images deleted for business: {}", businessId)
    }

    // ==================== Private Helper Methods ====================

    private fun validateImage(file: MultipartFile) {
        if (file.isEmpty) {
            throw BusinessImageValidationException("File is empty")
        }

        if (file.size > MAX_FILE_SIZE) {
            throw BusinessImageValidationException("File size exceeds 10MB limit")
        }

        val contentType = file.contentType
        if (contentType == null || contentType !in ALLOWED_CONTENT_TYPES) {
            throw BusinessImageValidationException("Invalid file type. Allowed: JPEG, PNG, WebP")
        }

        // Validate file signature (magic bytes)
        val fileBytes = file.bytes
        if (!isValidImageSignature(fileBytes, contentType)) {
            throw BusinessImageValidationException("File content doesn't match declared type")
        }

        // Verify the image can be read and decoded
        try {
            val inputStream = ByteArrayInputStream(fileBytes)
            val image = javax.imageio.ImageIO.read(inputStream)
            if (image == null) {
                throw BusinessImageValidationException("Invalid or corrupted image file")
            }
            image.flush()
        } catch (e: Exception) {
            logger.warn("Failed to read image file: {}", e.message)
            throw BusinessImageValidationException("Unable to process image file")
        }
    }

    private fun isValidImageSignature(bytes: ByteArray, contentType: String): Boolean {
        if (bytes.size < 8) return false

        return when (contentType) {
            "image/jpeg" -> {
                bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
            }
            "image/png" -> {
                bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() &&
                bytes[4] == 0x0D.toByte() && bytes[5] == 0x0A.toByte() &&
                bytes[6] == 0x1A.toByte() && bytes[7] == 0x0A.toByte()
            }
            "image/webp" -> {
                bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() &&
                bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() &&
                bytes.size >= 12 &&
                bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() &&
                bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte()
            }
            else -> false
        }
    }

    private fun resizeIfNeeded(imageBytes: ByteArray, maxSize: Int): ByteArray {
        return try {
            val dimensions = imageResizingService.getImageDimensions(ByteArrayInputStream(imageBytes))

            if (dimensions != null && (dimensions.first > maxSize || dimensions.second > maxSize)) {
                imageResizingService.resizeImage(
                    inputStream = ByteArrayInputStream(imageBytes),
                    width = maxSize,
                    height = maxSize,
                    maintainAspectRatio = true,
                    format = "jpg"
                )
            } else {
                imageBytes
            }
        } catch (e: Exception) {
            logger.warn("Failed to resize image, using original: {}", e.message)
            imageBytes
        }
    }

    private fun generateThumbnail(imageBytes: ByteArray, size: Int): ByteArray {
        return try {
            imageResizingService.resizeImage(
                inputStream = ByteArrayInputStream(imageBytes),
                width = size,
                height = size,
                maintainAspectRatio = true,
                format = "jpg"
            )
        } catch (e: Exception) {
            logger.warn("Failed to generate thumbnail, using resized original: {}", e.message)
            resizeIfNeeded(imageBytes, size)
        }
    }

    private fun getImageDimensions(imageBytes: ByteArray): Pair<Int, Int>? {
        return try {
            imageResizingService.getImageDimensions(ByteArrayInputStream(imageBytes))
        } catch (e: Exception) {
            logger.warn("Failed to get image dimensions: {}", e.message)
            null
        }
    }

    private fun uploadImage(objectKey: String, bytes: ByteArray, contentType: String) {
        objectStorageService.uploadFile(
            bytes = bytes,
            bucketName = storageProperties.defaultBucket,
            objectKey = objectKey,
            contentType = contentType,
            metadata = mapOf(
                "Content-Disposition" to "inline",
                "Cache-Control" to "max-age=31536000"
            )
        )
    }

    private fun deleteOldLogo(business: Business) {
        try {
            val prefix = "$LOGO_FOLDER/${business.uid}/"
            val existingFiles = objectStorageService.listObjects(
                bucketName = storageProperties.defaultBucket,
                prefix = prefix
            )

            existingFiles.forEach { file ->
                try {
                    objectStorageService.deleteObject(
                        bucketName = storageProperties.defaultBucket,
                        objectKey = file.objectKey
                    )
                    logger.debug("Deleted old logo: {}", file.objectKey)
                } catch (e: Exception) {
                    logger.warn("Failed to delete old logo: {}", file.objectKey, e)
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to list/delete old logos for business {}: {}", business.uid, e.message)
        }
    }

    private fun deleteFromStorage(objectKey: String) {
        try {
            objectStorageService.deleteObject(
                bucketName = storageProperties.defaultBucket,
                objectKey = objectKey
            )
        } catch (e: Exception) {
            logger.warn("Failed to delete from storage: {}", objectKey, e)
        }
    }

    private fun getExtensionFromContentType(contentType: String): String {
        return when (contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
    }
}
