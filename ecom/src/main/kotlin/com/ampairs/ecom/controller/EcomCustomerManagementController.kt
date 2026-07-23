package com.ampairs.ecom.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.service.EcomCustomerService
import com.ampairs.ecom.domain.dto.EcomContactResponse
import com.ampairs.ecom.domain.dto.SetEcomContactStatusRequest
import com.ampairs.ecom.domain.dto.asEcomContactResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Owner-facing "ecom users" management: every buyer linked to any of this workspace's CRM
 * customers, across all customers at once, with a restrict/re-enable toggle. The per-customer
 * equivalent (scoped to one customer's detail page) lives in the `customer` module's
 * `CustomerController` — both read/write the same `CustomerContact` data via
 * [EcomCustomerService.listAllContacts] / [EcomCustomerService.setContactActive].
 */
@RestController
@RequestMapping("/v1/ecom/management/customers")
class EcomCustomerManagementController(
    private val ecomCustomerService: EcomCustomerService,
) {

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun listContacts(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
    ): ApiResponse<List<EcomContactResponse>> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            return ApiResponse.success(ecomCustomerService.listAllContacts().map { it.asEcomContactResponse() })
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @PatchMapping("/{contactUid}/status")
    @PreAuthorize("isAuthenticated()")
    fun setContactStatus(
        @RequestHeader("X-Workspace-ID") workspaceId: String,
        @PathVariable contactUid: String,
        @RequestBody request: SetEcomContactStatusRequest,
    ): ApiResponse<EcomContactResponse> {
        TenantContextHolder.setCurrentTenant(workspaceId)
        try {
            val contact = ecomCustomerService.setContactActive(contactUid, request.active)
            return ApiResponse.success(contact.asEcomContactResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }
}
