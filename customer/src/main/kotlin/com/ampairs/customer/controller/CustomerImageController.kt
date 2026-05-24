package com.ampairs.customer.controller

import com.ampairs.file.config.StorageProperties
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.customer.domain.dto.*
import com.ampairs.customer.domain.service.CustomerImageService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
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
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload customer image")
    @SwaggerApiResponse(responseCode = "201", description = "Image uploaded successfully")
    @SwaggerApiResponse(responseCode = "400", description = "Invalid request or file")
    @SwaggerApiResponse(responseCode = "413", description = "File too large")
    fun uploadImage(
        @Parameter(description = "Image file to upload")
        @RequestParam("file") file: MultipartFile,
        @Parameter(description = "Customer UID")
        @RequestParam("customerUid") customerUid: String,
        @Parameter(description = "Image UID (optional, generated if not provided)")
        @RequestParam("uid", required = false) uid: String?,
        @Parameter(description = "Image description")
        @RequestParam("description", required = false) description: String?,
        @Parameter(description = "Set as primary image")
        @RequestParam("isPrimary", defaultValue = "false") isPrimary: Boolean,
        @Parameter(description = "Display order")
        @RequestParam("displayOrder", required = false) displayOrder: Int?
    ): ApiResponse<CustomerImageResponse> {
        val workspaceSlug = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        val result = customerImageService.uploadImage(
            file,
            CustomerImageUploadRequest(
                customerUid = customerUid,
                uid = uid,
                description = description,
                isPrimary = isPrimary,
                displayOrder = displayOrder
            ),
            workspaceSlug
        )

        logger.info("Image uploaded successfully: customer={}, image={}", customerUid, result.image.uid)
        return ApiResponse.success(result.image)
    }

    @GetMapping("/{customerUid}")
    @Operation(summary = "Get customer images")
    fun getCustomerImages(@PathVariable customerUid: String): ApiResponse<CustomerImageListResponse> {
        return ApiResponse.success(customerImageService.getCustomerImages(customerUid))
    }

    @GetMapping("/{customerUid}/{imageUid}")
    @Operation(summary = "Get customer image details")
    fun getCustomerImage(
        @PathVariable customerUid: String,
        @PathVariable imageUid: String
    ): ApiResponse<CustomerImageResponse> {
        return ApiResponse.success(customerImageService.getCustomerImage(customerUid, imageUid))
    }

    @GetMapping("/{customerUid}/{imageUid}/download")
    @Operation(summary = "Download customer image")
    @SwaggerApiResponse(responseCode = "200", content = [Content(mediaType = "image/*")])
    fun downloadCustomerImage(
        @PathVariable customerUid: String,
        @PathVariable imageUid: String,
        response: HttpServletResponse
    ): ResponseEntity<InputStreamResource> {
        val (_, inputStream) = customerImageService.downloadCustomerImage(customerUid, imageUid)

        val cacheControl = CacheControl.maxAge(storageProperties.image.cacheMaxAge, TimeUnit.SECONDS)
            .cachePublic()
            .mustRevalidate()

        val headers = HttpHeaders().apply {
            setCacheControl(cacheControl)
            set("X-Content-Type-Options", "nosniff")
            set("X-Frame-Options", "SAMEORIGIN")
        }

        return ResponseEntity.ok().headers(headers).body(InputStreamResource(inputStream))
    }

    @PutMapping("/{customerUid}/{imageUid}")
    @Operation(summary = "Update customer image")
    fun updateCustomerImage(
        @PathVariable customerUid: String,
        @PathVariable imageUid: String,
        @Valid @RequestBody request: CustomerImageUpdateRequest
    ): ApiResponse<CustomerImageResponse> {
        val updatedImage = customerImageService.updateCustomerImage(customerUid, imageUid, request)
        logger.info("Image updated: customer={}, image={}", customerUid, imageUid)
        return ApiResponse.success(updatedImage)
    }

    @DeleteMapping("/{customerUid}/{imageUid}")
    @Operation(summary = "Delete customer image")
    fun deleteCustomerImage(
        @PathVariable customerUid: String,
        @PathVariable imageUid: String
    ): ApiResponse<Map<String, Boolean>> {
        val deleted = customerImageService.deleteCustomerImage(customerUid, imageUid)
        if (deleted) logger.info("Image deleted: customer={}, image={}", customerUid, imageUid)
        return ApiResponse.success(mapOf("deleted" to deleted))
    }

    @PutMapping("/{customerUid}/{imageUid}/primary")
    @Operation(summary = "Set primary image")
    fun setPrimaryImage(
        @PathVariable customerUid: String,
        @PathVariable imageUid: String
    ): ApiResponse<CustomerImageResponse> {
        val primaryImage = customerImageService.setPrimaryImage(customerUid, imageUid)
        logger.info("Primary image set: customer={}, image={}", customerUid, imageUid)
        return ApiResponse.success(primaryImage)
    }

    @PutMapping("/{customerUid}/reorder")
    @Operation(summary = "Reorder customer images")
    fun reorderImages(
        @PathVariable customerUid: String,
        @Valid @RequestBody request: CustomerImageReorderRequest
    ): ApiResponse<CustomerImageListResponse> {
        val reorderedImages = customerImageService.reorderImages(customerUid, request)
        logger.info("Images reordered: customer={}, count={}", customerUid, request.imageOrders.size)
        return ApiResponse.success(reorderedImages)
    }

    @DeleteMapping("/{customerUid}/bulk")
    @Operation(summary = "Bulk delete customer images")
    fun bulkDeleteImages(
        @PathVariable customerUid: String,
        @Valid @RequestBody request: CustomerImageBulkRequest
    ): ApiResponse<CustomerImageBulkResponse> {
        val result = customerImageService.bulkDeleteImages(customerUid, request)
        logger.info("Bulk delete completed: customer={}, success={}, failed={}",
            customerUid, result.successCount, result.failureCount)
        return ApiResponse.success(result)
    }

    @GetMapping("/{customerUid}/stats")
    @Operation(summary = "Get customer image statistics")
    fun getCustomerImageStats(@PathVariable customerUid: String): ApiResponse<CustomerImageStatsResponse> {
        return ApiResponse.success(customerImageService.getCustomerImageStats(customerUid))
    }

    @GetMapping("/{customerUid}/{imageUid}/thumbnail")
    @Operation(summary = "Get customer image thumbnail")
    @SwaggerApiResponse(responseCode = "200", content = [Content(mediaType = "image/*")])
    fun getThumbnail(
        @PathVariable customerUid: String,
        @PathVariable imageUid: String,
        @RequestParam("size", defaultValue = "300") size: Int,
        @RequestParam("format", defaultValue = "jpg") format: String,
        response: HttpServletResponse
    ): ResponseEntity<InputStreamResource> {
        val (image, thumbnailStream) = customerImageService.getThumbnail(customerUid, imageUid, size, format)

        val cacheControl = CacheControl.maxAge(storageProperties.image.thumbnails.cacheMaxAge, TimeUnit.SECONDS)
            .cachePublic()
            .mustRevalidate()

        val headers = HttpHeaders().apply {
            contentType = MediaType.parseMediaType("image/$format")
            setCacheControl(cacheControl)
            set("X-Thumbnail-Size", size.toString())
            set("X-Original-Image", image.uid)
            set("X-Content-Type-Options", "nosniff")
            set("X-Frame-Options", "SAMEORIGIN")
        }

        return ResponseEntity.ok().headers(headers).body(InputStreamResource(thumbnailStream))
    }

    @GetMapping("/{customerUid}/{imageUid}/thumbnail/{size}")
    @Operation(summary = "Get customer image thumbnail by size")
    @SwaggerApiResponse(responseCode = "200", content = [Content(mediaType = "image/*")])
    fun getThumbnailBySize(
        @PathVariable customerUid: String,
        @PathVariable imageUid: String,
        @PathVariable size: Int,
        response: HttpServletResponse
    ): ResponseEntity<InputStreamResource> {
        return getThumbnail(customerUid, imageUid, size, storageProperties.image.thumbnails.format, response)
    }

    @GetMapping("/{customerUid}/{imageUid}/thumbnails")
    @Operation(summary = "Get available thumbnails info")
    fun getAvailableThumbnails(
        @PathVariable customerUid: String,
        @PathVariable imageUid: String
    ): ApiResponse<ThumbnailSizesResponse> {
        return ApiResponse.success(customerImageService.getAvailableThumbnails(customerUid, imageUid))
    }

    @PostMapping("/{customerUid}/{imageUid}/thumbnails/generate")
    @Operation(summary = "Generate thumbnails for image")
    fun generateThumbnails(
        @PathVariable customerUid: String,
        @PathVariable imageUid: String,
        @RequestParam("sizes", required = false) sizes: List<Int>?
    ): ApiResponse<BulkThumbnailGenerationResponse> {
        val result = customerImageService.generateThumbnails(customerUid, imageUid, sizes)
        logger.info("Thumbnails generated: customer={}, image={}, count={}",
            customerUid, imageUid, result.totalThumbnailsGenerated)
        return ApiResponse.success(result)
    }

    @PostMapping("/{customerUid}/thumbnails/bulk-generate")
    @Operation(summary = "Bulk generate thumbnails")
    fun bulkGenerateThumbnails(
        @PathVariable customerUid: String,
        @Valid @RequestBody request: BulkThumbnailGenerationRequest
    ): ApiResponse<BulkThumbnailGenerationResponse> {
        val result = customerImageService.bulkGenerateThumbnails(customerUid, request)
        logger.info("Bulk thumbnails generated: customer={}, images={}, thumbnails={}",
            customerUid, result.totalImages, result.totalThumbnailsGenerated)
        return ApiResponse.success(result)
    }

    @DeleteMapping("/{customerUid}/{imageUid}/thumbnails")
    @Operation(summary = "Delete cached thumbnails")
    fun deleteThumbnails(
        @PathVariable customerUid: String,
        @PathVariable imageUid: String
    ): ApiResponse<ThumbnailCleanupResponse> {
        val result = customerImageService.deleteThumbnails(customerUid, imageUid)
        logger.info("Thumbnails deleted: customer={}, image={}, count={}",
            customerUid, imageUid, result.deletedThumbnails)
        return ApiResponse.success(result)
    }
}
