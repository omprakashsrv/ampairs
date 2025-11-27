package com.ampairs.subscription.provider

import com.ampairs.subscription.domain.dto.VerifyPurchaseRequest
import com.ampairs.subscription.domain.model.BillingCycle
import com.ampairs.subscription.domain.model.PaymentProvider
import com.ampairs.subscription.domain.repository.SubscriptionPlanRepository
import com.ampairs.subscription.domain.service.*
import com.ampairs.subscription.exception.SubscriptionException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.time.Instant

/**
 * Apple App Store integration service.
 * Handles purchase verification for iOS in-app subscriptions.
 */
@Service
class AppleAppStoreService(
    @Value("\${apple-app-store.shared-secret}") private val sharedSecret: String,
    @Value("\${apple-app-store.bundle-id}") private val bundleId: String,
    @Value("\${apple-app-store.production:true}") private val productionMode: Boolean,
    private val subscriptionPlanRepository: SubscriptionPlanRepository,
    private val objectMapper: ObjectMapper,
    private val webClientBuilder: WebClient.Builder
) : PaymentProviderService {

    private val logger = LoggerFactory.getLogger(AppleAppStoreService::class.java)

    override val provider = PaymentProvider.APP_STORE

    private val productionUrl = "https://buy.itunes.apple.com/verifyReceipt"
    private val sandboxUrl = "https://sandbox.itunes.apple.com/verifyReceipt"

    private val webClient = webClientBuilder.build()

    override suspend fun verifyPurchase(request: VerifyPurchaseRequest): PurchaseVerificationResult {
        return try {
            logger.debug("Verifying Apple App Store purchase: productId={}", request.productId)

            // Base64 encoded receipt data
            val receiptData = request.purchaseToken

            // Try production first, fall back to sandbox if needed
            var response = verifyReceiptWithApple(receiptData, production = productionMode)

            // Status 21007 means this is a sandbox receipt sent to production
            if (response.status == 21007 && productionMode) {
                logger.debug("Receipt is from sandbox, retrying with sandbox URL")
                response = verifyReceiptWithApple(receiptData, production = false)
            }

            // Status 0 = Valid
            if (response.status != 0) {
                logger.warn("Apple receipt verification failed with status: {}", response.status)
                return PurchaseVerificationResult(
                    valid = false,
                    planCode = null,
                    billingCycle = null,
                    externalSubscriptionId = null,
                    externalCustomerId = null,
                    orderId = null,
                    purchaseTime = null,
                    expiryTime = null,
                    errorMessage = getAppleErrorMessage(response.status)
                )
            }

            // Get latest receipt info for subscription
            val latestReceipt = response.latestReceiptInfo?.firstOrNull()
                ?: return PurchaseVerificationResult(
                    valid = false,
                    planCode = null,
                    billingCycle = null,
                    externalSubscriptionId = null,
                    externalCustomerId = null,
                    orderId = null,
                    purchaseTime = null,
                    expiryTime = null,
                    errorMessage = "No subscription found in receipt"
                )

            val productId = latestReceipt.productId
            val originalTransactionId = latestReceipt.originalTransactionId

            // Map product ID to plan code and billing cycle
            val (planCode, billingCycle) = mapProductIdToPlan(productId)

            // Check auto-renew status
            val pendingRenewalInfo = response.pendingRenewalInfo?.firstOrNull()
            val autoRenewing = pendingRenewalInfo?.autoRenewStatus == "1"

            val purchaseTime = latestReceipt.purchaseDateMs?.let { Instant.ofEpochMilli(it) }
            val expiryTime = latestReceipt.expiresDateMs?.let { Instant.ofEpochMilli(it) }

            logger.info("Apple purchase verified successfully: planCode={}, cycle={}, autoRenewing={}",
                planCode, billingCycle, autoRenewing)

            PurchaseVerificationResult(
                valid = true,
                planCode = planCode,
                billingCycle = billingCycle,
                externalSubscriptionId = originalTransactionId,
                externalCustomerId = null, // Apple doesn't use customer IDs
                orderId = latestReceipt.transactionId,
                purchaseTime = purchaseTime,
                expiryTime = expiryTime,
                autoRenewing = autoRenewing
            )
        } catch (e: Exception) {
            logger.error("Error verifying Apple App Store purchase", e)
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
        // App Store subscriptions are created on the device, not server-side
        throw UnsupportedOperationException("App Store subscriptions are created on device via StoreKit")
    }

    override suspend fun cancelSubscription(
        externalSubscriptionId: String,
        immediate: Boolean
    ): Boolean {
        // App Store subscriptions are cancelled by user in App Store settings
        logger.warn("App Store subscription cancellation requested but not supported server-side: {}", externalSubscriptionId)
        return false
    }

    override suspend fun pauseSubscription(externalSubscriptionId: String): Boolean {
        logger.warn("App Store subscription pause requested but not supported: {}", externalSubscriptionId)
        return false
    }

    override suspend fun resumeSubscription(externalSubscriptionId: String): Boolean {
        logger.warn("App Store subscription resume requested but not supported: {}", externalSubscriptionId)
        return false
    }

    override suspend fun changePlan(
        externalSubscriptionId: String,
        newPlanCode: String,
        billingCycle: BillingCycle,
        immediate: Boolean
    ): ProviderSubscriptionResult {
        // Plan changes happen on device via StoreKit
        throw UnsupportedOperationException("App Store plan changes are handled on device")
    }

    override suspend fun getSubscriptionStatus(externalSubscriptionId: String): ProviderSubscriptionStatus {
        logger.warn("Getting App Store subscription status by transaction ID is not directly supported: {}", externalSubscriptionId)
        return ProviderSubscriptionStatus(
            active = false,
            status = "UNKNOWN",
            currentPeriodStart = null,
            currentPeriodEnd = null,
            cancelAtPeriodEnd = false,
            cancelledAt = null
        )
    }

    override fun verifyWebhookSignature(payload: String, signature: String): Boolean {
        // Apple App Store Server Notifications use signed JWT tokens
        // Verification will be done in the webhook controller using Apple's public key
        return true
    }

    /**
     * Verify receipt with Apple's verifyReceipt API
     */
    private suspend fun verifyReceiptWithApple(
        receiptData: String,
        production: Boolean
    ): AppleReceiptResponse {
        val url = if (production) productionUrl else sandboxUrl

        val requestBody = mapOf(
            "receipt-data" to receiptData,
            "password" to sharedSecret,
            "exclude-old-transactions" to true
        )

        return try {
            val responseBody = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .awaitBody<String>()

            parseAppleResponse(responseBody)
        } catch (e: Exception) {
            logger.error("Error calling Apple verifyReceipt API", e)
            AppleReceiptResponse(status = 21100, latestReceiptInfo = null, pendingRenewalInfo = null)
        }
    }

    /**
     * Parse Apple's receipt verification response
     */
    private fun parseAppleResponse(responseBody: String): AppleReceiptResponse {
        val json = objectMapper.readTree(responseBody)

        val status = json.get("status")?.asInt() ?: 21100

        val latestReceiptInfo = json.get("latest_receipt_info")?.let { array ->
            array.map { parseReceiptInfo(it) }
        }

        val pendingRenewalInfo = json.get("pending_renewal_info")?.let { array ->
            array.map { parsePendingRenewalInfo(it) }
        }

        return AppleReceiptResponse(
            status = status,
            latestReceiptInfo = latestReceiptInfo,
            pendingRenewalInfo = pendingRenewalInfo
        )
    }

    private fun parseReceiptInfo(node: JsonNode): AppleReceiptInfo {
        return AppleReceiptInfo(
            productId = node.get("product_id")?.asText() ?: "",
            transactionId = node.get("transaction_id")?.asText() ?: "",
            originalTransactionId = node.get("original_transaction_id")?.asText() ?: "",
            purchaseDateMs = node.get("purchase_date_ms")?.asLong(),
            expiresDateMs = node.get("expires_date_ms")?.asLong()
        )
    }

    private fun parsePendingRenewalInfo(node: JsonNode): ApplePendingRenewalInfo {
        return ApplePendingRenewalInfo(
            autoRenewStatus = node.get("auto_renew_status")?.asText()
        )
    }

    /**
     * Map Apple product ID to our internal plan code and billing cycle.
     * Product ID format: com.ampairs.{plan}.{cycle}
     * Example: com.ampairs.professional.monthly -> (PROFESSIONAL, MONTHLY)
     */
    private fun mapProductIdToPlan(productId: String): Pair<String, BillingCycle> {
        if (!productId.startsWith("com.ampairs.")) {
            throw IllegalArgumentException("Invalid Apple product ID format: $productId")
        }

        val parts = productId.removePrefix("com.ampairs.").split(".")

        if (parts.size < 2) {
            throw IllegalArgumentException("Invalid Apple product ID format: $productId")
        }

        val plan = parts[0].uppercase()
        val cycle = parts[1].uppercase()

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
     * Get Apple product ID for a plan and billing cycle
     */
    fun getProductId(planCode: String, billingCycle: BillingCycle): String {
        val plan = subscriptionPlanRepository.findByPlanCode(planCode)
            ?: throw SubscriptionException.PlanNotFoundException(planCode)

        return when (billingCycle) {
            BillingCycle.MONTHLY -> plan.appStoreProductIdMonthly
            BillingCycle.ANNUAL -> plan.appStoreProductIdAnnual
            else -> throw IllegalArgumentException("App Store only supports MONTHLY and ANNUAL cycles")
        } ?: throw IllegalStateException("App Store product ID not configured for $planCode $billingCycle")
    }

    private fun getAppleErrorMessage(status: Int): String {
        return when (status) {
            21000 -> "App Store could not read the JSON object"
            21002 -> "Receipt data was malformed"
            21003 -> "Receipt could not be authenticated"
            21004 -> "Shared secret does not match"
            21005 -> "Receipt server is not available"
            21006 -> "Receipt is valid but subscription has expired"
            21007 -> "Sandbox receipt sent to production environment"
            21008 -> "Production receipt sent to sandbox environment"
            21010 -> "Receipt could not be authorized"
            else -> "Unknown error (status: $status)"
        }
    }
}

/**
 * Apple receipt verification response
 */
data class AppleReceiptResponse(
    val status: Int,
    val latestReceiptInfo: List<AppleReceiptInfo>?,
    val pendingRenewalInfo: List<ApplePendingRenewalInfo>?
)

/**
 * Apple receipt info
 */
data class AppleReceiptInfo(
    val productId: String,
    val transactionId: String,
    val originalTransactionId: String,
    val purchaseDateMs: Long?,
    val expiresDateMs: Long?
)

/**
 * Apple pending renewal info
 */
data class ApplePendingRenewalInfo(
    val autoRenewStatus: String?
)
