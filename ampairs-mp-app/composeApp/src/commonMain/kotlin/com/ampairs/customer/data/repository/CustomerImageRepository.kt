package com.ampairs.customer.data.repository

import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.customer.data.api.CustomerImageApi
import com.ampairs.customer.data.db.CustomerImageDao
import com.ampairs.customer.data.db.CustomerImageEntity
import com.ampairs.customer.data.db.toCustomerImage
import com.ampairs.customer.data.db.toEntity
import com.ampairs.customer.data.db.toListItem
import com.ampairs.customer.domain.CustomerImage
import com.ampairs.customer.domain.CustomerImageListItem
import com.ampairs.customer.domain.CustomerImageUploadRequest
import com.ampairs.customer.domain.CustomerImageUpdateRequest
import com.ampairs.customer.domain.CustomerImageStatus
import com.ampairs.customer.util.CustomerConstants.ERROR_CUSTOMER_IMAGE_UID_REQUIRED
import com.ampairs.customer.util.CustomerLogger
import com.ampairs.workspace.context.WorkspaceContextManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Repository for customer image operations with offline-first architecture.
 * Provides database-first operations with background server synchronization.
 */
class CustomerImageRepository(
    private val dao: CustomerImageDao,
    private val api: CustomerImageApi,
    private val appPreferences: AppPreferencesDataStore,
    private val workspaceContextManager: WorkspaceContextManager,
    private val fileManager: PlatformFileManager
) {

    private val workspaceId: String
        get() = workspaceContextManager.getCurrentWorkspaceId() ?: "default"

    // Observing operations

    fun observeCustomerImages(customerId: String): Flow<List<CustomerImageListItem>> {
        return dao.observeCustomerImages(customerId, workspaceId)
            .map { entities -> entities.map { it.toListItem() } }
    }

    fun observePrimaryImage(customerId: String): Flow<CustomerImage?> {
        return dao.observePrimaryCustomerImage(customerId, workspaceId)
            .map { it?.toCustomerImage() }
    }

    suspend fun getCustomerImages(customerId: String): List<CustomerImageListItem> {
        return dao.getCustomerImages(customerId, workspaceId)
            .map { it.toListItem() }
    }

    suspend fun getCustomerImage(imageId: String): CustomerImage? {
        return dao.getCustomerImage(imageId, workspaceId)?.toCustomerImage()
    }

    suspend fun getPrimaryImage(customerId: String): CustomerImage? {
        return dao.getPrimaryCustomerImage(customerId, workspaceId)?.toCustomerImage()
    }

    // Image upload operations

    @OptIn(ExperimentalTime::class)
    suspend fun uploadImage(
        customerId: String,
        fileName: String,
        contentType: String,
        fileSize: Long,
        imageData: ByteArray,
        description: String? = null,
        isPrimary: Boolean = false
    ): Result<CustomerImage> {
        val uid = UidGenerator.generateUid("IMG")
        val now = Clock.System.now().toString()

        // Create upload request
        val uploadRequest = CustomerImageUploadRequest(
            customerId = customerId,
            fileName = fileName,
            contentType = contentType,
            fileSize = fileSize,
            description = description,
            isPrimary = isPrimary,
            sortOrder = getNextSortOrder(customerId)
        )

        // Create local customer image
        val customerImage = CustomerImage(
            uid = uid,
            customerId = customerId,
            fileName = fileName,
            contentType = contentType,
            fileSize = fileSize,
            description = description,
            isPrimary = isPrimary,
            sortOrder = uploadRequest.sortOrder,
            uploadStatus = CustomerImageStatus.PENDING,
            localPath = null // Will be set after local file save
        )

        // 1. Save to local database first (offline-first)
        val entity = customerImage.toEntity(workspaceId, synced = false, localCreatedAt = now, localUpdatedAt = now)
        dao.insertCustomerImage(entity)

        // 2. Save image file locally for offline access
        try {
            val localPath = fileManager.saveImageToCache(uid, imageData, fileName)
            dao.updateLocalPath(uid, workspaceId, localPath, now)

            // Update local object with file path
            val updatedImage = customerImage.copy(localPath = localPath)
            dao.insertCustomerImage(updatedImage.toEntity(workspaceId, synced = false, localCreatedAt = now, localUpdatedAt = now))
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageRepository", "Failed to save image locally", e)
        }

        // 3. Handle primary image logic locally
        if (isPrimary) {
            dao.clearPrimaryImages(customerId, workspaceId, now)
            dao.setPrimaryImage(uid, workspaceId, now)
        }

        // 4. Background server upload using multipart form data
        try {
            dao.updateUploadStatus(uid, workspaceId, CustomerImageStatus.UPLOADING, now)

            // Wrap upload operation in timeout (60 seconds)
            val serverImage = withTimeout(60_000L) {
                // Direct multipart upload to backend
                val uploadResponse = api.uploadCustomerImageMultipart(
                    customerId = customerId,
                    fileName = fileName,
                    contentType = contentType,
                    imageData = imageData,
                    description = description,
                    isPrimary = isPrimary,
                    displayOrder = uploadRequest.sortOrder
                )

                // Create final server image object
                val result = customerImage.copy(
                    imageUrl = uploadResponse.imageUrl,
                    thumbnailUrl = uploadResponse.thumbnailUrl,
                    uploadStatus = CustomerImageStatus.COMPLETED
                )

                // Update local database with server URLs and mark as synced
                val syncedEntity = result.toEntity(workspaceId, synced = true, localCreatedAt = now, localUpdatedAt = now)
                dao.insertCustomerImage(syncedEntity)

                CustomerLogger.i("CustomerImageRepository", "Multipart upload successful for: $fileName")
                result
            }
            return Result.success(serverImage)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            CustomerLogger.w("CustomerImageRepository", "Upload timeout for: $fileName after 60 seconds")
            dao.updateUploadStatus(uid, workspaceId, CustomerImageStatus.FAILED, now)
            return Result.success(customerImage.copy(uploadStatus = CustomerImageStatus.FAILED))
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageRepository", "Background multipart upload failed", e)
            dao.updateUploadStatus(uid, workspaceId, CustomerImageStatus.FAILED, now)
            return Result.success(customerImage.copy(uploadStatus = CustomerImageStatus.FAILED))
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun updateImage(imageId: String, updateRequest: CustomerImageUpdateRequest): Result<CustomerImage> {
        val existing = dao.getCustomerImage(imageId, workspaceId)
            ?: return Result.failure(Exception("Image not found"))

        val now = Clock.System.now().toString()

        // Create updated image
        val updatedImage = existing.toCustomerImage().copy(
            description = updateRequest.description ?: existing.description,
            isPrimary = updateRequest.isPrimary ?: existing.isPrimary,
            sortOrder = updateRequest.sortOrder ?: existing.sortOrder,
            tags = updateRequest.tags,
            metadata = updateRequest.metadata
        )

        // 1. Update local database first (offline-first)
        val unsyncedEntity = updatedImage.toEntity(workspaceId, synced = false, localUpdatedAt = now)
        dao.insertCustomerImage(unsyncedEntity)

        // 2. Handle primary image logic
        if (updateRequest.isPrimary == true) {
            dao.clearPrimaryImages(existing.customerId, workspaceId, now)
            dao.setPrimaryImage(imageId, workspaceId, now)
        }

        // 3. Background server sync
        try {
            val serverImage = api.updateCustomerImage(existing.customerId, imageId, updateRequest)
            val syncedEntity = serverImage.toEntity(workspaceId, synced = true, localUpdatedAt = now)
            dao.insertCustomerImage(syncedEntity)
            return Result.success(serverImage)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageRepository", "Failed to sync image update to server", e)
            return Result.success(updatedImage)
        }
    }

    suspend fun deleteImage(imageId: String): Result<Unit> {
        val existing = dao.getCustomerImage(imageId, workspaceId)
            ?: return Result.failure(Exception("Image not found"))

        // 1. Delete from local database first
        dao.deleteCustomerImage(imageId, workspaceId)

        // 2. Delete local file if exists
        existing.localPath?.let { localPath ->
            try {
                fileManager.deleteFile(localPath)
            } catch (e: Exception) {
                CustomerLogger.w("CustomerImageRepository", "Failed to delete local file: $localPath", e)
            }
        }

        // 3. Background server delete
        try {
            api.deleteCustomerImage(existing.customerId, imageId)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageRepository", "Failed to delete image from server", e)
            // Note: Since image is already deleted locally, we don't restore it
            // Server sync will handle cleanup on next sync cycle
        }

        return Result.success(Unit)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun setPrimaryImage(imageId: String): Result<CustomerImage> {
        val existing = dao.getCustomerImage(imageId, workspaceId)
            ?: return Result.failure(Exception("Image not found"))

        val now = Clock.System.now().toString()

        // 1. Update local database first
        dao.clearPrimaryImages(existing.customerId, workspaceId, now)
        dao.setPrimaryImage(imageId, workspaceId, now)

        // 2. Mark as unsynced
        dao.updateSyncStatus(imageId, workspaceId, synced = false, syncPending = true, lastSyncAttempt = null)

        val updatedImage = existing.toCustomerImage().copy(isPrimary = true)

        // 3. Background server sync
        try {
            val serverImage = api.setPrimaryImage(existing.customerId, imageId)
            val syncedEntity = serverImage.toEntity(workspaceId, synced = true, localUpdatedAt = now)
            dao.insertCustomerImage(syncedEntity)
            return Result.success(serverImage)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageRepository", "Failed to sync primary image to server", e)
            return Result.success(updatedImage)
        }
    }

    // Batch synchronization

    @OptIn(ExperimentalTime::class)
    suspend fun syncCustomerImages(customerId: String): Result<Int> {
        return try {
            CustomerLogger.i("CustomerImageSync", "Starting customer image sync for customer: $customerId")

            // 0. Clean up stale UPLOADING records (older than 5 minutes)
            cleanupStaleUploadingRecords(customerId)

            // 1. Sync unsynced local images to server first
            val unsyncedImages = dao.getUnsyncedCustomerImages(workspaceId).filter { it.customerId == customerId }
            var syncedCount = 0

            for (entity in unsyncedImages) {
                try {
                    val image = entity.toCustomerImage()
                    if (entity.uploadStatus == CustomerImageStatus.PENDING || entity.uploadStatus == CustomerImageStatus.FAILED) {
                        // Handle pending and failed uploads - retry upload using multipart
                        if (entity.localPath != null && fileManager.fileExists(entity.localPath)) {
                            try {
                                dao.updateUploadStatus(entity.uid, workspaceId, CustomerImageStatus.UPLOADING, Clock.System.now().toString())

                                // Wrap upload operation in timeout (60 seconds)
                                withTimeout(60_000L) {
                                    // Read the file data from local storage
                                    val imageData = fileManager.readFile(entity.localPath)
                                    CustomerLogger.d("CustomerImageSync", "Read ${imageData.size} bytes from local file for retry: ${entity.uid}")

                                    // Perform multipart upload using the existing API
                                    val uploadResponse = api.uploadCustomerImageMultipart(
                                        customerId = entity.customerId,
                                        fileName = entity.fileName,
                                        contentType = entity.contentType,
                                        imageData = imageData,
                                        description = entity.description,
                                        isPrimary = entity.isPrimary,
                                        displayOrder = entity.sortOrder
                                    )

                                    // Update with server response
                                    val serverImage = entity.toCustomerImage().copy(
                                        imageUrl = uploadResponse.imageUrl,
                                        thumbnailUrl = uploadResponse.thumbnailUrl,
                                        uploadStatus = CustomerImageStatus.COMPLETED
                                    )

                                    // Mark as synced and update local database
                                    val syncedEntity = serverImage.toEntity(workspaceId, synced = true, localUpdatedAt = Clock.System.now().toString())
                                    dao.insertCustomerImage(syncedEntity)
                                    syncedCount++

                                    CustomerLogger.i("CustomerImageSync", "Successfully retried upload for: ${entity.uid}")
                                }
                            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                                CustomerLogger.w("CustomerImageSync", "Upload timeout for image ${entity.uid} after 60 seconds")
                                dao.updateUploadStatus(entity.uid, workspaceId, CustomerImageStatus.FAILED, Clock.System.now().toString())
                            } catch (e: Exception) {
                                CustomerLogger.e("CustomerImageSync", "Failed to retry upload for image ${entity.uid}", e)
                                dao.updateUploadStatus(entity.uid, workspaceId, CustomerImageStatus.FAILED, Clock.System.now().toString())
                            }
                        } else {
                            // Local file not found, mark as failed
                            CustomerLogger.w("CustomerImageSync", "Local file not found for upload retry: ${entity.uid} (status: ${entity.uploadStatus})")
                            dao.updateUploadStatus(entity.uid, workspaceId, CustomerImageStatus.FAILED, Clock.System.now().toString())
                        }
                    } else {
                        // Handle metadata updates
                        val updateRequest = CustomerImageUpdateRequest(
                            description = image.description,
                            isPrimary = image.isPrimary,
                            sortOrder = image.sortOrder,
                            tags = image.tags,
                            metadata = image.metadata
                        )
                        val serverImage = api.updateCustomerImage(image.customerId, image.uid, updateRequest)
                        val syncedEntity = serverImage.toEntity(workspaceId, synced = true)
                        dao.insertCustomerImage(syncedEntity)
                        syncedCount++
                    }
                } catch (e: Exception) {
                    CustomerLogger.e("CustomerImageSync", "Failed to sync image ${entity.uid}", e)
                }
            }

            // 2. Fetch server images and update local database
            val lastSyncTime = appPreferences.getCustomerLastSyncTime().first()
            val serverImages = api.getCustomerImages(customerId, lastSyncTime)

            for (serverImage in serverImages) {
                val existing = dao.getCustomerImage(serverImage.uid, workspaceId)
                if (existing == null || existing.synced) {
                    // Only update if local image doesn't exist or is synced (to preserve unsynced local changes)
                    val entity = serverImage.toEntity(workspaceId, synced = true)
                    dao.insertCustomerImage(entity)
                    syncedCount++
                }
            }

            // 3. Update last sync time
            if (serverImages.isNotEmpty()) {
                val maxServerTime = serverImages.mapNotNull { it.updatedAt }.maxOrNull()
                if (maxServerTime != null) {
                    appPreferences.setCustomerLastSyncTime(maxServerTime)
                }
            }

            CustomerLogger.i("CustomerImageSync", "Customer image sync completed. Synced: $syncedCount images")
            Result.success(syncedCount)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageSync", "Customer image sync failed", e)
            Result.failure(e)
        }
    }

    // Helper methods

    /**
     * Clean up stale UPLOADING records for all customers that are older than the specified timeout.
     * This should be called on app startup to handle cases where the app crashed during upload operations.
     */
    @OptIn(ExperimentalTime::class)
    suspend fun cleanupAllStaleUploads(timeoutMinutes: Int = 5) {
        try {
            val now = Clock.System.now()
            val timeoutThreshold = now.minus(kotlin.time.Duration.parse("${timeoutMinutes}m"))
            val thresholdString = timeoutThreshold.toString()

            CustomerLogger.i("CustomerImageRepository", "Starting cleanup of all stale UPLOADING records older than $timeoutMinutes minutes")

            // Find all UPLOADING records that are older than the timeout
            val allUploadingImages = dao.getUnsyncedCustomerImages(workspaceId)
                .filter { entity ->
                    entity.uploadStatus == CustomerImageStatus.UPLOADING &&
                    entity.localUpdatedAt < thresholdString
                }

            if (allUploadingImages.isNotEmpty()) {
                CustomerLogger.w("CustomerImageRepository", "Found ${allUploadingImages.size} stale UPLOADING records to clean up")

                // Mark them as FAILED so they can be retried
                allUploadingImages.forEach { entity ->
                    CustomerLogger.w("CustomerImageRepository", "Marking stale UPLOADING record as FAILED: ${entity.uid} (customer: ${entity.customerId})")
                    dao.updateUploadStatus(entity.uid, workspaceId, CustomerImageStatus.FAILED, now.toString())
                }

                CustomerLogger.i("CustomerImageRepository", "Cleaned up ${allUploadingImages.size} stale UPLOADING records")
            } else {
                CustomerLogger.d("CustomerImageRepository", "No stale UPLOADING records found")
            }
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageRepository", "Error during global stale upload cleanup", e)
        }
    }

    /**
     * Clean up stale UPLOADING records that are older than the specified timeout.
     * This handles cases where the app crashed or was killed during upload operations.
     */
    @OptIn(ExperimentalTime::class)
    private suspend fun cleanupStaleUploadingRecords(customerId: String, timeoutMinutes: Int = 5) {
        try {
            val now = Clock.System.now()
            val timeoutThreshold = now.minus(kotlin.time.Duration.parse("${timeoutMinutes}m"))
            val thresholdString = timeoutThreshold.toString()

            CustomerLogger.d("CustomerImageSync", "Cleaning up UPLOADING records older than $timeoutMinutes minutes (before $thresholdString)")

            // Find all UPLOADING records for this customer that are older than the timeout
            val allUploadingImages = dao.getUnsyncedCustomerImages(workspaceId)
                .filter { entity ->
                    entity.customerId == customerId &&
                    entity.uploadStatus == CustomerImageStatus.UPLOADING &&
                    entity.localUpdatedAt < thresholdString
                }

            if (allUploadingImages.isNotEmpty()) {
                CustomerLogger.w("CustomerImageSync", "Found ${allUploadingImages.size} stale UPLOADING records to clean up")

                // Mark them as FAILED so they can be retried
                allUploadingImages.forEach { entity ->
                    CustomerLogger.w("CustomerImageSync", "Marking stale UPLOADING record as FAILED: ${entity.uid}")
                    dao.updateUploadStatus(entity.uid, workspaceId, CustomerImageStatus.FAILED, now.toString())
                }
            } else {
                CustomerLogger.d("CustomerImageSync", "No stale UPLOADING records found")
            }
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageSync", "Error during stale upload cleanup", e)
        }
    }

    private suspend fun getNextSortOrder(customerId: String): Int {
        val existingImages = dao.getCustomerImages(customerId, workspaceId)
        return (existingImages.maxOfOrNull { it.sortOrder } ?: 0) + 1
    }

    suspend fun getImageCount(customerId: String): Int {
        return dao.getCustomerImageCount(customerId, workspaceId)
    }

    suspend fun getUnsyncedCount(): Int {
        return dao.getUnsyncedCount(workspaceId)
    }

    suspend fun getPendingUploadCount(): Int {
        return dao.getPendingUploadCount(workspaceId)
    }

    suspend fun searchImages(customerId: String, query: String): List<CustomerImageListItem> {
        return dao.searchCustomerImages(customerId, workspaceId, query)
            .map { it.toListItem() }
    }

    suspend fun deleteAllImages(customerId: String): Result<Unit> {
        return try {
            // Get all images for cleanup
            val images = dao.getCustomerImages(customerId, workspaceId)

            // Delete local files
            images.forEach { entity ->
                entity.localPath?.let { localPath ->
                    try {
                        fileManager.deleteFile(localPath)
                    } catch (e: Exception) {
                        CustomerLogger.w("CustomerImageRepository", "Failed to delete local file: $localPath", e)
                    }
                }
            }

            // Delete from local database
            dao.deleteCustomerImagesByCustomer(customerId, workspaceId)

            // Background server delete
            try {
                api.deleteAllCustomerImages(customerId)
            } catch (e: Exception) {
                CustomerLogger.e("CustomerImageRepository", "Failed to delete all images from server", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}

/**
 * Platform-specific file manager interface for image storage.
 * Implementations should handle platform-specific file operations.
 */
interface PlatformFileManager {
    suspend fun saveImageToCache(imageId: String, imageData: ByteArray, fileName: String): String
    suspend fun deleteFile(filePath: String)
    suspend fun fileExists(filePath: String): Boolean
    suspend fun getFileSize(filePath: String): Long
    suspend fun getCacheDirectory(): String
    suspend fun readFile(filePath: String): ByteArray
}