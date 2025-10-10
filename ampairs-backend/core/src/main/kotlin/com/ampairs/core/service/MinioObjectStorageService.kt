package com.ampairs.core.service

import com.ampairs.core.config.MinioProperties
import com.ampairs.core.config.StorageProperties
import io.minio.*
import io.minio.errors.ErrorResponseException
import io.minio.errors.MinioException
import io.minio.http.Method
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.InputStream
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * MinIO implementation of ObjectStorageService
 */
@Service
@ConditionalOnProperty(
    name = ["ampairs.storage.provider"],
    havingValue = "MINIO"
)
class MinioObjectStorageService(
    private val minioProperties: MinioProperties,
    private val storageProperties: StorageProperties
) : ObjectStorageService {

    private val logger = LoggerFactory.getLogger(MinioObjectStorageService::class.java)

    private val minioClient: MinioClient by lazy {
        MinioClient.builder()
            .endpoint(minioProperties.endpoint)
            .credentials(minioProperties.accessKey, minioProperties.secretKey)
            .build()
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

            if (minioProperties.autoCreateBucket) {
                createBucketIfNotExists(bucketName)
            }

            val putObjectArgs = PutObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectKey)
                .stream(inputStream, contentLength, -1)
                .contentType(contentType)
                .userMetadata(metadata)
                .build()

            val response = minioClient.putObject(putObjectArgs)

            logger.info("File uploaded to MinIO: bucket={}, key={}, size={}", bucketName, objectKey, contentLength)

            UploadResult(
                objectKey = objectKey,
                etag = response.etag(),
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
        return uploadFile(
            bytes.inputStream(),
            bucketName,
            objectKey,
            contentType,
            bytes.size.toLong(),
            metadata
        )
    }

    override fun downloadFile(bucketName: String, objectKey: String): InputStream {
        return try {
            validateObjectKey(objectKey)

            if (!objectExists(bucketName, objectKey)) {
                throw ObjectNotFoundException("Object not found: $bucketName/$objectKey")
            }

            val getObjectArgs = GetObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectKey)
                .build()

            val inputStream = minioClient.getObject(getObjectArgs)
            logger.debug("File downloaded from MinIO: bucket={}, key={}", bucketName, objectKey)

            inputStream
        } catch (e: ObjectNotFoundException) {
            throw e
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

            val statObjectArgs = StatObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectKey)
                .build()

            val response = minioClient.statObject(statObjectArgs)

            ObjectMetadata(
                objectKey = objectKey,
                contentType = response.contentType() ?: "application/octet-stream",
                contentLength = response.size(),
                etag = response.etag(),
                lastModified = response.lastModified()?.toInstant(),
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

            val getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .`object`(objectKey)
                .expiry(expirationSeconds.toInt(), TimeUnit.SECONDS)
                .build()

            val presignedUrl = minioClient.getPresignedObjectUrl(getPresignedObjectUrlArgs)

            logger.debug("Generated presigned URL for MinIO object: bucket={}, key={}, expires={}s", bucketName, objectKey, expirationSeconds)
            presignedUrl
        } catch (e: Exception) {
            logger.error("Failed to generate presigned URL for MinIO: bucket={}, key={}, error={}", bucketName, objectKey, e.message, e)
            throw ObjectStorageException("Failed to generate presigned URL: ${e.message}", e)
        }
    }

    override fun objectExists(bucketName: String, objectKey: String): Boolean {
        return try {
            validateObjectKey(objectKey)

            val statObjectArgs = StatObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectKey)
                .build()

            minioClient.statObject(statObjectArgs)
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

            if (!objectExists(bucketName, objectKey)) {
                logger.warn("Attempted to delete non-existent object: bucket={}, key={}", bucketName, objectKey)
                return
            }

            val removeObjectArgs = RemoveObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectKey)
                .build()

            minioClient.removeObject(removeObjectArgs)
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

            if (minioProperties.autoCreateBucket) {
                createBucketIfNotExists(targetBucket)
            }

            val copySource = CopySource.builder()
                .bucket(sourceBucket)
                .`object`(sourceKey)
                .build()

            val copyObjectArgs = CopyObjectArgs.builder()
                .bucket(targetBucket)
                .`object`(targetKey)
                .source(copySource)
                .build()

            val response = minioClient.copyObject(copyObjectArgs)

            logger.info("Object copied in MinIO: from={}:{} to={}:{}", sourceBucket, sourceKey, targetBucket, targetKey)

            CopyResult(
                sourceKey = sourceKey,
                targetKey = targetKey,
                etag = response.etag(),
                lastModified = Instant.now()
            )
        } catch (e: Exception) {
            logger.error("Failed to copy object in MinIO: source={}:{}, target={}:{}, error={}",
                sourceBucket, sourceKey, targetBucket, targetKey, e.message, e)
            throw ObjectCopyException("Failed to copy object in MinIO: ${e.message}", e)
        }
    }

    override fun listObjects(bucketName: String, prefix: String, maxKeys: Int): List<ObjectSummary> {
        return try {
            val listObjectsArgs = ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .maxKeys(maxKeys)
                .build()

            val results = minioClient.listObjects(listObjectsArgs)

            results.map { result ->
                val item = result.get()
                ObjectSummary(
                    objectKey = item.objectName(),
                    size = item.size(),
                    lastModified = item.lastModified()?.toInstant(),
                    etag = item.etag()
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to list objects in MinIO: bucket={}, prefix={}, error={}", bucketName, prefix, e.message, e)
            emptyList()
        }
    }

    override fun createBucketIfNotExists(bucketName: String) {
        try {
            val bucketExistsArgs = BucketExistsArgs.builder().bucket(bucketName).build()
            val exists = minioClient.bucketExists(bucketExistsArgs)

            if (!exists) {
                val makeBucketArgs = MakeBucketArgs.builder().bucket(bucketName).build()
                minioClient.makeBucket(makeBucketArgs)
                logger.info("MinIO bucket created: {}", bucketName)
            } else {
                logger.debug("MinIO bucket already exists: {}", bucketName)
            }
        } catch (e: Exception) {
            logger.error("Failed to create MinIO bucket: {}, error={}", bucketName, e.message, e)
            throw BucketCreationException("Failed to create MinIO bucket: ${e.message}", e)
        }
    }

    private fun validateObjectKey(objectKey: String) {
        if (objectKey.isBlank()) {
            throw InvalidObjectKeyException("Object key cannot be blank")
        }
        if (objectKey.contains("//")) {
            throw InvalidObjectKeyException("Object key cannot contain double slashes")
        }
        if (objectKey.startsWith("/")) {
            throw InvalidObjectKeyException("Object key cannot start with slash")
        }
    }

    private fun validateFileSize(size: Long) {
        val maxSize = parseSize(storageProperties.maxFileSize)
        if (size > maxSize) {
            throw ObjectUploadException("File size ($size bytes) exceeds maximum allowed size ($maxSize bytes)")
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
