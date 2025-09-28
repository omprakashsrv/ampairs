package com.ampairs.core.service

import com.ampairs.core.config.StorageProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Local filesystem implementation of ObjectStorageService
 * For development and testing purposes
 */
@Service
@ConditionalOnProperty(
    name = ["ampairs.storage.provider"],
    havingValue = "LOCAL",
    matchIfMissing = false
)
class LocalObjectStorageService(
    private val storageProperties: StorageProperties
) : ObjectStorageService {

    private val logger = LoggerFactory.getLogger(LocalObjectStorageService::class.java)
    private val basePath: Path

    init {
        basePath = Paths.get(storageProperties.local.basePath).toAbsolutePath()
        if (storageProperties.local.createDirectories) {
            try {
                Files.createDirectories(basePath)
                logger.info("Created local storage directory: {}", basePath)
            } catch (e: Exception) {
                logger.error("Failed to create storage directory: {}", basePath, e)
                throw ObjectStorageException("Failed to initialize local storage", e)
            }
        }
        logger.info("Local storage initialized at: {}", basePath)
    }

    override fun uploadFile(
        bytes: ByteArray,
        bucketName: String,
        objectKey: String,
        contentType: String,
        metadata: Map<String, String>
    ): UploadResult {
        return try {
            val filePath = getFilePath(bucketName, objectKey)

            // Create parent directories if they don't exist
            Files.createDirectories(filePath.parent)

            // Write file
            Files.write(filePath, bytes)

            // Create metadata file
            saveMetadata(filePath, contentType, metadata)

            logger.debug("File uploaded to local storage: path={}, size={}", filePath, bytes.size)

            UploadResult(
                objectKey = objectKey,
                etag = generateETag(bytes),
                contentLength = bytes.size.toLong(),
                lastModified = LocalDateTime.now(),
                url = generateUrl(bucketName, objectKey)
            )
        } catch (e: Exception) {
            logger.error("Failed to upload file to local storage: bucket={}, key={}", bucketName, objectKey, e)
            throw ObjectStorageException("Failed to upload file", e)
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
            val filePath = getFilePath(bucketName, objectKey)

            // Create parent directories if they don't exist
            Files.createDirectories(filePath.parent)

            // Copy input stream to file
            val bytesWritten = Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING)

            // Create metadata file
            saveMetadata(filePath, contentType, metadata)

            logger.debug("File uploaded to local storage: path={}, size={}", filePath, bytesWritten)

            UploadResult(
                objectKey = objectKey,
                etag = generateETag(Files.readAllBytes(filePath)),
                contentLength = bytesWritten,
                lastModified = LocalDateTime.now(),
                url = generateUrl(bucketName, objectKey)
            )
        } catch (e: Exception) {
            logger.error("Failed to upload file to local storage: bucket={}, key={}", bucketName, objectKey, e)
            throw ObjectStorageException("Failed to upload file", e)
        }
    }

    override fun downloadFile(bucketName: String, objectKey: String): InputStream {
        return try {
            val filePath = getFilePath(bucketName, objectKey)

            if (!Files.exists(filePath)) {
                throw ObjectStorageException("File not found: $objectKey")
            }

            Files.newInputStream(filePath)
        } catch (e: Exception) {
            logger.error("Failed to download file from local storage: bucket={}, key={}", bucketName, objectKey, e)
            throw ObjectStorageException("Failed to download file", e)
        }
    }

    override fun deleteObject(bucketName: String, objectKey: String) {
        try {
            val filePath = getFilePath(bucketName, objectKey)
            val metadataPath = getMetadataPath(filePath)

            // Delete main file
            if (Files.exists(filePath)) {
                Files.delete(filePath)
                logger.debug("Deleted file from local storage: {}", filePath)
            }

            // Delete metadata file
            if (Files.exists(metadataPath)) {
                Files.delete(metadataPath)
                logger.debug("Deleted metadata file: {}", metadataPath)
            }

        } catch (e: Exception) {
            logger.error("Failed to delete file from local storage: bucket={}, key={}", bucketName, objectKey, e)
            throw ObjectStorageException("Failed to delete file", e)
        }
    }

    override fun objectExists(bucketName: String, objectKey: String): Boolean {
        val filePath = getFilePath(bucketName, objectKey)
        return Files.exists(filePath)
    }

    override fun getObjectMetadata(bucketName: String, objectKey: String): ObjectMetadata {
        return try {
            val filePath = getFilePath(bucketName, objectKey)

            if (!Files.exists(filePath)) {
                throw ObjectStorageException("File not found: $objectKey")
            }

            val fileAttrs = Files.readAttributes(filePath, BasicFileAttributes::class.java)
            val savedMetadata = loadMetadata(filePath)

            ObjectMetadata(
                objectKey = objectKey,
                contentType = savedMetadata["Content-Type"] ?: "application/octet-stream",
                contentLength = fileAttrs.size(),
                etag = savedMetadata["ETag"],
                lastModified = LocalDateTime.ofInstant(fileAttrs.lastModifiedTime().toInstant(), ZoneOffset.UTC),
                metadata = savedMetadata.filterKeys { !it.startsWith("Content-") && it != "ETag" }
            )
        } catch (e: Exception) {
            logger.error("Failed to get object metadata: bucket={}, key={}", bucketName, objectKey, e)
            throw ObjectStorageException("Failed to get metadata", e)
        }
    }

    override fun listObjects(bucketName: String, prefix: String, maxKeys: Int): List<ObjectSummary> {
        return try {
            val bucketPath = basePath.resolve(bucketName)

            if (!Files.exists(bucketPath)) {
                return emptyList()
            }

            Files.walk(bucketPath)
                .filter { Files.isRegularFile(it) && !it.fileName.toString().endsWith(".metadata") }
                .filter { prefix.isEmpty() || bucketPath.relativize(it).toString().startsWith(prefix) }
                .limit(maxKeys.toLong())
                .map { filePath ->
                    val objectKey = bucketPath.relativize(filePath).toString().replace("\\", "/")
                    val fileAttrs = Files.readAttributes(filePath, BasicFileAttributes::class.java)

                    ObjectSummary(
                        objectKey = objectKey,
                        size = fileAttrs.size(),
                        lastModified = LocalDateTime.ofInstant(fileAttrs.lastModifiedTime().toInstant(), ZoneOffset.UTC),
                        etag = loadMetadata(filePath)["ETag"]
                    )
                }
                .toList()
        } catch (e: Exception) {
            logger.error("Failed to list objects: bucket={}, prefix={}", bucketName, prefix, e)
            throw ObjectStorageException("Failed to list objects", e)
        }
    }

    override fun generatePresignedUrl(bucketName: String, objectKey: String, expirationSeconds: Long): String {
        // For local storage, return a direct URL (no pre-signing needed)
        return generateUrl(bucketName, objectKey)
    }

    override fun copyObject(sourceBucket: String, sourceKey: String, targetBucket: String, targetKey: String): CopyResult {
        return try {
            val sourceFilePath = getFilePath(sourceBucket, sourceKey)
            val targetFilePath = getFilePath(targetBucket, targetKey)

            if (!Files.exists(sourceFilePath)) {
                throw ObjectStorageException("Source file not found: $sourceKey")
            }

            // Create target directory if needed
            Files.createDirectories(targetFilePath.parent)

            // Copy file
            Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING)

            // Copy metadata
            val sourceMetadata = loadMetadata(sourceFilePath)
            saveMetadata(targetFilePath, sourceMetadata["Content-Type"] ?: "application/octet-stream", sourceMetadata)

            logger.debug("File copied: from={}:{} to={}:{}", sourceBucket, sourceKey, targetBucket, targetKey)

            CopyResult(
                sourceKey = sourceKey,
                targetKey = targetKey,
                etag = sourceMetadata["ETag"],
                lastModified = LocalDateTime.now()
            )
        } catch (e: Exception) {
            logger.error("Failed to copy file: from={}:{} to={}:{}", sourceBucket, sourceKey, targetBucket, targetKey, e)
            throw ObjectStorageException("Failed to copy file", e)
        }
    }

    override fun createBucketIfNotExists(bucketName: String) {
        try {
            val bucketPath = basePath.resolve(bucketName)
            if (!Files.exists(bucketPath)) {
                Files.createDirectories(bucketPath)
                logger.info("Created local bucket directory: {}", bucketPath)
            }
        } catch (e: Exception) {
            logger.error("Failed to create bucket: {}", bucketName, e)
            throw ObjectStorageException("Failed to create bucket", e)
        }
    }

    private fun getFilePath(bucketName: String, objectKey: String): Path {
        return basePath.resolve(bucketName).resolve(objectKey)
    }

    private fun getMetadataPath(filePath: Path): Path {
        return filePath.resolveSibling("${filePath.fileName}.metadata")
    }

    private fun saveMetadata(filePath: Path, contentType: String, metadata: Map<String, String>) {
        try {
            val metadataPath = getMetadataPath(filePath)
            val allMetadata = mutableMapOf<String, String>()
            allMetadata["Content-Type"] = contentType
            allMetadata["ETag"] = generateETag(Files.readAllBytes(filePath))
            allMetadata.putAll(metadata)

            val properties = Properties()
            allMetadata.forEach { (key, value) -> properties.setProperty(key, value) }

            Files.newOutputStream(metadataPath).use { output ->
                properties.store(output, "Local storage metadata")
            }
        } catch (e: Exception) {
            logger.warn("Failed to save metadata for file: {}", filePath, e)
        }
    }

    private fun loadMetadata(filePath: Path): Map<String, String> {
        return try {
            val metadataPath = getMetadataPath(filePath)

            if (!Files.exists(metadataPath)) {
                return emptyMap()
            }

            val properties = Properties()
            Files.newInputStream(metadataPath).use { input ->
                properties.load(input)
            }

            properties.stringPropertyNames().associateWith { properties.getProperty(it) }
        } catch (e: Exception) {
            logger.warn("Failed to load metadata for file: {}", filePath, e)
            emptyMap()
        }
    }

    private fun generateUrl(bucketName: String, objectKey: String): String {
        return "${storageProperties.local.urlBase}/$bucketName/$objectKey"
    }

    private fun generateETag(bytes: ByteArray): String {
        return "\"${bytes.contentHashCode().toString(16)}\""
    }
}