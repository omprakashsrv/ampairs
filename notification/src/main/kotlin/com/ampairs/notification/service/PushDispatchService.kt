package com.ampairs.notification.service

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.notification.model.NotificationLog
import com.ampairs.notification.port.DevicePushTokenPort
import com.ampairs.notification.repository.NotificationLogRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Single entry point other modules call to raise a push notification.
 *
 * It (1) persists a [NotificationLog] for the in-app center and (2) fans the push out to the relevant
 * device tokens via the queue. Workspace-wide when [targetUserId] is null; otherwise user-scoped.
 *
 * Cross-module callers (workspace invitations, domain-event hooks, announcement endpoint) depend on
 * this service interface only — never on the providers or the queue directly.
 */
@Service
class PushDispatchService(
    private val notificationLogRepository: NotificationLogRepository,
    private val notificationService: NotificationService,
    private val devicePushTokenPort: ObjectProvider<DevicePushTokenPort>,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(PushDispatchService::class.java)

    /**
     * @param workspaceId tenant the notification belongs to
     * @param type FcmMessageTypes value (order_update, workspace_invitation, system_announcement, …)
     * @param title notification title
     * @param body notification body
     * @param data extra key/value payload (e.g. deep_link, entity_id) merged into the push
     * @param targetUserId null = all workspace members; otherwise only this user
     * @param refId optional originating entity reference
     * @return the persisted notification log uid
     */
    @Transactional
    fun dispatch(
        workspaceId: String,
        type: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
        targetUserId: String? = null,
        refId: String? = null,
    ): String = TenantContextHolder.withTenant(workspaceId) {
        // 1. Persist the in-app notification record.
        val log = NotificationLog().apply {
            this.ownerId = workspaceId
            this.type = type
            this.title = title
            this.body = body
            this.targetUserId = targetUserId
            this.refId = refId
            this.dataPayload = if (data.isEmpty()) null else objectMapper.writeValueAsString(data)
        }
        val savedLog = notificationLogRepository.save(log)

        // 2. Resolve target device tokens and enqueue a push per token.
        val port = devicePushTokenPort.ifAvailable
        if (port == null) {
            logger.warn("No DevicePushTokenPort available; persisted notification {} but sent no push", savedLog.uid)
            return@withTenant savedLog.uid
        }

        val targets = if (targetUserId != null) {
            port.tokensForUser(targetUserId)
        } else {
            port.tokensForWorkspace(workspaceId)
        }

        val pushData = buildMap {
            putAll(data)
            put("type", type)
            put("notification_uid", savedLog.uid)
        }

        targets.forEach { target ->
            notificationService.queuePush(
                token = target.token,
                title = title,
                body = body,
                data = pushData,
                tenantId = workspaceId,
            )
        }

        logger.info(
            "Dispatched notification {} (type={}, targets={}) for workspace {}",
            savedLog.uid, type, targets.size, workspaceId,
        )
        savedLog.uid
    }
}
