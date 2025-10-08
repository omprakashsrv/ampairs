package com.ampairs.core.service

import java.io.InputStream
import java.time.LocalDateTime

/**
 * Object Storage Service interface that supports both S3 and MinIO implementations.
 * Provides workspace-aware file storage with metadata management.
 */
interface ObjectStorageService {

    /**
     * Upload file to object storage
     * @param inputStream File content as InputStream
     * @param bucketName Target bucket name
     * @param objectKey Full object key/path
     * @param contentType MIME type of the file
     * @param contentLength Size of the file in bytes
     * @param metadata Additional metadata for the object
     * @return Upload result containing metadata
     */
    fun uploadFile(
        inputStream: InputStream,
        bucketName: String,
        objectKey: String,
        contentType: String,
        contentLength: Long,
        metadata: Map<String, String> = emptyMap()
    ): UploadResult

    /**
     * Upload file from byte array
     */
    fun uploadFile(
        bytes: ByteArray,
        bucketName: String,
        objectKey: String,
        contentType: String,
        metadata: Map<String, String> = emptyMap()
    ): UploadResult

    /**
     * Download file from object storage
     * @param bucketName Source bucket name
     * @param objectKey Object key/path
     * @return InputStream of the file content
     */
    fun downloadFile(bucketName: String, objectKey: String): InputStream

    /**
     * Get object metadata without downloading the file
     */
    fun getObjectMetadata(bucketName: String, objectKey: String): ObjectMetadata

    /**
     * Generate presigned URL for file access
     * @param bucketName Bucket name
     * @param objectKey Object key/path
     * @param expirationSeconds URL expiration time in seconds
     * @return Presigned URL string
     */
    fun generatePresignedUrl(
        bucketName: String,
        objectKey: String,
        expirationSeconds: Long = 3600
    ): String

    /**
     * Check if object exists
     */
    fun objectExists(bucketName: String, objectKey: String): Boolean

    /**
     * Delete object from storage
     */
    fun deleteObject(bucketName: String, objectKey: String)

    /**
     * Copy object to another location
     */
    fun copyObject(
        sourceBucket: String,
        sourceKey: String,
        targetBucket: String,
        targetKey: String
    ): CopyResult

    /**
     * List objects with prefix
     */
    fun listObjects(
        bucketName: String,
        prefix: String = "",
        maxKeys: Int = 1000
    ): List<ObjectSummary>

    /**
     * Create bucket if it doesn't exist
     */
    fun createBucketIfNotExists(bucketName: String)

    /**
     * Get workspace-aware object key
     * Format: {workspaceSlug}/{module}/{entityUid}/{fileUid}.{extension}
     */
    fun generateWorkspaceKey(
        workspaceSlug: String,
        module: String,
        entityUid: String,
        fileUid: String,
        extension: String
    ): String {
        return "${workspaceSlug}/${module}/${entityUid}/${fileUid}.${extension}"
    }
}

/**
 * Result of file upload operation
 */
data class UploadResult(
    val objectKey: String,
    val etag: String?,
    val contentLength: Long,
    val lastModified: LocalDateTime?,
    val url: String? = null
)

/**
 * Result of copy operation
 */
data class CopyResult(
    val sourceKey: String,
    val targetKey: String,
    val etag: String?,
    val lastModified: LocalDateTime?
)

/**
 * Object metadata information
 */
data class ObjectMetadata(
    val objectKey: String,
    val contentType: String,
    val contentLength: Long,
    val etag: String?,
    val lastModified: LocalDateTime?,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Object summary for listing operations
 */
data class ObjectSummary(
    val objectKey: String,
    val size: Long,
    val lastModified: LocalDateTime?,
    val etag: String?
)

/**
 * Object storage exceptions
 */
open class ObjectStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class ObjectNotFoundException(message: String) : ObjectStorageException(message)
class ObjectUploadException(message: String, cause: Throwable? = null) : ObjectStorageException(message, cause)
class ObjectDownloadException(message: String, cause: Throwable? = null) : ObjectStorageException(message, cause)
class ObjectDeleteException(message: String, cause: Throwable? = null) : ObjectStorageException(message, cause)
class ObjectCopyException(message: String, cause: Throwable? = null) : ObjectStorageException(message, cause)
class BucketCreationException(message: String, cause: Throwable? = null) : ObjectStorageException(message, cause)
class InvalidObjectKeyException(message: String) : ObjectStorageException(message)