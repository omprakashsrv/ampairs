package com.ampairs.file.storage

import com.ampairs.file.config.StorageProperties
import io.awspring.cloud.s3.S3Template
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import java.io.InputStream
import java.time.Instant

/**
 * S3 implementation of ObjectStorageService using Spring Cloud AWS S3Template.
 */
@Service
@ConditionalOnProperty(
    name = ["ampairs.storage.provider"],
    havingValue = "S3",
    matchIfMissing = true
)
class S3ObjectStorageService(
    private val s3Template: S3Template,
    private val s3Client: S3Client,
    private val storageProperties: StorageProperties
) : ObjectStorageService {

    private val logger = LoggerFactory.getLogger(S3ObjectStorageService::class.java)

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

            val s3Resource = s3Template.upload(bucketName, objectKey, inputStream)

            logger.info("File uploaded to S3: bucket={}, key={}, size={}", bucketName, objectKey, contentLength)

            UploadResult(
                objectKey = objectKey,
                etag = s3Resource.contentLength().toString(),
                contentLength = contentLength,
                lastModified = Instant.now(),
                url = generatePresignedUrl(bucketName, objectKey)
            )
        } catch (e: Exception) {
            logger.error("Failed to upload file to S3: bucket={}, key={}, error={}", bucketName, objectKey, e.message, e)
            throw ObjectUploadException("Failed to upload file to S3: ${e.message}", e)
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

            val s3Resource = s3Template.download(bucketName, objectKey)
            logger.debug("File downloaded from S3: bucket={}, key={}", bucketName, objectKey)

            s3Resource.inputStream
        } catch (e: ObjectNotFoundException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to download file from S3: bucket={}, key={}, error={}", bucketName, objectKey, e.message, e)
            throw ObjectDownloadException("Failed to download file from S3: ${e.message}", e)
        }
    }

    override fun getObjectMetadata(bucketName: String, objectKey: String): ObjectMetadata {
        return try {
            validateObjectKey(objectKey)

            val headRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build()

            val response = s3Client.headObject(headRequest)

            ObjectMetadata(
                objectKey = objectKey,
                contentType = response.contentType() ?: "application/octet-stream",
                contentLength = response.contentLength(),
                etag = response.eTag(),
                lastModified = response.lastModified(),
                metadata = response.metadata()
            )
        } catch (e: NoSuchKeyException) {
            throw ObjectNotFoundException("Object not found: $bucketName/$objectKey")
        } catch (e: Exception) {
            logger.error(
                "Failed to get object metadata from S3: bucket={}, key={}, error={}",
                bucketName,
                objectKey,
                e.message,
                e
            )
            throw ObjectStorageException("Failed to get object metadata: ${e.message}", e)
        }
    }

    override fun generatePresignedUrl(bucketName: String, objectKey: String, expirationSeconds: Long): String {
        return try {
            validateObjectKey(objectKey)

            val duration = java.time.Duration.ofSeconds(expirationSeconds)
            val presignedUrl = s3Template.createSignedGetURL(bucketName, objectKey, duration)

            logger.debug(
                "Generated presigned URL for S3 object: bucket={}, key={}, expires={}s",
                bucketName,
                objectKey,
                expirationSeconds
            )
            presignedUrl.toString()
        } catch (e: Exception) {
            logger.error(
                "Failed to generate presigned URL for S3: bucket={}, key={}, error={}",
                bucketName,
                objectKey,
                e.message,
                e
            )
            throw ObjectStorageException("Failed to generate presigned URL: ${e.message}", e)
        }
    }

    override fun objectExists(bucketName: String, objectKey: String): Boolean {
        return try {
            validateObjectKey(objectKey)
            s3Template.objectExists(bucketName, objectKey)
        } catch (e: Exception) {
            logger.warn(
                "Error checking object existence in S3: bucket={}, key={}, error={}",
                bucketName,
                objectKey,
                e.message
            )
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

            s3Template.deleteObject(bucketName, objectKey)
            logger.info("File deleted from S3: bucket={}, key={}", bucketName, objectKey)
        } catch (e: Exception) {
            logger.error("Failed to delete object from S3: bucket={}, key={}, error={}", bucketName, objectKey, e.message, e)
            throw ObjectDeleteException("Failed to delete object from S3: ${e.message}", e)
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

            val copyRequest = CopyObjectRequest.builder()
                .sourceBucket(sourceBucket)
                .sourceKey(sourceKey)
                .destinationBucket(targetBucket)
                .destinationKey(targetKey)
                .build()

            val response = s3Client.copyObject(copyRequest)

            logger.info(
                "Object copied in S3: from={}:{} to={}:{}",
                sourceBucket,
                sourceKey,
                targetBucket,
                targetKey
            )

            CopyResult(
                sourceKey = sourceKey,
                targetKey = targetKey,
                etag = response.copyObjectResult().eTag(),
                lastModified = response.copyObjectResult().lastModified()
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to copy object in S3: source={}:{}, target={}:{}, error={}",
                sourceBucket,
                sourceKey,
                targetBucket,
                targetKey,
                e.message,
                e
            )
            throw ObjectCopyException("Failed to copy object in S3: ${e.message}", e)
        }
    }

    override fun listObjects(bucketName: String, prefix: String, maxKeys: Int): List<ObjectSummary> {
        return try {
            val request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix.ifBlank { null })
                .maxKeys(maxKeys)
                .build()

            val response = s3Client.listObjectsV2(request)

            response.contents()?.map {
                ObjectSummary(
                    objectKey = it.key(),
                    size = it.size(),
                    lastModified = it.lastModified(),
                    etag = it.eTag()
                )
            } ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to list objects in S3: bucket={}, prefix={}, error={}", bucketName, prefix, e.message, e)
            throw ObjectStorageException("Failed to list objects in S3: ${e.message}", e)
        }
    }

    override fun createBucketIfNotExists(bucketName: String) {
        try {
            val existingBuckets = s3Client.listBuckets().buckets()
            if (existingBuckets.any { it.name() == bucketName }) {
                logger.debug("S3 bucket already exists: {}", bucketName)
                return
            }

            val createBucketRequest = software.amazon.awssdk.services.s3.model.CreateBucketRequest.builder()
                .bucket(bucketName)
                .build()

            s3Client.createBucket(createBucketRequest)
            logger.info("S3 bucket created: {}", bucketName)
        } catch (e: Exception) {
            logger.error("Failed to create S3 bucket: {}, error={}", bucketName, e.message, e)
            throw BucketCreationException("Failed to create S3 bucket: ${e.message}", e)
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
