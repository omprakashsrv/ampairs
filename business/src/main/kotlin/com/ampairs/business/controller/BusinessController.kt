package com.ampairs.business.controller

import com.ampairs.business.model.dto.*
import com.ampairs.business.service.BusinessService
import com.ampairs.core.domain.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

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
    private val businessService: BusinessService
) {

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
}
