package com.ampairs.notification.service

import com.ampairs.notification.model.NotificationQueue
import com.ampairs.notification.provider.NotificationChannel
import com.ampairs.notification.provider.NotificationProvider
import com.ampairs.notification.provider.NotificationResult
import com.ampairs.notification.provider.NotificationStatus
import com.ampairs.notification.provider.sms.AwsSnsSmsProvider
import com.ampairs.notification.provider.sms.Msg91SmsProvider
import com.ampairs.notification.repository.NotificationQueueRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * Notification Service with scheduler for sending notifications across multiple channels
 * Supports multiple providers with failover mechanism
 */
@Service
@Primary
class NotificationService @Autowired constructor(
    private val notificationQueueRepository: NotificationQueueRepository,
    private val msg91SmsProvider: Msg91SmsProvider,
    private val awsSnsSmsProvider: AwsSnsSmsProvider,
    @Qualifier("notificationTaskExecutor") private val taskExecutor: Executor,
    private val notificationDatabaseService: NotificationDatabaseService,
) {

    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    @Value("\${notification.sms.primary-provider:MSG91}")
    private lateinit var primarySmsProvider: String

    @Value("\${notification.batch-size:10}")
    private var batchSize: Int = 10

    @Value("\${notification.retry-delay-minutes:5}")
    private var retryDelayMinutes: Long = 5

    @Value("\${notification.cleanup-days:30}")
    private var cleanupDays: Long = 30

    @Value("\${notification.parallel-threads:5}")
    private var parallelThreads: Int = 5

    /**
     * Send notification via specified channel
     */
    @Transactional
    fun sendNotification(
        recipient: String,
        message: String,
        channel: NotificationChannel = NotificationChannel.SMS,
    ): String {
        return queueNotification(recipient, message, channel)
    }

    /**
     * Queue notification for async sending
     */
    @Transactional
    fun queueNotification(
        recipient: String,
        message: String,
        channel: NotificationChannel = NotificationChannel.SMS,
    ): String {
        return queueNotificationWithTenant(recipient, message, channel, null)
    }

    /**
     * Queue notification with specific tenant context
     */
    @Transactional
    fun queueNotificationWithTenant(
        recipient: String,
        message: String,
        channel: NotificationChannel = NotificationChannel.SMS,
        tenantId: String? = null,
    ): String {
        // Use default tenant for notifications when no tenant context is available
        val effectiveTenantId = tenantId
            ?: com.ampairs.core.multitenancy.TenantContextHolder.getCurrentTenant()
            ?: "default"

        return com.ampairs.core.multitenancy.TenantContextHolder.withTenant(effectiveTenantId) {
            val notificationQueue = NotificationQueue().apply {
                this.recipient = recipient
                this.message = message
                this.channel = channel
                this.status = NotificationStatus.PENDING
                this.scheduledAt = LocalDateTime.now()
                // Explicitly set the tenant ID to match current context
                this.ownerId = effectiveTenantId
            }

            val savedNotification = notificationQueueRepository.save(notificationQueue)
            logger.info(
                "Notification queued for {}: {} with ID: {} (tenant: {})",
                channel, recipient, savedNotification.uid, effectiveTenantId
            )

            savedNotification.uid
        }
    }

    /**
     * Queue notification with delay
     */
    @Transactional
    fun queueNotification(
        recipient: String,
        message: String,
        channel: NotificationChannel = NotificationChannel.SMS,
        delayMinutes: Long,
    ): String {
        // Use default tenant for delayed notifications when no tenant context is available  
        val effectiveTenantId = com.ampairs.core.multitenancy.TenantContextHolder.getCurrentTenant()
            ?: "default"

        return com.ampairs.core.multitenancy.TenantContextHolder.withTenant(effectiveTenantId) {
            val notificationQueue = NotificationQueue().apply {
                this.recipient = recipient
                this.message = message
                this.channel = channel
                this.status = NotificationStatus.PENDING
                this.scheduledAt = LocalDateTime.now().plusMinutes(delayMinutes)
                // Explicitly set the tenant ID to match current context
                this.ownerId = effectiveTenantId
            }

            val savedNotification = notificationQueueRepository.save(notificationQueue)
            logger.info(
                "Notification queued for {}: {} with delay: {} minutes, ID: {} (tenant: {})",
                channel, recipient, delayMinutes, savedNotification.uid, effectiveTenantId
            )

            savedNotification.uid
        }
    }

    /**
     * Legacy SMS methods for backward compatibility
     */
    @Transactional
    fun queueSms(phoneNumber: String, message: String): String {
        return queueNotification(phoneNumber, message, NotificationChannel.SMS)
    }

    @Transactional
    fun queueSms(phoneNumber: String, message: String, delayMinutes: Long): String {
        return queueNotification(phoneNumber, message, NotificationChannel.SMS, delayMinutes)
    }

    /**
     * Process pending notifications - runs every 30 seconds
     */
    @Scheduled(fixedDelay = 30000) // 30 seconds
    fun processPendingNotifications() {
        try {
            val pendingNotifications = notificationQueueRepository.findPendingNotifications().take(batchSize)

            if (pendingNotifications.isNotEmpty()) {
                logger.info(
                    "Processing {} pending notifications in parallel with {} threads",
                    pendingNotifications.size, parallelThreads
                )

                // Process notifications in parallel using thread pool
                val futures = pendingNotifications.map { notification ->
                    CompletableFuture.runAsync({
                        processNotificationAsync(notification)
                    }, taskExecutor)
                }

                // Wait for all notifications to complete (with timeout handling)
                CompletableFuture.allOf(*futures.toTypedArray()).join()

                logger.debug("Completed processing {} notifications in parallel", pendingNotifications.size)
            }
        } catch (e: Exception) {
            logger.error("Error processing pending notifications", e)
        }
    }

    /**
     * Async method to process a single notification in a separate transaction
     */
    @Async
    @Transactional
    fun processNotificationAsync(notification: NotificationQueue) {
        try {
            logger.debug(
                "Processing notification async: {} for {} via {}",
                notification.uid, notification.recipient, notification.channel
            )
            processSingleNotification(notification)
        } catch (e: Exception) {
            logger.error("Error processing notification async: {}", notification.uid, e)
        }
    }

    /**
     * Process failed notifications for retry - runs every 5 minutes
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    fun processFailedNotifications() {
        try {
            val failedNotifications = notificationQueueRepository.findFailedNotificationsForRetry().take(batchSize)

            if (failedNotifications.isNotEmpty()) {
                logger.info("Retrying {} failed notifications in parallel", failedNotifications.size)

                // Process failed notifications in parallel
                val futures = failedNotifications.map { notification ->
                    CompletableFuture.runAsync({
                        processFailedNotificationAsync(notification)
                    }, taskExecutor)
                }

                // Wait for all retry operations to complete
                CompletableFuture.allOf(*futures.toTypedArray()).join()

                logger.debug("Completed retrying {} notifications in parallel", failedNotifications.size)
            }
        } catch (e: Exception) {
            logger.error("Error processing failed notifications", e)
        }
    }

    /**
     * Async method to process a single failed notification retry in a separate transaction
     */
    @Async
    fun processFailedNotificationAsync(notification: NotificationQueue) {
        try {
            if (notification.canRetry()) {
                // Update database in separate short transaction
                notificationDatabaseService.markNotificationForRetry(notification, retryDelayMinutes)
                logger.info(
                    "Notification marked for retry: {} (attempt {}/{})",
                    notification.uid, notification.retryCount, notification.maxRetries
                )
            } else {
                // Update database in separate short transaction
                notificationDatabaseService.markNotificationAsExhausted(notification)
                logger.warn("Notification retry exhausted: {}", notification.uid)
            }
        } catch (e: Exception) {
            logger.error("Error processing failed notification async: {}", notification.uid, e)
        }
    }

    /**
     * Clean up old notification records - runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    fun cleanupOldNotifications() {
        try {
            val cutoffDate = LocalDateTime.now().minusDays(cleanupDays)
            val oldNotifications = notificationQueueRepository.findOldCompletedNotifications(cutoffDate)

            if (oldNotifications.isNotEmpty()) {
                notificationQueueRepository.deleteAll(oldNotifications)
                logger.info(
                    "Cleaned up {} old notification records older than {} days",
                    oldNotifications.size, cleanupDays
                )
            }
        } catch (e: Exception) {
            logger.error("Error cleaning up old notification records", e)
        }
    }

    /**
     * Process a single notification
     */
    @Async
    fun processSingleNotification(notification: NotificationQueue) {
        try {
            logger.debug(
                "Processing notification: {} for {} via {}",
                notification.uid, notification.recipient, notification.channel
            )

            val providers = getProvidersForChannel(notification.channel)
            var lastResult: NotificationResult? = null

            for (provider in providers) {
                if (!provider.isAvailable()) {
                    logger.warn("Provider {} is not available, skipping", provider.getProviderName())
                    continue
                }

                logger.info(
                    "Attempting to send notification via {}: {}",
                    provider.getProviderName(),
                    notification.uid
                )

                // Call external API outside of database transaction
                val result = provider.sendNotification(notification.recipient, notification.message)
                lastResult = result

                if (result.success) {
                    // Update database in separate short transaction
                    notificationDatabaseService.updateNotificationAsSent(notification, result)
                    logger.info("Notification sent successfully via {}: {}", result.providerName, notification.uid)
                    return
                } else {
                    logger.warn(
                        "Notification failed via {}: {} - {}",
                        result.providerName, notification.uid, result.errorMessage
                    )
                }
            }

            // All providers failed - update database in separate transaction
            if (lastResult != null) {
                notificationDatabaseService.updateNotificationAsFailed(
                    notification,
                    lastResult.providerName,
                    lastResult.errorMessage,
                    lastResult.providerResponse
                )
            } else {
                notificationDatabaseService.updateNotificationAsFailed(
                    notification,
                    "ALL_PROVIDERS",
                    "No providers available",
                    null
                )
            }

            logger.error("Notification failed with all providers: {}", notification.uid)

        } catch (e: Exception) {
            logger.error("Unexpected error processing notification: {}", notification.uid, e)
            // Update database in separate transaction for system errors
            notificationDatabaseService.updateNotificationAsFailed(
                notification,
                "SYSTEM_ERROR",
                "System error: ${e.message}",
                null
            )
        }
    }

    /**
     * Get providers for a specific channel
     */
    private fun getProvidersForChannel(channel: NotificationChannel): List<NotificationProvider> {
        return when (channel) {
            NotificationChannel.SMS -> getSmsProvidersInOrder()
            NotificationChannel.EMAIL -> emptyList() // TODO: Implement email providers
            NotificationChannel.WHATSAPP -> emptyList() // TODO: Implement WhatsApp providers
            else -> emptyList()
        }
    }

    /**
     * Get SMS providers in preferred order
     */
    private fun getSmsProvidersInOrder(): List<NotificationProvider> {
        return when (primarySmsProvider.uppercase()) {
            "MSG91" -> listOf(msg91SmsProvider, awsSnsSmsProvider)
            "AWS_SNS" -> listOf(awsSnsSmsProvider, msg91SmsProvider)
            else -> {
                logger.warn("Unknown primary SMS provider: {}, using default order", primarySmsProvider)
                listOf(msg91SmsProvider, awsSnsSmsProvider)
            }
        }
    }

    /**
     * Get notification queue statistics
     */
    fun getNotificationStatistics(): Map<String, Any> {
        val overallStats = mapOf(
            "pending" to notificationQueueRepository.countByStatusIn(listOf(NotificationStatus.PENDING)),
            "retrying" to notificationQueueRepository.countByStatusIn(listOf(NotificationStatus.RETRYING)),
            "sent" to notificationQueueRepository.countByStatusIn(listOf(NotificationStatus.SENT)),
            "failed" to notificationQueueRepository.countByStatusIn(listOf(NotificationStatus.FAILED)),
            "exhausted" to notificationQueueRepository.countByStatusIn(listOf(NotificationStatus.EXHAUSTED))
        )

        val channelStats = NotificationChannel.entries.associateWith { channel ->
            mapOf(
                "pending" to notificationQueueRepository.countByStatusInAndChannel(
                    listOf(NotificationStatus.PENDING),
                    channel
                ),
                "sent" to notificationQueueRepository.countByStatusInAndChannel(
                    listOf(NotificationStatus.SENT),
                    channel
                ),
                "failed" to notificationQueueRepository.countByStatusInAndChannel(
                    listOf(NotificationStatus.FAILED),
                    channel
                )
            )
        }

        return mapOf(
            "overall" to overallStats,
            "byChannel" to channelStats
        )
    }

    /**
     * Legacy SMS statistics method for backward compatibility
     */
    fun getSmsStatistics(): Map<String, Long> {
        return mapOf(
            "pending" to notificationQueueRepository.countByStatusInAndChannel(
                listOf(NotificationStatus.PENDING),
                NotificationChannel.SMS
            ),
            "retrying" to notificationQueueRepository.countByStatusInAndChannel(
                listOf(NotificationStatus.RETRYING),
                NotificationChannel.SMS
            ),
            "sent" to notificationQueueRepository.countByStatusInAndChannel(
                listOf(NotificationStatus.SENT),
                NotificationChannel.SMS
            ),
            "failed" to notificationQueueRepository.countByStatusInAndChannel(
                listOf(NotificationStatus.FAILED),
                NotificationChannel.SMS
            ),
            "exhausted" to notificationQueueRepository.countByStatusInAndChannel(
                listOf(NotificationStatus.EXHAUSTED),
                NotificationChannel.SMS
            )
        )
    }

    /**
     * Send notification immediately (for testing or urgent messages)
     */
    @Async
    @Transactional
    fun sendImmediateNotification(
        recipient: String,
        message: String,
        channel: NotificationChannel = NotificationChannel.SMS,
    ): NotificationResult {
        val providers = getProvidersForChannel(channel)

        for (provider in providers) {
            if (provider.isAvailable()) {
                val result = provider.sendNotification(recipient, message)
                if (result.success) {
                    return result
                }
            }
        }

        return NotificationResult(
            success = false,
            errorMessage = "All providers failed for channel: $channel",
            providerName = "ALL_PROVIDERS",
            channel = channel
        )
    }

    /**
     * Legacy SMS immediate send method for backward compatibility
     */
    @Async
    @Transactional
    fun sendImmediateSms(phoneNumber: String, message: String): NotificationResult {
        return sendImmediateNotification(phoneNumber, message, NotificationChannel.SMS)
    }
}

/**
 * Legacy service alias for backward compatibility
 */
typealias AsyncSmsService = NotificationService