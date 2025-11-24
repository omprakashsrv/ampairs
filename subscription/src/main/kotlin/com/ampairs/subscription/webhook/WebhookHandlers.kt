package com.ampairs.subscription.webhook

import com.ampairs.subscription.domain.model.PaymentProvider
import com.ampairs.subscription.domain.service.PaymentOrchestrationService
import com.ampairs.subscription.exception.SubscriptionException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Base webhook handler interface
 */
interface WebhookHandler {
    val provider: PaymentProvider

    /**
     * Verify webhook signature
     */
    fun verifySignature(payload: String, signature: String, timestamp: String?): Boolean

    /**
     * Process webhook event
     */
    fun processEvent(eventType: String, payload: JsonNode)
}

/**
 * Google Play Real-Time Developer Notifications handler
 */
@Component
class GooglePlayWebhookHandler(
    private val paymentOrchestrationService: PaymentOrchestrationService,
    private val objectMapper: ObjectMapper
) : WebhookHandler {
    private val logger = LoggerFactory.getLogger(GooglePlayWebhookHandler::class.java)

    override val provider = PaymentProvider.GOOGLE_PLAY

    // Google uses Cloud Pub/Sub, signature verification is handled at the Pub/Sub level
    override fun verifySignature(payload: String, signature: String, timestamp: String?): Boolean {
        // Pub/Sub handles authentication
        return true
    }

    override fun processEvent(eventType: String, payload: JsonNode) {
        val subscriptionNotification = payload.path("subscriptionNotification")
        if (subscriptionNotification.isMissingNode) {
            logger.debug("Ignoring non-subscription Google Play notification")
            return
        }

        val notificationType = subscriptionNotification.path("notificationType").asInt()
        val purchaseToken = subscriptionNotification.path("purchaseToken").asText()
        val subscriptionId = subscriptionNotification.path("subscriptionId").asText()

        logger.info("Processing Google Play notification type {} for subscription {}", notificationType, subscriptionId)

        when (notificationType) {
            // SUBSCRIPTION_RECOVERED (1) - Recovered from account hold
            1 -> handleRecovery(subscriptionId, purchaseToken)

            // SUBSCRIPTION_RENEWED (2) - Active subscription renewed
            2 -> handleRenewal(subscriptionId, purchaseToken)

            // SUBSCRIPTION_CANCELED (3) - Subscription cancelled (voluntary or involuntary)
            3 -> handleCancellation(subscriptionId, purchaseToken, immediate = false)

            // SUBSCRIPTION_PURCHASED (4) - New subscription purchased
            4 -> handleNewPurchase(subscriptionId, purchaseToken)

            // SUBSCRIPTION_ON_HOLD (5) - Subscription entered account hold
            5 -> handleAccountHold(subscriptionId, purchaseToken)

            // SUBSCRIPTION_IN_GRACE_PERIOD (6) - Grace period started
            6 -> handleGracePeriod(subscriptionId, purchaseToken)

            // SUBSCRIPTION_RESTARTED (7) - User restarted subscription
            7 -> handleRestart(subscriptionId, purchaseToken)

            // SUBSCRIPTION_PRICE_CHANGE_CONFIRMED (8) - User confirmed price change
            8 -> logger.info("Price change confirmed for subscription: {}", subscriptionId)

            // SUBSCRIPTION_DEFERRED (9) - Subscription deferred
            9 -> logger.info("Subscription deferred: {}", subscriptionId)

            // SUBSCRIPTION_PAUSED (10) - Subscription paused
            10 -> handlePause(subscriptionId, purchaseToken)

            // SUBSCRIPTION_PAUSE_SCHEDULE_CHANGED (11) - Pause schedule changed
            11 -> logger.info("Pause schedule changed for subscription: {}", subscriptionId)

            // SUBSCRIPTION_REVOKED (12) - User revoked subscription
            12 -> handleCancellation(subscriptionId, purchaseToken, immediate = true)

            // SUBSCRIPTION_EXPIRED (13) - Subscription expired
            13 -> handleExpiry(subscriptionId, purchaseToken)

            else -> logger.warn("Unknown Google Play notification type: {}", notificationType)
        }
    }

    private fun handleNewPurchase(subscriptionId: String, purchaseToken: String) {
        logger.info("New subscription purchase: {} (will be verified via app)", subscriptionId)
    }

    private fun handleRenewal(subscriptionId: String, purchaseToken: String) {
        paymentOrchestrationService.handleRenewal(
            provider = PaymentProvider.GOOGLE_PLAY,
            externalSubscriptionId = subscriptionId,
            orderId = purchaseToken,
            amount = BigDecimal.ZERO, // Will be fetched from Google Play API
            currency = "USD",
            periodStart = Instant.now(),
            periodEnd = null
        )
    }

    private fun handleCancellation(subscriptionId: String, purchaseToken: String, immediate: Boolean) {
        paymentOrchestrationService.handleCancellation(
            provider = PaymentProvider.GOOGLE_PLAY,
            externalSubscriptionId = subscriptionId,
            immediate = immediate
        )
    }

    private fun handleRecovery(subscriptionId: String, purchaseToken: String) {
        logger.info("Subscription recovered from account hold: {}", subscriptionId)
    }

    private fun handleAccountHold(subscriptionId: String, purchaseToken: String) {
        paymentOrchestrationService.handlePaymentFailure(
            provider = PaymentProvider.GOOGLE_PLAY,
            externalSubscriptionId = subscriptionId,
            failureCode = "ACCOUNT_HOLD",
            failureReason = "Subscription entered account hold"
        )
    }

    private fun handleGracePeriod(subscriptionId: String, purchaseToken: String) {
        paymentOrchestrationService.handlePaymentFailure(
            provider = PaymentProvider.GOOGLE_PLAY,
            externalSubscriptionId = subscriptionId,
            failureCode = "GRACE_PERIOD",
            failureReason = "Payment failed, in grace period"
        )
    }

    private fun handleRestart(subscriptionId: String, purchaseToken: String) {
        logger.info("Subscription restarted: {}", subscriptionId)
    }

    private fun handlePause(subscriptionId: String, purchaseToken: String) {
        logger.info("Subscription paused: {}", subscriptionId)
    }

    private fun handleExpiry(subscriptionId: String, purchaseToken: String) {
        paymentOrchestrationService.handleCancellation(
            provider = PaymentProvider.GOOGLE_PLAY,
            externalSubscriptionId = subscriptionId,
            immediate = true
        )
    }
}

/**
 * Apple App Store Server Notifications V2 handler
 */
@Component
class AppStoreWebhookHandler(
    private val paymentOrchestrationService: PaymentOrchestrationService,
    private val objectMapper: ObjectMapper
) : WebhookHandler {
    private val logger = LoggerFactory.getLogger(AppStoreWebhookHandler::class.java)

    override val provider = PaymentProvider.APP_STORE

    override fun verifySignature(payload: String, signature: String, timestamp: String?): Boolean {
        // TODO: Implement JWS signature verification for App Store Server Notifications V2
        // The signedPayload is a JWS (JSON Web Signature) that needs to be verified
        // using Apple's root certificates
        return true
    }

    override fun processEvent(eventType: String, payload: JsonNode) {
        val notificationType = payload.path("notificationType").asText()
        val subtype = payload.path("subtype").asText("")
        val data = payload.path("data")
        val transactionInfo = data.path("signedTransactionInfo")
        val renewalInfo = data.path("signedRenewalInfo")

        logger.info("Processing App Store notification: {} (subtype: {})", notificationType, subtype)

        when (notificationType) {
            "SUBSCRIBED" -> handleSubscribed(subtype, transactionInfo)
            "DID_RENEW" -> handleRenewal(transactionInfo)
            "DID_CHANGE_RENEWAL_PREF" -> handleRenewalPrefChange(renewalInfo)
            "DID_CHANGE_RENEWAL_STATUS" -> handleRenewalStatusChange(subtype, renewalInfo)
            "DID_FAIL_TO_RENEW" -> handleRenewalFailure(subtype, transactionInfo)
            "EXPIRED" -> handleExpiry(subtype, transactionInfo)
            "GRACE_PERIOD_EXPIRED" -> handleGracePeriodExpired(transactionInfo)
            "REFUND" -> handleRefund(transactionInfo)
            "REVOKE" -> handleRevoke(transactionInfo)
            "CONSUMPTION_REQUEST" -> logger.info("Consumption request received")
            else -> logger.warn("Unknown App Store notification type: {}", notificationType)
        }
    }

    private fun handleSubscribed(subtype: String, transactionInfo: JsonNode) {
        val originalTransactionId = transactionInfo.path("originalTransactionId").asText()
        logger.info("New subscription: {} (subtype: {})", originalTransactionId, subtype)
    }

    private fun handleRenewal(transactionInfo: JsonNode) {
        val originalTransactionId = transactionInfo.path("originalTransactionId").asText()
        paymentOrchestrationService.handleRenewal(
            provider = PaymentProvider.APP_STORE,
            externalSubscriptionId = originalTransactionId,
            orderId = transactionInfo.path("transactionId").asText(),
            amount = BigDecimal.ZERO,
            currency = "USD",
            periodStart = null,
            periodEnd = null
        )
    }

    private fun handleRenewalPrefChange(renewalInfo: JsonNode) {
        logger.info("Renewal preference changed")
    }

    private fun handleRenewalStatusChange(subtype: String, renewalInfo: JsonNode) {
        val originalTransactionId = renewalInfo.path("originalTransactionId").asText()
        if (subtype == "AUTO_RENEW_DISABLED") {
            paymentOrchestrationService.handleCancellation(
                provider = PaymentProvider.APP_STORE,
                externalSubscriptionId = originalTransactionId,
                immediate = false
            )
        }
    }

    private fun handleRenewalFailure(subtype: String, transactionInfo: JsonNode) {
        val originalTransactionId = transactionInfo.path("originalTransactionId").asText()
        paymentOrchestrationService.handlePaymentFailure(
            provider = PaymentProvider.APP_STORE,
            externalSubscriptionId = originalTransactionId,
            failureCode = subtype,
            failureReason = "Renewal failed: $subtype"
        )
    }

    private fun handleExpiry(subtype: String, transactionInfo: JsonNode) {
        val originalTransactionId = transactionInfo.path("originalTransactionId").asText()
        paymentOrchestrationService.handleCancellation(
            provider = PaymentProvider.APP_STORE,
            externalSubscriptionId = originalTransactionId,
            immediate = true
        )
    }

    private fun handleGracePeriodExpired(transactionInfo: JsonNode) {
        val originalTransactionId = transactionInfo.path("originalTransactionId").asText()
        paymentOrchestrationService.handleCancellation(
            provider = PaymentProvider.APP_STORE,
            externalSubscriptionId = originalTransactionId,
            immediate = true
        )
    }

    private fun handleRefund(transactionInfo: JsonNode) {
        logger.info("Refund processed")
    }

    private fun handleRevoke(transactionInfo: JsonNode) {
        val originalTransactionId = transactionInfo.path("originalTransactionId").asText()
        paymentOrchestrationService.handleCancellation(
            provider = PaymentProvider.APP_STORE,
            externalSubscriptionId = originalTransactionId,
            immediate = true
        )
    }
}

/**
 * Razorpay webhook handler
 */
@Component
class RazorpayWebhookHandler(
    private val paymentOrchestrationService: PaymentOrchestrationService,
    private val objectMapper: ObjectMapper
) : WebhookHandler {
    private val logger = LoggerFactory.getLogger(RazorpayWebhookHandler::class.java)

    // TODO: Inject from configuration
    private var webhookSecret: String = ""

    override val provider = PaymentProvider.RAZORPAY

    override fun verifySignature(payload: String, signature: String, timestamp: String?): Boolean {
        if (webhookSecret.isEmpty()) {
            logger.warn("Razorpay webhook secret not configured")
            return true // Skip verification in dev
        }

        return try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(webhookSecret.toByteArray(), "HmacSHA256"))
            val expectedSignature = mac.doFinal(payload.toByteArray())
                .joinToString("") { "%02x".format(it) }
            signature == expectedSignature
        } catch (e: Exception) {
            logger.error("Failed to verify Razorpay signature", e)
            false
        }
    }

    override fun processEvent(eventType: String, payload: JsonNode) {
        logger.info("Processing Razorpay event: {}", eventType)

        when (eventType) {
            "subscription.charged" -> handleSubscriptionCharged(payload)
            "subscription.activated" -> handleSubscriptionActivated(payload)
            "subscription.pending" -> handleSubscriptionPending(payload)
            "subscription.halted" -> handleSubscriptionHalted(payload)
            "subscription.cancelled" -> handleSubscriptionCancelled(payload)
            "subscription.paused" -> handleSubscriptionPaused(payload)
            "subscription.resumed" -> handleSubscriptionResumed(payload)
            "subscription.completed" -> handleSubscriptionCompleted(payload)
            "payment.captured" -> handlePaymentCaptured(payload)
            "payment.failed" -> handlePaymentFailed(payload)
            "refund.created" -> handleRefundCreated(payload)
            else -> logger.warn("Unknown Razorpay event type: {}", eventType)
        }
    }

    private fun handleSubscriptionCharged(payload: JsonNode) {
        val subscription = payload.path("payload").path("subscription").path("entity")
        val subscriptionId = subscription.path("id").asText()
        val payment = payload.path("payload").path("payment").path("entity")
        val amountPaise = payment.path("amount").asLong()
        val amount = BigDecimal(amountPaise).divide(BigDecimal(100)) // Razorpay amounts are in paise
        val currency = payment.path("currency").asText("INR")

        paymentOrchestrationService.handleRenewal(
            provider = PaymentProvider.RAZORPAY,
            externalSubscriptionId = subscriptionId,
            orderId = payment.path("id").asText(),
            amount = amount,
            currency = currency,
            periodStart = null,
            periodEnd = null
        )
    }

    private fun handleSubscriptionActivated(payload: JsonNode) {
        val subscription = payload.path("payload").path("subscription").path("entity")
        logger.info("Subscription activated: {}", subscription.path("id").asText())
    }

    private fun handleSubscriptionPending(payload: JsonNode) {
        val subscription = payload.path("payload").path("subscription").path("entity")
        logger.info("Subscription pending: {}", subscription.path("id").asText())
    }

    private fun handleSubscriptionHalted(payload: JsonNode) {
        val subscription = payload.path("payload").path("subscription").path("entity")
        val subscriptionId = subscription.path("id").asText()

        paymentOrchestrationService.handlePaymentFailure(
            provider = PaymentProvider.RAZORPAY,
            externalSubscriptionId = subscriptionId,
            failureCode = "HALTED",
            failureReason = "Subscription halted due to payment failure"
        )
    }

    private fun handleSubscriptionCancelled(payload: JsonNode) {
        val subscription = payload.path("payload").path("subscription").path("entity")
        val subscriptionId = subscription.path("id").asText()

        paymentOrchestrationService.handleCancellation(
            provider = PaymentProvider.RAZORPAY,
            externalSubscriptionId = subscriptionId,
            immediate = true
        )
    }

    private fun handleSubscriptionPaused(payload: JsonNode) {
        logger.info("Subscription paused")
    }

    private fun handleSubscriptionResumed(payload: JsonNode) {
        logger.info("Subscription resumed")
    }

    private fun handleSubscriptionCompleted(payload: JsonNode) {
        val subscription = payload.path("payload").path("subscription").path("entity")
        val subscriptionId = subscription.path("id").asText()

        paymentOrchestrationService.handleCancellation(
            provider = PaymentProvider.RAZORPAY,
            externalSubscriptionId = subscriptionId,
            immediate = true
        )
    }

    private fun handlePaymentCaptured(payload: JsonNode) {
        logger.info("Payment captured")
    }

    private fun handlePaymentFailed(payload: JsonNode) {
        val payment = payload.path("payload").path("payment").path("entity")
        val errorCode = payment.path("error_code").asText()
        val errorDescription = payment.path("error_description").asText()

        logger.warn("Payment failed: {} - {}", errorCode, errorDescription)
    }

    private fun handleRefundCreated(payload: JsonNode) {
        logger.info("Refund created")
    }

    fun setWebhookSecret(secret: String) {
        this.webhookSecret = secret
    }
}

/**
 * Stripe webhook handler
 */
@Component
class StripeWebhookHandler(
    private val paymentOrchestrationService: PaymentOrchestrationService,
    private val objectMapper: ObjectMapper
) : WebhookHandler {
    private val logger = LoggerFactory.getLogger(StripeWebhookHandler::class.java)

    // TODO: Inject from configuration
    private var webhookSecret: String = ""

    override val provider = PaymentProvider.STRIPE

    override fun verifySignature(payload: String, signature: String, timestamp: String?): Boolean {
        if (webhookSecret.isEmpty()) {
            logger.warn("Stripe webhook secret not configured")
            return true // Skip verification in dev
        }

        // Stripe uses t=timestamp,v1=signature format
        val parts = signature.split(",").associate {
            val (key, value) = it.split("=", limit = 2)
            key to value
        }

        val stripeTimestamp = parts["t"] ?: return false
        val stripeSignature = parts["v1"] ?: return false

        return try {
            val signedPayload = "$stripeTimestamp.$payload"
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(webhookSecret.toByteArray(), "HmacSHA256"))
            val expectedSignature = mac.doFinal(signedPayload.toByteArray())
                .joinToString("") { "%02x".format(it) }
            stripeSignature == expectedSignature
        } catch (e: Exception) {
            logger.error("Failed to verify Stripe signature", e)
            false
        }
    }

    override fun processEvent(eventType: String, payload: JsonNode) {
        val dataObject = payload.path("data").path("object")

        logger.info("Processing Stripe event: {}", eventType)

        when (eventType) {
            "customer.subscription.created" -> handleSubscriptionCreated(dataObject)
            "customer.subscription.updated" -> handleSubscriptionUpdated(dataObject)
            "customer.subscription.deleted" -> handleSubscriptionDeleted(dataObject)
            "customer.subscription.trial_will_end" -> handleTrialWillEnd(dataObject)
            "invoice.paid" -> handleInvoicePaid(dataObject)
            "invoice.payment_failed" -> handleInvoicePaymentFailed(dataObject)
            "invoice.finalized" -> handleInvoiceFinalized(dataObject)
            "payment_intent.succeeded" -> handlePaymentSucceeded(dataObject)
            "payment_intent.payment_failed" -> handlePaymentFailed(dataObject)
            "customer.created" -> handleCustomerCreated(dataObject)
            else -> logger.debug("Unhandled Stripe event type: {}", eventType)
        }
    }

    private fun handleSubscriptionCreated(subscription: JsonNode) {
        logger.info("Subscription created: {}", subscription.path("id").asText())
    }

    private fun handleSubscriptionUpdated(subscription: JsonNode) {
        val subscriptionId = subscription.path("id").asText()
        val status = subscription.path("status").asText()

        logger.info("Subscription {} updated to status: {}", subscriptionId, status)

        when (status) {
            "active" -> { /* Already handled by invoice.paid */ }
            "past_due" -> {
                paymentOrchestrationService.handlePaymentFailure(
                    provider = PaymentProvider.STRIPE,
                    externalSubscriptionId = subscriptionId,
                    failureCode = "PAST_DUE",
                    failureReason = "Payment past due"
                )
            }
            "canceled", "unpaid" -> {
                paymentOrchestrationService.handleCancellation(
                    provider = PaymentProvider.STRIPE,
                    externalSubscriptionId = subscriptionId,
                    immediate = true
                )
            }
        }
    }

    private fun handleSubscriptionDeleted(subscription: JsonNode) {
        val subscriptionId = subscription.path("id").asText()
        paymentOrchestrationService.handleCancellation(
            provider = PaymentProvider.STRIPE,
            externalSubscriptionId = subscriptionId,
            immediate = true
        )
    }

    private fun handleTrialWillEnd(subscription: JsonNode) {
        logger.info("Trial will end for subscription: {}", subscription.path("id").asText())
    }

    private fun handleInvoicePaid(invoice: JsonNode) {
        val subscriptionId = invoice.path("subscription").asText()
        if (subscriptionId.isEmpty()) return

        val amountCents = invoice.path("amount_paid").asLong()
        val amount = BigDecimal(amountCents).divide(BigDecimal(100))
        val currency = invoice.path("currency").asText("usd").uppercase()
        val periodStart = invoice.path("period_start").asLong()
        val periodEnd = invoice.path("period_end").asLong()

        paymentOrchestrationService.handleRenewal(
            provider = PaymentProvider.STRIPE,
            externalSubscriptionId = subscriptionId,
            orderId = invoice.path("id").asText(),
            amount = amount,
            currency = currency,
            periodStart = Instant.ofEpochSecond(periodStart),
            periodEnd = Instant.ofEpochSecond(periodEnd)
        )
    }

    private fun handleInvoicePaymentFailed(invoice: JsonNode) {
        val subscriptionId = invoice.path("subscription").asText()
        if (subscriptionId.isEmpty()) return

        paymentOrchestrationService.handlePaymentFailure(
            provider = PaymentProvider.STRIPE,
            externalSubscriptionId = subscriptionId,
            failureCode = "INVOICE_FAILED",
            failureReason = "Invoice payment failed"
        )
    }

    private fun handleInvoiceFinalized(invoice: JsonNode) {
        logger.info("Invoice finalized: {}", invoice.path("id").asText())
    }

    private fun handlePaymentSucceeded(paymentIntent: JsonNode) {
        logger.info("Payment succeeded: {}", paymentIntent.path("id").asText())
    }

    private fun handlePaymentFailed(paymentIntent: JsonNode) {
        val error = paymentIntent.path("last_payment_error")
        logger.warn("Payment failed: {} - {}",
            error.path("code").asText(),
            error.path("message").asText()
        )
    }

    private fun handleCustomerCreated(customer: JsonNode) {
        logger.info("Customer created: {}", customer.path("id").asText())
    }

    fun setWebhookSecret(secret: String) {
        this.webhookSecret = secret
    }
}
