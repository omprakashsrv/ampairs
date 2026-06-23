package com.ampairs.notification.provider.push

import com.ampairs.notification.provider.NotificationResult
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/**
 * Firebase Cloud Messaging push provider.
 *
 * The [FirebaseMessaging] bean only exists when `notification.push.enabled=true` and credentials are
 * configured (see [com.ampairs.notification.config.FirebaseConfig]). When absent, [isAvailable]
 * returns false so the queue processor skips push and the row retries later.
 */
@Component
class FcmPushProvider(
    private val messagingProvider: ObjectProvider<FirebaseMessaging>,
) : PushNotificationProvider {

    private val logger = LoggerFactory.getLogger(FcmPushProvider::class.java)

    override fun sendPush(
        token: String,
        title: String?,
        body: String,
        data: Map<String, String>,
    ): NotificationResult {
        val messaging = messagingProvider.ifAvailable
            ?: return failure("FCM is not configured/available", token)

        if (token.isBlank()) {
            return failure("Empty device token", token)
        }

        val messageBuilder = Message.builder()
            .setToken(token)
            .putAllData(data)

        // Always include a notification block so the OS renders it when the app is backgrounded.
        messageBuilder.setNotification(
            Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build()
        )

        return try {
            val messageId = messaging.send(messageBuilder.build())
            logger.info("FCM push sent successfully: {}", messageId)
            NotificationResult(
                success = true,
                messageId = messageId,
                providerName = getProviderName(),
                channel = getChannel(),
            )
        } catch (e: FirebaseMessagingException) {
            val dead = e.messagingErrorCode == MessagingErrorCode.UNREGISTERED ||
                e.messagingErrorCode == MessagingErrorCode.INVALID_ARGUMENT
            if (dead) {
                logger.warn("FCM token rejected ({}), token should be pruned", e.messagingErrorCode)
            } else {
                logger.error("FCM push failed: {}", e.messagingErrorCode, e)
            }
            NotificationResult(
                success = false,
                errorMessage = "${e.messagingErrorCode}: ${e.message}",
                providerResponse = if (dead) TOKEN_INVALID_MARKER else e.messagingErrorCode?.name,
                providerName = getProviderName(),
                channel = getChannel(),
            )
        } catch (e: Exception) {
            logger.error("Unexpected error sending FCM push", e)
            failure("Internal error: ${e.message}", token)
        }
    }

    /**
     * Broadcast to every device subscribed to an FCM [topic] (e.g. "announcements"). Used for
     * system-wide announcements that aren't scoped to a single workspace or device token.
     */
    fun sendToTopic(
        topic: String,
        title: String?,
        body: String,
        data: Map<String, String> = emptyMap(),
    ): NotificationResult {
        val messaging = messagingProvider.ifAvailable
            ?: return failure("FCM is not configured/available", "topic:$topic")

        val message = Message.builder()
            .setTopic(topic)
            .putAllData(data)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .build()

        return try {
            val messageId = messaging.send(message)
            logger.info("FCM topic broadcast sent to '{}': {}", topic, messageId)
            NotificationResult(
                success = true,
                messageId = messageId,
                providerName = getProviderName(),
                channel = getChannel(),
            )
        } catch (e: Exception) {
            logger.error("FCM topic broadcast to '{}' failed", topic, e)
            failure("Topic broadcast failed: ${e.message}", "topic:$topic")
        }
    }

    override fun getProviderName(): String = "FCM"

    override fun isAvailable(): Boolean = messagingProvider.ifAvailable != null

    private fun failure(message: String, token: String): NotificationResult {
        logger.warn("FCM push not sent for token {}: {}", token.take(12), message)
        return NotificationResult(
            success = false,
            errorMessage = message,
            providerName = getProviderName(),
            channel = getChannel(),
        )
    }

    companion object {
        /** Marker placed in providerResponse so callers can prune dead device tokens. */
        const val TOKEN_INVALID_MARKER = "TOKEN_INVALID"
    }
}
