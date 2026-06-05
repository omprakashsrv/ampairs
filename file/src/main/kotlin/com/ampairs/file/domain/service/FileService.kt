package com.ampairs.file.domain.service

import com.ampairs.file.config.StorageProperties
import com.ampairs.file.domain.model.File
import com.ampairs.file.repository.FileRepository
import com.ampairs.file.storage.ObjectStorageService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class FileService(
    private val objectStorageService: ObjectStorageService,
    private val fileRepository: FileRepository,
    private val storageProperties: StorageProperties,
) {
    private val logger = LoggerFactory.getLogger(FileService::class.java)

    fun saveFile(
        bytes: ByteArray,
        name: String,
        contentType: String,
        folder: String? = null,
        bucket: String? = null,
    ): File {
        val resolvedFolder = folder ?: "uploads"
        val resolvedBucket = bucket ?: storageProperties.defaultBucket
        val objectKey = generateObjectKey(name, resolvedFolder)

        return try {
            val result = objectStorageService.uploadFile(bytes, resolvedBucket, objectKey, contentType)

            val file = File().apply {
                this.name = name
                this.objectKey = objectKey
                this.bucket = resolvedBucket
                this.contentType = contentType
                this.size = bytes.size.toLong()
                this.etag = result.etag ?: ""
            }

            fileRepository.save(file).also {
                logger.info("File saved: name={}, key={}, size={}", name, objectKey, bytes.size)
            }
        } catch (e: Exception) {
            logger.error("Failed to save file: name={}, error={}", name, e.message, e)
            throw FileUploadException("Failed to upload file: ${e.message}", e)
        }
    }

    @Transactional(readOnly = true)
    fun getFile(fileUid: String): File {
        return fileRepository.findByUid(fileUid)
            .orElseThrow { FileNotFoundException("File not found: $fileUid") }
    }

    @Transactional(readOnly = true)
    fun getFileUrl(fileUid: String, expirationMinutes: Long = 60): String {
        val file = fileRepository.findByUid(fileUid)
            .orElseThrow { FileNotFoundException("File not found: $fileUid") }
        return try {
            objectStorageService.generatePresignedUrl(file.bucket, file.objectKey, expirationMinutes * 60)
                .also { logger.debug("Generated URL for file: uid={}", fileUid) }
        } catch (e: Exception) {
            logger.error("Failed to generate URL: fileUid={}, error={}", fileUid, e.message, e)
            throw FileAccessException("Failed to generate file URL: ${e.message}", e)
        }
    }

    private fun generateObjectKey(fileName: String, folder: String): String {
        val timestamp = java.time.Instant.now().toString().replace(":", "-")
        val uuid = java.util.UUID.randomUUID().toString().substring(0, 8)
        val sanitizedFileName = fileName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        val cleanFolder = folder.trim('/').let { if (it.isEmpty()) "" else "$it/" }
        return "$cleanFolder$timestamp-$uuid-$sanitizedFileName"
    }
}

class FileUploadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class FileNotFoundException(message: String) : RuntimeException(message)
class FileAccessException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class FileDeletionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
