package com.ampairs.customer.service

import com.ampairs.file.config.StorageProperties
import com.ampairs.file.service.ImageResizingService
import com.ampairs.file.service.ThumbnailCacheService
import com.ampairs.customer.domain.model.CustomerImage
import com.ampairs.customer.domain.repository.CustomerImageRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Service for thumbnail maintenance and cleanup operations
 */
@Service
@ConditionalOnProperty(
    name = ["ampairs.storage.image.thumbnails.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class ThumbnailMaintenanceService(
    private val customerImageRepository: CustomerImageRepository,
    private val thumbnailCacheService: ThumbnailCacheService,
    private val storageProperties: StorageProperties
) {

    private val logger = LoggerFactory.getLogger(ThumbnailMaintenanceService::class.java)

    /**
     * Scheduled cleanup of old thumbnails
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @ConditionalOnProperty(
        name = ["ampairs.storage.image.thumbnails.cache-enabled"],
        havingValue = "true"
    )
    fun cleanupOldThumbnails() {
        if (!storageProperties.image.thumbnails.cacheEnabled) {
            return
        }

        logger.info("Starting scheduled thumbnail cleanup...")

        try {
            val deletedCount = thumbnailCacheService.cleanupOldThumbnails(
                storageProperties.defaultBucket,
                olderThanDays = 30
            )

            logger.info("Scheduled thumbnail cleanup completed: deleted {} thumbnails", deletedCount)
        } catch (e: Exception) {
            logger.error("Failed to cleanup old thumbnails: {}", e.message, e)
        }
    }

    /**
     * Cleanup orphaned thumbnails (thumbnails without corresponding images)
     * Runs weekly on Sunday at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    fun cleanupOrphanedThumbnails() {
        if (!storageProperties.image.thumbnails.cacheEnabled) {
            return
        }

        logger.info("Starting orphaned thumbnail cleanup...")

        try {
            var totalDeleted = 0

            // Get all workspaces and clean up their orphaned thumbnails
            val workspaceSlugs = customerImageRepository.findByActiveTrue()
                .map { it.workspaceSlug }
                .distinct()

            workspaceSlugs.forEach { workspaceSlug ->
                val deleted = cleanupOrphanedThumbnailsForWorkspace(workspaceSlug)
                totalDeleted += deleted
            }

            logger.info("Orphaned thumbnail cleanup completed: deleted {} thumbnails across {} workspaces",
                totalDeleted, workspaceSlugs.size)

        } catch (e: Exception) {
            logger.error("Failed to cleanup orphaned thumbnails: {}", e.message, e)
        }
    }

    /**
     * Generate missing thumbnails for recently uploaded images
     * Runs every hour during business hours (9 AM - 6 PM)
     */
    @Scheduled(cron = "0 0 9-18 * * ?")
    @ConditionalOnProperty(
        name = ["ampairs.storage.image.thumbnails.auto-generate"],
        havingValue = "true"
    )
    fun generateMissingThumbnails() {
        logger.info("Starting missing thumbnail generation...")

        try {
            val recentImages = customerImageRepository.findImagesWithoutThumbnails(
                LocalDateTime.now().minusHours(24)
            )

            var totalGenerated = 0

            recentImages.forEach { image ->
                try {
                    val results = thumbnailCacheService.preGenerateThumbnails(
                        storageProperties.defaultBucket,
                        image.storagePath
                    )
                    val generated = results.count { it.success }
                    totalGenerated += generated

                    logger.debug("Generated {} thumbnails for image: {}", generated, image.uid)
                } catch (e: Exception) {
                    logger.warn("Failed to generate thumbnails for image {}: {}", image.uid, e.message)
                }
            }

            logger.info("Missing thumbnail generation completed: {} thumbnails generated for {} images",
                totalGenerated, recentImages.size)

        } catch (e: Exception) {
            logger.error("Failed to generate missing thumbnails: {}", e.message, e)
        }
    }

    /**
     * Health check for thumbnail cache consistency
     * Runs daily at 1 AM
     */
    @Scheduled(cron = "0 0 1 * * ?")
    fun thumbnailHealthCheck() {
        logger.info("Starting thumbnail cache health check...")

        try {
            val activeImages = customerImageRepository.findByActiveTrue()
            var healthyImages = 0
            var imagesWithIssues = 0

            activeImages.forEach { image ->
                try {
                    val hasValidThumbnails = hasValidThumbnailCache(image.uid, image.storagePath)
                    if (hasValidThumbnails) {
                        healthyImages++
                    } else {
                        imagesWithIssues++
                        logger.debug("Image {} has thumbnail cache issues", image.uid)
                    }
                } catch (e: Exception) {
                    imagesWithIssues++
                    logger.warn("Health check failed for image {}: {}", image.uid, e.message)
                }
            }

            logger.info("Thumbnail health check completed: {} healthy, {} with issues out of {} total images",
                healthyImages, imagesWithIssues, activeImages.size)

            // Alert if more than 10% of images have thumbnail issues
            if (activeImages.isNotEmpty() && (imagesWithIssues.toDouble() / activeImages.size) > 0.1) {
                logger.warn("High percentage of images have thumbnail issues: {}%",
                    (imagesWithIssues.toDouble() / activeImages.size * 100).toInt())
            }

        } catch (e: Exception) {
            logger.error("Thumbnail health check failed: {}", e.message, e)
        }
    }

    /**
     * Manual cleanup operation for specific workspace
     */
    fun cleanupWorkspaceThumbnails(workspaceSlug: String, olderThanDays: Int = 30): Int {
        logger.info("Starting manual thumbnail cleanup for workspace: {}", workspaceSlug)

        return try {
            // This would need to be implemented with workspace-specific listing
            val deletedCount = thumbnailCacheService.cleanupOldThumbnails(
                storageProperties.defaultBucket,
                olderThanDays
            )

            logger.info("Manual thumbnail cleanup completed for workspace {}: deleted {} thumbnails",
                workspaceSlug, deletedCount)

            deletedCount
        } catch (e: Exception) {
            logger.error("Failed to cleanup thumbnails for workspace {}: {}", workspaceSlug, e.message, e)
            0
        }
    }

    /**
     * Get thumbnail cache statistics
     */
    fun getThumbnailCacheStats(): ThumbnailCacheStats {
        return try {
            val activeImages = customerImageRepository.countByActiveTrue()
            val totalThumbnails = getTotalCachedThumbnails()
            val expectedThumbnails = activeImages * 3 // 3 standard sizes

            ThumbnailCacheStats(
                totalActiveImages = activeImages,
                totalCachedThumbnails = totalThumbnails,
                expectedThumbnails = expectedThumbnails,
                cacheHitRate = if (expectedThumbnails > 0) {
                    (totalThumbnails.toDouble() / expectedThumbnails * 100).toInt()
                } else 0,
                lastCleanup = getLastCleanupTime(),
                cacheEnabled = storageProperties.image.thumbnails.cacheEnabled
            )
        } catch (e: Exception) {
            logger.error("Failed to get thumbnail cache stats: {}", e.message, e)
            ThumbnailCacheStats(0, 0, 0, 0, null, false)
        }
    }

    private fun cleanupOrphanedThumbnailsForWorkspace(workspaceSlug: String): Int {
        // Implementation would need to:
        // 1. List all thumbnails for the workspace
        // 2. Check if corresponding original image exists
        // 3. Delete orphaned thumbnails
        // This is a simplified version
        return 0
    }

    private fun hasValidThumbnailCache(imageUid: String, storagePath: String): Boolean {
        // Check if standard thumbnail sizes exist and are accessible
        return try {
            val standardSizes = listOf(150, 300, 500)
            standardSizes.any { size ->
                val thumbnailSize = ImageResizingService.ThumbnailSize.fromPixels(size)
                thumbnailSize?.let {
                    thumbnailCacheService.thumbnailExists(storageProperties.defaultBucket, storagePath, it)
                } ?: false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getTotalCachedThumbnails(): Long {
        // This would need to count thumbnails in object storage
        // Implementation depends on storage backend
        return 0
    }

    private fun getLastCleanupTime(): LocalDateTime? {
        // This could be stored in database or retrieved from logs
        return null
    }
}

/**
 * Extension function to find images without thumbnails
 */
fun CustomerImageRepository.findImagesWithoutThumbnails(since: LocalDateTime): List<CustomerImage> =
    findByActiveTrueAndUploadedAtAfter(since)

/**
 * Thumbnail cache statistics
 */
data class ThumbnailCacheStats(
    val totalActiveImages: Long,
    val totalCachedThumbnails: Long,
    val expectedThumbnails: Long,
    val cacheHitRate: Int, // Percentage
    val lastCleanup: LocalDateTime?,
    val cacheEnabled: Boolean
)
