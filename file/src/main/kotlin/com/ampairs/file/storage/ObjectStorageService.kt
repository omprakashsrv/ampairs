package com.ampairs.file.storage

import java.io.InputStream
import java.time.Instant

/**
 * Object Storage Service interface that supports both S3 and MinIO implementations.
 * Provides workspace-aware file storage with metadata management.
 */
interface ObjectStorageService {

    fun uploadFile(
        inputStream: InputStream,
        bucketName: String,
        objectKey: String,
        contentType: String,
        contentLength: Long,
        metadata: Map<String, String> = emptyMap()
    ): UploadResult

    fun uploadFile(
        bytes: ByteArray,
        bucketName: String,
        objectKey: String,
        contentType: String,
        metadata: Map<String, String> = emptyMap()
    ): UploadResult

    fun downloadFile(bucketName: String, objectKey: String): InputStream

    fun getObjectMetadata(bucketName: String, objectKey: String): ObjectMetadata

    fun generatePresignedUrl(
        bucketName: String,
        objectKey: String,
        expirationSeconds: Long = 3600
    ): String

    fun objectExists(bucketName: String, objectKey: String): Boolean

    fun deleteObject(bucketName: String, objectKey: String)

    fun copyObject(
        sourceBucket: String,
        sourceKey: String,
        targetBucket: String,
        targetKey: String
    ): CopyResult

    fun listObjects(
        bucketName: String,
        prefix: String = "",
        maxKeys: Int = 1000
    ): List<ObjectSummary>

    fun createBucketIfNotExists(bucketName: String)

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

data class UploadResult(
    val objectKey: String,
    val etag: String?,
    val contentLength: Long,
    val lastModified: Instant?,
    val url: String? = null
)

data class CopyResult(
    val sourceKey: String,
    val targetKey: String,
    val etag: String?,
    val lastModified: Instant?
)

data class ObjectMetadata(
    val objectKey: String,
    val contentType: String,
    val contentLength: Long,
    val etag: String?,
    val lastModified: Instant?,
    val metadata: Map<String, String> = emptyMap()
)

data class ObjectSummary(
    val objectKey: String,
    val size: Long,
    val lastModified: Instant?,
    val etag: String?
)

open class ObjectStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class ObjectNotFoundException(message: String) : ObjectStorageException(message)
class ObjectUploadException(message: String, cause: Throwable? = null) : ObjectStorageException(message, cause)
class ObjectDownloadException(message: String, cause: Throwable? = null) : ObjectStorageException(message, cause)
class ObjectDeleteException(message: String, cause: Throwable? = null) : ObjectStorageException(message, cause)
class ObjectCopyException(message: String, cause: Throwable? = null) : ObjectStorageException(message, cause)
class BucketCreationException(message: String, cause: Throwable? = null) : ObjectStorageException(message, cause)
class InvalidObjectKeyException(message: String) : ObjectStorageException(message)
