package com.ampairs.ecom.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.ecom.domain.dto.CartItemRequest
import com.ampairs.ecom.domain.dto.CartResponse
import com.ampairs.ecom.domain.dto.asCartResponse
import com.ampairs.ecom.service.CartService
import com.ampairs.ecom.service.StorefrontService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/store/{slug}/cart")
class CartController(
    private val cartService: CartService,
    private val storefrontService: StorefrontService,
) {

    @PostMapping
    fun createCart(
        @PathVariable slug: String,
        authentication: Authentication?,
    ): ApiResponse<CartResponse> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(slug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val customerId = authentication?.let { AuthenticationHelper.getUserId(it) }
            return ApiResponse.success(cartService.createCart(storefront.uid, customerId).asCartResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @GetMapping("/{sessionToken}")
    fun getCart(
        @PathVariable slug: String,
        @PathVariable sessionToken: String,
    ): ApiResponse<CartResponse> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(slug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            return ApiResponse.success(cartService.getCart(sessionToken).asCartResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @PostMapping("/{sessionToken}/items")
    fun addOrUpdateItem(
        @PathVariable slug: String,
        @PathVariable sessionToken: String,
        @RequestBody @Valid request: CartItemRequest,
    ): ApiResponse<CartResponse> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(slug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            return ApiResponse.success(cartService.addOrUpdateItem(sessionToken, request).asCartResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @DeleteMapping("/{sessionToken}/items/{itemId}")
    fun removeItem(
        @PathVariable slug: String,
        @PathVariable sessionToken: String,
        @PathVariable itemId: String,
    ): ApiResponse<CartResponse> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(slug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            return ApiResponse.success(cartService.removeItem(sessionToken, itemId).asCartResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @PostMapping("/{sessionToken}/claim")
    @PreAuthorize("isAuthenticated()")
    fun claimCart(
        @PathVariable slug: String,
        @PathVariable sessionToken: String,
        authentication: Authentication,
    ): ApiResponse<CartResponse> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(slug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            val customerId = AuthenticationHelper.getUserId(authentication)!!
            return ApiResponse.success(cartService.claimGuestCart(sessionToken, customerId, storefront.uid).asCartResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }

    @DeleteMapping("/{sessionToken}")
    fun clearCart(
        @PathVariable slug: String,
        @PathVariable sessionToken: String,
    ): ApiResponse<CartResponse> {
        val storefront = storefrontService.getPublishedStorefrontBySlug(slug)
        TenantContextHolder.setCurrentTenant(storefront.ownerId)
        try {
            return ApiResponse.success(cartService.clearCart(sessionToken).asCartResponse())
        } finally {
            TenantContextHolder.clearTenantContext()
        }
    }
}
