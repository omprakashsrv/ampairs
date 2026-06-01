package com.ampairs.file.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.file.config.StorageProperties
import com.ampairs.file.domain.dto.FileResponse
import com.ampairs.file.domain.dto.toFileResponse
import com.ampairs.file.domain.service.FileAccessException
import com.ampairs.file.domain.service.FileNotFoundException
import com.ampairs.file.domain.service.FileService
import com.ampairs.file.service.ImageResizingService
import com.ampairs.file.service.ImageResizingService.ThumbnailSize
import com.ampairs.file.service.ImageResizingService.ThumbnailSize.MEDIUM
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.net.URI
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/file/v1")
class FileController(
    private val fileService: FileService,
    private val imageResizingService: ImageResizingService,
    private val storageProperties: StorageProperties,
) {
    private val logger = LoggerFactory.getLogger(FileController::class.java)

    /**
     * Universal upload endpoint. Stores the file under {entityType}/{workspaceSlug}/{entityUid}/
     * and returns a FileResponse with pre-built download_url and thumbnail_url.
     *
     * entity_type: free-form label — PRODUCT, BRAND, CATEGORY, GROUP, SUB_CATEGORY, CUSTOMER, etc.
     * entity_uid:  UID of the owning entity (used as the sub-folder in storage).
     */
    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun upload(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("entity_type") entityType: String,
        @RequestParam("entity_uid") entityUid: String,
    ): ApiResponse<FileResponse> {
        val workspace = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")
        val folder = "${entityType.lowercase().trim()}/$workspace/$entityUid"
        val result = fileService.saveFile(
            bytes = file.inputStream.readAllBytes(),
            name = file.originalFilename ?: "file",
            contentType = file.contentType ?: "application/octet-stream",
            folder = folder,
        ).toFileResponse()
        logger.info("File uploaded: entityType={}, entityUid={}, file={}", entityType, entityUid, result.id)
        return ApiResponse.success(result)
    }

    /**
     * Redirect to a short-lived presigned URL for the file.
     * Works for S3, MinIO, and LOCAL (local returns a direct /files/... URL).
     */
    @GetMapping("/{fileUid}/download")
    fun download(@PathVariable fileUid: String): ResponseEntity<Void> {
        return try {
            val url = fileService.getFileUrl(fileUid, expirationMinutes = 60)
            ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build()
        } catch (e: FileNotFoundException) {
            ResponseEntity.notFound().build()
        } catch (e: FileAccessException) {
            logger.error("Failed to redirect file download: fileUid={}, error={}", fileUid, e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Stream a resized thumbnail. Accepts an optional ?size= query param (sm/md/lg suffix or pixel width).
     * Falls back to md if unrecognised. Cached for the configured max-age.
     */
    @GetMapping("/{fileUid}/thumbnail")
    fun thumbnail(
        @PathVariable fileUid: String,
        @RequestParam("size", defaultValue = "md") size: String,
    ): ResponseEntity<InputStreamResource> {
        return try {
            val (file, inputStream) = fileService.getFileContent(fileUid)

            val targetSize = ThumbnailSize.fromSuffix(size)
                ?: ThumbnailSize.fromPixels(size.toIntOrNull() ?: 0)
                ?: MEDIUM

            val contentType = file.contentType ?: "image/jpeg"
            val format = if (contentType.endsWith("png")) "png" else "jpg"

            val thumbnail = imageResizingService.generateThumbnail(inputStream, targetSize, format)

            val cacheControl = CacheControl.maxAge(
                storageProperties.image.thumbnails.cacheMaxAge, TimeUnit.SECONDS
            ).cachePublic()

            val headers = HttpHeaders().apply {
                setContentType(MediaType.parseMediaType(if (format == "png") "image/png" else "image/jpeg"))
                setContentLength(thumbnail.size.toLong())
                setCacheControl(cacheControl)
                set("X-Content-Type-Options", "nosniff")
            }

            ResponseEntity.ok()
                .headers(headers)
                .body(InputStreamResource(ByteArrayInputStream(thumbnail)))
        } catch (e: FileNotFoundException) {
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Failed to generate thumbnail: fileUid={}, size={}, error={}", fileUid, size, e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}
