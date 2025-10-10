package com.ampairs.core.service

import com.ampairs.core.config.StorageProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Service for managing thumbnail cache in object storage
 */
@Service
class ThumbnailCacheService(
    private val objectStorageService: ObjectStorageService,
    private val imageResizingService: ImageResizingService,
    private val storageProperties: StorageProperties
) {

    private val logger = LoggerFactory.getLogger(ThumbnailCacheService::class.java)

    /**
     * Get thumbnail from cache or generate if not exists
     * @param bucketName Storage bucket name
     * @param originalImagePath Path to original image
     * @param size Thumbnail size
     * @return Pair of (InputStream, isCached)
     */
    fun getThumbnail(
        bucketName: String,
        originalImagePath: String,
        size: ImageResizingService.ThumbnailSize
    ): Pair<InputStream, Boolean> {

        if (!storageProperties.image.thumbnails.enabled) {
            // If thumbnails disabled, return original image
            val originalStream = objectStorageService.downloadFile(bucketName, originalImagePath)
            return Pair(originalStream, false)
        }

        val thumbnailPath = imageResizingService.generateThumbnailPath(originalImagePath, size)

        return try {
            // Try to get cached thumbnail first
            if (storageProperties.image.thumbnails.cacheEnabled &&
                objectStorageService.objectExists(bucketName, thumbnailPath)) {

                logger.debug("Serving cached thumbnail: path={}, size={}", thumbnailPath, size)
                val cachedThumbnail = objectStorageService.downloadFile(bucketName, thumbnailPath)
                Pair(cachedThumbnail, true)
            } else {
                // Generate thumbnail on-demand
                logger.debug("Generating thumbnail on-demand: originalPath={}, size={}", originalImagePath, size)
                generateAndCacheThumbnail(bucketName, originalImagePath, thumbnailPath, size)
            }
        } catch (e: Exception) {
            logger.error("Failed to get thumbnail: originalPath={}, size={}, error={}",
                originalImagePath, size, e.message, e)

            // Fallback to original image
            val originalStream = objectStorageService.downloadFile(bucketName, originalImagePath)
            Pair(originalStream, false)
        }
    }

    /**
     * Generate and cache thumbnail
     */
    private fun generateAndCacheThumbnail(
        bucketName: String,
        originalImagePath: String,
        thumbnailPath: String,
        size: ImageResizingService.ThumbnailSize
    ): Pair<InputStream, Boolean> {

        // Download original image
        val originalStream = objectStorageService.downloadFile(bucketName, originalImagePath)

        // Generate thumbnail
        val thumbnailBytes = imageResizingService.generateThumbnail(
            originalStream,
            size,
            storageProperties.image.thumbnails.format
        )

        // Cache thumbnail if caching is enabled
        if (storageProperties.image.thumbnails.cacheEnabled) {
            try {
                objectStorageService.uploadFile(
                    thumbnailBytes,
                    bucketName,
                    thumbnailPath,
                    "image/${storageProperties.image.thumbnails.format}",
                    mapOf(
                        "thumbnail-size" to size.pixels.toString(),
                        "original-path" to originalImagePath,
                        "generated-at" to Instant.now().toString(),
                        "cache-type" to "thumbnail"
                    )
                )
                logger.info("Cached thumbnail: path={}, size={}, bytes={}",
                    thumbnailPath, size, thumbnailBytes.size)
            } catch (e: Exception) {
                logger.warn("Failed to cache thumbnail, serving directly: path={}, error={}",
                    thumbnailPath, e.message)
            }
        }

        return Pair(ByteArrayInputStream(thumbnailBytes), false)
    }

    /**
     * Pre-generate thumbnails for an image (useful for upload processing)
     */
    fun preGenerateThumbnails(
        bucketName: String,
        originalImagePath: String,
        sizes: List<ImageResizingService.ThumbnailSize> = ImageResizingService.ThumbnailSize.values().toList()
    ): List<ThumbnailGenerationResult> {

        if (!storageProperties.image.thumbnails.enabled || !storageProperties.image.thumbnails.autoGenerate) {
            return emptyList()
        }

        val results = mutableListOf<ThumbnailGenerationResult>()

        sizes.forEach { size ->
            try {
                val thumbnailPath = imageResizingService.generateThumbnailPath(originalImagePath, size)
                generateAndCacheThumbnail(bucketName, originalImagePath, thumbnailPath, size)

                results.add(ThumbnailGenerationResult(
                    size = size,
                    path = thumbnailPath,
                    success = true,
                    error = null
                ))

            } catch (e: Exception) {
                logger.error("Failed to pre-generate thumbnail: originalPath={}, size={}, error={}",
                    originalImagePath, size, e.message, e)

                results.add(ThumbnailGenerationResult(
                    size = size,
                    path = null,
                    success = false,
                    error = e.message
                ))
            }
        }

        return results
    }

    /**
     * Check if thumbnail exists in cache
     */
    fun thumbnailExists(bucketName: String, originalImagePath: String, size: ImageResizingService.ThumbnailSize): Boolean {
        if (!storageProperties.image.thumbnails.cacheEnabled) return false

        val thumbnailPath = imageResizingService.generateThumbnailPath(originalImagePath, size)
        return objectStorageService.objectExists(bucketName, thumbnailPath)
    }

    /**
     * Delete cached thumbnails for an image
     */
    fun deleteThumbnails(bucketName: String, originalImagePath: String): List<String> {
        val deletedPaths = mutableListOf<String>()

        ImageResizingService.ThumbnailSize.values().forEach { size ->
            try {
                val thumbnailPath = imageResizingService.generateThumbnailPath(originalImagePath, size)

                if (objectStorageService.objectExists(bucketName, thumbnailPath)) {
                    objectStorageService.deleteObject(bucketName, thumbnailPath)
                    deletedPaths.add(thumbnailPath)
                    logger.debug("Deleted cached thumbnail: path={}", thumbnailPath)
                }
            } catch (e: Exception) {
                logger.warn("Failed to delete thumbnail: originalPath={}, size={}, error={}",
                    originalImagePath, size, e.message)
            }
        }

        return deletedPaths
    }

    /**
     * Get thumbnail metadata without downloading
     */
    fun getThumbnailMetadata(
        bucketName: String,
        originalImagePath: String,
        size: ImageResizingService.ThumbnailSize
    ): ThumbnailMetadata? {

        val thumbnailPath = imageResizingService.generateThumbnailPath(originalImagePath, size)

        return try {
            if (objectStorageService.objectExists(bucketName, thumbnailPath)) {
                val metadata = objectStorageService.getObjectMetadata(bucketName, thumbnailPath)
                ThumbnailMetadata(
                    path = thumbnailPath,
                    size = size,
                    contentType = metadata.contentType,
                    contentLength = metadata.contentLength,
                    lastModified = metadata.lastModified,
                    etag = metadata.etag,
                    cached = true
                )
            } else {
                null
            }
        } catch (e: Exception) {
            logger.warn("Failed to get thumbnail metadata: path={}, error={}", thumbnailPath, e.message)
            null
        }
    }

    /**
     * Clean up old cached thumbnails (utility method for maintenance)
     */
    fun cleanupOldThumbnails(bucketName: String, olderThanDays: Int = 30): Int {
        var deletedCount = 0
        val cutoffInstant = Instant.now().minus(olderThanDays.toLong(), ChronoUnit.DAYS)

        try {
            // List objects with thumbs prefix
            val thumbnailObjects = objectStorageService.listObjects(bucketName, "thumbs/", 1000)

            thumbnailObjects.forEach { objectSummary ->
                if (objectSummary.lastModified?.isBefore(cutoffInstant) == true) {
                    try {
                        objectStorageService.deleteObject(bucketName, objectSummary.objectKey)
                        deletedCount++
                        logger.debug("Deleted old thumbnail: path={}, lastModified={}",
                            objectSummary.objectKey, objectSummary.lastModified)
                    } catch (e: Exception) {
                        logger.warn("Failed to delete old thumbnail: path={}, error={}",
                            objectSummary.objectKey, e.message)
                    }
                }
            }

            if (deletedCount > 0) {
                logger.info("Cleaned up {} old thumbnails from bucket: {}", deletedCount, bucketName)
            }

        } catch (e: Exception) {
            logger.error("Failed to cleanup old thumbnails: bucket={}, error={}", bucketName, e.message, e)
        }

        return deletedCount
    }
}

/**
 * Result of thumbnail generation operation
 */
data class ThumbnailGenerationResult(
    val size: ImageResizingService.ThumbnailSize,
    val path: String?,
    val success: Boolean,
    val error: String?
)

/**
 * Thumbnail metadata information
 */
data class ThumbnailMetadata(
    val path: String,
    val size: ImageResizingService.ThumbnailSize,
    val contentType: String,
    val contentLength: Long,
    val lastModified: Instant?,
    val etag: String?,
    val cached: Boolean
)
