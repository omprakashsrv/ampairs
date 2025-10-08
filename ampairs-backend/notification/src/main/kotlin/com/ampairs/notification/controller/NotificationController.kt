package com.ampairs.notification.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.notification.provider.NotificationChannel
import com.ampairs.notification.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

/**
 * Notification Controller for monitoring and managing notification operations
 */
@RestController
@RequestMapping("/notification/v1")
class NotificationController @Autowired constructor(
    private val notificationService: NotificationService,
) {

    private val logger = LoggerFactory.getLogger(NotificationController::class.java)

    /**
     * Get notification queue statistics
     */
    @GetMapping("/stats")
    fun getNotificationStatistics(): ApiResponse<Any> {
        logger.info("Fetching notification statistics")
        val stats = notificationService.getNotificationStatistics()
        return ApiResponse.success(stats)
    }

    /**
     * Get SMS-specific statistics (for backward compatibility)
     */
    @GetMapping("/sms/stats")
    fun getSmsStatistics(): ApiResponse<Any> {
        logger.info("Fetching SMS statistics")
        val stats = notificationService.getSmsStatistics()
        return ApiResponse.success(stats)
    }

    /**
     * Send test notification
     */
    @PostMapping("/test")
    fun sendTestNotification(
        @RequestParam recipient: String,
        @RequestParam(defaultValue = "Test notification from Ampairs") message: String,
        @RequestParam(defaultValue = "SMS") channel: String,
    ): Map<String, Any> {
        logger.info("Sending test notification to: {} via {}", recipient, channel)

        return try {
            val notificationChannel = NotificationChannel.valueOf(channel.uppercase())
            val notificationId = notificationService.queueNotification(recipient, message, notificationChannel)

            mapOf(
                "success" to true,
                "data" to mapOf(
                    "notificationId" to notificationId,
                    "channel" to notificationChannel
                ),
                "message" to "Test notification queued successfully"
            )
        } catch (e: IllegalArgumentException) {
            logger.error("Invalid notification channel: {}", channel, e)
            mapOf(
                "success" to false,
                "error" to mapOf(
                    "code" to "INVALID_CHANNEL",
                    "message" to "Invalid notification channel: $channel. Valid channels: ${
                        NotificationChannel.entries.joinToString(
                            ", "
                        )
                    }"
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to send test notification to: {}", recipient, e)

            mapOf(
                "success" to false,
                "error" to mapOf(
                    "code" to "NOTIFICATION_QUEUE_ERROR",
                    "message" to "Failed to queue notification: ${e.message}"
                )
            )
        }
    }

    /**
     * Send test SMS (for backward compatibility)
     */
    @PostMapping("/sms/test")
    fun sendTestSms(
        @RequestParam phoneNumber: String,
        @RequestParam(defaultValue = "Test SMS from Ampairs") message: String,
    ): Map<String, Any> {
        logger.info("Sending test SMS to: {}", phoneNumber)

        return try {
            val smsId = notificationService.queueSms(phoneNumber, message)

            mapOf(
                "success" to true,
                "data" to mapOf("smsId" to smsId),
                "message" to "Test SMS queued successfully"
            )
        } catch (e: Exception) {
            logger.error("Failed to send test SMS to: {}", phoneNumber, e)

            mapOf(
                "success" to false,
                "error" to mapOf(
                    "code" to "SMS_QUEUE_ERROR",
                    "message" to "Failed to queue SMS: ${e.message}"
                )
            )
        }
    }

    /**
     * Send immediate notification (for urgent messages)
     */
    @PostMapping("/send/immediate")
    fun sendImmediateNotification(
        @RequestParam recipient: String,
        @RequestParam message: String,
        @RequestParam(defaultValue = "SMS") channel: String,
    ): Map<String, Any> {
        logger.info("Sending immediate notification to: {} via {}", recipient, channel)

        return try {
            val notificationChannel = NotificationChannel.valueOf(channel.uppercase())
            val result = notificationService.sendImmediateNotification(recipient, message, notificationChannel)

            if (result.success) {
                mapOf(
                    "success" to true,
                    "data" to mapOf(
                        "messageId" to result.messageId,
                        "provider" to result.providerName,
                        "channel" to result.channel
                    ),
                    "message" to "Notification sent successfully"
                )
            } else {
                mapOf(
                    "success" to false,
                    "error" to mapOf(
                        "code" to "NOTIFICATION_SEND_FAILED",
                        "message" to result.errorMessage,
                        "channel" to result.channel
                    )
                )
            }
        } catch (e: IllegalArgumentException) {
            logger.error("Invalid notification channel: {}", channel, e)
            mapOf(
                "success" to false,
                "error" to mapOf(
                    "code" to "INVALID_CHANNEL",
                    "message" to "Invalid notification channel: $channel"
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to send immediate notification to: {}", recipient, e)

            mapOf(
                "success" to false,
                "error" to mapOf(
                    "code" to "NOTIFICATION_SEND_ERROR",
                    "message" to "Failed to send notification: ${e.message}"
                )
            )
        }
    }
}