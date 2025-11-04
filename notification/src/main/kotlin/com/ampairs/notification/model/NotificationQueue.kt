package com.ampairs.notification.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.notification.provider.NotificationChannel
import com.ampairs.notification.provider.NotificationStatus
import jakarta.persistence.*

/**
 * Notification Queue entity for storing notification requests for async processing
 */
@Entity
@Table(name = "notification_queue")
class NotificationQueue : OwnableBaseDomain() {

    override fun obtainSeqIdPrefix(): String = "NQ"

    @Column(name = "recipient", nullable = false, length = 255)
    var recipient: String = ""

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    var message: String = ""

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
    var scheduledAt: java.time.LocalDateTime = java.time.LocalDateTime.now()

    @Column(name = "last_attempt_at")
    var lastAttemptAt: java.time.LocalDateTime? = null

    @Column(name = "provider_used", length = 50)
    var providerUsed: String? = null

    @Column(name = "provider_message_id")
    var providerMessageId: String? = null

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null

    @Column(name = "provider_response", columnDefinition = "TEXT")
    var providerResponse: String? = null

    fun isReadyForRetry(): Boolean {
        return status == NotificationStatus.FAILED &&
                retryCount < maxRetries &&
                scheduledAt.isBefore(java.time.LocalDateTime.now())
    }

    fun canRetry(): Boolean {
        return retryCount < maxRetries
    }

    fun markForRetry(delayMinutes: Long = 5) {
        this.retryCount++
        this.status = NotificationStatus.RETRYING
        this.scheduledAt = java.time.LocalDateTime.now().plusMinutes(delayMinutes)
    }

    fun markAsExhausted() {
        this.status = NotificationStatus.EXHAUSTED
    }

    fun markAsSent(providerName: String, messageId: String?, response: String?) {
        this.status = NotificationStatus.SENT
        this.providerUsed = providerName
        this.providerMessageId = messageId
        this.providerResponse = response
        this.lastAttemptAt = java.time.LocalDateTime.now()
    }

    fun markAsFailed(providerName: String, error: String?, response: String?) {
        this.status = NotificationStatus.FAILED
        this.providerUsed = providerName
        this.errorMessage = error
        this.providerResponse = response
        this.lastAttemptAt = java.time.LocalDateTime.now()
    }
}

/**
 * Legacy SMS Queue alias for backward compatibility
 */
typealias SmsQueue = NotificationQueue