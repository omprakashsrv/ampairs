package com.ampairs.file.domain.service

import com.ampairs.file.config.StorageProperties
import com.ampairs.file.domain.model.File
import com.ampairs.file.repository.FileRepository
import com.ampairs.file.storage.ObjectStorageService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream

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

    fun saveFile(
        inputStream: InputStream,
        name: String,
        contentType: String,
        contentLength: Long,
        folder: String? = null,
        bucket: String? = null,
    ): File {
        val resolvedFolder = folder ?: "uploads"
        val resolvedBucket = bucket ?: storageProperties.defaultBucket
        val objectKey = generateObjectKey(name, resolvedFolder)

        return try {
            val result = objectStorageService.uploadFile(inputStream, resolvedBucket, objectKey, contentType, contentLength)

            val file = File().apply {
                this.name = name
                this.objectKey = objectKey
                this.bucket = resolvedBucket
                this.contentType = contentType
                this.size = contentLength
                this.etag = result.etag ?: ""
            }

            fileRepository.save(file).also {
                logger.info("File saved from stream: name={}, key={}, size={}", name, objectKey, contentLength)
            }
        } catch (e: Exception) {
            logger.error("Failed to save file from stream: name={}, error={}", name, e.message, e)
            throw FileUploadException("Failed to upload file: ${e.message}", e)
        }
    }

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

    fun getFileContent(fileUid: String): Pair<com.ampairs.file.domain.model.File, InputStream> {
        val file = fileRepository.findByUid(fileUid)
            .orElseThrow { FileNotFoundException("File not found: $fileUid") }
        return try {
            val stream = objectStorageService.downloadFile(file.bucket, file.objectKey)
                .also { logger.debug("Downloaded file: uid={}, key={}", fileUid, file.objectKey) }
            Pair(file, stream)
        } catch (e: Exception) {
            logger.error("Failed to get file content: fileUid={}, error={}", fileUid, e.message, e)
            throw FileAccessException("Failed to get file content: ${e.message}", e)
        }
    }

    fun getFileUrl(fileId: Int, expirationMinutes: Long = 60): String {
        val file = fileRepository.findById(fileId)
            .orElseThrow { FileNotFoundException("File not found with id: $fileId") }

        return try {
            objectStorageService.generatePresignedUrl(file.bucket, file.objectKey, expirationMinutes * 60)
                .also { logger.debug("Generated URL for file: id={}", fileId) }
        } catch (e: Exception) {
            logger.error("Failed to generate URL: fileId={}, error={}", fileId, e.message, e)
            throw FileAccessException("Failed to generate file URL: ${e.message}", e)
        }
    }

    fun getFileContent(fileId: Int): InputStream {
        val file = fileRepository.findById(fileId)
            .orElseThrow { FileNotFoundException("File not found with id: $fileId") }

        return try {
            objectStorageService.downloadFile(file.bucket, file.objectKey)
                .also { logger.debug("Downloaded file: id={}, key={}", fileId, file.objectKey) }
        } catch (e: Exception) {
            logger.error("Failed to get file content: fileId={}, error={}", fileId, e.message, e)
            throw FileAccessException("Failed to get file content: ${e.message}", e)
        }
    }

    fun fileExists(fileId: Int): Boolean {
        val file = fileRepository.findById(fileId).orElse(null) ?: return false
        return try {
            objectStorageService.objectExists(file.bucket, file.objectKey)
        } catch (e: Exception) {
            logger.warn("Error checking file existence: fileId={}, error={}", fileId, e.message)
            false
        }
    }

    fun deleteFile(fileId: Int) {
        val file = fileRepository.findById(fileId)
            .orElseThrow { FileNotFoundException("File not found with id: $fileId") }

        try {
            objectStorageService.deleteObject(file.bucket, file.objectKey)
            fileRepository.delete(file)
            logger.info("File deleted: id={}, key={}", fileId, file.objectKey)
        } catch (e: Exception) {
            logger.error("Failed to delete file: fileId={}, error={}", fileId, e.message, e)
            throw FileDeletionException("Failed to delete file: ${e.message}", e)
        }
    }

    fun listFiles(folder: String, bucket: String? = null): List<String> {
        val resolvedBucket = bucket ?: storageProperties.defaultBucket
        return try {
            objectStorageService.listObjects(resolvedBucket, folder).map { it.objectKey }
                .also { logger.debug("Listed {} files in folder: {}", it.size, folder) }
        } catch (e: Exception) {
            logger.error("Failed to list files: folder={}, error={}", folder, e.message, e)
            emptyList()
        }
    }

    fun copyFile(fileId: Int, newFolder: String, newName: String? = null): File {
        val sourceFile = fileRepository.findById(fileId)
            .orElseThrow { FileNotFoundException("File not found with id: $fileId") }

        val targetName = newName ?: sourceFile.name
        val targetKey = generateObjectKey(targetName, newFolder)

        return try {
            val result = objectStorageService.copyObject(sourceFile.bucket, sourceFile.objectKey, sourceFile.bucket, targetKey)

            val copiedFile = File().apply {
                this.name = targetName
                this.objectKey = targetKey
                this.bucket = sourceFile.bucket
                this.contentType = sourceFile.contentType
                this.size = sourceFile.size
                this.etag = result.etag ?: ""
            }

            fileRepository.save(copiedFile).also {
                logger.info("File copied: {} -> {}", sourceFile.objectKey, targetKey)
            }
        } catch (e: Exception) {
            logger.error("Failed to copy file: fileId={}, error={}", fileId, e.message, e)
            throw FileOperationException("Failed to copy file: ${e.message}", e)
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
class FileOperationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class FileSizeExceededException(message: String) : RuntimeException(message)
class UnsupportedContentTypeException(message: String) : RuntimeException(message)
