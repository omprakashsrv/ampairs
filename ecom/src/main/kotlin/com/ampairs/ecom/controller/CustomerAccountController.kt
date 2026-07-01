package com.ampairs.ecom.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.ecom.domain.dto.CustomerAddressRequest
import com.ampairs.ecom.domain.dto.CustomerAddressResponse
import com.ampairs.ecom.domain.dto.EcomOrderResponse
import com.ampairs.ecom.domain.dto.asAddressResponse
import com.ampairs.ecom.domain.dto.asEcomOrderResponse
import com.ampairs.ecom.service.CustomerAddressService
import com.ampairs.ecom.service.EcomOrderService
import com.ampairs.ecom.service.StorefrontService
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
) {

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
