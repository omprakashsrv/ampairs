package com.ampairs.customer.domain.service

import com.ampairs.core.config.StorageProperties
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.service.*
import com.ampairs.customer.domain.dto.*
import com.ampairs.customer.domain.model.CustomerImage
import com.ampairs.customer.domain.repository.CustomerImageRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.LocalDateTime
import javax.imageio.ImageIO

/**
 * Service for managing customer images with object storage integration
 */
@Service
@Transactional
class CustomerImageService(
    private val customerImageRepository: CustomerImageRepository,
    private val objectStorageService: ObjectStorageService,
    private val thumbnailCacheService: ThumbnailCacheService,
    private val imageResizingService: ImageResizingService,
    private val storageProperties: StorageProperties
) {

    private val logger = LoggerFactory.getLogger(CustomerImageService::class.java)

    /**
     * Upload customer image to object storage and save metadata
     */
    fun uploadImage(
        file: MultipartFile,
        request: CustomerImageUploadRequest,
        workspaceSlug: String
    ): CustomerImageUploadResponse {
        val startTime = System.currentTimeMillis()

        return try {
            validateUploadRequest(file, request)

            // Create customer image entity
            val customerImage = createCustomerImageEntity(file, request, workspaceSlug)

            // Upload to object storage
            logger.debug("Uploading image with storage path: {}", customerImage.storagePath)
            val uploadResult = uploadToStorage(file, customerImage)

            // Update entity with storage metadata
            customerImage.updateStorageMetadata(
                url = uploadResult.url,
                etag = uploadResult.etag,
                lastModified = uploadResult.lastModified
            )

            // Extract and save image dimensions
            extractAndSaveImageDimensions(file, customerImage)

            // Handle primary image logic
            if (request.isPrimary) {
                handlePrimaryImageLogic(customerImage)
            } else {
                // Set display order if not specified
                if (request.displayOrder == null) {
                    customerImage.displayOrder = customerImageRepository.getNextDisplayOrder(request.customerUid)
                }
            }

            val savedImage = customerImageRepository.save(customerImage)
            val processingTime = System.currentTimeMillis() - startTime

            logger.info(
                "Customer image uploaded successfully: customer={}, image={}, size={}, time={}ms",
                request.customerUid, savedImage.uid, file.size, processingTime
            )

            CustomerImageUploadResponse(
                image = savedImage.asCustomerImageResponse(),
                uploadedAt = savedImage.uploadedAt,
                processingTime = processingTime
            )
        } catch (e: Exception) {
            logger.error("Failed to upload customer image: customer={}, error={}", request.customerUid, e.message, e)
            throw CustomerImageException("Failed to upload image: ${e.message}", e)
        }
    }

    /**
     * Get all images for a customer
     */
    @Transactional(readOnly = true)
    fun getCustomerImages(customerUid: String): CustomerImageListResponse {
        val images = customerImageRepository.findByCustomerUidAndActiveTrue(customerUid)
        return images.asCustomerImageListResponse()
    }

    /**
     * Get specific customer image
     */
    @Transactional(readOnly = true)
    fun getCustomerImage(customerUid: String, imageUid: String): CustomerImageResponse {
        val image = customerImageRepository.findByUidAndCustomerUid(imageUid, customerUid)
            ?: throw CustomerImageNotFoundException("Image not found: $imageUid for customer: $customerUid")

        if (!image.active) {
            throw CustomerImageNotFoundException("Image is not active: $imageUid")
        }

        return image.asCustomerImageResponse()
    }

    /**
     * Download customer image from object storage
     */
    @Transactional(readOnly = true)
    fun downloadCustomerImage(customerUid: String, imageUid: String): Pair<CustomerImage, java.io.InputStream> {
        val image = customerImageRepository.findByUidAndCustomerUid(imageUid, customerUid)
            ?: throw CustomerImageNotFoundException("Image not found: $imageUid for customer: $customerUid")

        if (!image.active) {
            throw CustomerImageNotFoundException("Image is not active: $imageUid")
        }

        try {
            val inputStream = objectStorageService.downloadFile(storageProperties.defaultBucket, image.storagePath)
            return Pair(image, inputStream)
        } catch (e: ObjectNotFoundException) {
            logger.warn("Image file not found in storage: bucket={}, path={}", storageProperties.defaultBucket, image.storagePath)
            throw CustomerImageNotFoundException("Image file not found in storage: ${image.originalFilename}")
        }
    }

    /**
     * Update customer image metadata
     */
    fun updateCustomerImage(
        customerUid: String,
        imageUid: String,
        request: CustomerImageUpdateRequest
    ): CustomerImageResponse {
        val image = customerImageRepository.findByUidAndCustomerUid(imageUid, customerUid)
            ?: throw CustomerImageNotFoundException("Image not found: $imageUid for customer: $customerUid")

        if (!image.active) {
            throw CustomerImageNotFoundException("Image is not active: $imageUid")
        }

        // Update fields
        request.altText?.let { image.altText = it }
        request.description?.let { image.description = it }
        request.displayOrder?.let { image.displayOrder = it }

        // Handle primary image logic
        request.isPrimary?.let { isPrimary ->
            if (isPrimary && !image.isPrimary) {
                handlePrimaryImageLogic(image)
            } else if (!isPrimary && image.isPrimary) {
                image.isPrimary = false
            }
        }

        val updatedImage = customerImageRepository.save(image)

        logger.info("Customer image updated: customer={}, image={}", customerUid, imageUid)

        return updatedImage.asCustomerImageResponse()
    }

    /**
     * Delete customer image
     */
    fun deleteCustomerImage(customerUid: String, imageUid: String): Boolean {
        val image = customerImageRepository.findByUidAndCustomerUid(imageUid, customerUid)
            ?: throw CustomerImageNotFoundException("Image not found: $imageUid for customer: $customerUid")

        return try {
            // Soft delete in database
            customerImageRepository.softDelete(imageUid)

            // Delete from object storage
            objectStorageService.deleteObject(storageProperties.defaultBucket, image.storagePath)

            logger.info("Customer image deleted: customer={}, image={}", customerUid, imageUid)
            true
        } catch (e: Exception) {
            logger.error("Failed to delete customer image: customer={}, image={}, error={}", customerUid, imageUid, e.message, e)
            false
        }
    }

    /**
     * Set image as primary
     */
    fun setPrimaryImage(customerUid: String, imageUid: String): CustomerImageResponse {
        val image = customerImageRepository.findByUidAndCustomerUid(imageUid, customerUid)
            ?: throw CustomerImageNotFoundException("Image not found: $imageUid for customer: $customerUid")

        if (!image.active) {
            throw CustomerImageNotFoundException("Image is not active: $imageUid")
        }

        handlePrimaryImageLogic(image)
        val updatedImage = customerImageRepository.save(image)

        logger.info("Primary image set: customer={}, image={}", customerUid, imageUid)

        return updatedImage.asCustomerImageResponse()
    }

    /**
     * Reorder customer images
     */
    fun reorderImages(customerUid: String, request: CustomerImageReorderRequest): CustomerImageListResponse {
        val imageOrders = request.imageOrders

        // Validate all images belong to the customer
        val images = imageOrders.map { orderItem ->
            customerImageRepository.findByUidAndCustomerUid(orderItem.imageUid, customerUid)
                ?: throw CustomerImageNotFoundException("Image not found: ${orderItem.imageUid} for customer: $customerUid")
        }

        // Update display orders
        imageOrders.forEach { orderItem ->
            customerImageRepository.updateDisplayOrder(orderItem.imageUid, orderItem.displayOrder)
        }

        logger.info("Customer images reordered: customer={}, count={}", customerUid, imageOrders.size)

        return getCustomerImages(customerUid)
    }

    /**
     * Bulk delete customer images
     */
    fun bulkDeleteImages(customerUid: String, request: CustomerImageBulkRequest): CustomerImageBulkResponse {
        val successfulUids = mutableListOf<String>()
        val failedUids = mutableListOf<String>()
        val errors = mutableListOf<String>()

        request.imageUids.forEach { imageUid ->
            try {
                if (deleteCustomerImage(customerUid, imageUid)) {
                    successfulUids.add(imageUid)
                } else {
                    failedUids.add(imageUid)
                    errors.add("Failed to delete image: $imageUid")
                }
            } catch (e: Exception) {
                failedUids.add(imageUid)
                errors.add("Error deleting image $imageUid: ${e.message}")
            }
        }

        return CustomerImageBulkResponse(
            successCount = successfulUids.size,
            failureCount = failedUids.size,
            successfulImageUids = successfulUids,
            failedImageUids = failedUids,
            errors = errors
        )
    }

    /**
     * Get customer image statistics
     */
    @Transactional(readOnly = true)
    fun getCustomerImageStats(customerUid: String): CustomerImageStatsResponse {
        val images = customerImageRepository.findByCustomerUidAndActiveTrue(customerUid)

        if (images.isEmpty()) {
            return CustomerImageStatsResponse(
                customerUid = customerUid,
                totalImages = 0,
                primaryImages = 0,
                totalSize = 0,
                formattedTotalSize = "0 B",
                averageSize = 0,
                largestImage = null,
                oldestImage = null,
                newestImage = null
            )
        }

        val totalSize = images.sumOf { it.fileSize }
        val largestImage = images.maxByOrNull { it.fileSize }
        val oldestImage = images.minByOrNull { it.uploadedAt }
        val newestImage = images.maxByOrNull { it.uploadedAt }

        return CustomerImageStatsResponse(
            customerUid = customerUid,
            totalImages = images.size,
            primaryImages = images.count { it.isPrimary },
            totalSize = totalSize,
            formattedTotalSize = formatFileSize(totalSize),
            averageSize = totalSize / images.size,
            largestImage = largestImage?.asCustomerImageResponse(),
            oldestImage = oldestImage?.asCustomerImageResponse(),
            newestImage = newestImage?.asCustomerImageResponse()
        )
    }

    /**
     * Get thumbnail for customer image
     */
    @Transactional(readOnly = true)
    fun getThumbnail(
        customerUid: String,
        imageUid: String,
        size: Int,
        format: String = storageProperties.image.thumbnails.format
    ): Pair<CustomerImage, InputStream> {
        val image = customerImageRepository.findByUidAndCustomerUid(imageUid, customerUid)
            ?: throw CustomerImageNotFoundException("Image not found: $imageUid for customer: $customerUid")

        if (!image.active) {
            throw CustomerImageNotFoundException("Image is not active: $imageUid")
        }

        val thumbnailSize = ImageResizingService.ThumbnailSize.fromPixels(size)
            ?: ImageResizingService.ThumbnailSize.fromPixels(storageProperties.image.thumbnails.defaultSize)
            ?: ImageResizingService.ThumbnailSize.MEDIUM

        val (thumbnailStream, isCached) = thumbnailCacheService.getThumbnail(
            storageProperties.defaultBucket,
            image.storagePath,
            thumbnailSize
        )

        logger.debug("Serving thumbnail: customer={}, image={}, size={}, cached={}",
            customerUid, imageUid, size, isCached)

        return Pair(image, thumbnailStream)
    }

    /**
     * Get available thumbnail sizes for an image
     */
    @Transactional(readOnly = true)
    fun getAvailableThumbnails(customerUid: String, imageUid: String): ThumbnailSizesResponse {
        val image = customerImageRepository.findByUidAndCustomerUid(imageUid, customerUid)
            ?: throw CustomerImageNotFoundException("Image not found: $imageUid for customer: $customerUid")

        val availableThumbnails = mutableListOf<ThumbnailResponse>()

        ImageResizingService.ThumbnailSize.values().forEach { size ->
            val thumbnailMetadata = thumbnailCacheService.getThumbnailMetadata(
                storageProperties.defaultBucket,
                image.storagePath,
                size
            )

            if (thumbnailMetadata != null) {
                availableThumbnails.add(ThumbnailResponse(
                    size = size.pixels,
                    width = calculateThumbnailDimension(image.width ?: 0, image.height ?: 0, size.pixels).first,
                    height = calculateThumbnailDimension(image.width ?: 0, image.height ?: 0, size.pixels).second,
                    format = storageProperties.image.thumbnails.format,
                    contentType = thumbnailMetadata.contentType,
                    contentLength = thumbnailMetadata.contentLength,
                    formattedFileSize = formatFileSize(thumbnailMetadata.contentLength),
                    url = null, // Will be set by controller
                    cached = thumbnailMetadata.cached,
                    lastModified = thumbnailMetadata.lastModified,
                    etag = thumbnailMetadata.etag
                ))
            }
        }

        return ThumbnailSizesResponse(
            availableSizes = storageProperties.image.thumbnails.supportedSizes,
            defaultSize = storageProperties.image.thumbnails.defaultSize,
            supportedFormats = listOf("jpg", "jpeg", "png", "webp"),
            thumbnails = availableThumbnails
        )
    }

    /**
     * Generate thumbnails for customer image
     */
    fun generateThumbnails(customerUid: String, imageUid: String, sizes: List<Int>? = null): BulkThumbnailGenerationResponse {
        val image = customerImageRepository.findByUidAndCustomerUid(imageUid, customerUid)
            ?: throw CustomerImageNotFoundException("Image not found: $imageUid for customer: $customerUid")

        val targetSizes = sizes?.mapNotNull { ImageResizingService.ThumbnailSize.fromPixels(it) }
            ?: ImageResizingService.ThumbnailSize.values().toList()

        val results = thumbnailCacheService.preGenerateThumbnails(
            storageProperties.defaultBucket,
            image.storagePath,
            targetSizes
        )

        val thumbnailResponses = results.filter { it.success }.map { result ->
            ThumbnailResponse(
                size = result.size.pixels,
                width = calculateThumbnailDimension(image.width ?: 0, image.height ?: 0, result.size.pixels).first,
                height = calculateThumbnailDimension(image.width ?: 0, image.height ?: 0, result.size.pixels).second,
                format = storageProperties.image.thumbnails.format,
                contentType = "image/${storageProperties.image.thumbnails.format}",
                contentLength = 0, // Would need to be calculated
                formattedFileSize = "Unknown",
                url = null,
                cached = true,
                lastModified = LocalDateTime.now(),
                etag = null
            )
        }

        return BulkThumbnailGenerationResponse(
            totalImages = 1,
            successfulImages = if (results.any { it.success }) 1 else 0,
            failedImages = if (results.any { !it.success }) 1 else 0,
            totalThumbnailsGenerated = results.count { it.success },
            results = listOf(ImageThumbnailResult(
                imageUid = imageUid,
                success = results.any { it.success },
                thumbnailsGenerated = results.count { it.success },
                error = results.firstOrNull { !it.success }?.error,
                thumbnails = thumbnailResponses
            ))
        )
    }

    /**
     * Bulk generate thumbnails for multiple images
     */
    fun bulkGenerateThumbnails(customerUid: String, request: BulkThumbnailGenerationRequest): BulkThumbnailGenerationResponse {
        val results = mutableListOf<ImageThumbnailResult>()
        var totalThumbnailsGenerated = 0

        request.imageUids.forEach { imageUid ->
            try {
                val response = generateThumbnails(customerUid, imageUid, request.sizes)
                results.addAll(response.results)
                totalThumbnailsGenerated += response.totalThumbnailsGenerated
            } catch (e: Exception) {
                logger.error("Failed to generate thumbnails for image: customer={}, image={}, error={}",
                    customerUid, imageUid, e.message)
                results.add(ImageThumbnailResult(
                    imageUid = imageUid,
                    success = false,
                    thumbnailsGenerated = 0,
                    error = e.message
                ))
            }
        }

        return BulkThumbnailGenerationResponse(
            totalImages = request.imageUids.size,
            successfulImages = results.count { it.success },
            failedImages = results.count { !it.success },
            totalThumbnailsGenerated = totalThumbnailsGenerated,
            results = results
        )
    }

    /**
     * Delete thumbnails for customer image
     */
    fun deleteThumbnails(customerUid: String, imageUid: String): ThumbnailCleanupResponse {
        val image = customerImageRepository.findByUidAndCustomerUid(imageUid, customerUid)
            ?: throw CustomerImageNotFoundException("Image not found: $imageUid for customer: $customerUid")

        val deletedPaths = thumbnailCacheService.deleteThumbnails(
            storageProperties.defaultBucket,
            image.storagePath
        )

        return ThumbnailCleanupResponse(
            deletedThumbnails = deletedPaths.size,
            totalSizeFreed = 0, // Would need to be calculated before deletion
            formattedSizeFreed = "Unknown",
            deletedPaths = deletedPaths
        )
    }

    private fun calculateThumbnailDimension(originalWidth: Int, originalHeight: Int, targetSize: Int): Pair<Int, Int> {
        if (originalWidth == 0 || originalHeight == 0) {
            return Pair(targetSize, targetSize)
        }

        val aspectRatio = originalWidth.toDouble() / originalHeight.toDouble()
        return if (originalWidth > originalHeight) {
            val width = targetSize
            val height = (targetSize / aspectRatio).toInt()
            Pair(width, height)
        } else {
            val height = targetSize
            val width = (targetSize * aspectRatio).toInt()
            Pair(width, height)
        }
    }

    private fun validateUploadRequest(file: MultipartFile, request: CustomerImageUploadRequest) {
        if (file.isEmpty) {
            throw CustomerImageException("File cannot be empty")
        }

        val contentType = file.contentType ?: "application/octet-stream"
        if (!storageProperties.allowedContentTypes.contains(contentType)) {
            throw CustomerImageException("Content type not allowed: $contentType")
        }

        if (!contentType.startsWith("image/")) {
            throw CustomerImageException("File must be an image")
        }

        val maxSize = parseSize(storageProperties.maxFileSize)
        if (file.size > maxSize) {
            throw CustomerImageException("File size exceeds maximum allowed: ${formatFileSize(maxSize)}")
        }
    }

    private fun createCustomerImageEntity(
        file: MultipartFile,
        request: CustomerImageUploadRequest,
        workspaceSlug: String
    ): CustomerImage {
        val fileExtension = getFileExtension(file.originalFilename ?: "image", file.contentType)

        return CustomerImage().apply {
            // Use provided uid or generate a new one
            request.uid?.let { uid = it }
            customerUid = request.customerUid
            this.workspaceSlug = workspaceSlug
            originalFilename = file.originalFilename ?: "image.$fileExtension"
            this.fileExtension = fileExtension
            contentType = file.contentType ?: "application/octet-stream"
            fileSize = file.size
            storagePath = generateStoragePath()
            isPrimary = request.isPrimary
            displayOrder = request.displayOrder ?: 0
            altText = request.altText
            description = request.description
            uploadedAt = LocalDateTime.now()
            active = true
            // ownerId will be set by @TenantId
        }
    }

    private fun uploadToStorage(file: MultipartFile, customerImage: CustomerImage) =
        objectStorageService.uploadFile(
            file.inputStream,
            storageProperties.defaultBucket,
            customerImage.storagePath,
            customerImage.contentType,
            customerImage.fileSize,
            mapOf(
                "customer-uid" to customerImage.customerUid,
                "workspace-slug" to customerImage.workspaceSlug,
                "original-filename" to customerImage.originalFilename,
                "upload-timestamp" to customerImage.uploadedAt.toString()
            )
        )

    private fun extractAndSaveImageDimensions(file: MultipartFile, customerImage: CustomerImage) {
        try {
            val bufferedImage: BufferedImage = ImageIO.read(ByteArrayInputStream(file.bytes))
            customerImage.updateDimensions(bufferedImage.width, bufferedImage.height)
        } catch (e: Exception) {
            logger.warn("Failed to extract image dimensions: {}", e.message)
        }
    }

    private fun handlePrimaryImageLogic(newPrimaryImage: CustomerImage) {
        // Clear existing primary status for customer
        customerImageRepository.clearPrimaryStatus(newPrimaryImage.customerUid)
        // Set new primary
        newPrimaryImage.setAsPrimary()
    }

    private fun getFileExtension(filename: String, contentType: String? = null): String {
        val lastDotIndex = filename.lastIndexOf('.')
        return if (lastDotIndex > 0 && lastDotIndex < filename.length - 1) {
            filename.substring(lastDotIndex + 1).lowercase()
        } else {
            // Try to determine extension from content type
            when (contentType?.lowercase()) {
                "image/jpeg", "image/jpg" -> "jpg"
                "image/png" -> "png"
                "image/gif" -> "gif"
                "image/webp" -> "webp"
                "image/bmp" -> "bmp"
                "image/tiff" -> "tiff"
                else -> "jpg" // Default to jpg for images without extension
            }
        }
    }

    private fun parseSize(sizeStr: String): Long {
        val size = sizeStr.uppercase()
        return when {
            size.endsWith("KB") -> size.dropLast(2).toLong() * 1024
            size.endsWith("MB") -> size.dropLast(2).toLong() * 1024 * 1024
            size.endsWith("GB") -> size.dropLast(2).toLong() * 1024 * 1024 * 1024
            else -> size.toLong()
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes} B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}

/**
 * Custom exceptions for customer image operations
 */
class CustomerImageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class CustomerImageNotFoundException(message: String) : RuntimeException(message)