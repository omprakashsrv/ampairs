package com.ampairs.subscription.domain.model

import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.*
import java.time.Instant

/**
 * Tracks processed webhook events to ensure idempotency.
 * Prevents duplicate processing of the same webhook event.
 */
@Entity
@Table(
    name = "webhook_events",
    indexes = [
        Index(name = "idx_webhook_event_uid", columnList = "uid", unique = true),
        Index(name = "idx_webhook_provider_event_id", columnList = "provider,event_id", unique = true),
        Index(name = "idx_webhook_processed_at", columnList = "processed_at")
    ]
)
class WebhookEvent : BaseDomain() {

    /**
     * Payment provider that sent the webhook
     */
    @Column(name = "provider", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var provider: PaymentProvider = PaymentProvider.MANUAL

    /**
     * Unique event ID from payment provider
     */
    @Column(name = "event_id", nullable = false, length = 255)
    var eventId: String = ""

    /**
     * Type of event (e.g., subscription.charged, customer.subscription.updated)
     */
    @Column(name = "event_type", nullable = false, length = 100)
    var eventType: String = ""

    /**
     * Raw webhook payload (for debugging and replay)
     */
    @Column(name = "payload", columnDefinition = "TEXT")
    var payload: String? = null

    /**
     * When the webhook was processed
     */
    @Column(name = "processed_at", nullable = false)
    var processedAt: Instant = Instant.now()

    /**
     * External subscription ID if available
     */
    @Column(name = "external_subscription_id", length = 255)
    var externalSubscriptionId: String? = null

    /**
     * Workspace ID if identified
     */
    @Column(name = "workspace_id", length = 200)
    var workspaceId: String? = null

    override fun obtainSeqIdPrefix(): String = "WHE"
}
