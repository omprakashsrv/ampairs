package com.ampairs.workspace.service

import com.ampairs.file.config.StorageProperties
import com.ampairs.file.service.ImageResizingService
import com.ampairs.file.storage.ObjectStorageService
import com.ampairs.workspace.model.Workspace
import com.ampairs.workspace.repository.WorkspaceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.time.Instant

/**
 * Service for handling workspace avatar uploads.
 * Handles validation, resizing, thumbnail generation, and storage.
 */
@Service
class WorkspaceAvatarService(
    private val objectStorageService: ObjectStorageService,
    private val imageResizingService: ImageResizingService,
    private val storageProperties: StorageProperties,
    private val workspaceRepository: WorkspaceRepository,
) {

    private val logger = LoggerFactory.getLogger(WorkspaceAvatarService::class.java)

    companion object {
        private const val AVATAR_FOLDER = "workspace-avatars"
        private const val FULL_SIZE_MAX = 512
        private const val THUMBNAIL_SIZE = 256
        private const val MAX_FILE_SIZE = 5L * 1024 * 1024 // 5MB

        private val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp"
        )
    }

    /**
     * Upload and process an avatar for a workspace.
     * Generates both a full-size image (512x512 max) and a thumbnail (256x256).
     *
     * @param workspace The workspace to update
     * @param file The uploaded image file
     * @return Updated workspace with new avatar URLs
     */
    fun uploadAvatar(workspace: Workspace, file: MultipartFile): Workspace {
        // Validate the file
        validateAvatar(file)

        val fileBytes = file.bytes
        val contentType = file.contentType ?: "image/jpeg"
        val extension = getExtensionFromContentType(contentType)

        // Generate unique filenames
        val timestamp = System.currentTimeMillis()
        val fullSizeKey = generateObjectKey(workspace.uid, "avatar_$timestamp.$extension")
        val thumbnailKey = generateObjectKey(workspace.uid, "avatar_thumb_$timestamp.$extension")

        // Resize full-size image if needed
        val resizedBytes = resizeIfNeeded(fileBytes, FULL_SIZE_MAX)

        // Generate thumbnail
        val thumbnailBytes = generateThumbnail(fileBytes, THUMBNAIL_SIZE)

        // Delete old avatars if they exist
        deleteOldAvatars(workspace)

        // Upload both images
        uploadImage(fullSizeKey, resizedBytes, contentType)
        uploadImage(thumbnailKey, thumbnailBytes, contentType)

        // Store object keys (not URLs) - backend API will serve the images
        workspace.avatarUrl = fullSizeKey
        workspace.avatarThumbnailUrl = thumbnailKey
        workspace.avatarUpdatedAt = Instant.now()

        val savedWorkspace = workspaceRepository.save(workspace)

        logger.info(
            "Avatar uploaded for workspace {}: full={}, thumbnail={}",
            workspace.uid,
            workspace.avatarUrl,
            workspace.avatarThumbnailUrl
        )

        return savedWorkspace
    }

    /**
     * Get the avatar bytes for a workspace.
     *
     * @param objectKey The object key stored in the workspace record
     * @return The image bytes
     */
    fun getAvatar(objectKey: String): ByteArray {
        return try {
            objectStorageService.downloadFile(
                bucketName = storageProperties.defaultBucket,
                objectKey = objectKey
            ).readBytes()
        } catch (e: Exception) {
            logger.error("Failed to download avatar: {}", objectKey, e)
            throw WorkspaceAvatarNotFoundException("Avatar not found")
        }
    }

    /**
     * Delete a workspace's avatar.
     *
     * @param workspace The workspace whose avatar to delete
     * @return Updated workspace with null avatar URLs
     */
    fun deleteAvatar(workspace: Workspace): Workspace {
        deleteOldAvatars(workspace)

        workspace.avatarUrl = null
        workspace.avatarThumbnailUrl = null
        workspace.avatarUpdatedAt = Instant.now()

        return workspaceRepository.save(workspace)
    }

    private fun validateAvatar(file: MultipartFile) {
        if (file.isEmpty) {
            throw WorkspaceAvatarValidationException("File is empty")
        }

        if (file.size > MAX_FILE_SIZE) {
            throw WorkspaceAvatarValidationException("File size exceeds 5MB limit")
        }

        val contentType = file.contentType
        if (contentType == null || contentType !in ALLOWED_CONTENT_TYPES) {
            throw WorkspaceAvatarValidationException("Invalid file type. Allowed: JPEG, PNG, WebP")
        }

        // Validate file signature (magic bytes) to ensure content matches declared type
        val fileBytes = file.bytes
        if (!isValidImageSignature(fileBytes, contentType)) {
            throw WorkspaceAvatarValidationException("File content doesn't match declared type")
        }

        // Verify the image can be read and decoded
        try {
            val inputStream = ByteArrayInputStream(fileBytes)
            val image = javax.imageio.ImageIO.read(inputStream)
            if (image == null) {
                throw WorkspaceAvatarValidationException("Invalid or corrupted image file")
            }
            image.flush()
        } catch (e: Exception) {
            logger.warn("Failed to read image file: {}", e.message)
            throw WorkspaceAvatarValidationException("Unable to process image file")
        }
    }

    private fun isValidImageSignature(bytes: ByteArray, contentType: String): Boolean {
        if (bytes.size < 8) return false

        return when (contentType) {
            "image/jpeg" -> {
                // JPEG magic bytes: FF D8 FF
                bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
            }
            "image/png" -> {
                // PNG magic bytes: 89 50 4E 47 0D 0A 1A 0A
                bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() &&
                bytes[4] == 0x0D.toByte() && bytes[5] == 0x0A.toByte() &&
                bytes[6] == 0x1A.toByte() && bytes[7] == 0x0A.toByte()
            }
            "image/webp" -> {
                // WebP magic bytes: RIFF....WEBP
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
            val inputStream = ByteArrayInputStream(imageBytes)
            val dimensions = imageResizingService.getImageDimensions(ByteArrayInputStream(imageBytes))

            if (dimensions != null && (dimensions.first > maxSize || dimensions.second > maxSize)) {
                imageResizingService.resizeImage(
                    inputStream = inputStream,
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

    private fun uploadImage(objectKey: String, bytes: ByteArray, contentType: String) {
        objectStorageService.uploadFile(
            bytes = bytes,
            bucketName = storageProperties.defaultBucket,
            objectKey = objectKey,
            contentType = contentType,
            metadata = mapOf(
                "Content-Disposition" to "inline",
                "Cache-Control" to "max-age=31536000" // 1 year cache
            )
        )
    }

    private fun generateObjectKey(workspaceId: String, filename: String): String {
        return "$AVATAR_FOLDER/$workspaceId/$filename"
    }

    private fun deleteOldAvatars(workspace: Workspace) {
        try {
            // List and delete all files in the workspace's avatars folder
            val prefix = generateObjectKey(workspace.uid, "")
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
                    logger.debug("Deleted old avatar: {}", file.objectKey)
                } catch (e: Exception) {
                    logger.warn("Failed to delete old avatar: {}", file.objectKey, e)
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to list/delete old avatars for workspace {}: {}", workspace.uid, e.message)
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

/**
 * Exception thrown when workspace avatar validation fails
 */
class WorkspaceAvatarValidationException(message: String) : RuntimeException(message)

/**
 * Exception thrown when workspace avatar is not found
 */
class WorkspaceAvatarNotFoundException(message: String) : RuntimeException(message)
