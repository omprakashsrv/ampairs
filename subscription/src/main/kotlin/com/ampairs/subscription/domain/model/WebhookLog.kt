package com.ampairs.subscription.domain.model

import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.*
import java.time.Instant

/**
 * Logs all incoming webhook requests for debugging and replay.
 * Enables recovery from failed webhook processing.
 */
@Entity
@Table(
    name = "webhook_logs",
    indexes = [
        Index(name = "idx_webhook_log_uid", columnList = "uid", unique = true),
        Index(name = "idx_webhook_log_provider", columnList = "provider"),
        Index(name = "idx_webhook_log_status", columnList = "status"),
        Index(name = "idx_webhook_log_received_at", columnList = "received_at")
    ]
)
class WebhookLog : BaseDomain() {

    /**
     * Payment provider that sent the webhook
     */
    @Column(name = "provider", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var provider: PaymentProvider = PaymentProvider.MANUAL

    /**
     * Raw webhook payload
     */
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    var payload: String = ""

    /**
     * Webhook signature header
     */
    @Column(name = "signature", length = 500)
    var signature: String? = null

    /**
     * Processing status
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: WebhookStatus = WebhookStatus.RECEIVED

    /**
     * When the webhook was received
     */
    @Column(name = "received_at", nullable = false)
    var receivedAt: Instant = Instant.now()

    /**
     * When processing completed (if applicable)
     */
    @Column(name = "processed_at")
    var processedAt: Instant? = null

    /**
     * Error message if processing failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null

    /**
     * Number of retry attempts
     */
    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0

    /**
     * Next retry time (for failed webhooks)
     */
    @Column(name = "next_retry_at")
    var nextRetryAt: Instant? = null

    /**
     * HTTP headers (JSON format)
     */
    @Column(name = "headers", columnDefinition = "TEXT")
    var headers: String? = null

    override fun obtainSeqIdPrefix(): String = "WHL"
}

/**
 * Webhook processing status
 */
enum class WebhookStatus {
    RECEIVED,           // Webhook received, not yet processed
    PROCESSING,         // Currently being processed
    PROCESSED,          // Successfully processed
    SIGNATURE_FAILED,   // Signature verification failed
    FAILED,             // Processing failed
    RETRY_SCHEDULED     // Scheduled for retry
}
