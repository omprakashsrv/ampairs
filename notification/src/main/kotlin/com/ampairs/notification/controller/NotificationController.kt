package com.ampairs.notification.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.notification.provider.NotificationChannel
import com.ampairs.notification.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

/**
 * Notification Controller for monitoring and managing notification operations
 */
@RestController
@RequestMapping("/notification/v1")
class NotificationController(
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
    ): ApiResponse<Map<String, Any>> {
        logger.info("Sending test notification to: {} via {}", recipient, channel)
        val notificationChannel = NotificationChannel.valueOf(channel.uppercase())
        val notificationId = notificationService.queueNotification(recipient, message, notificationChannel)
        return ApiResponse.success(mapOf("notificationId" to notificationId, "channel" to notificationChannel.name))
    }

    /**
     * Send test SMS (for backward compatibility)
     */
    @PostMapping("/sms/test")
    fun sendTestSms(
        @RequestParam phoneNumber: String,
        @RequestParam(defaultValue = "Test SMS from Ampairs") message: String,
    ): ApiResponse<Map<String, Any>> {
        logger.info("Sending test SMS to: {}", phoneNumber)
        val smsId = notificationService.queueSms(phoneNumber, message)
        return ApiResponse.success(mapOf("smsId" to smsId))
    }

    /**
     * Send immediate notification (for urgent messages)
     */
    @PostMapping("/send/immediate")
    fun sendImmediateNotification(
        @RequestParam recipient: String,
        @RequestParam message: String,
        @RequestParam(defaultValue = "SMS") channel: String,
    ): ApiResponse<Map<String, Any>> {
        logger.info("Sending immediate notification to: {} via {}", recipient, channel)
        val notificationChannel = NotificationChannel.valueOf(channel.uppercase())
        val result = notificationService.sendImmediateNotification(recipient, message, notificationChannel)
        if (!result.success) {
            throw IllegalStateException(result.errorMessage ?: "Notification send failed")
        }
        return ApiResponse.success(
            mapOf(
                "messageId" to (result.messageId ?: ""),
                "provider" to result.providerName,
                "channel" to result.channel.name,
            )
        )
    }
}
