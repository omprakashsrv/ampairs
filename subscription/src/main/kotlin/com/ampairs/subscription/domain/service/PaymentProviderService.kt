package com.ampairs.subscription.domain.service

import com.ampairs.subscription.domain.dto.*
import com.ampairs.subscription.domain.model.*
import com.ampairs.subscription.domain.repository.*
import com.ampairs.subscription.exception.SubscriptionException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * Base interface for payment provider integrations
 */
interface PaymentProviderService {
    val provider: PaymentProvider

    /**
     * Verify a purchase token from mobile app
     */
    suspend fun verifyPurchase(request: VerifyPurchaseRequest): PurchaseVerificationResult

    /**
     * Create a subscription for the provider
     */
    suspend fun createSubscription(
        workspaceId: String,
        planCode: String,
        billingCycle: BillingCycle,
        currency: String,
        customerId: String?
    ): ProviderSubscriptionResult

    /**
     * Cancel subscription at provider
     */
    suspend fun cancelSubscription(
        externalSubscriptionId: String,
        immediate: Boolean
    ): Boolean

    /**
     * Pause subscription at provider
     */
    suspend fun pauseSubscription(externalSubscriptionId: String): Boolean

    /**
     * Resume subscription at provider
     */
    suspend fun resumeSubscription(externalSubscriptionId: String): Boolean

    /**
     * Change subscription plan
     */
    suspend fun changePlan(
        externalSubscriptionId: String,
        newPlanCode: String,
        billingCycle: BillingCycle,
        immediate: Boolean
    ): ProviderSubscriptionResult

    /**
     * Get subscription status from provider
     */
    suspend fun getSubscriptionStatus(externalSubscriptionId: String): ProviderSubscriptionStatus

    /**
     * Verify webhook signature
     */
    fun verifyWebhookSignature(payload: String, signature: String): Boolean
}

/**
 * Result of purchase verification
 */
data class PurchaseVerificationResult(
    val valid: Boolean,
    val planCode: String?,
    val billingCycle: BillingCycle?,
    val externalSubscriptionId: String?,
    val externalCustomerId: String?,
    val orderId: String?,
    val purchaseTime: java.time.Instant?,
    val expiryTime: java.time.Instant?,
    val autoRenewing: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Result of subscription creation at provider
 */
data class ProviderSubscriptionResult(
    val success: Boolean,
    val externalSubscriptionId: String?,
    val externalCustomerId: String?,
    val checkoutUrl: String?,
    val clientSecret: String?,
    val orderId: String?,
    val currentPeriodStart: java.time.Instant?,
    val currentPeriodEnd: java.time.Instant?,
    val errorMessage: String? = null
)

/**
 * Subscription status from provider
 */
data class ProviderSubscriptionStatus(
    val active: Boolean,
    val status: String,
    val currentPeriodStart: java.time.Instant?,
    val currentPeriodEnd: java.time.Instant?,
    val cancelAtPeriodEnd: Boolean = false,
    val cancelledAt: java.time.Instant? = null
)

/**
 * Orchestrates payment operations across different providers
 */
@Service
@org.springframework.transaction.annotation.Transactional
class PaymentOrchestrationService(
    private val subscriptionService: SubscriptionService,
    private val billingService: BillingService,
    private val subscriptionRepository: SubscriptionRepository,
    private val subscriptionPlanRepository: SubscriptionPlanRepository
) {
    private val logger = LoggerFactory.getLogger(PaymentOrchestrationService::class.java)

    private val providerServices = mutableMapOf<PaymentProvider, PaymentProviderService>()

    fun registerProvider(service: PaymentProviderService) {
        providerServices[service.provider] = service
    }

    fun getProviderService(provider: PaymentProvider): PaymentProviderService {
        return providerServices[provider]
            ?: throw SubscriptionException.ProviderError(provider.name, "Provider not configured")
    }

    /**
     * Initiate purchase for desktop (returns checkout URL)
     */
    suspend fun initiatePurchase(
        workspaceId: String,
        request: InitiatePurchaseRequest,
        provider: PaymentProvider
    ): InitiatePurchaseResponse {
        val plan = subscriptionPlanRepository.findByPlanCode(request.planCode)
            ?: throw SubscriptionException.PlanNotFoundException(request.planCode)

        val existingSubscription = subscriptionRepository.findByWorkspaceId(workspaceId)
        val customerId = existingSubscription?.externalCustomerId

        val providerService = getProviderService(provider)
        val result = providerService.createSubscription(
            workspaceId = workspaceId,
            planCode = request.planCode,
            billingCycle = request.billingCycle,
            currency = request.currency,
            customerId = customerId
        )

        if (!result.success) {
            throw SubscriptionException.PaymentFailed(result.errorMessage ?: "Unknown error")
        }

        val price = request.billingCycle.calculateDiscountedPrice(plan.getMonthlyPrice(request.currency))

        return InitiatePurchaseResponse(
            checkoutUrl = result.checkoutUrl,
            checkoutSessionId = result.externalSubscriptionId,
            provider = provider,
            subscriptionId = existingSubscription?.uid,
            razorpayOrderId = if (provider == PaymentProvider.RAZORPAY) result.orderId else null,
            razorpaySubscriptionId = if (provider == PaymentProvider.RAZORPAY) result.externalSubscriptionId else null,
            stripeClientSecret = if (provider == PaymentProvider.STRIPE) result.clientSecret else null,
            amount = price,
            currency = request.currency
        )
    }

    /**
     * Verify mobile in-app purchase
     */
    suspend fun verifyPurchase(
        workspaceId: String,
        request: VerifyPurchaseRequest
    ): SubscriptionResponse {
        val providerService = getProviderService(request.provider)
        val result = providerService.verifyPurchase(request)

        if (!result.valid) {
            throw SubscriptionException.InvalidPurchaseToken(request.provider.name)
        }

        val planCode = result.planCode
            ?: throw SubscriptionException.InvalidPurchaseToken(request.provider.name)

        val billingCycle = result.billingCycle ?: BillingCycle.MONTHLY

        // Activate subscription
        val subscription = subscriptionService.activateSubscription(
            workspaceId = workspaceId,
            planCode = planCode,
            billingCycle = billingCycle,
            provider = request.provider,
            externalSubscriptionId = result.externalSubscriptionId,
            externalCustomerId = result.externalCustomerId,
            currency = determineCurrency(request.provider)
        )

        // Record transaction
        val plan = subscriptionPlanRepository.findByPlanCode(planCode)
        val amount = plan?.let { billingCycle.calculateDiscountedPrice(it.getMonthlyPrice(subscription.currency)) } ?: BigDecimal.ZERO

        billingService.recordTransaction(
            subscriptionId = subscription.uid,
            workspaceId = workspaceId,
            provider = request.provider,
            externalPaymentId = request.orderId,
            externalInvoiceId = null,
            amount = amount,
            currency = subscription.currency,
            status = PaymentStatus.SUCCEEDED,
            description = "Subscription: $planCode ($billingCycle)",
            billingPeriodStart = result.purchaseTime,
            billingPeriodEnd = result.expiryTime,
            paymentMethodType = PaymentMethodType.IN_APP_PURCHASE,
            paymentMethodLast4 = null,
            cardBrand = null,
            receiptUrl = null
        )

        logger.info(
            "Verified {} purchase for workspace {}: plan={}, cycle={}",
            request.provider, workspaceId, planCode, billingCycle
        )

        return subscriptionService.getSubscription(workspaceId)
    }

    /**
     * Handle subscription renewal from webhook
     */
    fun handleRenewal(
        provider: PaymentProvider,
        externalSubscriptionId: String,
        orderId: String?,
        amount: BigDecimal,
        currency: String,
        periodStart: java.time.Instant?,
        periodEnd: java.time.Instant?
    ) {
        val subscription = subscriptionRepository.findByExternalSubscriptionId(externalSubscriptionId)
            ?: run {
                logger.warn("Renewal webhook for unknown subscription: {}", externalSubscriptionId)
                return
            }

        // Renew subscription
        subscriptionService.renewSubscription(subscription.workspaceId)

        // Record transaction
        billingService.recordTransaction(
            subscriptionId = subscription.uid,
            workspaceId = subscription.workspaceId,
            provider = provider,
            externalPaymentId = orderId,
            externalInvoiceId = null,
            amount = amount,
            currency = currency,
            status = PaymentStatus.SUCCEEDED,
            description = "Subscription renewal: ${subscription.planCode}",
            billingPeriodStart = periodStart,
            billingPeriodEnd = periodEnd,
            paymentMethodType = null,
            paymentMethodLast4 = null,
            cardBrand = null,
            receiptUrl = null
        )

        logger.info("Processed renewal for subscription: {}", externalSubscriptionId)
    }

    /**
     * Handle payment failure from webhook
     */
    fun handlePaymentFailure(
        provider: PaymentProvider,
        externalSubscriptionId: String,
        failureCode: String?,
        failureReason: String?
    ) {
        val subscription = subscriptionRepository.findByExternalSubscriptionId(externalSubscriptionId)
            ?: run {
                logger.warn("Payment failure webhook for unknown subscription: {}", externalSubscriptionId)
                return
            }

        subscriptionService.handlePaymentFailure(subscription.workspaceId, failureReason)
        logger.warn(
            "Payment failure for subscription {}: {} - {}",
            externalSubscriptionId, failureCode, failureReason
        )
    }

    /**
     * Handle cancellation from webhook
     */
    fun handleCancellation(
        provider: PaymentProvider,
        externalSubscriptionId: String,
        immediate: Boolean
    ) {
        val subscription = subscriptionRepository.findByExternalSubscriptionId(externalSubscriptionId)
            ?: run {
                logger.warn("Cancellation webhook for unknown subscription: {}", externalSubscriptionId)
                return
            }

        subscriptionService.cancelSubscription(
            subscription.workspaceId,
            immediate,
            "Cancelled via $provider"
        )
        logger.info("Processed cancellation for subscription: {} (immediate: {})", externalSubscriptionId, immediate)
    }

    private fun determineCurrency(provider: PaymentProvider): String {
        return when (provider) {
            PaymentProvider.RAZORPAY -> "INR"
            PaymentProvider.GOOGLE_PLAY, PaymentProvider.APP_STORE -> "USD"
            PaymentProvider.STRIPE -> "USD"
            PaymentProvider.MANUAL -> "INR"
        }
    }
}
