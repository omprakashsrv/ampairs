package com.ampairs.user.service

import com.ampairs.file.config.StorageProperties
import com.ampairs.file.service.ImageResizingService
import com.ampairs.file.storage.ObjectStorageService
import com.ampairs.file.storage.UploadResult
import com.ampairs.user.model.User
import com.ampairs.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.time.Instant

/**
 * Service for handling user profile picture uploads.
 * Handles validation, resizing, thumbnail generation, and storage.
 */
@Service
class ProfilePictureService(
    private val objectStorageService: ObjectStorageService,
    private val imageResizingService: ImageResizingService,
    private val storageProperties: StorageProperties,
    private val userRepository: UserRepository,
) {

    private val logger = LoggerFactory.getLogger(ProfilePictureService::class.java)

    companion object {
        private const val PROFILE_PICTURE_FOLDER = "profile-pictures"
        private const val FULL_SIZE_MAX = 512
        private const val THUMBNAIL_SIZE = 256
        private const val MAX_FILE_SIZE = 5L * 1024 * 1024 // 5MB

        private val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp"
        )
    }

    data class ProfilePictureUrls(
        val fullUrl: String,
        val thumbnailUrl: String,
    )

    /**
     * Upload and process a profile picture for a user.
     * Generates both a full-size image (512x512 max) and a thumbnail (256x256).
     *
     * @param user The user to update
     * @param file The uploaded image file
     * @return Updated user with new profile picture URLs
     */
    fun uploadProfilePicture(user: User, file: MultipartFile): User {
        // Validate the file
        validateProfilePicture(file)

        val fileBytes = file.bytes
        val contentType = file.contentType ?: "image/jpeg"
        val extension = getExtensionFromContentType(contentType)

        // Generate unique filenames
        val timestamp = System.currentTimeMillis()
        val fullSizeKey = generateObjectKey(user.uid, "profile_$timestamp.$extension")
        val thumbnailKey = generateObjectKey(user.uid, "profile_thumb_$timestamp.$extension")

        // Resize full-size image if needed
        val resizedBytes = resizeIfNeeded(fileBytes, FULL_SIZE_MAX)

        // Generate thumbnail
        val thumbnailBytes = generateThumbnail(fileBytes, THUMBNAIL_SIZE)

        // Delete old profile pictures if they exist
        deleteOldProfilePictures(user)

        // Upload both images
        uploadImage(fullSizeKey, resizedBytes, contentType)
        uploadImage(thumbnailKey, thumbnailBytes, contentType)

        // Store object keys (not URLs) - backend API will serve the images
        user.profilePictureUrlField = fullSizeKey
        user.profilePictureThumbnailUrl = thumbnailKey
        user.profilePictureUpdatedAt = Instant.now()

        val savedUser = userRepository.save(user)

        logger.info(
            "Profile picture uploaded for user {}: full={}, thumbnail={}",
            user.uid,
            user.profilePictureUrlField,
            user.profilePictureThumbnailUrl
        )

        return savedUser
    }

    /**
     * Get the profile picture bytes for a user.
     *
     * @param objectKey The object key stored in the user record
     * @return The image bytes
     */
    fun getProfilePicture(objectKey: String): ByteArray {
        return try {
            objectStorageService.downloadFile(
                bucketName = storageProperties.defaultBucket,
                objectKey = objectKey
            ).readBytes()
        } catch (e: Exception) {
            logger.error("Failed to download profile picture: {}", objectKey, e)
            throw ProfilePictureNotFoundException("Profile picture not found")
        }
    }

    /**
     * Delete a user's profile picture.
     *
     * @param user The user whose profile picture to delete
     * @return Updated user with null profile picture URLs
     */
    fun deleteProfilePicture(user: User): User {
        deleteOldProfilePictures(user)

        user.profilePictureUrlField = null
        user.profilePictureThumbnailUrl = null
        user.profilePictureUpdatedAt = Instant.now()

        return userRepository.save(user)
    }

    private fun validateProfilePicture(file: MultipartFile) {
        if (file.isEmpty) {
            throw ProfilePictureValidationException("File is empty")
        }

        if (file.size > MAX_FILE_SIZE) {
            throw ProfilePictureValidationException("File size exceeds 5MB limit")
        }

        val contentType = file.contentType
        if (contentType == null || contentType !in ALLOWED_CONTENT_TYPES) {
            throw ProfilePictureValidationException("Invalid file type. Allowed: JPEG, PNG, WebP")
        }

        // Validate file signature (magic bytes) to ensure content matches declared type
        val fileBytes = file.bytes
        if (!isValidImageSignature(fileBytes, contentType)) {
            throw ProfilePictureValidationException("File content doesn't match declared type")
        }

        // Verify the image can be read and decoded
        try {
            val inputStream = ByteArrayInputStream(fileBytes)
            val image = javax.imageio.ImageIO.read(inputStream)
            if (image == null) {
                throw ProfilePictureValidationException("Invalid or corrupted image file")
            }
            image.flush()
        } catch (e: Exception) {
            logger.warn("Failed to read image file: {}", e.message)
            throw ProfilePictureValidationException("Unable to process image file")
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

    private fun uploadImage(objectKey: String, bytes: ByteArray, contentType: String): UploadResult {
        return objectStorageService.uploadFile(
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

    private fun generateObjectKey(userId: String, filename: String): String {
        return "$PROFILE_PICTURE_FOLDER/$userId/$filename"
    }

    private fun deleteOldProfilePictures(user: User) {
        try {
            // List and delete all files in the user's profile pictures folder
            val prefix = generateObjectKey(user.uid, "")
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
                    logger.debug("Deleted old profile picture: {}", file.objectKey)
                } catch (e: Exception) {
                    logger.warn("Failed to delete old profile picture: {}", file.objectKey, e)
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to list/delete old profile pictures for user {}: {}", user.uid, e.message)
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
 * Exception thrown when profile picture validation fails
 */
class ProfilePictureValidationException(message: String) : RuntimeException(message)

/**
 * Exception thrown when profile picture is not found
 */
class ProfilePictureNotFoundException(message: String) : RuntimeException(message)
