package com.ampairs.ecom.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.ecom.domain.dto.StorefrontAccessBulkImportResult
import com.ampairs.ecom.domain.dto.StorefrontAccessEntryRequest
import com.ampairs.ecom.domain.dto.StorefrontAccessEntryResponse
import com.ampairs.ecom.domain.dto.asResponse
import com.ampairs.ecom.service.StorefrontAccessService
import com.ampairs.ecom.service.StorefrontService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/ecom/management/storefront/access")
class StorefrontAccessController(
    private val storefrontService: StorefrontService,
    private val storefrontAccessService: StorefrontAccessService,
) {

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun listAccessEntries(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<StorefrontAccessEntryResponse>> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            val storefront = storefrontService.getStorefront(workspaceId)
            val entries = storefrontAccessService.listAccessEntries(storefront.uid, PageRequest.of(page, size))
            return ApiResponse.success(PageResponse.from(entries) { it.asResponse() })
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    fun addAccessEntry(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
        @RequestBody @Valid request: StorefrontAccessEntryRequest,
    ): ApiResponse<StorefrontAccessEntryResponse> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            val storefront = storefrontService.getStorefront(workspaceId)
            val entry = storefrontAccessService.addAccessEntry(storefront.uid, workspaceId, request)
            return ApiResponse.success(entry.asResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @DeleteMapping("/{entryUid}")
    @PreAuthorize("isAuthenticated()")
    fun removeAccessEntry(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
        @PathVariable entryUid: String,
    ): ApiResponse<Map<String, Any>> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            val storefront = storefrontService.getStorefront(workspaceId)
            storefrontAccessService.removeAccessEntry(entryUid, storefront.uid)
            return ApiResponse.success(mapOf("deleted" to true, "uid" to entryUid))
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @PostMapping("/bulk-import", consumes = [MediaType.TEXT_PLAIN_VALUE])
    @PreAuthorize("isAuthenticated()")
    fun bulkImportAccessEntries(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
        @RequestBody csvContent: String,
    ): ApiResponse<StorefrontAccessBulkImportResult> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            val storefront = storefrontService.getStorefront(workspaceId)
            val result = storefrontAccessService.bulkImport(storefront.uid, workspaceId, csvContent)
            return ApiResponse.success(result)
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }
}
