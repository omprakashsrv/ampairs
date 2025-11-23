package com.ampairs.subscription.controller

import com.ampairs.subscription.domain.model.PaymentProvider
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
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(WebhookController::class.java)

    /**
     * Google Play Real-Time Developer Notifications
     * Configured in Google Play Console to receive Pub/Sub messages
     */
    @PostMapping("/google-play")
    @Hidden
    @Operation(summary = "Google Play RTDN webhook")
    fun handleGooglePlayWebhook(
        @RequestBody payload: String
    ): ResponseEntity<String> {
        return try {
            logger.info("Received Google Play webhook")

            // Google Pub/Sub wraps the notification in a message
            val message = objectMapper.readTree(payload)
            val data = message.path("message").path("data").asText()

            // Decode base64 data
            val decodedData = String(java.util.Base64.getDecoder().decode(data))
            val notification = objectMapper.readTree(decodedData)

            googlePlayWebhookHandler.processEvent("notification", notification)

            ResponseEntity.ok("OK")
        } catch (e: Exception) {
            logger.error("Error processing Google Play webhook", e)
            ResponseEntity.ok("OK") // Always return 200 to prevent retries for bad data
        }
    }

    /**
     * Apple App Store Server Notifications V2
     */
    @PostMapping("/app-store")
    @Hidden
    @Operation(summary = "Apple App Store Server Notifications V2 webhook")
    fun handleAppStoreWebhook(
        @RequestBody payload: String
    ): ResponseEntity<String> {
        return try {
            logger.info("Received App Store webhook")

            val body = objectMapper.readTree(payload)
            val signedPayload = body.path("signedPayload").asText()

            // TODO: Verify and decode JWS
            // For now, just process (in production, verify signature)
            val decodedPayload = decodeJws(signedPayload)

            val notificationType = decodedPayload.path("notificationType").asText()
            appStoreWebhookHandler.processEvent(notificationType, decodedPayload)

            ResponseEntity.ok("OK")
        } catch (e: Exception) {
            logger.error("Error processing App Store webhook", e)
            ResponseEntity.ok("OK")
        }
    }

    /**
     * Razorpay webhook
     */
    @PostMapping("/razorpay")
    @Hidden
    @Operation(summary = "Razorpay webhook")
    fun handleRazorpayWebhook(
        @RequestBody payload: String,
        @RequestHeader("X-Razorpay-Signature") signature: String?
    ): ResponseEntity<String> {
        return try {
            logger.info("Received Razorpay webhook")

            // Verify signature
            if (signature != null && !razorpayWebhookHandler.verifySignature(payload, signature, null)) {
                logger.warn("Invalid Razorpay webhook signature")
                throw SubscriptionException.WebhookVerificationFailed("Razorpay")
            }

            val body = objectMapper.readTree(payload)
            val eventType = body.path("event").asText()

            razorpayWebhookHandler.processEvent(eventType, body)

            ResponseEntity.ok().body("""{"status":"ok"}""")
        } catch (e: SubscriptionException.WebhookVerificationFailed) {
            ResponseEntity.status(401).body("""{"error":"signature_verification_failed"}""")
        } catch (e: Exception) {
            logger.error("Error processing Razorpay webhook", e)
            ResponseEntity.ok().body("""{"status":"ok"}""")
        }
    }

    /**
     * Stripe webhook
     */
    @PostMapping("/stripe")
    @Hidden
    @Operation(summary = "Stripe webhook")
    fun handleStripeWebhook(
        @RequestBody payload: String,
        @RequestHeader("Stripe-Signature") signature: String?
    ): ResponseEntity<String> {
        return try {
            logger.info("Received Stripe webhook")

            // Verify signature
            if (signature != null && !stripeWebhookHandler.verifySignature(payload, signature, null)) {
                logger.warn("Invalid Stripe webhook signature")
                throw SubscriptionException.WebhookVerificationFailed("Stripe")
            }

            val body = objectMapper.readTree(payload)
            val eventType = body.path("type").asText()

            stripeWebhookHandler.processEvent(eventType, body)

            ResponseEntity.ok().body("""{"received":true}""")
        } catch (e: SubscriptionException.WebhookVerificationFailed) {
            ResponseEntity.status(401).body("""{"error":"signature_verification_failed"}""")
        } catch (e: Exception) {
            logger.error("Error processing Stripe webhook", e)
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
