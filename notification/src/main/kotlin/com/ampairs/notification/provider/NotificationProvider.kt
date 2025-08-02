package com.ampairs.notification.provider

/**
 * Notification Provider interface for different notification channels
 */
interface NotificationProvider {

    /**
     * Send notification to a recipient
     *
     * @param recipient The recipient (phone number, email, etc.)
     * @param message The notification message content
     * @return NotificationResult indicating success/failure
     */
    fun sendNotification(recipient: String, message: String): NotificationResult

    /**
     * Get the provider name
     */
    fun getProviderName(): String

    /**
     * Get the notification channel this provider supports
     */
    fun getChannel(): NotificationChannel

    /**
     * Check if the provider is available/enabled
     */
    fun isAvailable(): Boolean
}

/**
 * Result of notification sending operation
 */
data class NotificationResult(
    val success: Boolean,
    val messageId: String? = null,
    val errorMessage: String? = null,
    val providerResponse: String? = null,
    val providerName: String,
    val channel: NotificationChannel,
)

/**
 * Notification request model for queuing
 */
data class NotificationRequest(
    val id: String,
    val recipient: String,
    val message: String,
    val channel: NotificationChannel,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    val scheduledAt: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    val lastAttemptAt: java.time.LocalDateTime? = null,
    val status: NotificationStatus = NotificationStatus.PENDING,
)

/**
 * Notification channels enum
 */
enum class NotificationChannel {
    SMS,
    EMAIL,
    WHATSAPP,
    PUSH_NOTIFICATION,
    SLACK,
    DISCORD
}

/**
 * Notification status enum
 */
enum class NotificationStatus {
    PENDING,
    SENT,
    FAILED,
    RETRYING,
    EXHAUSTED
}