package com.ampairs.product.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.product.service.ProductEcomService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/products/{productId}/ecom")
class ProductEcomController(
    private val productEcomService: ProductEcomService,
) {

    @PutMapping("/list")
    @PreAuthorize("isAuthenticated()")
    fun listProduct(
        @PathVariable productId: String,
        @RequestHeader("X-Workspace-ID") workspaceId: String,
    ): ApiResponse<Any> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            productEcomService.listProductOnEcom(productId, workspaceId)
            return ApiResponse.success(mapOf("listed" to true))
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @PutMapping("/unlist")
    @PreAuthorize("isAuthenticated()")
    fun unlistProduct(
        @PathVariable productId: String,
        @RequestHeader("X-Workspace-ID") workspaceId: String,
    ): ApiResponse<Any> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            productEcomService.unlistProductFromEcom(productId, workspaceId)
            return ApiResponse.success(mapOf("listed" to false))
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }
}
