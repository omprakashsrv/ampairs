package com.ampairs.business.controller

import com.ampairs.business.model.dto.BusinessCreateRequest
import com.ampairs.business.model.dto.BusinessResponse
import com.ampairs.business.model.dto.BusinessUpdateRequest
import com.ampairs.business.model.dto.asBusinessResponse
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
 * REST controller for Business profile management.
 *
 * **Base Path**: `/api/v1/business`
 *
 * **Endpoints**:
 * - GET /api/v1/business - Get business profile for current workspace
 * - POST /api/v1/business - Create new business profile
 * - PUT /api/v1/business - Update business profile
 *
 * **Multi-Tenancy**:
 * - All operations scoped to current workspace (from X-Workspace-ID header)
 * - Service layer uses TenantContextHolder to get workspace ID
 *
 * **Error Handling**:
 * - Exceptions handled by BusinessExceptionHandler
 * - Returns ApiResponse<T> format consistently
 */
@RestController
@RequestMapping("/api/v1/business")
@Tag(name = "Business Profile", description = "Business profile management for workspace")
class BusinessController @Autowired constructor(
    private val businessService: BusinessService
) {

    /**
     * Get business profile for current workspace.
     *
     * **Returns**: 200 OK with business profile
     * **Errors**:
     * - 404 NOT_FOUND if no business profile exists
     */
    @GetMapping
    @Operation(
        summary = "Get business profile",
        description = "Retrieve business profile for the current workspace"
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
    fun getBusinessProfile(): ApiResponse<BusinessResponse> {
        val business = businessService.getBusinessProfile()
        return ApiResponse.success(business.asBusinessResponse())
    }

    /**
     * Create business profile for current workspace.
     *
     * **Returns**: 201 CREATED with created business profile
     * **Errors**:
     * - 409 CONFLICT if business already exists
     * - 400 BAD_REQUEST for validation errors
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create business profile",
        description = "Create a new business profile for the current workspace. Only one business profile allowed per workspace."
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
     * Update business profile for current workspace.
     *
     * **Returns**: 200 OK with updated business profile
     * **Errors**:
     * - 404 NOT_FOUND if business doesn't exist
     * - 400 BAD_REQUEST for validation errors
     */
    @PutMapping
    @Operation(
        summary = "Update business profile",
        description = "Update existing business profile for the current workspace. Supports partial updates."
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
        @Valid @RequestBody request: BusinessUpdateRequest
    ): ApiResponse<BusinessResponse> {
        val business = businessService.updateBusinessProfile(request)
        return ApiResponse.success(business.asBusinessResponse())
    }

    /**
     * Check if business profile exists for current workspace.
     *
     * **Returns**: 200 OK with exists flag
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

    /**
     * Get full formatted address for current business.
     *
     * **Returns**: 200 OK with formatted address
     * **Errors**:
     * - 404 NOT_FOUND if business doesn't exist
     */
    @GetMapping("/address")
    @Operation(
        summary = "Get formatted address",
        description = "Get full formatted address for the business profile"
    )
    fun getFullAddress(): ApiResponse<Map<String, String>> {
        val address = businessService.getFullAddress()
        return ApiResponse.success(mapOf("address" to address))
    }
}
