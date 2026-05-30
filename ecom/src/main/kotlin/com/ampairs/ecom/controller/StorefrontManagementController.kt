package com.ampairs.ecom.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.ecom.domain.dto.StorefrontRequest
import com.ampairs.ecom.domain.dto.StorefrontResponse
import com.ampairs.ecom.domain.dto.StorefrontUpdateRequest
import com.ampairs.ecom.domain.dto.asStorefrontResponse
import com.ampairs.ecom.service.StorefrontService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.ResponseStatus

@RestController
@RequestMapping("/api/v1/ecom/management")
class StorefrontManagementController(
    private val storefrontService: StorefrontService,
) {

    @PostMapping("/storefront")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    fun createStorefront(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
        @RequestBody @Valid request: StorefrontRequest,
    ): ApiResponse<StorefrontResponse> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            return ApiResponse.success(storefrontService.createStorefront(request, workspaceId).asStorefrontResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @GetMapping("/storefront")
    @PreAuthorize("isAuthenticated()")
    fun getStorefront(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
    ): ApiResponse<StorefrontResponse> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            return ApiResponse.success(storefrontService.getStorefront(workspaceId).asStorefrontResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @PutMapping("/storefront")
    @PreAuthorize("isAuthenticated()")
    fun updateStorefront(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
        @RequestBody @Valid request: StorefrontUpdateRequest,
    ): ApiResponse<StorefrontResponse> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            return ApiResponse.success(storefrontService.updateStorefront(request, workspaceId).asStorefrontResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @PutMapping("/storefront/publish")
    @PreAuthorize("isAuthenticated()")
    fun publishStorefront(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
    ): ApiResponse<StorefrontResponse> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            return ApiResponse.success(storefrontService.publishStorefront(workspaceId).asStorefrontResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @PutMapping("/storefront/unpublish")
    @PreAuthorize("isAuthenticated()")
    fun unpublishStorefront(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
    ): ApiResponse<StorefrontResponse> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            return ApiResponse.success(storefrontService.unpublishStorefront(workspaceId).asStorefrontResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }
}
