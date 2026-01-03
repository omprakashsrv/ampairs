package com.ampairs.subscription.controller

import com.ampairs.subscription.domain.model.PaymentProvider
import com.ampairs.subscription.domain.model.WebhookStatus
import com.ampairs.subscription.domain.service.WebhookIdempotencyService
import com.ampairs.subscription.exception.SubscriptionException
import com.ampairs.subscription.webhook.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/webhooks")
@Tag(name = "Webhooks", description = "Payment provider webhook endpoints")
class WebhookController(
    private val googlePlayWebhookHandler: GooglePlayWebhookHandler,
    private val appStoreWebhookHandler: AppStoreWebhookHandler,
    private val razorpayWebhookHandler: RazorpayWebhookHandler,
    private val stripeWebhookHandler: StripeWebhookHandler,
    private val webhookIdempotencyService: WebhookIdempotencyService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(WebhookController::class.java)

    /**
     * Google Play Real-Time Developer Notifications with idempotency and logging
     * Configured in Google Play Console to receive Pub/Sub messages
     */
    @PostMapping("/google-play")
    @Hidden
    @Operation(summary = "Google Play RTDN webhook")
    fun handleGooglePlayWebhook(
        @RequestBody payload: String
    ): ResponseEntity<String> {
        // 1. Log webhook FIRST (before any processing)
        val webhookLog = webhookIdempotencyService.logWebhook(
            provider = PaymentProvider.GOOGLE_PLAY,
            payload = payload,
            signature = null  // Google Play uses Pub/Sub authentication
        )

        return try {
            logger.info("Received Google Play webhook: log_id={}", webhookLog.uid)

            // 2. Parse Pub/Sub message
            val message = objectMapper.readTree(payload)
            val data = message.path("message").path("data").asText()

            // Decode base64 data
            val decodedData = String(java.util.Base64.getDecoder().decode(data))
            val notification = objectMapper.readTree(decodedData)

            // 3. Extract event ID (use messageId from Pub/Sub message)
            val messageId = message.path("message").path("messageId").asText()
            val eventId = messageId.takeIf { it.isNotEmpty() }
                ?: notification.path("subscriptionNotification").path("purchaseToken").asText()

            // 4. Check idempotency (prevent duplicate processing)
            if (webhookIdempotencyService.isProcessed(PaymentProvider.GOOGLE_PLAY, eventId)) {
                logger.info("Duplicate Google Play webhook ignored: event_id={}, log_id={}", eventId, webhookLog.uid)
                webhookIdempotencyService.updateWebhookStatus(webhookLog, WebhookStatus.PROCESSED)
                return ResponseEntity.ok("OK")
            }

            // 5. Mark as processing
            webhookIdempotencyService.updateWebhookStatus(webhookLog, WebhookStatus.PROCESSING)

            // 6. Process event
            googlePlayWebhookHandler.processEvent("notification", notification)

            // 7. Extract subscription ID for tracking
            val externalSubId = notification.path("subscriptionNotification")
                .path("subscriptionId").asText().takeIf { it.isNotEmpty() }

            // 8. Mark event as processed (idempotency record)
            webhookIdempotencyService.markAsProcessed(
                provider = PaymentProvider.GOOGLE_PLAY,
                eventId = eventId,
                eventType = "notification",
                payload = decodedData,
                externalSubscriptionId = externalSubId
            )

            // 9. Mark webhook log as successful
            webhookIdempotencyService.updateWebhookStatus(webhookLog, WebhookStatus.PROCESSED)

            logger.info("Google Play webhook processed successfully: event_id={}, log_id={}", eventId, webhookLog.uid)
            ResponseEntity.ok("OK")

        } catch (e: Exception) {
            logger.error("Error processing Google Play webhook: log_id={}", webhookLog.uid, e)
            webhookIdempotencyService.updateWebhookStatus(
                webhookLog,
                WebhookStatus.FAILED,
                errorMessage = e.message
            )
            // Always return 200 to prevent retries for bad data
            ResponseEntity.ok("OK")
        }
    }

    /**
     * Apple App Store Server Notifications V2 with idempotency and logging
     */
    @PostMapping("/app-store")
    @Hidden
    @Operation(summary = "Apple App Store Server Notifications V2 webhook")
    fun handleAppStoreWebhook(
        @RequestBody payload: String
    ): ResponseEntity<String> {
        // 1. Log webhook FIRST (before any processing)
        val webhookLog = webhookIdempotencyService.logWebhook(
            provider = PaymentProvider.APP_STORE,
            payload = payload,
            signature = null  // Apple uses JWS signed payload
        )

        return try {
            logger.info("Received App Store webhook: log_id={}", webhookLog.uid)

            // 2. Parse and decode JWS
            val body = objectMapper.readTree(payload)
            val signedPayload = body.path("signedPayload").asText()

            // TODO: Verify JWS signature in production
            val decodedPayload = decodeJws(signedPayload)

            // 3. Extract event ID (use notificationUUID)
            val notificationType = decodedPayload.path("notificationType").asText()
            val eventId = decodedPayload.path("notificationUUID").asText()

            // 4. Check idempotency (prevent duplicate processing)
            if (webhookIdempotencyService.isProcessed(PaymentProvider.APP_STORE, eventId)) {
                logger.info("Duplicate App Store webhook ignored: event_id={}, log_id={}", eventId, webhookLog.uid)
                webhookIdempotencyService.updateWebhookStatus(webhookLog, WebhookStatus.PROCESSED)
                return ResponseEntity.ok("OK")
            }

            // 5. Mark as processing
            webhookIdempotencyService.updateWebhookStatus(webhookLog, WebhookStatus.PROCESSING)

            // 6. Process event
            appStoreWebhookHandler.processEvent(notificationType, decodedPayload)

            // 7. Extract subscription ID for tracking
            val transactionInfo = decodedPayload.path("data").path("signedTransactionInfo").asText()
            val externalSubId = if (transactionInfo.isNotEmpty()) {
                val decoded = decodeJws(transactionInfo)
                decoded.path("originalTransactionId").asText().takeIf { it.isNotEmpty() }
            } else null

            // 8. Mark event as processed (idempotency record)
            webhookIdempotencyService.markAsProcessed(
                provider = PaymentProvider.APP_STORE,
                eventId = eventId,
                eventType = notificationType,
                payload = decodedPayload.toString(),
                externalSubscriptionId = externalSubId
            )

            // 9. Mark webhook log as successful
            webhookIdempotencyService.updateWebhookStatus(webhookLog, WebhookStatus.PROCESSED)

            logger.info("App Store webhook processed successfully: event_id={}, log_id={}", eventId, webhookLog.uid)
            ResponseEntity.ok("OK")

        } catch (e: Exception) {
            logger.error("Error processing App Store webhook: log_id={}", webhookLog.uid, e)
            webhookIdempotencyService.updateWebhookStatus(
                webhookLog,
                WebhookStatus.FAILED,
                errorMessage = e.message
            )
            // Always return 200 to prevent retries
            ResponseEntity.ok("OK")
        }
    }

    /**
     * Razorpay webhook with idempotency and logging
     */
    @PostMapping("/razorpay")
    @Hidden
    @Operation(summary = "Razorpay webhook")
    fun handleRazorpayWebhook(
        @RequestBody payload: String,
        @RequestHeader("X-Razorpay-Signature") signature: String?
    ): ResponseEntity<String> {
        // 1. Log webhook FIRST (before any processing)
        val webhookLog = webhookIdempotencyService.logWebhook(
            provider = PaymentProvider.RAZORPAY,
            payload = payload,
            signature = signature
        )

        return try {
            logger.info("Received Razorpay webhook: log_id={}", webhookLog.uid)

            // 2. Verify signature
            if (signature != null && !razorpayWebhookHandler.verifySignature(payload, signature, null)) {
                logger.warn("Invalid Razorpay webhook signature: log_id={}", webhookLog.uid)
                webhookIdempotencyService.updateWebhookStatus(
                    webhookLog,
                    WebhookStatus.SIGNATURE_FAILED
                )
                return ResponseEntity.status(401).body("""{"error":"signature_verification_failed"}""")
            }

            // 3. Parse payload and extract event ID
            val body = objectMapper.readTree(payload)
            val eventType = body.path("event").asText()
            val eventId = body.path("id").asText()  // Razorpay event ID

            // 4. Check idempotency (prevent duplicate processing)
            if (webhookIdempotencyService.isProcessed(PaymentProvider.RAZORPAY, eventId)) {
                logger.info("Duplicate Razorpay webhook ignored: event_id={}, log_id={}", eventId, webhookLog.uid)
                webhookIdempotencyService.updateWebhookStatus(webhookLog, WebhookStatus.PROCESSED)
                return ResponseEntity.ok().body("""{"status":"ok","duplicate":true}""")
            }

            // 5. Mark as processing
            webhookIdempotencyService.updateWebhookStatus(webhookLog, WebhookStatus.PROCESSING)

            // 6. Process event
            razorpayWebhookHandler.processEvent(eventType, body)

            // 7. Extract subscription ID for tracking
            val subscription = body.path("payload").path("subscription").path("entity")
            val externalSubId = subscription.path("id").asText()

            // 8. Mark event as processed (idempotency record)
            webhookIdempotencyService.markAsProcessed(
                provider = PaymentProvider.RAZORPAY,
                eventId = eventId,
                eventType = eventType,
                payload = payload,
                externalSubscriptionId = externalSubId.takeIf { it.isNotEmpty() }
            )

            // 9. Mark webhook log as successful
            webhookIdempotencyService.updateWebhookStatus(webhookLog, WebhookStatus.PROCESSED)

            logger.info("Razorpay webhook processed successfully: event_id={}, log_id={}", eventId, webhookLog.uid)
            ResponseEntity.ok().body("""{"status":"ok"}""")

        } catch (e: SubscriptionException.WebhookVerificationFailed) {
            logger.error("Razorpay webhook verification failed: log_id={}", webhookLog.uid, e)
            webhookIdempotencyService.updateWebhookStatus(
                webhookLog,
                WebhookStatus.SIGNATURE_FAILED,
                errorMessage = e.message
            )
            ResponseEntity.status(401).body("""{"error":"signature_verification_failed"}""")
        } catch (e: Exception) {
            logger.error("Error processing Razorpay webhook: log_id={}", webhookLog.uid, e)
            webhookIdempotencyService.updateWebhookStatus(
                webhookLog,
                WebhookStatus.FAILED,
                errorMessage = e.message
            )
            // Still return 200 to prevent provider retries - use internal retry mechanism
            ResponseEntity.ok().body("""{"status":"ok"}""")
        }
    }

    /**
     * Stripe webhook with idempotency and logging
     */
    @PostMapping("/stripe")
    @Hidden
    @Operation(summary = "Stripe webhook")
    fun handleStripeWebhook(
        @RequestBody payload: String,
        @RequestHeader("Stripe-Signature") signature: String?
    ): ResponseEntity<String> {
        // 1. Log webhook FIRST (before any processing)
        val webhookLog = webhookIdempotencyService.logWebhook(
            provider = PaymentProvider.STRIPE,
            payload = payload,
            signature = signature
        )

        return try {
            logger.info("Received Stripe webhook: log_id={}", webhookLog.uid)

            // 2. Verify signature
            if (signature != null && !stripeWebhookHandler.verifySignature(payload, signature, null)) {
                logger.warn("Invalid Stripe webhook signature: log_id={}", webhookLog.uid)
                webhookIdempotencyService.updateWebhookStatus(
                    webhookLog,
                    WebhookStatus.SIGNATURE_FAILED
                )
                return ResponseEntity.status(401).body("""{"error":"signature_verification_failed"}""")
            }

            // 3. Parse payload and extract event ID
            val body = objectMapper.readTree(payload)
            val eventType = body.path("type").asText()
            val eventId = body.path("id").asText()  // Stripe event ID

            // 4. Check idempotency (prevent duplicate processing)
            if (webhookIdempotencyService.isProcessed(PaymentProvider.STRIPE, eventId)) {
                logger.info("Duplicate Stripe webhook ignored: event_id={}, log_id={}", eventId, webhookLog.uid)
                webhookIdempotencyService.updateWebhookStatus(webhookLog, WebhookStatus.PROCESSED)
                return ResponseEntity.ok().body("""{"received":true,"duplicate":true}""")
            }

            // 5. Mark as processing
            webhookIdempotencyService.updateWebhookStatus(webhookLog, WebhookStatus.PROCESSING)

            // 6. Process event
            stripeWebhookHandler.processEvent(eventType, body)

            // 7. Extract subscription ID for tracking
            val dataObject = body.path("data").path("object")
            val externalSubId = when {
                dataObject.has("subscription") -> dataObject.path("subscription").asText()
                dataObject.has("id") && eventType.contains("customer.subscription") -> dataObject.path("id").asText()
                else -> null
            }

            // 8. Mark event as processed (idempotency record)
            webhookIdempotencyService.markAsProcessed(
                provider = PaymentProvider.STRIPE,
                eventId = eventId,
                eventType = eventType,
                payload = payload,
                externalSubscriptionId = externalSubId
            )

            // 9. Mark webhook log as successful
            webhookIdempotencyService.updateWebhookStatus(webhookLog, WebhookStatus.PROCESSED)

            logger.info("Stripe webhook processed successfully: event_id={}, log_id={}", eventId, webhookLog.uid)
            ResponseEntity.ok().body("""{"received":true}""")

        } catch (e: SubscriptionException.WebhookVerificationFailed) {
            logger.error("Stripe webhook verification failed: log_id={}", webhookLog.uid, e)
            webhookIdempotencyService.updateWebhookStatus(
                webhookLog,
                WebhookStatus.SIGNATURE_FAILED,
                errorMessage = e.message
            )
            ResponseEntity.status(401).body("""{"error":"signature_verification_failed"}""")
        } catch (e: Exception) {
            logger.error("Error processing Stripe webhook: log_id={}", webhookLog.uid, e)
            webhookIdempotencyService.updateWebhookStatus(
                webhookLog,
                WebhookStatus.FAILED,
                errorMessage = e.message
            )
            // Still return 200 to prevent provider retries - use internal retry mechanism
            ResponseEntity.ok().body("""{"received":true}""")
        }
    }

    /**
     * Generic webhook endpoint for testing
     */
    @PostMapping("/test/{provider}")
    @Hidden
    fun handleTestWebhook(
        @PathVariable provider: String,
        @RequestBody payload: String
    ): ResponseEntity<String> {
        logger.info("Received test webhook for provider: {}", provider)
        logger.debug("Payload: {}", payload)
        return ResponseEntity.ok("OK")
    }

    /**
     * Decode JWS (simplified - in production use proper JWT library)
     */
    private fun decodeJws(jws: String): JsonNode {
        // JWS format: header.payload.signature
        val parts = jws.split(".")
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid JWS format")
        }

        val payloadJson = String(java.util.Base64.getUrlDecoder().decode(parts[1]))
        return objectMapper.readTree(payloadJson)
    }
}
