package com.ampairs.notification.service

import com.ampairs.notification.model.NotificationQueue
import com.ampairs.notification.provider.NotificationResult
import com.ampairs.notification.repository.NotificationQueueRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Separate service for database operations with isolated transactions
 * This ensures database operations are not mixed with external API calls
 */
@Service
class NotificationDatabaseService(
    private val notificationQueueRepository: NotificationQueueRepository,
) {

    private val logger = LoggerFactory.getLogger(NotificationDatabaseService::class.java)

    /**
     * Update notification as sent in a separate short database transaction
     */
    @Transactional
    fun updateNotificationAsSent(notification: NotificationQueue, result: NotificationResult) {
        try {
            notification.markAsSent(result.providerName, result.messageId, result.providerResponse)
            notificationQueueRepository.save(notification)
            logger.debug(
                "Database updated: notification {} marked as sent via {}",
                notification.uid, result.providerName
            )
        } catch (e: Exception) {
            logger.error("Failed to update notification {} as sent in database", notification.uid, e)
            throw e // Re-throw to handle at caller level if needed
        }
    }

    /**
     * Update notification as failed in a separate short database transaction
     */
    @Transactional
    fun updateNotificationAsFailed(
        notification: NotificationQueue,
        providerName: String,
        errorMessage: String?,
        providerResponse: String?,
    ) {
        try {
            notification.markAsFailed(providerName, errorMessage, providerResponse)
            notificationQueueRepository.save(notification)
            logger.debug(
                "Database updated: notification {} marked as failed via {}: {}",
                notification.uid, providerName, errorMessage
            )
        } catch (e: Exception) {
            logger.error("Failed to update notification {} as failed in database", notification.uid, e)
            throw e // Re-throw to handle at caller level if needed
        }
    }

    /**
     * Mark notification for retry in a separate short database transaction
     */
    @Transactional
    fun markNotificationForRetry(notification: NotificationQueue, delayMinutes: Long) {
        try {
            notification.markForRetry(delayMinutes)
            notificationQueueRepository.save(notification)
            logger.debug(
                "Database updated: notification {} marked for retry (attempt {}/{})",
                notification.uid, notification.retryCount, notification.maxRetries
            )
        } catch (e: Exception) {
            logger.error("Failed to mark notification {} for retry in database", notification.uid, e)
            throw e
        }
    }

    /**
     * Mark notification as exhausted in a separate short database transaction
     */
    @Transactional
    fun markNotificationAsExhausted(notification: NotificationQueue) {
        try {
            notification.markAsExhausted()
            notificationQueueRepository.save(notification)
            logger.debug("Database updated: notification {} marked as exhausted", notification.uid)
        } catch (e: Exception) {
            logger.error("Failed to mark notification {} as exhausted in database", notification.uid, e)
            throw e
        }
    }
}