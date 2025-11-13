package com.ampairs.core.appupdate.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import java.io.InputStream

/**
 * Service for streaming files from S3.
 *
 * Used by app update download endpoint to stream app binaries
 * without exposing S3 URLs to clients.
 */
@Service
class S3FileStreamService(
    private val s3Client: S3Client,
    @Value("\${ampairs.app-updates.storage.bucket}")
    private val bucket: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Stream a file from S3.
     *
     * @param s3Key S3 object key (e.g., "updates/macos-1.0.0.10.dmg")
     * @return ResponseBytes containing file data and metadata
     * @throws NoSuchKeyException if file not found in S3
     */
    fun streamFile(s3Key: String): ResponseBytes<GetObjectResponse> {
        logger.info("Streaming file from S3: bucket=$bucket, key=$s3Key")

        val getObjectRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(s3Key)
            .build()

        return try {
            s3Client.getObjectAsBytes(getObjectRequest)
        } catch (e: NoSuchKeyException) {
            logger.error("File not found in S3: bucket=$bucket, key=$s3Key", e)
            throw IllegalStateException("Download file not found: $s3Key")
        } catch (e: Exception) {
            logger.error("Error streaming file from S3: bucket=$bucket, key=$s3Key", e)
            throw IllegalStateException("Failed to retrieve download file", e)
        }
    }

    /**
     * Check if a file exists in S3.
     *
     * @param s3Key S3 object key
     * @return true if file exists, false otherwise
     */
    fun fileExists(s3Key: String): Boolean {
        return try {
            val headObjectRequest = software.amazon.awssdk.services.s3.model.HeadObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build()

            s3Client.headObject(headObjectRequest)
            logger.debug("File exists in S3: bucket=$bucket, key=$s3Key")
            true
        } catch (e: NoSuchKeyException) {
            logger.warn("File does not exist in S3: bucket=$bucket, key=$s3Key")
            false
        } catch (e: Exception) {
            logger.error("Error checking file existence in S3: bucket=$bucket, key=$s3Key", e)
            false
        }
    }

    /**
     * Get file metadata without downloading.
     *
     * @param s3Key S3 object key
     * @return File size in bytes
     */
    fun getFileSize(s3Key: String): Long {
        return try {
            val headObjectRequest = software.amazon.awssdk.services.s3.model.HeadObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build()

            val response = s3Client.headObject(headObjectRequest)
            response.contentLength()
        } catch (e: Exception) {
            logger.warn("Could not retrieve file size for: $s3Key", e)
            0L
        }
    }

    /**
     * Get content type for file based on extension.
     */
    fun getContentType(filename: String): String {
        return when {
            filename.endsWith(".dmg", ignoreCase = true) -> "application/x-apple-diskimage"
            filename.endsWith(".exe", ignoreCase = true) -> "application/vnd.microsoft.portable-executable"
            filename.endsWith(".msi", ignoreCase = true) -> "application/x-msi"
            filename.endsWith(".deb", ignoreCase = true) -> "application/vnd.debian.binary-package"
            filename.endsWith(".rpm", ignoreCase = true) -> "application/x-rpm"
            filename.endsWith(".appimage", ignoreCase = true) -> "application/x-executable"
            else -> "application/octet-stream"
        }
    }
}
