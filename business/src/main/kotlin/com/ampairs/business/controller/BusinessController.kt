package com.ampairs.business.controller

import com.ampairs.business.exception.BusinessImageNotFoundException
import com.ampairs.business.model.BusinessImageType
import com.ampairs.business.model.dto.*
import com.ampairs.business.service.BusinessImageService
import com.ampairs.business.service.BusinessService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.security.AuthenticationHelper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.TimeUnit

/**
 * REST controller for Business Management.
 *
 * **Base Path**: `/api/v1/business`
 *
 * **Module Structure**:
 * 1. Overview - Dashboard summary
 * 2. Profile - Company profile and registration
 * 3. Operations - Operational settings
 * 4. Tax Configuration - Tax and compliance settings
 *
 * **Multi-Tenancy**:
 * - All operations scoped to current workspace (from X-Workspace-ID header)
 * - Service layer uses TenantContextHolder
 *
 * **Error Handling**:
 * - Exceptions handled by BusinessExceptionHandler
 * - Returns ApiResponse<T> format consistently
 */
@RestController
@RequestMapping("/api/v1/business")
@Tag(name = "Business Management", description = "Complete business configuration and management")
class BusinessController @Autowired constructor(
    private val businessService: BusinessService,
    private val businessImageService: BusinessImageService
) {

    private fun getCurrentUserId(): String? {
        val auth = SecurityContextHolder.getContext().authentication
        return auth?.let { AuthenticationHelper.getCurrentUserId(it) }
    }

    // ==================== Main Business Endpoints ====================

    /**
     * Get complete business profile.
     *
     * **Route**: GET /api/v1/business
     * **Returns**: Complete business profile including all settings
     */
    @GetMapping
    @Operation(
        summary = "Get business profile",
        description = "Retrieve complete business profile with all configuration"
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Business profile retrieved successfully"
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Business profile not found"
            )
        ]
    )
    fun getBusiness(): ApiResponse<BusinessResponse> {
        val business = businessService.getBusinessProfile()
        return ApiResponse.success(business.asBusinessResponse())
    }

    /**
     * Update business profile.
     *
     * **Route**: PUT /api/v1/business
     * **Returns**: Updated business profile
     * **Note**: Supports partial updates - only provided fields are updated
     */
    @PutMapping
    @Operation(
        summary = "Update business profile",
        description = "Update business profile (supports partial updates)"
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Business profile updated successfully"
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Business profile not found"
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid business data"
            )
        ]
    )
    fun updateBusiness(
        @Valid @RequestBody request: BusinessUpdateRequest
    ): ApiResponse<BusinessResponse> {
        val business = businessService.updateBusinessProfile(request)
        return ApiResponse.success(business.asBusinessResponse())
    }

    // ==================== Overview Endpoints ====================

    /**
     * Get business overview for dashboard.
     *
     * **Route**: GET /api/v1/business/overview
     * **Returns**: Summary information for dashboard display
     */
    @GetMapping("/overview")
    @Operation(
        summary = "Get business overview",
        description = "Retrieve business overview summary for dashboard display"
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Business overview retrieved successfully"
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Business profile not found"
            )
        ]
    )
    fun getBusinessOverview(): ApiResponse<BusinessOverviewResponse> {
        val business = businessService.getBusinessOverview()
        return ApiResponse.success(business.asBusinessOverviewResponse())
    }


    // ==================== Initial Setup Endpoints ====================

    /**
     * Create business profile for workspace (initial setup).
     *
     * **Route**: POST /api/v1/business
     * **Returns**: Created business profile
     * **Note**: Only called during initial workspace setup
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create business profile",
        description = "Create initial business profile for workspace (one-time setup)"
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Business profile created successfully"
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Business profile already exists"
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid business data"
            )
        ]
    )
    fun createBusinessProfile(
        @Valid @RequestBody request: BusinessCreateRequest
    ): ApiResponse<BusinessResponse> {
        val business = businessService.createBusinessProfile(request)
        return ApiResponse.success(business.asBusinessResponse())
    }

    /**
     * Check if business profile exists for workspace.
     *
     * **Route**: GET /api/v1/business/exists
     * **Returns**: Boolean flag
     */
    @GetMapping("/exists")
    @Operation(
        summary = "Check if business profile exists",
        description = "Check whether a business profile exists for the current workspace"
    )
    fun checkBusinessExists(): ApiResponse<Map<String, Boolean>> {
        val exists = businessService.businessProfileExists()
        return ApiResponse.success(mapOf("exists" to exists))
    }

    // ==================== Logo Endpoints ====================

    /**
     * Upload business logo.
     *
     * **Route**: POST /api/v1/business/logo
     * **Accepts**: JPEG, PNG, WebP up to 10MB
     * **Returns**: Updated business with logo URLs
     */
    @PostMapping("/logo", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "Upload business logo",
        description = "Upload a logo image for the business. Accepts JPEG, PNG, WebP up to 10MB."
    )
    fun uploadLogo(
        @RequestPart("file") file: MultipartFile
    ): ApiResponse<BusinessResponse> {
        val business = businessService.getBusinessProfile()
        val updatedBusiness = businessImageService.uploadLogo(business, file)
        return ApiResponse.success(updatedBusiness.asBusinessResponse())
    }

    /**
     * Get business logo (full size).
     *
     * **Route**: GET /api/v1/business/logo
     */
    @GetMapping("/logo")
    @Operation(
        summary = "Get business logo",
        description = "Get the business logo image (full size)"
    )
    fun getLogo(): ResponseEntity<ByteArray> {
        val business = businessService.getBusinessProfile()
        val objectKey = business.logoUrl
            ?: throw BusinessImageNotFoundException("No logo set for this business")

        val imageBytes = businessImageService.getLogo(objectKey)

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
            .contentType(getMediaTypeFromKey(objectKey))
            .body(imageBytes)
    }

    /**
     * Get business logo thumbnail.
     *
     * **Route**: GET /api/v1/business/logo/thumbnail
     */
    @GetMapping("/logo/thumbnail")
    @Operation(
        summary = "Get business logo thumbnail",
        description = "Get the business logo thumbnail image (256x256)"
    )
    fun getLogoThumbnail(): ResponseEntity<ByteArray> {
        val business = businessService.getBusinessProfile()
        val objectKey = business.logoThumbnailUrl
            ?: throw BusinessImageNotFoundException("No logo set for this business")

        val imageBytes = businessImageService.getLogo(objectKey)

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
            .contentType(getMediaTypeFromKey(objectKey))
            .body(imageBytes)
    }

    /**
     * Delete business logo.
     *
     * **Route**: DELETE /api/v1/business/logo
     */
    @DeleteMapping("/logo")
    @Operation(
        summary = "Delete business logo",
        description = "Delete the business logo"
    )
    fun deleteLogo(): ApiResponse<BusinessResponse> {
        val business = businessService.getBusinessProfile()
        val updatedBusiness = businessImageService.deleteLogo(business)
        return ApiResponse.success(updatedBusiness.asBusinessResponse())
    }

    // ==================== Gallery Image Endpoints ====================

    /**
     * Upload a gallery image.
     *
     * **Route**: POST /api/v1/business/images
     */
    @PostMapping("/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Upload business image",
        description = "Upload a gallery image for the business. Accepts JPEG, PNG, WebP up to 10MB."
    )
    fun uploadImage(
        @RequestPart("file") file: MultipartFile,
        @RequestParam(required = false) imageType: String?,
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) description: String?,
        @RequestParam(required = false) altText: String?,
        @RequestParam(required = false, defaultValue = "false") isPrimary: Boolean
    ): ApiResponse<BusinessImageResponse> {
        val business = businessService.getBusinessProfile()
        val type = imageType?.let { BusinessImageType.fromString(it) } ?: BusinessImageType.GALLERY

        val image = businessImageService.uploadImage(
            business = business,
            file = file,
            imageType = type,
            title = title,
            description = description,
            altText = altText,
            isPrimary = isPrimary,
            uploadedBy = getCurrentUserId()
        )

        return ApiResponse.success(image.asBusinessImageResponse())
    }

    /**
     * Get all gallery images for the business.
     *
     * **Route**: GET /api/v1/business/images
     */
    @GetMapping("/images")
    @Operation(
        summary = "Get business images",
        description = "Get all gallery images for the business"
    )
    fun getImages(
        @RequestParam(required = false) imageType: String?
    ): ApiResponse<List<BusinessImageResponse>> {
        val business = businessService.getBusinessProfile()

        val images = if (imageType != null) {
            val type = BusinessImageType.fromString(imageType)
            businessImageService.getBusinessImagesByType(business.uid, type)
        } else {
            businessImageService.getBusinessImages(business.uid)
        }

        return ApiResponse.success(images.map { it.asBusinessImageResponse() })
    }

    /**
     * Get a specific image by UID.
     *
     * **Route**: GET /api/v1/business/images/{imageUid}
     */
    @GetMapping("/images/{imageUid}")
    @Operation(
        summary = "Get image details",
        description = "Get details of a specific business image"
    )
    fun getImageDetails(@PathVariable imageUid: String): ApiResponse<BusinessImageResponse> {
        val image = businessImageService.getImageByUid(imageUid)
        return ApiResponse.success(image.asBusinessImageResponse())
    }

    /**
     * Get image file (full size).
     *
     * **Route**: GET /api/v1/business/images/{imageUid}/file
     */
    @GetMapping("/images/{imageUid}/file")
    @Operation(
        summary = "Get image file",
        description = "Get the image file (full size)"
    )
    fun getImageFile(@PathVariable imageUid: String): ResponseEntity<ByteArray> {
        val image = businessImageService.getImageByUid(imageUid)
        val imageBytes = businessImageService.getImageBytes(image.imageUrl)

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
            .contentType(getMediaTypeFromKey(image.imageUrl))
            .body(imageBytes)
    }

    /**
     * Get image thumbnail.
     *
     * **Route**: GET /api/v1/business/images/{imageUid}/thumbnail
     */
    @GetMapping("/images/{imageUid}/thumbnail")
    @Operation(
        summary = "Get image thumbnail",
        description = "Get the image thumbnail"
    )
    fun getImageThumbnail(@PathVariable imageUid: String): ResponseEntity<ByteArray> {
        val image = businessImageService.getImageByUid(imageUid)
        val objectKey = image.thumbnailUrl ?: image.imageUrl
        val imageBytes = businessImageService.getImageBytes(objectKey)

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
            .contentType(getMediaTypeFromKey(objectKey))
            .body(imageBytes)
    }

    /**
     * Update image metadata.
     *
     * **Route**: PUT /api/v1/business/images/{imageUid}
     */
    @PutMapping("/images/{imageUid}")
    @Operation(
        summary = "Update image metadata",
        description = "Update image title, description, alt text, or type"
    )
    fun updateImage(
        @PathVariable imageUid: String,
        @RequestBody request: UpdateBusinessImageRequest
    ): ApiResponse<BusinessImageResponse> {
        val image = businessImageService.updateImage(
            imageUid = imageUid,
            title = request.title,
            description = request.description,
            altText = request.altText,
            imageType = request.imageType?.let { BusinessImageType.fromString(it) }
        )
        return ApiResponse.success(image.asBusinessImageResponse())
    }

    /**
     * Set an image as primary.
     *
     * **Route**: POST /api/v1/business/images/{imageUid}/set-primary
     */
    @PostMapping("/images/{imageUid}/set-primary")
    @Operation(
        summary = "Set image as primary",
        description = "Set a specific image as the primary/featured image"
    )
    fun setImageAsPrimary(@PathVariable imageUid: String): ApiResponse<BusinessImageResponse> {
        val image = businessImageService.setAsPrimary(imageUid)
        return ApiResponse.success(image.asBusinessImageResponse())
    }

    /**
     * Reorder images.
     *
     * **Route**: POST /api/v1/business/images/reorder
     */
    @PostMapping("/images/reorder")
    @Operation(
        summary = "Reorder images",
        description = "Reorder gallery images by providing ordered list of image UIDs"
    )
    fun reorderImages(@RequestBody request: ReorderImagesRequest): ApiResponse<String> {
        val business = businessService.getBusinessProfile()
        businessImageService.reorderImages(business.uid, request.imageUids)
        return ApiResponse.success("Images reordered successfully")
    }

    /**
     * Delete an image.
     *
     * **Route**: DELETE /api/v1/business/images/{imageUid}
     */
    @DeleteMapping("/images/{imageUid}")
    @Operation(
        summary = "Delete image",
        description = "Delete a business gallery image"
    )
    fun deleteImage(@PathVariable imageUid: String): ApiResponse<String> {
        val result = businessImageService.deleteImage(imageUid)
        return ApiResponse.success(result)
    }

    // ==================== Helper Methods ====================

    private fun getMediaTypeFromKey(objectKey: String): MediaType {
        return when {
            objectKey.endsWith(".png") -> MediaType.IMAGE_PNG
            objectKey.endsWith(".webp") -> MediaType.parseMediaType("image/webp")
            else -> MediaType.IMAGE_JPEG
        }
    }
}
