package com.ampairs.notification.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.service.UserService
import com.ampairs.notification.domain.dto.NotificationLogRequest
import com.ampairs.notification.domain.dto.NotificationLogResponse
import com.ampairs.notification.provider.NotificationChannel
import com.ampairs.notification.service.NotificationLogService
import com.ampairs.notification.service.NotificationService
import com.ampairs.notification.service.PushDispatchService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Notification Controller for monitoring and managing notification operations
 */
@RestController
@RequestMapping("/notification/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService,
    private val notificationLogService: NotificationLogService,
    private val pushDispatchService: PushDispatchService,
    private val userService: UserService,
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
        return ApiResponse.success(mapOf("notification_id" to notificationId, "channel" to notificationChannel.name))
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
        return ApiResponse.success(mapOf("sms_id" to smsId))
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

    // ===================================================================================
    // In-app notification center — canonical offline-sync feed + read/dismiss + announce
    // (see docs/guides/offline-sync-contract.md). Tenant set by SessionUserFilter.
    // ===================================================================================

    /**
     * Incremental pull feed of the current user's notifications (workspace-wide + user-scoped),
     * INCLUDING dismissed rows so deletions propagate. Ordered by updatedAt ASC, paginated.
     */
    @GetMapping("/feed/sync")
    fun getFeedSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<NotificationLogResponse>> {
        val jpaSort = if (sortBy == "createdAt") "createdAt" else "updatedAt"
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), jpaSort))
        val userId = userService.getCurrentUserId().orEmpty()
        return ApiResponse.success(PageResponse.from(notificationLogService.getFeedAfterSync(lastSync, userId, pageable)))
    }

    /**
     * Bulk upsert keyed by uid — the app pushes read/dismiss flag changes back here.
     */
    @PostMapping("/feed/sync")
    fun pushFeed(
        @RequestBody requests: List<@Valid NotificationLogRequest>,
    ): ApiResponse<List<NotificationLogResponse>> =
        ApiResponse.success(notificationLogService.bulkUpsert(requests))

    /** Mark a single notification read. */
    @PostMapping("/{uid}/read")
    fun markRead(@PathVariable uid: String): ApiResponse<Map<String, Boolean>> {
        notificationLogService.markRead(uid)
        return ApiResponse.success(mapOf("updated" to true))
    }

    /** Mark all of the current user's notifications read. */
    @PostMapping("/read-all")
    fun markAllRead(): ApiResponse<Map<String, Boolean>> {
        notificationLogService.markAllRead(userService.getCurrentUserId().orEmpty())
        return ApiResponse.success(mapOf("updated" to true))
    }

    /**
     * Broadcast a system announcement to every member of the current workspace (push + in-app).
     */
    @PostMapping("/announce")
    fun announce(
        @RequestParam title: String,
        @RequestParam message: String,
        @RequestParam(required = false) deepLink: String?,
    ): ApiResponse<Map<String, String>> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")
        val data = if (deepLink.isNullOrBlank()) emptyMap() else mapOf("deep_link" to deepLink)
        val uid = pushDispatchService.dispatch(
            workspaceId = workspaceId,
            type = "system_announcement",
            title = title,
            body = message,
            data = data,
        )
        return ApiResponse.success(mapOf("notification_uid" to uid))
    }

    /**
     * System-wide announcement to every app install subscribed to the global `announcements` FCM
     * topic (push-only; not stored in any workspace feed). SUPER_ADMIN only.
     */
    @PostMapping("/announce/global")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun announceGlobal(
        @RequestParam title: String,
        @RequestParam message: String,
        @RequestParam(required = false) deepLink: String?,
    ): ApiResponse<Map<String, Any>> {
        val data = if (deepLink.isNullOrBlank()) emptyMap() else mapOf("deep_link" to deepLink)
        val result = notificationService.sendGlobalAnnouncement(title, message, data)
        return ApiResponse.success(
            mapOf("sent" to result.success, "message_id" to (result.messageId ?: "")),
        )
    }
}
