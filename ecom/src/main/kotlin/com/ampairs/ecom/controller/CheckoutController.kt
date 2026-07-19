package com.ampairs.ecom.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.ecom.domain.dto.CheckoutRequest
import com.ampairs.ecom.domain.dto.EcomOrderResponse
import com.ampairs.ecom.domain.dto.asEcomOrderResponse
import com.ampairs.ecom.exception.StoreUnauthenticatedException
import com.ampairs.ecom.service.CheckoutService
import com.ampairs.ecom.service.StorefrontService
import com.ampairs.user.model.User
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
class CheckoutController(
    private val checkoutService: CheckoutService,
    private val storefrontService: StorefrontService,
) {

    @PostMapping("/v1/store/{slug}/cart/{sessionToken}/checkout")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    fun checkout(
        @PathVariable slug: String,
        @PathVariable sessionToken: String,
        @RequestBody @Valid request: CheckoutRequest,
        authentication: Authentication,
    ): ApiResponse<EcomOrderResponse> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(slug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val customerId = AuthenticationHelper.getUserId(authentication)
                ?: throw StoreUnauthenticatedException("Authentication required to checkout")
            val principal = authentication.principal
            val customerEmail = if (principal is User) principal.email ?: "" else ""
            val customerName = if (principal is User) principal.getDisplayName() else customerId
            // Recorded on the order for the merchant's record; linking itself now happens explicitly
            // via CustomerAccountController's link-candidate/confirm endpoints, not at checkout time.
            val customerPhone = if (principal is User) principal.phone.takeIf { it.isNotBlank() } else null

            val order = checkoutService.checkout(sessionToken, request, customerId, customerEmail, customerName, customerPhone, storefront)
            return ApiResponse.success(order.asEcomOrderResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }
}
