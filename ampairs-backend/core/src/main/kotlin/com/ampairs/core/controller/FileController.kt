package com.ampairs.core.controller

import com.ampairs.core.config.StorageProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.FileNotFoundException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Controller for serving local files when using LOCAL storage provider
 * Provides HTTP access to files stored in the local filesystem
 */
@RestController
@RequestMapping("/files")
@ConditionalOnProperty(
    name = ["ampairs.storage.provider"],
    havingValue = "LOCAL",
    matchIfMissing = false
)
class FileController(
    private val storageProperties: StorageProperties
) {

    private val logger = LoggerFactory.getLogger(FileController::class.java)
    private val basePath: Path = Paths.get(storageProperties.local.basePath).toAbsolutePath()

    @GetMapping("/{bucket}/**")
    fun serveFile(
        @PathVariable bucket: String,
        request: jakarta.servlet.http.HttpServletRequest
    ): ResponseEntity<InputStreamResource> {

        return try {
            // Extract the file path from the request URI
            val requestUri = request.requestURI
            val basePattern = "/files/$bucket/"
            val relativePath = requestUri.substring(requestUri.indexOf(basePattern) + basePattern.length)

            // URL decode the path to handle special characters
            val decodedPath = URLDecoder.decode(relativePath, StandardCharsets.UTF_8.toString())

            // Construct the full file path
            val filePath = basePath.resolve(bucket).resolve(decodedPath)

            // Security check: ensure the file is within the allowed directory
            if (!filePath.normalize().startsWith(basePath.normalize())) {
                logger.warn("Attempted path traversal attack: bucket={}, path={}", bucket, decodedPath)
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            }

            // Check if file exists
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                logger.debug("File not found: {}", filePath)
                return ResponseEntity.notFound().build()
            }

            // Determine content type
            val contentType = Files.probeContentType(filePath) ?: "application/octet-stream"

            // Get file attributes
            val fileSize = Files.size(filePath)
            val lastModified = Files.getLastModifiedTime(filePath)

            // Create response headers
            val headers = HttpHeaders().apply {
                setContentType(MediaType.parseMediaType(contentType))
                setContentLength(fileSize)
                setLastModified(lastModified.toInstant())

                // Set cache headers for better performance
                setCacheControl("public, max-age=3600") // Cache for 1 hour

                // Security headers
                set("X-Content-Type-Options", "nosniff")
                set("X-Frame-Options", "SAMEORIGIN")

                // Set content disposition for downloads
                val fileName = filePath.fileName.toString()
                if (contentType.startsWith("image/")) {
                    setContentDispositionFormData("inline", fileName)
                } else {
                    setContentDispositionFormData("attachment", fileName)
                }
            }

            // Create input stream resource
            val inputStream = Files.newInputStream(filePath)
            val resource = InputStreamResource(inputStream)

            logger.debug("Serving file: bucket={}, path={}, size={}, contentType={}",
                bucket, decodedPath, fileSize, contentType)

            ResponseEntity.ok()
                .headers(headers)
                .body(resource)

        } catch (e: FileNotFoundException) {
            logger.debug("File not found: bucket={}, error={}", bucket, e.message)
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Error serving file: bucket={}, error={}", bucket, e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        return try {
            val info: Map<String, Any> = mapOf(
                "status" to "UP",
                "storage" to mapOf(
                    "provider" to "LOCAL",
                    "basePath" to basePath.toString(),
                    "exists" to Files.exists(basePath),
                    "writable" to Files.isWritable(basePath)
                )
            )
            ResponseEntity.ok(info)
        } catch (e: Exception) {
            val error: Map<String, Any> = mapOf(
                "status" to "DOWN",
                "error" to (e.message ?: "Unknown error")
            )
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error)
        }
    }
}