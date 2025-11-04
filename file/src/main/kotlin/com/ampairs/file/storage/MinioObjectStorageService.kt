package com.ampairs.file.storage

import com.ampairs.file.config.MinioProperties
import com.ampairs.file.config.StorageProperties
import io.minio.*
import io.minio.errors.ErrorResponseException
import io.minio.http.Method
import io.minio.messages.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.InputStream
import java.time.Instant

/**
 * MinIO implementation of ObjectStorageService.
 */
@Service
@ConditionalOnProperty(name = ["ampairs.storage.provider"], havingValue = "MINIO")
class MinioObjectStorageService(
    private val minioClient: MinioClient,
    private val storageProperties: StorageProperties,
    private val minioProperties: MinioProperties
) : ObjectStorageService {

    private val logger = LoggerFactory.getLogger(MinioObjectStorageService::class.java)

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
            createBucketIfNotExists(bucketName)

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectKey)
                    .stream(inputStream, contentLength, -1)
                    .contentType(contentType)
                    .headers(metadata)
                    .build()
            )

            logger.info("File uploaded to MinIO: bucket={}, key={}, size={}", bucketName, objectKey, contentLength)

            UploadResult(
                objectKey = objectKey,
                etag = null,
                contentLength = contentLength,
                lastModified = Instant.now(),
                url = generatePresignedUrl(bucketName, objectKey)
            )
        } catch (e: Exception) {
            logger.error("Failed to upload file to MinIO: bucket={}, key={}, error={}", bucketName, objectKey, e.message, e)
            throw ObjectUploadException("Failed to upload file to MinIO: ${e.message}", e)
        }
    }

    override fun uploadFile(
        bytes: ByteArray,
        bucketName: String,
        objectKey: String,
        contentType: String,
        metadata: Map<String, String>
    ): UploadResult {
        return uploadFile(bytes.inputStream(), bucketName, objectKey, contentType, bytes.size.toLong(), metadata)
    }

    override fun downloadFile(bucketName: String, objectKey: String): InputStream {
        return try {
            validateObjectKey(objectKey)
            val args = GetObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectKey)
                .build()

            minioClient.getObject(args).also {
                logger.debug("File downloaded from MinIO: bucket={}, key={}", bucketName, objectKey)
            }
        } catch (e: ErrorResponseException) {
            if (e.errorResponse().code() == "NoSuchKey") {
                throw ObjectNotFoundException("Object not found: $bucketName/$objectKey")
            }
            logger.error("Failed to download file from MinIO: bucket={}, key={}, error={}", bucketName, objectKey, e.message, e)
            throw ObjectDownloadException("Failed to download file from MinIO: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Failed to download file from MinIO: bucket={}, key={}, error={}", bucketName, objectKey, e.message, e)
            throw ObjectDownloadException("Failed to download file from MinIO: ${e.message}", e)
        }
    }

    override fun getObjectMetadata(bucketName: String, objectKey: String): ObjectMetadata {
        return try {
            validateObjectKey(objectKey)

            val args = StatObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectKey)
                .build()

            val response = minioClient.statObject(args)

            ObjectMetadata(
                objectKey = objectKey,
                contentType = response.contentType(),
                contentLength = response.size(),
                etag = response.etag(),
                lastModified = response.lastModified().toInstant(),
                metadata = response.userMetadata()
            )
        } catch (e: ErrorResponseException) {
            if (e.errorResponse().code() == "NoSuchKey") {
                throw ObjectNotFoundException("Object not found: $bucketName/$objectKey")
            }
            logger.error("Failed to get object metadata from MinIO: bucket={}, key={}, error={}", bucketName, objectKey, e.message, e)
            throw ObjectStorageException("Failed to get object metadata: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Failed to get object metadata from MinIO: bucket={}, key={}, error={}", bucketName, objectKey, e.message, e)
            throw ObjectStorageException("Failed to get object metadata: ${e.message}", e)
        }
    }

    override fun generatePresignedUrl(bucketName: String, objectKey: String, expirationSeconds: Long): String {
        return try {
            validateObjectKey(objectKey)

            val args = GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .`object`(objectKey)
                .expiry(expirationSeconds.toInt())
                .build()

            minioClient.getPresignedObjectUrl(args).also {
                logger.debug("Generated presigned URL for MinIO object: bucket={}, key={}, expires={}s", bucketName, objectKey, expirationSeconds)
            }
        } catch (e: Exception) {
            logger.error("Failed to generate presigned URL for MinIO: bucket={}, key={}, error={}", bucketName, objectKey, e.message, e)
            throw ObjectStorageException("Failed to generate presigned URL: ${e.message}", e)
        }
    }

    override fun objectExists(bucketName: String, objectKey: String): Boolean {
        return try {
            validateObjectKey(objectKey)

            val args = StatObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectKey)
                .build()

            minioClient.statObject(args)
            true
        } catch (e: ErrorResponseException) {
            if (e.errorResponse().code() == "NoSuchKey") {
                false
            } else {
                logger.warn("Error checking object existence in MinIO: bucket={}, key={}, error={}", bucketName, objectKey, e.message)
                false
            }
        } catch (e: Exception) {
            logger.warn("Error checking object existence in MinIO: bucket={}, key={}, error={}", bucketName, objectKey, e.message)
            false
        }
    }

    override fun deleteObject(bucketName: String, objectKey: String) {
        try {
            validateObjectKey(objectKey)

            val args = RemoveObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectKey)
                .build()

            minioClient.removeObject(args)
            logger.info("File deleted from MinIO: bucket={}, key={}", bucketName, objectKey)
        } catch (e: Exception) {
            logger.error("Failed to delete object from MinIO: bucket={}, key={}, error={}", bucketName, objectKey, e.message, e)
            throw ObjectDeleteException("Failed to delete object from MinIO: ${e.message}", e)
        }
    }

    override fun copyObject(
        sourceBucket: String,
        sourceKey: String,
        targetBucket: String,
        targetKey: String
    ): CopyResult {
        return try {
            validateObjectKey(sourceKey)
            validateObjectKey(targetKey)

            minioClient.copyObject(
                CopyObjectArgs.builder()
                    .bucket(targetBucket)
                    .`object`(targetKey)
                    .source(CopySource.builder().bucket(sourceBucket).`object`(sourceKey).build())
                    .build()
            )

            logger.info("Object copied in MinIO: from={}:{} to={}:{}", sourceBucket, sourceKey, targetBucket, targetKey)

            CopyResult(
                sourceKey = sourceKey,
                targetKey = targetKey,
                etag = null,
                lastModified = Instant.now()
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to copy object in MinIO: source={}:{}, target={}:{}, error={}",
                sourceBucket,
                sourceKey,
                targetBucket,
                targetKey,
                e.message,
                e
            )
            throw ObjectCopyException("Failed to copy object in MinIO: ${e.message}", e)
        }
    }

    override fun listObjects(bucketName: String, prefix: String, maxKeys: Int): List<ObjectSummary> {
        return try {
            val args = ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(prefix.ifBlank { null })
                .recursive(true)
                .build()

            minioClient.listObjects(args)
                .asSequence()
                .take(maxKeys)
                .mapNotNull { result ->
                    try {
                        val item = result.get()
                        ObjectSummary(
                            objectKey = item.objectName(),
                            size = item.size(),
                            lastModified = item.lastModified()?.toInstant(),
                            etag = item.etag()
                        )
                    } catch (e: Exception) {
                        logger.warn("Failed to process MinIO object listing: {}", e.message)
                        null
                    }
                }.toList()
        } catch (e: Exception) {
            logger.error("Failed to list objects in MinIO: bucket={}, prefix={}, error={}", bucketName, prefix, e.message, e)
            throw ObjectStorageException("Failed to list objects in MinIO: ${e.message}", e)
        }
    }

    override fun createBucketIfNotExists(bucketName: String) {
        try {
            val bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
            )
            if (bucketExists) {
                logger.debug("MinIO bucket already exists: {}", bucketName)
                return
            }

            minioClient.makeBucket(
                MakeBucketArgs.builder().bucket(bucketName).build()
            )
            logger.info("MinIO bucket created: {}", bucketName)
        } catch (e: Exception) {
            logger.error("Failed to create MinIO bucket: {}, error={}", bucketName, e.message, e)
            throw BucketCreationException("Failed to create MinIO bucket: ${e.message}", e)
        }
    }

    private fun validateObjectKey(objectKey: String) {
        if (objectKey.isBlank()) {
            throw InvalidObjectKeyException("Object key must not be blank")
        }
        if (objectKey.contains("..")) {
            throw InvalidObjectKeyException("Object key must not contain '..'")
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
}
