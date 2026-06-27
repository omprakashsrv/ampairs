package com.ampairs.notification.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.notification.provider.NotificationChannel
import com.ampairs.notification.provider.NotificationStatus
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "notification_queue")
class NotificationQueue : OwnableBaseDomain() {

    override fun obtainSeqIdPrefix(): String = "NQ"

    @Column(name = "recipient", nullable = false, length = 255)
    var recipient: String = ""

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    var message: String = ""

    /** Optional title — used by push (FCM) rows; null for SMS. */
    @Column(name = "title", length = 255)
    var title: String? = null

    /** Optional structured data payload (JSON) delivered alongside push notifications. */
    @Column(name = "data_payload", columnDefinition = "TEXT")
    var dataPayload: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    var channel: NotificationChannel = NotificationChannel.SMS

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: NotificationStatus = NotificationStatus.PENDING

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0

    @Column(name = "max_retries", nullable = false)
    var maxRetries: Int = 3

    @Column(name = "scheduled_at", nullable = false)
    var scheduledAt: Instant = Instant.now()

    @Column(name = "last_attempt_at")
    var lastAttemptAt: Instant? = null

    @Column(name = "provider_used", length = 50)
    var providerUsed: String? = null

    @Column(name = "provider_message_id")
    var providerMessageId: String? = null

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null

    @Column(name = "provider_response", columnDefinition = "TEXT")
    var providerResponse: String? = null

    /** EMAIL subject line (null for SMS/push). */
    @Column(name = "subject", length = 500)
    var subject: String? = null

    /** Originating module for cross-module correlation (e.g. "communication"). */
    @Column(name = "source_module", length = 50)
    var sourceModule: String? = null

    /** Correlation id in the source module (e.g. communication_log.uid) for delivery-status feedback. */
    @Column(name = "source_ref", length = 200)
    var sourceRef: String? = null

    /** Workspace provider credential resolved for this send (null = platform/shared). */
    @Column(name = "credential_uid", length = 200)
    var credentialUid: String? = null

    /** Billing attribution: CLIENT_OWN (client's own credential) vs PLATFORM (billable to client). */
    @Column(name = "billing_mode", length = 20)
    var billingMode: String? = null

    fun isReadyForRetry(): Boolean {
        return status == NotificationStatus.FAILED &&
                retryCount < maxRetries &&
                scheduledAt.isBefore(Instant.now())
    }

    fun canRetry(): Boolean {
        return retryCount < maxRetries
    }

    fun markForRetry(delayMinutes: Long = 5) {
        this.retryCount++
        this.status = NotificationStatus.RETRYING
        this.scheduledAt = Instant.now().plusSeconds(delayMinutes * 60)
    }

    fun markAsExhausted() {
        this.status = NotificationStatus.EXHAUSTED
    }

    fun markAsSent(providerName: String, messageId: String?, response: String?) {
        this.status = NotificationStatus.SENT
        this.providerUsed = providerName
        this.providerMessageId = messageId
        this.providerResponse = response
        this.lastAttemptAt = Instant.now()
    }

    fun markAsFailed(providerName: String, error: String?, response: String?) {
        this.status = NotificationStatus.FAILED
        this.providerUsed = providerName
        this.errorMessage = error
        this.providerResponse = response
        this.lastAttemptAt = Instant.now()
    }
}

typealias SmsQueue = NotificationQueue
