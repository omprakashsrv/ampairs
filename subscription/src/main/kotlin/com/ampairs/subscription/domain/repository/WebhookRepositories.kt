package com.ampairs.subscription.domain.repository

import com.ampairs.subscription.domain.model.PaymentProvider
import com.ampairs.subscription.domain.model.WebhookEvent
import com.ampairs.subscription.domain.model.WebhookLog
import com.ampairs.subscription.domain.model.WebhookStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Repository for webhook event deduplication
 */
@Repository
interface WebhookEventRepository : JpaRepository<WebhookEvent, Long> {

    /**
     * Check if webhook event already processed (for idempotency)
     */
    fun existsByProviderAndEventId(provider: PaymentProvider, eventId: String): Boolean

    /**
     * Find webhook event by provider and event ID
     */
    fun findByProviderAndEventId(provider: PaymentProvider, eventId: String): WebhookEvent?

    /**
     * Find all webhook events for a subscription
     */
    fun findByExternalSubscriptionIdOrderByProcessedAtDesc(externalSubscriptionId: String): List<WebhookEvent>

    /**
     * Find recent webhook events by provider
     */
    @Query("SELECT w FROM WebhookEvent w WHERE w.provider = :provider AND w.processedAt >= :since ORDER BY w.processedAt DESC")
    fun findRecentByProvider(provider: PaymentProvider, since: Instant): List<WebhookEvent>

    /**
     * Delete old webhook events (for cleanup)
     */
    fun deleteByProcessedAtBefore(before: Instant): Int
}

/**
 * Repository for webhook logging and replay
 */
@Repository
interface WebhookLogRepository : JpaRepository<WebhookLog, Long> {

    /**
     * Find webhooks by status
     */
    fun findByStatus(status: WebhookStatus): List<WebhookLog>

    /**
     * Find failed webhooks ready for retry
     */
    @Query("""
        SELECT w FROM WebhookLog w
        WHERE w.status IN ('FAILED', 'RETRY_SCHEDULED')
        AND w.retryCount < :maxRetries
        AND (w.nextRetryAt IS NULL OR w.nextRetryAt <= :now)
        ORDER BY w.receivedAt ASC
    """)
    fun findFailedWebhooksForRetry(maxRetries: Int, now: Instant): List<WebhookLog>

    /**
     * Find webhooks by provider and time range
     */
    @Query("""
        SELECT w FROM WebhookLog w
        WHERE w.provider = :provider
        AND w.receivedAt BETWEEN :startTime AND :endTime
        ORDER BY w.receivedAt DESC
    """)
    fun findByProviderAndTimeRange(
        provider: PaymentProvider,
        startTime: Instant,
        endTime: Instant
    ): List<WebhookLog>

    /**
     * Count webhooks by status
     */
    fun countByStatus(status: WebhookStatus): Long

    /**
     * Delete old webhook logs (for cleanup)
     */
    fun deleteByReceivedAtBefore(before: Instant): Int
}
