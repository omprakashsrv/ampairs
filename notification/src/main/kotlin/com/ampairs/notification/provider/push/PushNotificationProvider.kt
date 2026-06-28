package com.ampairs.notification.provider.push

import com.ampairs.notification.provider.NotificationChannel
import com.ampairs.notification.provider.NotificationProvider
import com.ampairs.notification.provider.NotificationResult

/**
 * Push Notification Provider interface for FCM-style implementations.
 */
interface PushNotificationProvider : NotificationProvider {

    /**
     * Send a push notification to a device token.
     *
     * @param token The device FCM registration token
     * @param title The notification title
     * @param body The notification body
     * @param data Structured key/value payload delivered alongside the notification
     * @return NotificationResult indicating success/failure
     */
    fun sendPush(
        token: String,
        title: String?,
        body: String,
        data: Map<String, String>,
    ): NotificationResult

    /**
     * Default implementation for the plain [NotificationProvider] contract — no title/data.
     */
    override fun sendNotification(recipient: String, message: String): NotificationResult =
        sendPush(recipient, null, message, emptyMap())

    /**
     * Title + data-aware path used by the queue processor for push rows.
     */
    override fun sendNotification(
        recipient: String,
        message: String,
        title: String?,
        data: Map<String, String>,
    ): NotificationResult = sendPush(recipient, title, message, data)

    /**
     * Push providers always use the push channel.
     */
    override fun getChannel(): NotificationChannel = NotificationChannel.PUSH_NOTIFICATION
}
