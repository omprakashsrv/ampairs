package com.ampairs.file.storage

import com.ampairs.file.config.StorageProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant

/**
 * Local filesystem implementation of ObjectStorageService.
 */
@Service
@ConditionalOnProperty(name = ["ampairs.storage.provider"], havingValue = "LOCAL")
class LocalObjectStorageService(
    private val storageProperties: StorageProperties
) : ObjectStorageService {

    private val logger = LoggerFactory.getLogger(LocalObjectStorageService::class.java)
    private val basePath: Path = Paths.get(storageProperties.local.basePath).toAbsolutePath()

    init {
        if (storageProperties.local.createDirectories) {
            Files.createDirectories(basePath)
            logger.info("Local storage base directory ensured at {}", basePath)
        }
    }

    override fun uploadFile(
        inputStream: InputStream,
        bucketName: String,
        objectKey: String,
        contentType: String,
        contentLength: Long,
        metadata: Map<String, String>
    ): UploadResult {
        return try {
            validateObjectKey(objectKey)
            validateFileSize(contentLength)

            val filePath = resolvePath(bucketName, objectKey)
            Files.createDirectories(filePath.parent)
            Files.copy(inputStream, filePath)

            logger.info("File stored locally: path={}", filePath)

            UploadResult(
                objectKey = objectKey,
                etag = null,
                contentLength = contentLength,
                lastModified = Files.getLastModifiedTime(filePath).toInstant(),
                url = "${storageProperties.local.urlBase.trimEnd('/')}/$bucketName/$objectKey"
            )
        } catch (e: Exception) {
            logger.error("Failed to store file locally: bucket={}, key={}, error={}", bucketName, objectKey, e.message, e)
            throw ObjectUploadException("Failed to store file locally: ${e.message}", e)
        }
    }

    override fun uploadFile(
        bytes: ByteArray,
        bucketName: String,
        objectKey: String,
        contentType: String,
        metadata: Map<String, String>
    ): UploadResult {
        return uploadFile(ByteArrayInputStream(bytes), bucketName, objectKey, contentType, bytes.size.toLong(), metadata)
    }

    override fun downloadFile(bucketName: String, objectKey: String): InputStream {
        return try {
            validateObjectKey(objectKey)

            val filePath = resolvePath(bucketName, objectKey)
            if (!Files.exists(filePath)) {
                throw ObjectNotFoundException("File not found: $bucketName/$objectKey")
            }

            Files.newInputStream(filePath)
        } catch (e: ObjectNotFoundException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to download local file: bucket={}, key={}, error={}", bucketName, objectKey, e.message, e)
            throw ObjectDownloadException("Failed to download local file: ${e.message}", e)
        }
    }

    override fun getObjectMetadata(bucketName: String, objectKey: String): ObjectMetadata {
        return try {
            val filePath = resolvePath(bucketName, objectKey)
            if (!Files.exists(filePath)) {
                throw ObjectNotFoundException("File not found: $bucketName/$objectKey")
            }

            val contentType = Files.probeContentType(filePath) ?: "application/octet-stream"
            val size = Files.size(filePath)
            val lastModified = Files.getLastModifiedTime(filePath).toInstant()

            ObjectMetadata(
                objectKey = objectKey,
                contentType = contentType,
                contentLength = size,
                etag = calculateEtag(filePath.toFile()),
                lastModified = lastModified
            )
        } catch (e: ObjectNotFoundException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to get local file metadata: bucket={}, key={}, error={}", bucketName, objectKey, e.message, e)
            throw ObjectStorageException("Failed to get local file metadata: ${e.message}", e)
        }
    }

    override fun generatePresignedUrl(bucketName: String, objectKey: String, expirationSeconds: Long): String {
        validateObjectKey(objectKey)
        return "${storageProperties.local.urlBase.trimEnd('/')}/$bucketName/$objectKey"
    }

    override fun objectExists(bucketName: String, objectKey: String): Boolean {
        return try {
            validateObjectKey(objectKey)
            Files.exists(resolvePath(bucketName, objectKey))
        } catch (e: Exception) {
            logger.warn("Error checking local file existence: bucket={}, key={}, error={}", bucketName, objectKey, e.message)
            false
        }
    }

    override fun deleteObject(bucketName: String, objectKey: String) {
        try {
            val filePath = resolvePath(bucketName, objectKey)
            Files.deleteIfExists(filePath)
            logger.info("Local file deleted: {}", filePath)
        } catch (e: Exception) {
            logger.error("Failed to delete local file: bucket={}, key={}, error={}", bucketName, objectKey, e.message, e)
            throw ObjectDeleteException("Failed to delete local file: ${e.message}", e)
        }
    }

    override fun copyObject(
        sourceBucket: String,
        sourceKey: String,
        targetBucket: String,
        targetKey: String
    ): CopyResult {
        return try {
            val sourcePath = resolvePath(sourceBucket, sourceKey)
            val targetPath = resolvePath(targetBucket, targetKey)

            Files.createDirectories(targetPath.parent)
            Files.copy(sourcePath, targetPath)

            logger.info("Local file copied: {} -> {}", sourcePath, targetPath)

            CopyResult(
                sourceKey = sourceKey,
                targetKey = targetKey,
                etag = calculateEtag(targetPath.toFile()),
                lastModified = Files.getLastModifiedTime(targetPath).toInstant()
            )
        } catch (e: Exception) {
            logger.error("Failed to copy local file: source={}, target={}, error={}", sourceKey, targetKey, e.message, e)
            throw ObjectCopyException("Failed to copy local file: ${e.message}", e)
        }
    }

    override fun listObjects(bucketName: String, prefix: String, maxKeys: Int): List<ObjectSummary> {
        return try {
            val startPath = resolvePath(bucketName, prefix)
            if (!Files.exists(startPath.parent)) {
                return emptyList()
            }

            Files.walk(startPath.parent)
                .filter { Files.isRegularFile(it) && it.startsWith(startPath.parent) }
                .map { path ->
                    val relative = startPath.parent.relativize(path).toString().replace(File.separatorChar, '/')
                    ObjectSummary(
                        objectKey = relative,
                        size = Files.size(path),
                        lastModified = Files.getLastModifiedTime(path).toInstant(),
                        etag = calculateEtag(path.toFile())
                    )
                }
                .limit(maxKeys.toLong())
                .toList()
        } catch (e: Exception) {
            logger.error("Failed to list local files: bucket={}, prefix={}, error={}", bucketName, prefix, e.message, e)
            throw ObjectStorageException("Failed to list local files: ${e.message}", e)
        }
    }

    override fun createBucketIfNotExists(bucketName: String) {
        try {
            Files.createDirectories(basePath.resolve(bucketName))
        } catch (e: Exception) {
            logger.error("Failed to create local bucket directory: {}. Error={}", bucketName, e.message, e)
            throw BucketCreationException("Failed to create local bucket directory: ${e.message}", e)
        }
    }

    private fun resolvePath(bucketName: String, objectKey: String): Path {
        return basePath.resolve(bucketName).resolve(objectKey).normalize()
    }

    private fun validateObjectKey(objectKey: String) {
        if (objectKey.isBlank()) {
            throw InvalidObjectKeyException("Object key must not be blank")
        }
        if (objectKey.contains("..")) {
            throw InvalidObjectKeyException("Object key must not contain '..'")
        }
        if (objectKey.startsWith("/") || objectKey.startsWith("\\")) {
            throw InvalidObjectKeyException("Object key must not start with a path separator")
        }
        if (objectKey.length > 1024) {
            throw InvalidObjectKeyException("Object key exceeds maximum length of 1024 characters")
        }
    }

    private fun validateFileSize(size: Long) {
        val maxSize = parseSize(storageProperties.maxFileSize)
        if (size > maxSize) {
            throw ObjectUploadException("File size exceeds maximum allowed size of $maxSize bytes")
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

    private fun calculateEtag(file: File): String {
        return "${file.lastModified()}-${file.length()}"
    }
}
