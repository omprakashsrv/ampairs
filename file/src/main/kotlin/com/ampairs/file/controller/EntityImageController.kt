package com.ampairs.file.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.exception.NotFoundException
import com.ampairs.file.config.StorageProperties
import com.ampairs.file.domain.dto.EntityImageListResponse
import com.ampairs.file.domain.dto.EntityImageResponse
import com.ampairs.file.domain.dto.EntityImageUploadRequest
import com.ampairs.file.domain.service.EntityImageService
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.http.CacheControl
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/file/v1/images")
class EntityImageController(
    private val entityImageService: EntityImageService,
    private val storageProperties: StorageProperties,
) {
    private val logger = LoggerFactory.getLogger(EntityImageController::class.java)

    /**
     * Upload an image for an entity. Runs validation, auto-resize, EXIF stripping, deduplication.
     * Pass an optional `uid` to have the server use the client-generated UID (offline-first).
     */
    @PostMapping("/{entityType}/{entityUid}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun upload(
        @PathVariable entityType: String,
        @PathVariable entityUid: String,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("is_primary", defaultValue = "false") isPrimary: Boolean,
        @RequestParam("description", required = false) description: String?,
        @RequestParam("uid", required = false) uid: String?,
        @RequestParam("display_order", required = false) displayOrder: Int?,
    ): ApiResponse<EntityImageResponse> {
        val request = EntityImageUploadRequest(
            entityType = entityType.uppercase(),
            entityUid = entityUid,
            uid = uid,
            description = description,
            isPrimary = isPrimary,
            displayOrder = displayOrder,
        )
        val baseUrl = resolveBaseUrl()
        val response = entityImageService.upload(entityType.uppercase(), entityUid, file, request, baseUrl)
        logger.info("Image uploaded: entityType={}, entityUid={}, uid={}", entityType, entityUid, response.uid)
        return ApiResponse.success(response)
    }

    /**
     * List all active images for an entity. Optional last_sync timestamp for incremental pulls.
     */
    @GetMapping("/{entityType}/{entityUid}")
    fun getImages(
        @PathVariable entityType: String,
        @PathVariable entityUid: String,
        @RequestParam("last_sync", required = false) lastSync: String?,
    ): ApiResponse<EntityImageListResponse> {
        val baseUrl = resolveBaseUrl()
        val response = entityImageService.getImages(entityType.uppercase(), entityUid, baseUrl)
        logger.debug("getImages: entityType={}, entityUid={}, count={}", entityType, entityUid, response.totalCount)
        return ApiResponse.success(response)
    }

    /**
     * Stream the raw image bytes by image UID.
     */
    @GetMapping("/{imageUid}/download")
    fun download(@PathVariable imageUid: String): ResponseEntity<InputStreamResource> {
        return try {
            val (image, stream) = entityImageService.downloadByUid(imageUid)
            val headers = HttpHeaders().apply {
                setContentType(MediaType.parseMediaType(image.contentType))
                // RFC 6266 encoding — raw filenames may contain non-Latin-1 chars (e.g. the U+202F
                // narrow no-break space in macOS screenshot names), which Tomcat rejects, dropping
                // the whole header. ContentDisposition emits the filename*=UTF-8'' form.
                setContentDisposition(
                    ContentDisposition.inline().filename(image.originalFilename, StandardCharsets.UTF_8).build()
                )
                setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
            }
            ResponseEntity.ok().headers(headers).body(InputStreamResource(stream))
        } catch (_: NotFoundException) {
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Failed to download image: imageUid={}, error={}", imageUid, e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Stream a cached thumbnail for an image. Accepts an optional ?size= query param
     * (sm/md/lg suffix or pixel width). Falls back to md if unrecognised.
     */
    @GetMapping("/{imageUid}/thumbnail")
    fun thumbnail(
        @PathVariable imageUid: String,
        @RequestParam("size", defaultValue = "md") size: String,
    ): ResponseEntity<InputStreamResource> {
        return try {
            val (image, stream) = entityImageService.thumbnailByUid(imageUid, size)
            val format = if (image.fileExtension == "png") "png" else "jpg"
            val headers = HttpHeaders().apply {
                setContentType(MediaType.parseMediaType(if (format == "png") "image/png" else "image/jpeg"))
                setCacheControl(CacheControl.maxAge(storageProperties.image.thumbnails.cacheMaxAge, TimeUnit.SECONDS).cachePublic())
                set("X-Content-Type-Options", "nosniff")
            }
            ResponseEntity.ok().headers(headers).body(InputStreamResource(stream))
        } catch (_: NotFoundException) {
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Failed to generate thumbnail: imageUid={}, size={}, error={}", imageUid, size, e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Soft-delete an image by its UID.
     */
    @DeleteMapping("/{imageUid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteImage(@PathVariable imageUid: String) {
        entityImageService.deleteByUid(imageUid)
        logger.debug("deleteImage: imageUid={}", imageUid)
    }

    /**
     * Set an image as the primary for its entity.
     */
    @PutMapping("/{imageUid}/primary")
    fun setPrimaryImage(@PathVariable imageUid: String): ApiResponse<EntityImageResponse> {
        val baseUrl = resolveBaseUrl()
        val response = entityImageService.setPrimaryByUid(imageUid, baseUrl)
        logger.debug("setPrimaryImage: imageUid={}", imageUid)
        return ApiResponse.success(response)
    }

    private fun resolveBaseUrl(): String = "/api/file/v1"
}
