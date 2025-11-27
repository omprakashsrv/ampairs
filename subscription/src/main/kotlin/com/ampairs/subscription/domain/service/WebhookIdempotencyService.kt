package com.ampairs.subscription.domain.service

import com.ampairs.subscription.domain.model.PaymentProvider
import com.ampairs.subscription.domain.model.WebhookEvent
import com.ampairs.subscription.domain.model.WebhookLog
import com.ampairs.subscription.domain.model.WebhookStatus
import com.ampairs.subscription.domain.repository.WebhookEventRepository
import com.ampairs.subscription.domain.repository.WebhookLogRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Service for webhook idempotency and logging
 */
@Service
@Transactional
class WebhookIdempotencyService(
    private val webhookEventRepository: WebhookEventRepository,
    private val webhookLogRepository: WebhookLogRepository
) {
    private val logger = LoggerFactory.getLogger(WebhookIdempotencyService::class.java)

    /**
     * Check if webhook event has already been processed
     */
    fun isProcessed(provider: PaymentProvider, eventId: String): Boolean {
        return webhookEventRepository.existsByProviderAndEventId(provider, eventId)
    }

    /**
     * Mark webhook event as processed (idempotency)
     */
    fun markAsProcessed(
        provider: PaymentProvider,
        eventId: String,
        eventType: String,
        payload: String? = null,
        externalSubscriptionId: String? = null,
        workspaceId: String? = null
    ): WebhookEvent {
        // Check if already exists
        val existing = webhookEventRepository.findByProviderAndEventId(provider, eventId)
        if (existing != null) {
            logger.debug("Webhook event already processed: provider={}, eventId={}", provider, eventId)
            return existing
        }

        // Create new record
        val webhookEvent = WebhookEvent().apply {
            this.provider = provider
            this.eventId = eventId
            this.eventType = eventType
            this.payload = payload
            this.processedAt = Instant.now()
            this.externalSubscriptionId = externalSubscriptionId
            this.workspaceId = workspaceId
        }

        return webhookEventRepository.save(webhookEvent)
    }

    /**
     * Log incoming webhook
     */
    fun logWebhook(
        provider: PaymentProvider,
        payload: String,
        signature: String?,
        headers: String? = null
    ): WebhookLog {
        val webhookLog = WebhookLog().apply {
            this.provider = provider
            this.payload = payload
            this.signature = signature
            this.status = WebhookStatus.RECEIVED
            this.receivedAt = Instant.now()
            this.headers = headers
        }

        return webhookLogRepository.save(webhookLog)
    }

    /**
     * Update webhook log status
     */
    fun updateWebhookStatus(
        webhookLog: WebhookLog,
        status: WebhookStatus,
        errorMessage: String? = null
    ): WebhookLog {
        webhookLog.status = status
        webhookLog.errorMessage = errorMessage

        when (status) {
            WebhookStatus.PROCESSED -> {
                webhookLog.processedAt = Instant.now()
            }
            WebhookStatus.FAILED, WebhookStatus.RETRY_SCHEDULED -> {
                webhookLog.retryCount++
                webhookLog.nextRetryAt = calculateNextRetry(webhookLog.retryCount)
            }
            else -> {}
        }

        return webhookLogRepository.save(webhookLog)
    }

    /**
     * Calculate exponential backoff for retry
     */
    private fun calculateNextRetry(retryCount: Int): Instant {
        // Exponential backoff: 1min, 5min, 30min, 2h, 12h
        val delayMinutes = when (retryCount) {
            1 -> 1L
            2 -> 5L
            3 -> 30L
            4 -> 120L
            else -> 720L
        }
        return Instant.now().plus(delayMinutes, ChronoUnit.MINUTES)
    }

    /**
     * Get failed webhooks ready for retry
     */
    fun getFailedWebhooksForRetry(maxRetries: Int = 5): List<WebhookLog> {
        return webhookLogRepository.findFailedWebhooksForRetry(maxRetries, Instant.now())
    }

    /**
     * Cleanup old webhook records (run periodically)
     */
    fun cleanupOldWebhooks(retentionDays: Int = 30) {
        val cutoffDate = Instant.now().minus(retentionDays.toLong(), ChronoUnit.DAYS)

        val eventsDeleted = webhookEventRepository.deleteByProcessedAtBefore(cutoffDate)
        val logsDeleted = webhookLogRepository.deleteByReceivedAtBefore(cutoffDate)

        logger.info("Cleaned up old webhooks: {} events, {} logs", eventsDeleted, logsDeleted)
    }

    /**
     * Get webhook statistics
     */
    fun getWebhookStats(): Map<String, Any> {
        return mapOf(
            "total_received" to webhookLogRepository.count(),
            "processed" to webhookLogRepository.countByStatus(WebhookStatus.PROCESSED),
            "failed" to webhookLogRepository.countByStatus(WebhookStatus.FAILED),
            "pending_retry" to webhookLogRepository.countByStatus(WebhookStatus.RETRY_SCHEDULED),
            "signature_failed" to webhookLogRepository.countByStatus(WebhookStatus.SIGNATURE_FAILED)
        )
    }
}
