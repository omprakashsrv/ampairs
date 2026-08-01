package com.ampairs.ecom.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.core.service.EcomCustomerAccount
import com.ampairs.core.service.EcomCustomerService
import com.ampairs.core.service.EcomLinkCandidate
import com.ampairs.ecom.domain.dto.ConfirmLinkRequest
import com.ampairs.ecom.domain.dto.CustomerAddressRequest
import com.ampairs.ecom.domain.dto.CustomerAddressResponse
import com.ampairs.ecom.domain.dto.EcomOrderResponse
import com.ampairs.ecom.domain.dto.asAddressResponse
import com.ampairs.ecom.domain.dto.asEcomOrderResponse
import com.ampairs.ecom.service.CustomerAddressService
import com.ampairs.ecom.service.EcomOrderService
import com.ampairs.ecom.service.StorefrontService
import com.ampairs.user.model.User
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/ecom/account")
@PreAuthorize("isAuthenticated()")
class CustomerAccountController(
    private val addressService: CustomerAddressService,
    private val orderService: EcomOrderService,
    private val storefrontService: StorefrontService,
    private val ecomCustomerService: EcomCustomerService,
) {

    /**
     * The CRM accounts the signed-in buyer can order for (the "ordering for" picker). Typically 0 or
     * 1 (individual shopper); returns >1 for a buyer linked to multiple accounts (owner/manager/worker).
     */
    @GetMapping("/customers")
    fun getCustomers(
        @RequestParam("storefront_slug") storefrontSlug: String,
        authentication: Authentication,
    ): ApiResponse<List<EcomCustomerAccount>> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(storefrontSlug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val userId = AuthenticationHelper.getUserId(authentication)!!
            return ApiResponse.success(ecomCustomerService.listAccountsForUser(userId))
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    /**
     * Read-only: is there a CRM customer in this workspace whose phone matches the signed-in buyer's
     * own (verified, JWT-sourced) phone? Never creates a link — the result is a candidate for
     * [confirmLink] to commit once the buyer approves it.
     */
    @GetMapping("/link-candidate")
    fun getLinkCandidate(
        @RequestParam("storefront_slug") storefrontSlug: String,
        authentication: Authentication,
    ): ApiResponse<EcomLinkCandidate?> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(storefrontSlug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val phone = principalPhone(authentication)
            return ApiResponse.success(phone?.let { ecomCustomerService.findLinkCandidateByPhone(it) })
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    /**
     * Commits the buyer's link to a candidate returned by [getLinkCandidate]. Re-validated
     * server-side inside [EcomCustomerService.confirmLink] — the buyer can never link to an account
     * whose phone doesn't match their own, regardless of what the client sends.
     */
    @PostMapping("/link")
    fun confirmLink(
        @RequestParam("storefront_slug") storefrontSlug: String,
        @RequestBody @Valid request: ConfirmLinkRequest,
        authentication: Authentication,
    ): ApiResponse<EcomCustomerAccount> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(storefrontSlug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val userId = AuthenticationHelper.getUserId(authentication)!!
            val principal = authentication.principal
            val name = if (principal is User) principal.getDisplayName() else userId
            val email = if (principal is User) principal.email else null
            val account = ecomCustomerService.confirmLink(userId, request.customerId, name, principalPhone(authentication), email)
            return ApiResponse.success(account)
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    // The login's phone, sourced only from the authenticated principal — never from the request —
    // so a buyer can never probe or link against a phone number that isn't their own.
    private fun principalPhone(authentication: Authentication): String? =
        (authentication.principal as? User)?.phone?.takeIf { it.isNotBlank() }

    @GetMapping("/addresses")
    fun getAddresses(authentication: Authentication): ApiResponse<List<CustomerAddressResponse>> {
        val customerId = AuthenticationHelper.getUserId(authentication)!!
        return ApiResponse.success(addressService.getAddresses(customerId).map { it.asAddressResponse() })
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    fun addAddress(
        @RequestBody @Valid request: CustomerAddressRequest,
        authentication: Authentication,
    ): ApiResponse<CustomerAddressResponse> {
        val customerId = AuthenticationHelper.getUserId(authentication)!!
        return ApiResponse.success(addressService.addAddress(customerId, request).asAddressResponse())
    }

    @PutMapping("/addresses/{addressId}")
    fun updateAddress(
        @PathVariable addressId: String,
        @RequestBody @Valid request: CustomerAddressRequest,
        authentication: Authentication,
    ): ApiResponse<CustomerAddressResponse> {
        val customerId = AuthenticationHelper.getUserId(authentication)!!
        return ApiResponse.success(addressService.updateAddress(customerId, addressId, request).asAddressResponse())
    }

    @DeleteMapping("/addresses/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAddress(
        @PathVariable addressId: String,
        authentication: Authentication,
    ) {
        val customerId = AuthenticationHelper.getUserId(authentication)!!
        addressService.deleteAddress(customerId, addressId)
    }

    @GetMapping("/orders")
    fun getOrders(
        @RequestParam("storefront_slug") storefrontSlug: String,
        @PageableDefault(size = 20) pageable: Pageable,
        authentication: Authentication,
    ): ApiResponse<PageResponse<EcomOrderResponse>> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(storefrontSlug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val customerId = AuthenticationHelper.getUserId(authentication)!!
            val page = orderService.getCustomerOrders(customerId, storefront.uid, pageable)
            return ApiResponse.success(PageResponse.from(page))
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @GetMapping("/orders/{ecomOrderRef}")
    fun getOrder(
        @PathVariable ecomOrderRef: String,
        @RequestParam("storefront_slug") storefrontSlug: String,
        authentication: Authentication,
    ): ApiResponse<EcomOrderResponse> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(storefrontSlug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val customerId = AuthenticationHelper.getUserId(authentication)!!
            return ApiResponse.success(orderService.getCustomerOrder(customerId, ecomOrderRef).asEcomOrderResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }
}
