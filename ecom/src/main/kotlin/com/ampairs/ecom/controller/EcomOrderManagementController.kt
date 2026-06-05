package com.ampairs.ecom.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.ecom.domain.dto.EcomOrderLineItemEditRequest
import com.ampairs.ecom.domain.dto.EcomOrderManagementResponse
import com.ampairs.ecom.domain.dto.asEcomOrderManagementResponse
import com.ampairs.ecom.domain.enums.EcomOrderStatus
import com.ampairs.ecom.service.EcomOrderService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/ecom/management/orders")
@PreAuthorize("isAuthenticated()")
class EcomOrderManagementController(
    private val ecomOrderService: EcomOrderService,
) {

    @GetMapping
    fun getOrders(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
        @RequestParam(required = false) status: EcomOrderStatus?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<PageResponse<EcomOrderManagementResponse>> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            val page = ecomOrderService.getManagementOrders(workspaceId, status, pageable)
                .map { it.asEcomOrderManagementResponse() }
            return ApiResponse.success(PageResponse.from(page))
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @GetMapping("/{ecomOrderRef}")
    fun getOrder(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
        @PathVariable ecomOrderRef: String,
    ): ApiResponse<EcomOrderManagementResponse> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            return ApiResponse.success(ecomOrderService.getManagementOrder(workspaceId, ecomOrderRef).asEcomOrderManagementResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @PutMapping("/{ecomOrderRef}/line-items")
    fun editLineItems(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
        @PathVariable ecomOrderRef: String,
        @RequestBody @Valid items: List<EcomOrderLineItemEditRequest>,
    ): ApiResponse<EcomOrderManagementResponse> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            return ApiResponse.success(ecomOrderService.editLineItems(workspaceId, ecomOrderRef, items).asEcomOrderManagementResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @PostMapping("/{ecomOrderRef}/confirm")
    @ResponseStatus(HttpStatus.OK)
    fun confirmOrder(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
        @PathVariable ecomOrderRef: String,
    ): ApiResponse<EcomOrderManagementResponse> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            return ApiResponse.success(ecomOrderService.confirmOrder(workspaceId, ecomOrderRef).asEcomOrderManagementResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @PutMapping("/{ecomOrderRef}/status")
    fun updateStatus(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
        @PathVariable ecomOrderRef: String,
        @RequestParam newStatus: EcomOrderStatus,
    ): ApiResponse<EcomOrderManagementResponse> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            return ApiResponse.success(ecomOrderService.advanceStatus(workspaceId, ecomOrderRef, newStatus).asEcomOrderManagementResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }
}
