package com.ampairs.core.domain.service

import com.ampairs.core.config.AmpairsAwsProperties
import com.ampairs.core.domain.model.File
import com.ampairs.core.respository.FileRepository
import io.awspring.cloud.s3.S3Template
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.time.LocalDateTime
import java.util.*

/**
 * Enhanced File Service using Spring Cloud AWS S3Template
 * This uses Spring Cloud AWS auto-configuration instead of manual S3Client configuration
 */
@Service
@Transactional
@ConditionalOnProperty(name = ["spring.cloud.aws.s3.enabled"], havingValue = "true", matchIfMissing = true)
class FileService(
    private val s3Template: S3Template,
    private val fileRepository: FileRepository,
    private val ampairsAwsProperties: AmpairsAwsProperties,
) {
    private val logger = LoggerFactory.getLogger(FileService::class.java)

    /**
     * Save file to S3 using Spring Cloud AWS S3Template
     */
    fun saveFile(
        bytes: ByteArray,
        name: String,
        contentType: String,
        folder: String = ampairsAwsProperties.s3.uploadFolder,
        bucket: String = ampairsAwsProperties.s3.defaultBucket,
    ): File {
        val objectKey = generateObjectKey(name, folder)

        return try {
            // Validate file size
            validateFileSize(bytes.size.toLong())

            // Validate content type
            validateContentType(contentType)

            // Upload using S3Template
            val s3Resource = s3Template.upload(bucket, objectKey, bytes.inputStream())

            val file = File().apply {
                this.name = name
                this.objectKey = objectKey
                this.bucket = bucket
                this.contentType = contentType
                this.size = bytes.size.toLong()
                this.etag = s3Resource.contentLength().toString() // Use content length as ETag substitute
            }

            fileRepository.save(file).also {
                logger.info(
                    "File saved successfully using S3Template: name={}, key={}, size={}",
                    name,
                    objectKey,
                    bytes.size
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to save file using S3Template: name={}, error={}", name, e.message, e)
            throw FileUploadException("Failed to upload file: ${e.message}", e)
        }
    }

    /**
     * Save file from InputStream
     */
    fun saveFile(
        inputStream: InputStream,
        name: String,
        contentType: String,
        contentLength: Long,
        folder: String = ampairsAwsProperties.s3.uploadFolder,
        bucket: String = ampairsAwsProperties.s3.defaultBucket,
    ): File {
        val objectKey = generateObjectKey(name, folder)

        return try {
            validateFileSize(contentLength)
            validateContentType(contentType)

            val s3Resource = s3Template.upload(bucket, objectKey, inputStream)

            val file = File().apply {
                this.name = name
                this.objectKey = objectKey
                this.bucket = bucket
                this.contentType = contentType
                this.size = contentLength
                this.etag = s3Resource.contentLength().toString()
            }

            fileRepository.save(file).also {
                logger.info(
                    "File saved from InputStream using S3Template: name={}, key={}, size={}",
                    name,
                    objectKey,
                    contentLength
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to save file from InputStream: name={}, error={}", name, e.message, e)
            throw FileUploadException("Failed to upload file: ${e.message}", e)
        }
    }

    /**
     * Get presigned URL for file access
     */
    fun getFileUrl(fileId: Int, expirationMinutes: Long = 60): String {
        val file = fileRepository.findById(fileId)
            .orElseThrow { throw FileNotFoundException("File not found with id: $fileId") }

        return try {
            val duration = java.time.Duration.ofMinutes(expirationMinutes)
            val presignedUrl = s3Template.createSignedGetURL(file.bucket, file.objectKey, duration)

            logger.debug("Generated presigned URL for file: id={}, expiresIn={}min", fileId, expirationMinutes)
            presignedUrl.toString()
        } catch (e: Exception) {
            logger.error("Failed to generate presigned URL using S3Template: fileId={}, error={}", fileId, e.message, e)
            throw FileAccessException("Failed to generate file URL: ${e.message}", e)
        }
    }

    /**
     * Get file content as InputStream
     */
    fun getFileContent(fileId: Int): InputStream {
        val file = fileRepository.findById(fileId)
            .orElseThrow { throw FileNotFoundException("File not found with id: $fileId") }

        return try {
            val s3Resource = s3Template.download(file.bucket, file.objectKey)
            logger.debug("Downloaded file content: id={}, key={}", fileId, file.objectKey)
            s3Resource.inputStream
        } catch (e: Exception) {
            logger.error("Failed to get file content: fileId={}, error={}", fileId, e.message, e)
            throw FileAccessException("Failed to get file content: ${e.message}", e)
        }
    }

    /**
     * Check if file exists in S3
     */
    fun fileExists(fileId: Int): Boolean {
        val file = fileRepository.findById(fileId)
            .orElse(null) ?: return false

        return try {
            s3Template.objectExists(file.bucket, file.objectKey)
        } catch (e: Exception) {
            logger.warn("Error checking file existence: fileId={}, error={}", fileId, e.message)
            false
        }
    }

    /**
     * Delete file from S3 and database
     */
    fun deleteFile(fileId: Int) {
        val file = fileRepository.findById(fileId)
            .orElseThrow { throw FileNotFoundException("File not found with id: $fileId") }

        try {
            // Delete from S3 using S3Template
            s3Template.deleteObject(file.bucket, file.objectKey)

            // Delete from database
            fileRepository.delete(file)
            logger.info("File deleted successfully using S3Template: id={}, key={}", fileId, file.objectKey)
        } catch (e: Exception) {
            logger.error("Failed to delete file using S3Template: fileId={}, error={}", fileId, e.message, e)
            throw FileDeletionException("Failed to delete file: ${e.message}", e)
        }
    }

    /**
     * List files in a folder
     */
    fun listFiles(
        folder: String = ampairsAwsProperties.s3.uploadFolder,
        bucket: String = ampairsAwsProperties.s3.defaultBucket,
    ): List<String> {
        return try {
            val prefix = if (folder.endsWith("/")) folder else "$folder/"
            // Use AWS SDK directly for listing objects since S3Template API is limited
            val s3Client = software.amazon.awssdk.services.s3.S3Client.create()
            val listRequest = software.amazon.awssdk.services.s3.model.ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build()
            val response = s3Client.listObjectsV2(listRequest)
            response.contents()
                .map { it.key() }
                .also { logger.debug("Listed {} files in folder: {}", it.size, folder) }
        } catch (e: Exception) {
            logger.error("Failed to list files in folder: {}, error={}", folder, e.message, e)
            emptyList()
        }
    }

    /**
     * Copy file to another location
     */
    fun copyFile(fileId: Int, newFolder: String, newName: String? = null): File {
        val sourceFile = fileRepository.findById(fileId)
            .orElseThrow { throw FileNotFoundException("File not found with id: $fileId") }

        val targetName = newName ?: sourceFile.name
        val targetKey = generateObjectKey(targetName, newFolder)

        return try {
            // Copy operation using AWS SDK directly since S3Template doesn't provide copy
            val s3Client = software.amazon.awssdk.services.s3.S3Client.create()
            s3Client.copyObject(
                software.amazon.awssdk.services.s3.model.CopyObjectRequest.builder()
                    .sourceBucket(sourceFile.bucket)
                    .sourceKey(sourceFile.objectKey)
                    .destinationBucket(sourceFile.bucket)
                    .destinationKey(targetKey)
                    .build()
            )

            val copiedFile = File().apply {
                this.name = targetName
                this.objectKey = targetKey
                this.bucket = sourceFile.bucket
                this.contentType = sourceFile.contentType
                this.size = sourceFile.size
                this.etag = sourceFile.etag
            }

            fileRepository.save(copiedFile).also {
                logger.info("File copied successfully: from={} to={}", sourceFile.objectKey, targetKey)
            }
        } catch (e: Exception) {
            logger.error("Failed to copy file: fileId={}, error={}", fileId, e.message, e)
            throw FileOperationException("Failed to copy file: ${e.message}", e)
        }
    }

    private fun generateObjectKey(fileName: String, folder: String): String {
        val timestamp = LocalDateTime.now().toString().replace(":", "-")
        val uuid = UUID.randomUUID().toString().substring(0, 8)
        val sanitizedFileName = fileName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        val cleanFolder = folder.trim('/').let { if (it.isEmpty()) "" else "$it/" }
        return "$cleanFolder$timestamp-$uuid-$sanitizedFileName"
    }

    private fun validateFileSize(size: Long) {
        val maxSize = parseSize(ampairsAwsProperties.s3.maxFileSize)
        if (size > maxSize) {
            throw FileSizeExceededException("File size ($size bytes) exceeds maximum allowed size ($maxSize bytes)")
        }
    }

    private fun validateContentType(contentType: String) {
        if (ampairsAwsProperties.s3.allowedContentTypes.isNotEmpty() &&
            !ampairsAwsProperties.s3.allowedContentTypes.contains(contentType)
        ) {
            throw UnsupportedContentTypeException("Content type '$contentType' is not allowed")
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
}

// Exception classes
class FileUploadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class FileNotFoundException(message: String) : RuntimeException(message)
class FileAccessException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class FileDeletionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class FileOperationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class FileSizeExceededException(message: String) : RuntimeException(message)
class UnsupportedContentTypeException(message: String) : RuntimeException(message)