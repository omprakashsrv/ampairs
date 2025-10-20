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

    // ==================== Legacy Endpoint (Backward Compatibility) ====================

    /**
     * Get complete business profile (legacy endpoint).
     *
     * **Route**: GET /api/v1/business
     * **Returns**: Complete business profile
     * **Note**: Kept for backward compatibility. New code should use specific endpoints.
     */
    @GetMapping
    @Operation(
        summary = "Get complete business profile",
        description = "Retrieve complete business profile (legacy endpoint)"
    )
    fun getCompleteBusiness(): ApiResponse<BusinessResponse> {
        val business = businessService.getBusinessProfile()
        return ApiResponse.success(business.asBusinessResponse())
    }

    /**
     * Update complete business profile (legacy endpoint).
     *
     * **Route**: PUT /api/v1/business
     * **Returns**: Updated business profile
     * **Note**: Kept for backward compatibility. New code should use specific endpoints.
     */
    @PutMapping
    @Operation(
        summary = "Update complete business profile",
        description = "Update complete business profile (legacy endpoint)"
    )
    fun updateCompleteBusiness(
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

    // ==================== Profile Endpoints ====================

    /**
     * Get business profile and registration details.
     *
     * **Route**: GET /api/v1/business/profile
     * **Returns**: Detailed company profile and registration information
     */
    @GetMapping("/profile")
    @Operation(
        summary = "Get business profile",
        description = "Retrieve detailed business profile and registration information"
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
    fun getBusinessProfile(): ApiResponse<BusinessProfileResponse> {
        val business = businessService.getBusinessProfileDetails()
        return ApiResponse.success(business.asBusinessProfileResponse())
    }

    /**
     * Update business profile and registration details.
     *
     * **Route**: PUT /api/v1/business/profile
     * **Returns**: Updated business profile
     */
    @PutMapping("/profile")
    @Operation(
        summary = "Update business profile",
        description = "Update business profile and registration information"
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
    fun updateBusinessProfile(
        @Valid @RequestBody request: BusinessProfileUpdateRequest
    ): ApiResponse<BusinessProfileResponse> {
        val business = businessService.updateBusinessProfileDetails(request)
        return ApiResponse.success(business.asBusinessProfileResponse())
    }

    // ==================== Operations Endpoints ====================

    /**
     * Get business operational settings.
     *
     * **Route**: GET /api/v1/business/operations
     * **Returns**: Operational configuration (timezone, currency, hours, etc.)
     */
    @GetMapping("/operations")
    @Operation(
        summary = "Get operational settings",
        description = "Retrieve business operational configuration"
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Operational settings retrieved successfully"
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Business profile not found"
            )
        ]
    )
    fun getBusinessOperations(): ApiResponse<BusinessOperationsResponse> {
        val business = businessService.getBusinessOperations()
        return ApiResponse.success(business.asBusinessOperationsResponse())
    }

    /**
     * Update business operational settings.
     *
     * **Route**: PUT /api/v1/business/operations
     * **Returns**: Updated operational settings
     */
    @PutMapping("/operations")
    @Operation(
        summary = "Update operational settings",
        description = "Update business operational configuration"
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Operational settings updated successfully"
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Business profile not found"
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid operational settings"
            )
        ]
    )
    fun updateBusinessOperations(
        @Valid @RequestBody request: BusinessOperationsUpdateRequest
    ): ApiResponse<BusinessOperationsResponse> {
        val business = businessService.updateBusinessOperations(request)
        return ApiResponse.success(business.asBusinessOperationsResponse())
    }

    // ==================== Tax Configuration Endpoints ====================

    /**
     * Get tax configuration settings.
     *
     * **Route**: GET /api/v1/business/tax-config
     * **Returns**: Tax and compliance configuration
     */
    @GetMapping("/tax-config")
    @Operation(
        summary = "Get tax configuration",
        description = "Retrieve tax and compliance configuration settings"
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Tax configuration retrieved successfully"
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Business profile not found"
            )
        ]
    )
    fun getTaxConfiguration(): ApiResponse<TaxConfigurationResponse> {
        val business = businessService.getTaxConfiguration()
        return ApiResponse.success(business.asTaxConfigurationResponse())
    }

    /**
     * Update tax configuration settings.
     *
     * **Route**: PUT /api/v1/business/tax-config
     * **Returns**: Updated tax configuration
     */
    @PutMapping("/tax-config")
    @Operation(
        summary = "Update tax configuration",
        description = "Update tax and compliance configuration settings"
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Tax configuration updated successfully"
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Business profile not found"
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid tax configuration"
            )
        ]
    )
    fun updateTaxConfiguration(
        @Valid @RequestBody request: TaxConfigurationUpdateRequest
    ): ApiResponse<TaxConfigurationResponse> {
        val business = businessService.updateTaxConfiguration(request)
        return ApiResponse.success(business.asTaxConfigurationResponse())
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
    ): ApiResponse<BusinessProfileResponse> {
        val business = businessService.createBusinessProfile(request)
        return ApiResponse.success(business.asBusinessProfileResponse())
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
