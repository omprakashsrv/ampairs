package com.ampairs.subscription.provider

import com.ampairs.subscription.domain.dto.VerifyPurchaseRequest
import com.ampairs.subscription.domain.model.BillingCycle
import com.ampairs.subscription.domain.model.PaymentProvider
import com.ampairs.subscription.domain.repository.SubscriptionPlanRepository
import com.ampairs.subscription.domain.service.*
import com.ampairs.subscription.exception.SubscriptionException
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.time.Instant
import jakarta.annotation.PostConstruct

/**
 * Google Play Billing integration service.
 * Handles purchase verification for Android in-app subscriptions.
 */
@Service
class GooglePlayBillingService(
    @Value("\${google-play.package-name}") private val packageName: String,
    @Value("\${google-play.service-account-json-path}") private val serviceAccountJsonPath: String,
    private val subscriptionPlanRepository: SubscriptionPlanRepository
) : PaymentProviderService {

    private val logger = LoggerFactory.getLogger(GooglePlayBillingService::class.java)

    override val provider = PaymentProvider.GOOGLE_PLAY

    private lateinit var publisher: AndroidPublisher

    @PostConstruct
    fun init() {
        try {
            // Load service account JSON from file path
            val serviceAccountFile = File(serviceAccountJsonPath)

            if (!serviceAccountFile.exists()) {
                logger.error("Google Play service account file not found at: {}", serviceAccountJsonPath)
                throw IllegalStateException("Service account file not found: $serviceAccountJsonPath")
            }

            val credentials = GoogleCredentials.fromStream(
                FileInputStream(serviceAccountFile)
            ).createScoped(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER))

            publisher = AndroidPublisher.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                HttpCredentialsAdapter(credentials)
            ).setApplicationName("Ampairs").build()

            logger.info("Google Play Billing service initialized for package: {} from {}", packageName, serviceAccountJsonPath)
        } catch (e: Exception) {
            logger.error("Failed to initialize Google Play Billing service from path: {}", serviceAccountJsonPath, e)
            throw IllegalStateException("Google Play Billing service initialization failed", e)
        }
    }

    override suspend fun verifyPurchase(request: VerifyPurchaseRequest): PurchaseVerificationResult {
        return try {
            logger.debug("Verifying Google Play purchase: productId={}, token={}",
                request.productId, request.purchaseToken.take(10) + "...")

            // Call Google Play Developer API to verify subscription purchase
            val purchase = publisher.purchases()
                .subscriptions()
                .get(packageName, request.productId, request.purchaseToken)
                .execute()

            // Validate subscription state
            // paymentState: 0 = Payment pending, 1 = Payment received, 2 = Free trial, 3 = Pending deferred upgrade/downgrade
            val valid = purchase.paymentState == 1 || purchase.paymentState == 2

            if (!valid) {
                logger.warn("Google Play purchase verification failed: paymentState={}", purchase.paymentState)
                return PurchaseVerificationResult(
                    valid = false,
                    planCode = null,
                    billingCycle = null,
                    externalSubscriptionId = null,
                    externalCustomerId = null,
                    orderId = null,
                    purchaseTime = null,
                    expiryTime = null,
                    autoRenewing = false,
                    errorMessage = "Payment not received (paymentState: ${purchase.paymentState})"
                )
            }

            // Map Google Play product ID to our plan code and billing cycle
            val (planCode, billingCycle) = mapProductIdToPlan(request.productId)

            val autoRenewing = purchase.autoRenewing ?: false
            val purchaseTime = purchase.startTimeMillis?.let { Instant.ofEpochMilli(it) }
            val expiryTime = purchase.expiryTimeMillis?.let { Instant.ofEpochMilli(it) }

            logger.info("Google Play purchase verified successfully: planCode={}, cycle={}, autoRenewing={}",
                planCode, billingCycle, autoRenewing)

            PurchaseVerificationResult(
                valid = true,
                planCode = planCode,
                billingCycle = billingCycle,
                externalSubscriptionId = purchase.orderId,
                externalCustomerId = null, // Google Play doesn't use customer IDs
                orderId = purchase.orderId,
                purchaseTime = purchaseTime,
                expiryTime = expiryTime,
                autoRenewing = autoRenewing
            )
        } catch (e: Exception) {
            logger.error("Error verifying Google Play purchase", e)
            PurchaseVerificationResult(
                valid = false,
                planCode = null,
                billingCycle = null,
                externalSubscriptionId = null,
                externalCustomerId = null,
                orderId = null,
                purchaseTime = null,
                expiryTime = null,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }

    override suspend fun createSubscription(
        workspaceId: String,
        planCode: String,
        billingCycle: BillingCycle,
        currency: String,
        customerId: String?
    ): ProviderSubscriptionResult {
        // Google Play subscriptions are created on the device, not server-side
        throw UnsupportedOperationException("Google Play subscriptions are created on device via Google Play Billing Library")
    }

    override suspend fun cancelSubscription(
        externalSubscriptionId: String,
        immediate: Boolean
    ): Boolean {
        // Google Play subscriptions are typically cancelled by user in Play Store
        // Server-side cancellation is not commonly supported
        logger.warn("Google Play subscription cancellation requested but not supported server-side: {}", externalSubscriptionId)
        return false
    }

    override suspend fun pauseSubscription(externalSubscriptionId: String): Boolean {
        // Pause is handled by user in Google Play Store
        logger.warn("Google Play subscription pause requested but not supported server-side: {}", externalSubscriptionId)
        return false
    }

    override suspend fun resumeSubscription(externalSubscriptionId: String): Boolean {
        // Resume is handled by user in Google Play Store
        logger.warn("Google Play subscription resume requested but not supported server-side: {}", externalSubscriptionId)
        return false
    }

    override suspend fun changePlan(
        externalSubscriptionId: String,
        newPlanCode: String,
        billingCycle: BillingCycle,
        immediate: Boolean
    ): ProviderSubscriptionResult {
        // Plan changes happen on device via Google Play Billing Library
        throw UnsupportedOperationException("Google Play plan changes are handled on device")
    }

    override suspend fun getSubscriptionStatus(externalSubscriptionId: String): ProviderSubscriptionStatus {
        return try {
            // We can't get status by orderId directly, this is a limitation
            // In real scenarios, we'd need the productId and purchaseToken
            logger.warn("Getting subscription status by orderId is not fully supported in Google Play: {}", externalSubscriptionId)

            ProviderSubscriptionStatus(
                active = false,
                status = "UNKNOWN",
                currentPeriodStart = null,
                currentPeriodEnd = null,
                cancelAtPeriodEnd = false,
                cancelledAt = null
            )
        } catch (e: Exception) {
            logger.error("Error getting Google Play subscription status", e)
            throw SubscriptionException.ProviderError("GOOGLE_PLAY", e.message ?: "Unknown error")
        }
    }

    override fun verifyWebhookSignature(payload: String, signature: String): Boolean {
        // Google Play Real-Time Developer Notifications use Cloud Pub/Sub
        // Signature verification is handled by Google Cloud infrastructure
        // For our purposes, we'll validate the JWT token in the webhook controller
        return true
    }

    /**
     * Map Google Play product ID to our internal plan code and billing cycle.
     * Product ID format: ampairs_{plan}_{cycle}
     * Example: ampairs_professional_monthly -> (PROFESSIONAL, MONTHLY)
     */
    private fun mapProductIdToPlan(productId: String): Pair<String, BillingCycle> {
        val parts = productId.lowercase().split("_")

        if (parts.size < 3 || parts[0] != "ampairs") {
            throw IllegalArgumentException("Invalid Google Play product ID format: $productId")
        }

        val plan = parts[1].uppercase()
        val cycle = parts[2].uppercase()

        val billingCycle = when (cycle) {
            "MONTHLY" -> BillingCycle.MONTHLY
            "ANNUAL" -> BillingCycle.ANNUAL
            else -> throw IllegalArgumentException("Unsupported billing cycle in product ID: $productId")
        }

        // Validate plan exists
        subscriptionPlanRepository.findByPlanCode(plan)
            ?: throw SubscriptionException.PlanNotFoundException(plan)

        return Pair(plan, billingCycle)
    }

    /**
     * Get Google Play product ID for a plan and billing cycle
     */
    fun getProductId(planCode: String, billingCycle: BillingCycle): String {
        val plan = subscriptionPlanRepository.findByPlanCode(planCode)
            ?: throw SubscriptionException.PlanNotFoundException(planCode)

        return when (billingCycle) {
            BillingCycle.MONTHLY -> plan.googlePlayProductIdMonthly
            BillingCycle.ANNUAL -> plan.googlePlayProductIdAnnual
            else -> throw IllegalArgumentException("Google Play only supports MONTHLY and ANNUAL cycles")
        } ?: throw IllegalStateException("Google Play product ID not configured for $planCode $billingCycle")
    }
}
