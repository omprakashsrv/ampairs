package com.ampairs.customer.controller

import com.ampairs.core.config.StorageProperties
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.customer.domain.dto.*
import com.ampairs.customer.domain.service.CustomerImageService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.http.*
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.TimeUnit
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

/**
 * REST Controller for Customer Image operations
 * Provides endpoints for uploading, retrieving, and managing customer images
 */
@RestController
@RequestMapping("/customer/v1/images")
@Tag(name = "Customer Images", description = "Customer image management endpoints")
@SecurityRequirement(name = "bearerAuth")
class CustomerImageController(
    private val customerImageService: CustomerImageService,
    private val storageProperties: StorageProperties
) {

    private val logger = LoggerFactory.getLogger(CustomerImageController::class.java)

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "Upload customer image",
        description = "Upload an image for a customer with metadata"
    )
    @SwaggerApiResponse(responseCode = "201", description = "Image uploaded successfully")
    @SwaggerApiResponse(responseCode = "400", description = "Invalid request or file")
    @SwaggerApiResponse(responseCode = "403", description = "Access denied")
    @SwaggerApiResponse(responseCode = "413", description = "File too large")
//    @PreAuthorize("hasRole('USER')")
    fun uploadImage(
        @Parameter(description = "Image file to upload")
        @RequestParam("file") file: MultipartFile,

        @Parameter(description = "Customer UID")
        @RequestParam("customerUid") customerUid: String,

        @Parameter(description = "Image UID (optional, generated if not provided)")
        @RequestParam("uid", required = false) uid: String?,

        @Parameter(description = "Alt text for accessibility")
        @RequestParam("altText", required = false) altText: String?,

        @Parameter(description = "Image description")
        @RequestParam("description", required = false) description: String?,

        @Parameter(description = "Set as primary image")
        @RequestParam("isPrimary", defaultValue = "false") isPrimary: Boolean,

        @Parameter(description = "Display order")
        @RequestParam("displayOrder", required = false) displayOrder: Int?,

        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<CustomerImageResponse>> {

        val workspaceSlug = getWorkspaceSlugFromHeaders(request)
            ?: return ResponseEntity.badRequest()
                .body(ApiResponse.error("BAD_REQUEST", "Workspace context required"))

        return try {
            val uploadRequest = CustomerImageUploadRequest(
                customerUid = customerUid,
                uid = uid,
                altText = altText,
                description = description,
                isPrimary = isPrimary,
                displayOrder = displayOrder
            )

            val result = customerImageService.uploadImage(file, uploadRequest, workspaceSlug)

            logger.info("Image uploaded successfully: customer={}, image={}", customerUid, result.image.uid)

            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result.image))

        } catch (e: Exception) {
            logger.error("Failed to upload image: customer={}, error={}", customerUid, e.message, e)
            ResponseEntity.badRequest()
                .body(ApiResponse.error("FILE_UPLOAD_FAILED", "Failed to upload image: ${e.message}"))
        }
    }

    @GetMapping("/{customerUid}")
    @Operation(
        summary = "Get customer images",
        description = "Retrieve all images for a specific customer"
    )
    @SwaggerApiResponse(responseCode = "200", description = "Images retrieved successfully")
    @SwaggerApiResponse(responseCode = "404", description = "Customer not found")
//    @PreAuthorize("hasRole('USER')")
    fun getCustomerImages(
        @Parameter(description = "Customer UID")
        @PathVariable customerUid: String
    ): ResponseEntity<ApiResponse<CustomerImageListResponse>> {

        return try {
            val images = customerImageService.getCustomerImages(customerUid)

            ResponseEntity.ok(ApiResponse.success(images))

        } catch (e: Exception) {
            logger.error("Failed to get customer images: customer={}, error={}", customerUid, e.message, e)
            ResponseEntity.badRequest()
                .body(ApiResponse.error("BAD_REQUEST", "Failed to retrieve images: ${e.message}"))
        }
    }

    @GetMapping("/{customerUid}/{imageUid}")
    @Operation(
        summary = "Get customer image details",
        description = "Retrieve metadata for a specific customer image"
    )
    @SwaggerApiResponse(responseCode = "200", description = "Image details retrieved successfully")
    @SwaggerApiResponse(responseCode = "404", description = "Image not found")
//    @PreAuthorize("hasRole('USER')")
    fun getCustomerImage(
        @Parameter(description = "Customer UID")
        @PathVariable customerUid: String,

        @Parameter(description = "Image UID")
        @PathVariable imageUid: String
    ): ResponseEntity<ApiResponse<CustomerImageResponse>> {

        return try {
            val image = customerImageService.getCustomerImage(customerUid, imageUid)

            ResponseEntity.ok(ApiResponse.success(image))

        } catch (e: Exception) {
            logger.error("Failed to get customer image: customer={}, image={}, error={}", customerUid, imageUid, e.message, e)

            val status = if (e.message?.contains("not found") == true) HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            val errorCode = if (e.message?.contains("not found") == true) "NOT_FOUND" else "BAD_REQUEST"
            ResponseEntity.status(status)
                .body(ApiResponse.error(errorCode, "Failed to retrieve image: ${e.message}"))
        }
    }

    @GetMapping("/{customerUid}/{imageUid}/download")
    @Operation(
        summary = "Download customer image",
        description = "Download the actual image file with proper cache headers"
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "Image downloaded successfully",
        content = [Content(mediaType = "image/*")]
    )
    @SwaggerApiResponse(responseCode = "404", description = "Image not found")
//    @PreAuthorize("hasRole('USER')")
    fun downloadCustomerImage(
        @Parameter(description = "Customer UID")
        @PathVariable customerUid: String,

        @Parameter(description = "Image UID")
        @PathVariable imageUid: String,

        response: HttpServletResponse
    ): ResponseEntity<InputStreamResource> {

        return try {
            val (image, inputStream) = customerImageService.downloadCustomerImage(customerUid, imageUid)

            // Set cache control headers for client-side caching
            val cacheControl = CacheControl.maxAge(storageProperties.image.cacheMaxAge, TimeUnit.SECONDS)
                .cachePublic()
                .mustRevalidate()

            val headers = HttpHeaders().apply {
                contentType = MediaType.parseMediaType(image.contentType)
                contentLength = image.fileSize
                setContentDispositionFormData("inline", image.originalFilename)
                setCacheControl(cacheControl)

                // Set ETag for cache validation
                image.etag?.let { eTag = "\"$it\"" }

                // Set Last-Modified header
                image.lastModified?.let {
                    lastModified = it.toEpochMilli()
                }

                // Additional headers for image optimization
                set("X-Content-Type-Options", "nosniff")
                set("X-Frame-Options", "SAMEORIGIN")
            }

            logger.debug("Image downloaded: customer={}, image={}, size={}", customerUid, imageUid, image.fileSize)

            ResponseEntity.ok()
                .headers(headers)
                .body(InputStreamResource(inputStream))

        } catch (e: Exception) {
            logger.error("Failed to download image: customer={}, image={}, error={}", customerUid, imageUid, e.message, e)

            val status = if (e.message?.contains("not found") == true) HttpStatus.NOT_FOUND else HttpStatus.INTERNAL_SERVER_ERROR
            ResponseEntity.status(status).build()
        }
    }

    @PutMapping("/{customerUid}/{imageUid}")
    @Operation(
        summary = "Update customer image",
        description = "Update metadata for a customer image"
    )
    @SwaggerApiResponse(responseCode = "200", description = "Image updated successfully")
    @SwaggerApiResponse(responseCode = "404", description = "Image not found")
//    @PreAuthorize("hasRole('USER')")
    fun updateCustomerImage(
        @Parameter(description = "Customer UID")
        @PathVariable customerUid: String,

        @Parameter(description = "Image UID")
        @PathVariable imageUid: String,

        @Valid @RequestBody request: CustomerImageUpdateRequest
    ): ResponseEntity<ApiResponse<CustomerImageResponse>> {

        return try {
            val updatedImage = customerImageService.updateCustomerImage(customerUid, imageUid, request)

            logger.info("Image updated: customer={}, image={}", customerUid, imageUid)

            ResponseEntity.ok(ApiResponse.success(updatedImage))

        } catch (e: Exception) {
            logger.error("Failed to update image: customer={}, image={}, error={}", customerUid, imageUid, e.message, e)

            val status = if (e.message?.contains("not found") == true) HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            val errorCode = if (e.message?.contains("not found") == true) "NOT_FOUND" else "BAD_REQUEST"
            ResponseEntity.status(status)
                .body(ApiResponse.error(errorCode, "Failed to update image: ${e.message}"))
        }
    }

    @DeleteMapping("/{customerUid}/{imageUid}")
    @Operation(
        summary = "Delete customer image",
        description = "Delete a customer image and remove it from storage"
    )
    @SwaggerApiResponse(responseCode = "204", description = "Image deleted successfully")
    @SwaggerApiResponse(responseCode = "404", description = "Image not found")
//    @PreAuthorize("hasRole('USER')")
    fun deleteCustomerImage(
        @Parameter(description = "Customer UID")
        @PathVariable customerUid: String,

        @Parameter(description = "Image UID")
        @PathVariable imageUid: String
    ): ResponseEntity<ApiResponse<Void>> {

        return try {
            val deleted = customerImageService.deleteCustomerImage(customerUid, imageUid)

            if (deleted) {
                logger.info("Image deleted: customer={}, image={}", customerUid, imageUid)
                ResponseEntity.noContent().build()
            } else {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "Failed to delete image"))
            }

        } catch (e: Exception) {
            logger.error("Failed to delete image: customer={}, image={}, error={}", customerUid, imageUid, e.message, e)

            val status = if (e.message?.contains("not found") == true) HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            val errorCode = if (e.message?.contains("not found") == true) "NOT_FOUND" else "BAD_REQUEST"
            ResponseEntity.status(status)
                .body(ApiResponse.error(errorCode, "Failed to delete image: ${e.message}"))
        }
    }

    @PutMapping("/{customerUid}/{imageUid}/primary")
    @Operation(
        summary = "Set primary image",
        description = "Set a specific image as the primary image for the customer"
    )
    @SwaggerApiResponse(responseCode = "200", description = "Primary image set successfully")
    @SwaggerApiResponse(responseCode = "404", description = "Image not found")
//    @PreAuthorize("hasRole('USER')")
    fun setPrimaryImage(
        @Parameter(description = "Customer UID")
        @PathVariable customerUid: String,

        @Parameter(description = "Image UID")
        @PathVariable imageUid: String
    ): ResponseEntity<ApiResponse<CustomerImageResponse>> {

        return try {
            val primaryImage = customerImageService.setPrimaryImage(customerUid, imageUid)

            logger.info("Primary image set: customer={}, image={}", customerUid, imageUid)

            ResponseEntity.ok(ApiResponse.success(primaryImage))

        } catch (e: Exception) {
            logger.error("Failed to set primary image: customer={}, image={}, error={}", customerUid, imageUid, e.message, e)

            val status = if (e.message?.contains("not found") == true) HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            val errorCode = if (e.message?.contains("not found") == true) "NOT_FOUND" else "BAD_REQUEST"
            ResponseEntity.status(status)
                .body(ApiResponse.error(errorCode, "Failed to set primary image: ${e.message}"))
        }
    }

    @PutMapping("/{customerUid}/reorder")
    @Operation(
        summary = "Reorder customer images",
        description = "Update the display order of customer images"
    )
    @SwaggerApiResponse(responseCode = "200", description = "Images reordered successfully")
    @SwaggerApiResponse(responseCode = "400", description = "Invalid request")
//    @PreAuthorize("hasRole('USER')")
    fun reorderImages(
        @Parameter(description = "Customer UID")
        @PathVariable customerUid: String,

        @Valid @RequestBody request: CustomerImageReorderRequest
    ): ResponseEntity<ApiResponse<CustomerImageListResponse>> {

        return try {
            val reorderedImages = customerImageService.reorderImages(customerUid, request)

            logger.info("Images reordered: customer={}, count={}", customerUid, request.imageOrders.size)

            ResponseEntity.ok(ApiResponse.success(reorderedImages))

        } catch (e: Exception) {
            logger.error("Failed to reorder images: customer={}, error={}", customerUid, e.message, e)
            ResponseEntity.badRequest()
                .body(ApiResponse.error("BAD_REQUEST", "Failed to reorder images: ${e.message}"))
        }
    }

    @DeleteMapping("/{customerUid}/bulk")
    @Operation(
        summary = "Bulk delete customer images",
        description = "Delete multiple customer images in a single operation"
    )
    @SwaggerApiResponse(responseCode = "200", description = "Bulk operation completed")
    @SwaggerApiResponse(responseCode = "400", description = "Invalid request")
//    @PreAuthorize("hasRole('USER')")
    fun bulkDeleteImages(
        @Parameter(description = "Customer UID")
        @PathVariable customerUid: String,

        @Valid @RequestBody request: CustomerImageBulkRequest
    ): ResponseEntity<ApiResponse<CustomerImageBulkResponse>> {

        return try {
            val result = customerImageService.bulkDeleteImages(customerUid, request)

            logger.info("Bulk delete completed: customer={}, success={}, failed={}",
                customerUid, result.successCount, result.failureCount)

            ResponseEntity.ok(ApiResponse.success(result))

        } catch (e: Exception) {
            logger.error("Failed bulk delete: customer={}, error={}", customerUid, e.message, e)
            ResponseEntity.badRequest()
                .body(ApiResponse.error("BAD_REQUEST", "Failed to delete images: ${e.message}"))
        }
    }

    @GetMapping("/{customerUid}/stats")
    @Operation(
        summary = "Get customer image statistics",
        description = "Retrieve statistics about customer images"
    )
    @SwaggerApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
//    @PreAuthorize("hasRole('USER')")
    fun getCustomerImageStats(
        @Parameter(description = "Customer UID")
        @PathVariable customerUid: String
    ): ResponseEntity<ApiResponse<CustomerImageStatsResponse>> {

        return try {
            val stats = customerImageService.getCustomerImageStats(customerUid)

            ResponseEntity.ok(ApiResponse.success(stats))

        } catch (e: Exception) {
            logger.error("Failed to get image stats: customer={}, error={}", customerUid, e.message, e)
            ResponseEntity.badRequest()
                .body(ApiResponse.error("BAD_REQUEST", "Failed to retrieve statistics: ${e.message}"))
        }
    }

    /**
     * Thumbnail Endpoints
     */

    @GetMapping("/{customerUid}/{imageUid}/thumbnail")
    @Operation(
        summary = "Get customer image thumbnail",
        description = "Download thumbnail with specified size and cache headers"
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "Thumbnail downloaded successfully",
        content = [Content(mediaType = "image/*")]
    )
    @SwaggerApiResponse(responseCode = "404", description = "Image not found")
//    @PreAuthorize("hasRole('USER')")
    fun getThumbnail(
        @Parameter(description = "Customer UID")
        @PathVariable customerUid: String,

        @Parameter(description = "Image UID")
        @PathVariable imageUid: String,

        @Parameter(description = "Thumbnail size in pixels")
        @RequestParam("size", defaultValue = "300") size: Int,

        @Parameter(description = "Image format")
        @RequestParam("format", defaultValue = "jpg") format: String,

        response: HttpServletResponse
    ): ResponseEntity<InputStreamResource> {

        return try {
            val (image, thumbnailStream) = customerImageService.getThumbnail(customerUid, imageUid, size, format)

            // Set cache control headers for thumbnails (longer cache time)
            val cacheControl = CacheControl.maxAge(storageProperties.image.thumbnails.cacheMaxAge, TimeUnit.SECONDS)
                .cachePublic()
                .mustRevalidate()

            val headers = HttpHeaders().apply {
                contentType = MediaType.parseMediaType("image/$format")
                setContentDispositionFormData("inline", "thumbnail_${size}_${image.originalFilename}")
                setCacheControl(cacheControl)

                // Additional headers for thumbnails
                set("X-Thumbnail-Size", size.toString())
                set("X-Original-Image", image.uid)
                set("X-Content-Type-Options", "nosniff")
                set("X-Frame-Options", "SAMEORIGIN")
            }

            logger.debug("Thumbnail served: customer={}, image={}, size={}", customerUid, imageUid, size)

            ResponseEntity.ok()
                .headers(headers)
                .body(InputStreamResource(thumbnailStream))

        } catch (e: Exception) {
            logger.error("Failed to serve thumbnail: customer={}, image={}, size={}, error={}",
                customerUid, imageUid, size, e.message, e)

            val status = if (e.message?.contains("not found") == true) HttpStatus.NOT_FOUND else HttpStatus.INTERNAL_SERVER_ERROR
            ResponseEntity.status(status).build()
        }
    }

    @GetMapping("/{customerUid}/{imageUid}/thumbnail/{size}")
    @Operation(
        summary = "Get customer image thumbnail by size",
        description = "Download thumbnail with predefined size (150, 300, 500)"
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "Thumbnail downloaded successfully",
        content = [Content(mediaType = "image/*")]
    )
//    @PreAuthorize("hasRole('USER')")
    fun getThumbnailBySize(
        @Parameter(description = "Customer UID")
        @PathVariable customerUid: String,

        @Parameter(description = "Image UID")
        @PathVariable imageUid: String,

        @Parameter(description = "Thumbnail size (150, 300, 500)")
        @PathVariable size: Int,

        response: HttpServletResponse
    ): ResponseEntity<InputStreamResource> {

        return getThumbnail(customerUid, imageUid, size, storageProperties.image.thumbnails.format, response)
    }

    @GetMapping("/{customerUid}/{imageUid}/thumbnails")
    @Operation(
        summary = "Get available thumbnails info",
        description = "Get information about available thumbnail sizes and cached thumbnails"
    )
    @SwaggerApiResponse(responseCode = "200", description = "Thumbnail info retrieved successfully")
//    @PreAuthorize("hasRole('USER')")
    fun getAvailableThumbnails(
        @Parameter(description = "Customer UID")
        @PathVariable customerUid: String,

        @Parameter(description = "Image UID")
        @PathVariable imageUid: String
    ): ResponseEntity<ApiResponse<ThumbnailSizesResponse>> {

        return try {
            val thumbnailInfo = customerImageService.getAvailableThumbnails(customerUid, imageUid)

            ResponseEntity.ok(ApiResponse.success(thumbnailInfo))

        } catch (e: Exception) {
            logger.error("Failed to get thumbnail info: customer={}, image={}, error={}",
                customerUid, imageUid, e.message, e)

            val status = if (e.message?.contains("not found") == true) HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            val errorCode = if (e.message?.contains("not found") == true) "NOT_FOUND" else "BAD_REQUEST"
            ResponseEntity.status(status)
                .body(ApiResponse.error(errorCode, "Failed to get thumbnail info: ${e.message}"))
        }
    }

    @PostMapping("/{customerUid}/{imageUid}/thumbnails/generate")
    @Operation(
        summary = "Generate thumbnails for image",
        description = "Pre-generate thumbnails in specified sizes"
    )
    @SwaggerApiResponse(responseCode = "200", description = "Thumbnails generated successfully")
//    @PreAuthorize("hasRole('USER')")
    fun generateThumbnails(
        @Parameter(description = "Customer UID")
        @PathVariable customerUid: String,

        @Parameter(description = "Image UID")
        @PathVariable imageUid: String,

        @RequestParam("sizes", required = false) sizes: List<Int>?
    ): ResponseEntity<ApiResponse<BulkThumbnailGenerationResponse>> {

        return try {
            val result = customerImageService.generateThumbnails(customerUid, imageUid, sizes)

            logger.info("Thumbnails generated: customer={}, image={}, count={}",
                customerUid, imageUid, result.totalThumbnailsGenerated)

            ResponseEntity.ok(ApiResponse.success(result))

        } catch (e: Exception) {
            logger.error("Failed to generate thumbnails: customer={}, image={}, error={}",
                customerUid, imageUid, e.message, e)

            val status = if (e.message?.contains("not found") == true) HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            val errorCode = if (e.message?.contains("not found") == true) "NOT_FOUND" else "BAD_REQUEST"
            ResponseEntity.status(status)
                .body(ApiResponse.error(errorCode, "Failed to generate thumbnails: ${e.message}"))
        }
    }

    @PostMapping("/{customerUid}/thumbnails/bulk-generate")
    @Operation(
        summary = "Bulk generate thumbnails",
        description = "Generate thumbnails for multiple images"
    )
    @SwaggerApiResponse(responseCode = "200", description = "Bulk thumbnail generation completed")
//    @PreAuthorize("hasRole('USER')")
    fun bulkGenerateThumbnails(
        @Parameter(description = "Customer UID")
        @PathVariable customerUid: String,

        @Valid @RequestBody request: BulkThumbnailGenerationRequest
    ): ResponseEntity<ApiResponse<BulkThumbnailGenerationResponse>> {

        return try {
            val result = customerImageService.bulkGenerateThumbnails(customerUid, request)

            logger.info("Bulk thumbnails generated: customer={}, images={}, thumbnails={}",
                customerUid, result.totalImages, result.totalThumbnailsGenerated)

            ResponseEntity.ok(ApiResponse.success(result))

        } catch (e: Exception) {
            logger.error("Failed bulk thumbnail generation: customer={}, error={}", customerUid, e.message, e)
            ResponseEntity.badRequest()
                .body(ApiResponse.error("BAD_REQUEST", "Failed to generate thumbnails: ${e.message}"))
        }
    }

    @DeleteMapping("/{customerUid}/{imageUid}/thumbnails")
    @Operation(
        summary = "Delete cached thumbnails",
        description = "Delete all cached thumbnails for an image"
    )
    @SwaggerApiResponse(responseCode = "200", description = "Thumbnails deleted successfully")
//    @PreAuthorize("hasRole('USER')")
    fun deleteThumbnails(
        @Parameter(description = "Customer UID")
        @PathVariable customerUid: String,

        @Parameter(description = "Image UID")
        @PathVariable imageUid: String
    ): ResponseEntity<ApiResponse<ThumbnailCleanupResponse>> {

        return try {
            val result = customerImageService.deleteThumbnails(customerUid, imageUid)

            logger.info("Thumbnails deleted: customer={}, image={}, count={}",
                customerUid, imageUid, result.deletedThumbnails)

            ResponseEntity.ok(ApiResponse.success(result))

        } catch (e: Exception) {
            logger.error("Failed to delete thumbnails: customer={}, image={}, error={}",
                customerUid, imageUid, e.message, e)

            val status = if (e.message?.contains("not found") == true) HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            val errorCode = if (e.message?.contains("not found") == true) "NOT_FOUND" else "BAD_REQUEST"
            ResponseEntity.status(status)
                .body(ApiResponse.error(errorCode, "Failed to delete thumbnails: ${e.message}"))
        }
    }

    /**
     * Get workspace slug from request headers
     * This follows the multi-tenant pattern used in the project
     */
    private fun getWorkspaceSlugFromHeaders(request: HttpServletRequest): String? {
        // First try to get from X-Workspace-Slug header
        request.getHeader("X-Workspace-Slug")?.let { return it }

        // Fallback to tenant context if available
        return TenantContextHolder.getCurrentTenant()
    }
}
