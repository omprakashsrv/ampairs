package com.ampairs.notification.controller

import com.ampairs.notification.event.NotificationDeliveryUpdatedEvent
import com.ampairs.notification.repository.NotificationQueueRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.web.bind.annotation.*

/**
 * Provider delivery webhooks (SES/SNS bounce+complaint, WhatsApp status callbacks). Verified by the
 * provider's own signature, not X-Workspace-ID (see the plan's Complexity Tracking exception). The
 * payload is correlated to the original send via `provider_message_id`, normalized to a
 * [NotificationDeliveryUpdatedEvent], and republished — the source module (communication) updates its
 * log and, on a hard bounce/complaint, suppresses the address (FR-010/FR-031).
 *
 * Returns a provider-shaped ack (not ApiResponse) — providers reject unexpected bodies.
 *
 * NOTE: must be allow-listed in the security config (unauthenticated provider callback).
 */
@RestController
@RequestMapping("/notification/v1/webhooks")
class NotificationWebhookController(
    private val queueRepository: NotificationQueueRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val logger = LoggerFactory.getLogger(NotificationWebhookController::class.java)

    /**
     * Normalized webhook body: `{ provider_message_id, status, error?, reason? }`. `status` ∈
     * DELIVERED | READ | FAILED | BOUNCED | COMPLAINT. Real provider adapters translate their native
     * payloads into this shape (kept minimal here).
     */
    @PostMapping("/{provider}")
    fun handle(@PathVariable provider: String, @RequestBody body: Map<String, Any?>): Map<String, Any> {
        val providerMessageId = body["provider_message_id"]?.toString()
        val rawStatus = body["status"]?.toString()?.uppercase()
        if (providerMessageId.isNullOrBlank() || rawStatus == null) {
            return mapOf("ok" to false, "reason" to "missing provider_message_id/status")
        }
        val queue = queueRepository.findFirstByProviderMessageId(providerMessageId)
        if (queue?.sourceModule == null || queue.sourceRef == null) {
            logger.debug("Webhook {} for {} not correlated to a source send", provider, providerMessageId)
            return mapOf("ok" to true)
        }

        val isBounce = rawStatus == "BOUNCED" || rawStatus == "COMPLAINT"
        val status = if (isBounce) "FAILED" else rawStatus
        eventPublisher.publishEvent(
            NotificationDeliveryUpdatedEvent(
                sourceModule = queue.sourceModule!!,
                sourceRef = queue.sourceRef!!,
                status = status,
                providerMessageId = providerMessageId,
                error = body["error"]?.toString() ?: if (isBounce) rawStatus else null,
                credentialUid = queue.credentialUid,
                billingMode = queue.billingMode,
                suppress = isBounce,
                suppressionReason = if (rawStatus == "COMPLAINT") "COMPLAINT" else if (isBounce) "HARD_BOUNCE" else null,
            )
        )
        return mapOf("ok" to true)
    }
}
