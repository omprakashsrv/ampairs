package com.ampairs.subscription.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.subscription.domain.dto.*
import com.ampairs.subscription.domain.service.PaymentOrchestrationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.springframework.web.bind.annotation.*

/**
 * Unified payment controller for all payment providers.
 * Handles both mobile (Google Play, App Store) and desktop/web (Razorpay, Stripe) payments.
 */
@RestController
@RequestMapping("/api/v1/subscription/payment")
@Tag(name = "Payment", description = "Payment and purchase APIs")
class PaymentController(
    private val orchestrationService: PaymentOrchestrationService
) {

    /**
     * Initiate purchase for desktop/web (Razorpay/Stripe).
     * Returns checkout URL for user to complete payment.
     *
     * Flow:
     * 1. Client calls this endpoint with plan and provider
     * 2. Backend creates checkout session at payment provider
     * 3. Returns checkout URL
     * 4. Client opens checkout URL in browser
     * 5. User completes payment
     * 6. Payment provider sends webhook to our server
     * 7. Webhook activates subscription
     */
    @PostMapping("/initiate")
    @Operation(summary = "Initiate purchase (Desktop/Web)", description = "Create checkout session for Razorpay or Stripe")
    fun initiatePurchase(
        @RequestBody @Valid request: InitiatePurchaseRequest
    ): ApiResponse<InitiatePurchaseResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        val response = runBlocking {
            orchestrationService.initiatePurchase(workspaceId, request, request.provider)
        }

        return ApiResponse.success(response)
    }

    /**
     * Verify mobile in-app purchase (Google Play/App Store).
     * Called after user completes purchase on device.
     *
     * Flow:
     * 1. User purchases subscription in mobile app
     * 2. Mobile app receives purchase token
     * 3. Mobile app calls this endpoint with token
     * 4. Backend verifies token with Google/Apple
     * 5. If valid, activates subscription
     * 6. Returns subscription details
     */
    @PostMapping("/verify")
    @Operation(summary = "Verify mobile purchase", description = "Verify Google Play or App Store purchase token")
    fun verifyPurchase(
        @RequestBody @Valid request: VerifyPurchaseRequest
    ): ApiResponse<SubscriptionResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        val response = runBlocking {
            orchestrationService.verifyPurchase(workspaceId, request)
        }

        return ApiResponse.success(response)
    }

    /**
     * Get available product IDs for a plan (for mobile apps).
     * Returns Google Play and App Store product IDs.
     */
    @GetMapping("/products/{planCode}")
    @Operation(summary = "Get product IDs", description = "Get Google Play and App Store product IDs for a plan")
    fun getProductIds(
        @PathVariable planCode: String,
        @RequestParam(defaultValue = "MONTHLY") billingCycle: String
    ): ApiResponse<ProductIdsResponse> {
        // Implementation will be added later
        return ApiResponse.success(
            ProductIdsResponse(
                planCode = planCode,
                googlePlayMonthly = "ampairs_${planCode.lowercase()}_monthly",
                googlePlayAnnual = "ampairs_${planCode.lowercase()}_annual",
                appStoreMonthly = "com.ampairs.${planCode.lowercase()}.monthly",
                appStoreAnnual = "com.ampairs.${planCode.lowercase()}.annual"
            )
        )
    }
}

/**
 * Response containing product IDs for mobile platforms
 */
data class ProductIdsResponse(
    val planCode: String,
    val googlePlayMonthly: String?,
    val googlePlayAnnual: String?,
    val appStoreMonthly: String?,
    val appStoreAnnual: String?
)
