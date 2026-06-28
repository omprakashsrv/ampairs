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
     * Send a richer notification carrying a title and a structured data payload.
     *
     * Channels that don't support these extras (e.g. SMS) ignore them via this default delegation
     * to the plain [sendNotification]. Push providers override this to forward title + data to FCM.
     *
     * @param recipient The recipient (device push token, phone number, etc.)
     * @param message The notification body content
     * @param title Optional title (push only)
     * @param data Optional key/value data payload (push only)
     */
    fun sendNotification(
        recipient: String,
        message: String,
        title: String?,
        data: Map<String, String>,
    ): NotificationResult = sendNotification(recipient, message)

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
    val createdAt: java.time.Instant = java.time.Instant.now(),
    val scheduledAt: java.time.Instant = java.time.Instant.now(),
    val lastAttemptAt: java.time.Instant? = null,
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