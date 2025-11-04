package com.ampairs.file.service

import com.ampairs.file.config.StorageProperties
import com.ampairs.file.storage.ObjectStorageService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Service for managing thumbnail cache in object storage.
 */
@Service
class ThumbnailCacheService(
    private val objectStorageService: ObjectStorageService,
    private val imageResizingService: ImageResizingService,
    private val storageProperties: StorageProperties
) {

    private val logger = LoggerFactory.getLogger(ThumbnailCacheService::class.java)

    fun getThumbnail(
        bucketName: String,
        originalImagePath: String,
        size: ImageResizingService.ThumbnailSize
    ): Pair<InputStream, Boolean> {

        if (!storageProperties.image.thumbnails.enabled) {
            val originalStream = objectStorageService.downloadFile(bucketName, originalImagePath)
            return Pair(originalStream, false)
        }

        val thumbnailPath = imageResizingService.generateThumbnailPath(originalImagePath, size)

        return try {
            if (storageProperties.image.thumbnails.cacheEnabled &&
                objectStorageService.objectExists(bucketName, thumbnailPath)
            ) {
                logger.debug("Serving cached thumbnail: path={}, size={}", thumbnailPath, size)
                val cachedThumbnail = objectStorageService.downloadFile(bucketName, thumbnailPath)
                Pair(cachedThumbnail, true)
            } else {
                logger.debug("Generating thumbnail on-demand: originalPath={}, size={}", originalImagePath, size)
                generateAndCacheThumbnail(bucketName, originalImagePath, thumbnailPath, size)
            }
        } catch (e: Exception) {
            logger.error(
                "Failed to get thumbnail: originalPath={}, size={}, error={}",
                originalImagePath,
                size,
                e.message,
                e
            )

            val originalStream = objectStorageService.downloadFile(bucketName, originalImagePath)
            Pair(originalStream, false)
        }
    }

    private fun generateAndCacheThumbnail(
        bucketName: String,
        originalImagePath: String,
        thumbnailPath: String,
        size: ImageResizingService.ThumbnailSize
    ): Pair<InputStream, Boolean> {

        val originalStream = objectStorageService.downloadFile(bucketName, originalImagePath)

        val thumbnailBytes = imageResizingService.generateThumbnail(
            originalStream,
            size,
            storageProperties.image.thumbnails.format
        )

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
                logger.info(
                    "Cached thumbnail: path={}, size={}, bytes={}",
                    thumbnailPath,
                    size,
                    thumbnailBytes.size
                )
            } catch (e: Exception) {
                logger.warn(
                    "Failed to cache thumbnail, serving directly: path={}, error={}",
                    thumbnailPath,
                    e.message
                )
            }
        }

        return Pair(ByteArrayInputStream(thumbnailBytes), false)
    }

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

                results.add(
                    ThumbnailGenerationResult(
                        size = size,
                        path = thumbnailPath,
                        success = true,
                        error = null
                    )
                )

            } catch (e: Exception) {
                logger.error(
                    "Failed to pre-generate thumbnail: originalPath={}, size={}, error={}",
                    originalImagePath,
                    size,
                    e.message,
                    e
                )

                results.add(
                    ThumbnailGenerationResult(
                        size = size,
                        path = null,
                        success = false,
                        error = e.message
                    )
                )
            }
        }

        return results
    }

    fun thumbnailExists(
        bucketName: String,
        originalImagePath: String,
        size: ImageResizingService.ThumbnailSize
    ): Boolean {
        if (!storageProperties.image.thumbnails.cacheEnabled) return false

        val thumbnailPath = imageResizingService.generateThumbnailPath(originalImagePath, size)
        return objectStorageService.objectExists(bucketName, thumbnailPath)
    }

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
                logger.warn(
                    "Failed to delete thumbnail: originalPath={}, size={}, error={}",
                    originalImagePath,
                    size,
                    e.message
                )
            }
        }

        return deletedPaths
    }

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

    fun cleanupOldThumbnails(bucketName: String, olderThanDays: Int = 30): Int {
        var deletedCount = 0
        val cutoffInstant = Instant.now().minus(olderThanDays.toLong(), ChronoUnit.DAYS)

        try {
            val thumbnailObjects = objectStorageService.listObjects(bucketName, "thumbs/", 1000)

            thumbnailObjects.forEach { objectSummary ->
                if (objectSummary.lastModified?.isBefore(cutoffInstant) == true) {
                    try {
                        objectStorageService.deleteObject(bucketName, objectSummary.objectKey)
                        deletedCount++
                        logger.debug(
                            "Deleted old thumbnail: path={}, lastModified={}",
                            objectSummary.objectKey,
                            objectSummary.lastModified
                        )
                    } catch (e: Exception) {
                        logger.warn(
                            "Failed to delete old thumbnail: path={}, error={}",
                            objectSummary.objectKey,
                            e.message
                        )
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

data class ThumbnailGenerationResult(
    val size: ImageResizingService.ThumbnailSize,
    val path: String?,
    val success: Boolean,
    val error: String?
)

data class ThumbnailMetadata(
    val path: String,
    val size: ImageResizingService.ThumbnailSize,
    val contentType: String,
    val contentLength: Long,
    val lastModified: Instant?,
    val etag: String?,
    val cached: Boolean
)
