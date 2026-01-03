package com.ampairs.subscription.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.subscription.domain.dto.*
import com.ampairs.subscription.domain.service.BillingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/billing")
@Tag(name = "Billing Management", description = "APIs for billing, invoices, and payment methods")
class BillingController(
    private val billingService: BillingService
) {

    // =====================
    // Payment History APIs
    // =====================

    @GetMapping("/invoices")
    @Operation(summary = "Get billing/payment history")
    fun getPaymentHistory(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<PageResponse<PaymentTransactionResponse>> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        val pageResult = billingService.getPaymentHistory(workspaceId, page, size)
        return ApiResponse.success(PageResponse.from(pageResult) { it })
    }

    // =====================
    // Payment Method APIs
    // =====================

    @GetMapping("/payment-methods")
    @Operation(summary = "Get all payment methods")
    fun getPaymentMethods(): ApiResponse<List<PaymentMethodResponse>> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        return ApiResponse.success(billingService.getPaymentMethods(workspaceId))
    }

    @GetMapping("/payment-methods/default")
    @Operation(summary = "Get default payment method")
    fun getDefaultPaymentMethod(): ApiResponse<PaymentMethodResponse?> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        return ApiResponse.success(billingService.getDefaultPaymentMethod(workspaceId))
    }

    @PostMapping("/payment-methods/default")
    @Operation(summary = "Set default payment method")
    fun setDefaultPaymentMethod(
        @RequestBody @Valid request: SetDefaultPaymentMethodRequest
    ): ApiResponse<PaymentMethodResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        return ApiResponse.success(
            billingService.setDefaultPaymentMethod(workspaceId, request.paymentMethodUid)
        )
    }

    @DeleteMapping("/payment-methods/{uid}")
    @Operation(summary = "Remove a payment method")
    fun removePaymentMethod(@PathVariable uid: String): ApiResponse<Map<String, Boolean>> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        billingService.removePaymentMethod(workspaceId, uid)
        return ApiResponse.success(mapOf("removed" to true))
    }
}
