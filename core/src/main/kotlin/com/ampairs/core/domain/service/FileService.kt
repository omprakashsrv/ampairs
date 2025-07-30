package com.ampairs.core.domain.service

import com.ampairs.core.config.AwsProperties
import com.ampairs.core.domain.model.File
import com.ampairs.core.respository.FileRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class FileService(
    private val s3Client: S3Client,
    private val fileRepository: FileRepository,
    private val awsProperties: AwsProperties,
) {
    private val logger = LoggerFactory.getLogger(FileService::class.java)

    fun saveFile(
        bytes: ByteArray,
        name: String,
        contentType: String,
        folder: String = "uploads",
    ): File {
        val objectKey = generateObjectKey(name, folder)
        val bucket = awsProperties.s3.bucket

        return try {
            val putObjectResult = s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .serverSideEncryption(ServerSideEncryption.AES256)
                    .build(),
                RequestBody.fromBytes(bytes)
            )

            val file = File().apply {
                this.name = name
                this.objectKey = objectKey
                this.bucket = bucket
                this.contentType = contentType
                this.size = bytes.size.toLong()
                this.etag = putObjectResult.eTag()
            }

            fileRepository.save(file).also {
                logger.info("File saved successfully: name={}, key={}, size={}", name, objectKey, bytes.size)
            }
        } catch (e: Exception) {
            logger.error("Failed to save file: name={}, error={}", name, e.message, e)
            throw FileUploadException("Failed to upload file: ${e.message}", e)
        }
    }

    fun getFileUrl(fileId: Int, expirationMinutes: Long = 60): String {
        val file = fileRepository.findById(fileId)
            .orElseThrow { throw FileNotFoundException("File not found with id: $fileId") }

        return try {
            val getObjectRequest = GetObjectRequest.builder()
                .bucket(file.bucket)
                .key(file.objectKey)
                .build()

            GetObjectPresignRequest.builder()
                .signatureDuration(java.time.Duration.ofMinutes(expirationMinutes))
                .getObjectRequest(getObjectRequest)
                .build()

            s3Client.utilities().getUrl(
                GetUrlRequest.builder()
                    .bucket(file.bucket)
                    .key(file.objectKey)
                    .build()
            ).toString()
        } catch (e: Exception) {
            logger.error("Failed to generate file URL: fileId={}, error={}", fileId, e.message, e)
            throw FileAccessException("Failed to generate file URL: ${e.message}", e)
        }
    }

    fun deleteFile(fileId: Int) {
        val file = fileRepository.findById(fileId)
            .orElseThrow { throw FileNotFoundException("File not found with id: $fileId") }

        try {
            s3Client.deleteObject(
                DeleteObjectRequest.builder()
                    .bucket(file.bucket)
                    .key(file.objectKey)
                    .build()
            )

            fileRepository.delete(file)
            logger.info("File deleted successfully: id={}, key={}", fileId, file.objectKey)
        } catch (e: Exception) {
            logger.error("Failed to delete file: fileId={}, error={}", fileId, e.message, e)
            throw FileDeletionException("Failed to delete file: ${e.message}", e)
        }
    }

    private fun generateObjectKey(fileName: String, folder: String): String {
        val timestamp = LocalDateTime.now().toString().replace(":", "-")
        val uuid = UUID.randomUUID().toString().substring(0, 8)
        val sanitizedFileName = fileName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        return "$folder/$timestamp-$uuid-$sanitizedFileName"
    }
}

class FileUploadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class FileNotFoundException(message: String) : RuntimeException(message)
class FileAccessException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class FileDeletionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)