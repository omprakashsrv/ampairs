package com.ampairs.subscription.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.subscription.domain.dto.*
import com.ampairs.subscription.domain.model.PaymentProvider
import com.ampairs.subscription.domain.service.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscription Management", description = "APIs for subscription and billing management")
class SubscriptionController(
    private val subscriptionService: SubscriptionService,
    private val paymentOrchestrationService: PaymentOrchestrationService,
    private val deviceRegistrationService: DeviceRegistrationService,
    private val billingService: BillingService
) {

    // =====================
    // Plan APIs
    // =====================

    @GetMapping("/plans")
    @Operation(summary = "Get all available subscription plans")
    fun getPlans(): ApiResponse<List<PlanResponse>> {
        return ApiResponse.success(subscriptionService.getAvailablePlans())
    }

    @GetMapping("/plans/{planCode}")
    @Operation(summary = "Get a specific subscription plan")
    fun getPlan(@PathVariable planCode: String): ApiResponse<PlanResponse> {
        return ApiResponse.success(subscriptionService.getPlanByCode(planCode))
    }

    // =====================
    // Subscription APIs
    // =====================

    @GetMapping("/current")
    @Operation(summary = "Get current subscription for the workspace")
    fun getCurrentSubscription(): ApiResponse<SubscriptionResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")
        return ApiResponse.success(subscriptionService.getSubscription(workspaceId))
    }

    @PostMapping("/purchase/initiate")
    @Operation(summary = "Initiate subscription purchase (Desktop - returns checkout URL)")
    fun initiatePurchase(
        @RequestBody @Valid request: InitiatePurchaseRequest,
        @RequestParam provider: PaymentProvider
    ): ApiResponse<InitiatePurchaseResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        val response = runBlocking {
            paymentOrchestrationService.initiatePurchase(workspaceId, request, provider)
        }
        return ApiResponse.success(response)
    }

    @PostMapping("/purchase/verify")
    @Operation(summary = "Verify mobile in-app purchase (Google Play / App Store)")
    fun verifyPurchase(@RequestBody @Valid request: VerifyPurchaseRequest): ApiResponse<SubscriptionResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        val response = runBlocking {
            paymentOrchestrationService.verifyPurchase(workspaceId, request)
        }
        return ApiResponse.success(response)
    }

    @PostMapping("/change-plan")
    @Operation(summary = "Change subscription plan (upgrade/downgrade)")
    fun changePlan(@RequestBody @Valid request: ChangePlanRequest): ApiResponse<ChangePlanResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        return ApiResponse.success(
            subscriptionService.changePlan(
                workspaceId,
                request.newPlanCode,
                request.billingCycle,
                request.immediate
            )
        )
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel subscription")
    fun cancelSubscription(@RequestBody @Valid request: CancelSubscriptionRequest): ApiResponse<SubscriptionResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        return ApiResponse.success(
            subscriptionService.cancelSubscription(workspaceId, request.immediate, request.reason)
        )
    }

    @PostMapping("/pause")
    @Operation(summary = "Pause subscription")
    fun pauseSubscription(@RequestBody @Valid request: PauseSubscriptionRequest): ApiResponse<SubscriptionResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        return ApiResponse.success(
            subscriptionService.pauseSubscription(workspaceId, request.pauseDays, request.reason)
        )
    }

    @PostMapping("/resume")
    @Operation(summary = "Resume paused subscription")
    fun resumeSubscription(): ApiResponse<SubscriptionResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        return ApiResponse.success(subscriptionService.resumeSubscription(workspaceId))
    }

    @PostMapping("/trial")
    @Operation(summary = "Start trial for a plan")
    fun startTrial(
        @RequestParam planCode: String,
        @RequestParam(defaultValue = "14") trialDays: Int
    ): ApiResponse<SubscriptionResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        return ApiResponse.success(subscriptionService.startTrial(workspaceId, planCode, trialDays))
    }

    // =====================
    // Usage & Limits APIs
    // =====================

    @GetMapping("/usage")
    @Operation(summary = "Get current usage for the workspace")
    fun getUsage(): ApiResponse<UsageResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        return ApiResponse.success(subscriptionService.getUsage(workspaceId))
    }

    @GetMapping("/limits/check")
    @Operation(summary = "Check if a resource limit is exceeded")
    fun checkLimit(
        @RequestParam resourceType: String,
        @RequestParam currentCount: Int
    ): ApiResponse<LimitCheckResult> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        return ApiResponse.success(subscriptionService.checkLimit(workspaceId, resourceType, currentCount))
    }

    @GetMapping("/features/{feature}")
    @Operation(summary = "Check if a feature is available")
    fun hasFeature(@PathVariable feature: String): ApiResponse<Map<String, Boolean>> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        val hasFeature = subscriptionService.hasFeature(workspaceId, feature)
        return ApiResponse.success(mapOf("available" to hasFeature))
    }

    // =====================
    // Device Sync APIs
    // =====================

    @PostMapping("/sync")
    @Operation(summary = "Sync subscription state (for offline-first apps)")
    fun syncSubscription(
        @RequestBody @Valid request: SyncSubscriptionRequest,
        @RequestHeader("X-User-Id") userId: String
    ): ApiResponse<SyncSubscriptionResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        return ApiResponse.success(
            deviceRegistrationService.syncSubscription(workspaceId, userId, request)
        )
    }

    // =====================
    // Payment History APIs (alias for /billing/invoices)
    // =====================

    @GetMapping("/payments")
    @Operation(summary = "Get payment history")
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
    // Payment Method APIs (alias for /billing/payment-methods)
    // =====================

    @GetMapping("/payment-methods")
    @Operation(summary = "Get all saved payment methods")
    fun getPaymentMethods(): ApiResponse<List<PaymentMethodResponse>> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        return ApiResponse.success(billingService.getPaymentMethods(workspaceId))
    }

    @PutMapping("/payment-methods/{uid}/default")
    @Operation(summary = "Set a payment method as default")
    fun setDefaultPaymentMethod(@PathVariable uid: String): ApiResponse<PaymentMethodResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")

        return ApiResponse.success(billingService.setDefaultPaymentMethod(workspaceId, uid))
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
