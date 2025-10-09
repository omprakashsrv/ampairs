package com.ampairs.event.listener

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.event.config.Constants
import com.ampairs.event.domain.WebSocketSession
import com.ampairs.event.domain.DeviceStatus
import com.ampairs.event.domain.dto.UserStatusEvent
import com.ampairs.event.repository.WebSocketSessionRepository
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import java.time.LocalDateTime

@Component
class WebSocketEventListener(
    private val webSocketSessionRepository: WebSocketSessionRepository,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = LoggerFactory.getLogger(WebSocketEventListener::class.java)

    @EventListener
    fun handleWebSocketConnectEvent(event: SessionConnectEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val sessionAttributes = accessor.sessionAttributes ?: return

        val sessionId = accessor.sessionId ?: return
        val userId = sessionAttributes["userId"] as? String ?: return
        val tenantId = sessionAttributes["tenantId"] as? String ?: return
        val deviceId = sessionAttributes["deviceId"] as? String ?: return

        logger.debug(
            "WebSocket CONNECT event: session={}, user={}, tenant={}, device={}",
            sessionId, userId, tenantId, deviceId
        )
    }

    @EventListener
    fun handleWebSocketConnectedEvent(event: SessionConnectedEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val sessionAttributes = accessor.sessionAttributes ?: return

        val sessionId = accessor.sessionId ?: return
        val userId = sessionAttributes["userId"] as? String ?: return
        val tenantAttr = sessionAttributes["tenantId"] as? String
        val workspaceAttr = sessionAttributes["workspaceId"] as? String
        val deviceId = sessionAttributes["deviceId"] as? String ?: return
        val workspaceIdCandidate = tenantAttr?.takeIf { it.isNotBlank() } ?: workspaceAttr
        if (workspaceIdCandidate.isNullOrBlank()) {
            logger.warn(
                "Skipping WebSocket CONNECTED processing due to missing workspace. session={}, user={}, tenantAttr={}, workspaceAttr={}",
                sessionId, userId, tenantAttr, workspaceAttr
            )
            return
        }
        val workspaceId = workspaceIdCandidate

        logger.info(
            "WebSocket CONNECTED: session={}, user={}, tenant={}, device={}",
            sessionId, userId, workspaceId, deviceId
        )

        TenantContextHolder.setCurrentTenant(workspaceId)
        var registeredDeviceName: String? = null
        try {
            // Check if session already exists (reconnection)
            val existingSession = webSocketSessionRepository.findByWorkspaceIdAndUserIdAndDeviceId(
                workspaceId, userId, deviceId
            )

            val deviceSession = existingSession ?: WebSocketSession()

            deviceSession.apply {
                this.workspaceId = workspaceId
                this.userId = userId
                this.deviceId = deviceId
                this.sessionId = sessionId
                status = DeviceStatus.ONLINE
                lastHeartbeat = LocalDateTime.now()
                if (existingSession == null) {
                    connectedAt = LocalDateTime.now()
                    // Could extract device name from User-Agent header if available
                    deviceName = extractDeviceName(accessor)
                } else {
                    // Reconnection
                    disconnectedAt = null
                }
            }

            webSocketSessionRepository.save(deviceSession)
            registeredDeviceName = deviceSession.deviceName

            logger.info(
                "Registered device session: uid={}, workspace={}, user={}, device={}",
                deviceSession.uid, workspaceId, userId, deviceId
            )
        } finally {
            TenantContextHolder.clearTenantContext()
        }

        // Broadcast status change to workspace
        broadcastStatusChange(
            workspaceId = workspaceId,
            userId = userId,
            deviceId = deviceId,
            status = DeviceStatus.ONLINE,
            deviceName = registeredDeviceName
        )
    }

    @EventListener
    fun handleWebSocketDisconnectEvent(event: SessionDisconnectEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = event.sessionId

        logger.info(
            "WebSocket DISCONNECT: session={}, closeStatus={}",
            sessionId,
            event.closeStatus?.code
        )

        // Find and update session
        val session = webSocketSessionRepository.findBySessionId(sessionId)
        if (session != null) {
            TenantContextHolder.setCurrentTenant(session.workspaceId)
            try {
                session.markOffline()
                webSocketSessionRepository.save(session)
            } finally {
                TenantContextHolder.clearTenantContext()
            }

            // Broadcast status change to workspace
            broadcastStatusChange(
                workspaceId = session.workspaceId,
                userId = session.userId,
                deviceId = session.deviceId,
                status = DeviceStatus.OFFLINE,
                deviceName = session.deviceName
            )

            logger.debug("Device session marked offline: {}", session.uid)
        } else {
            logger.warn("No device session found for disconnecting session: {}", sessionId)
        }
    }

    private fun broadcastStatusChange(
        workspaceId: String,
        userId: String,
        deviceId: String,
        status: DeviceStatus,
        deviceName: String? = null
    ) {
        try {
            val statusEvent = UserStatusEvent(
                userId = userId,
                deviceId = deviceId,
                status = status,
                deviceName = deviceName
            )

            messagingTemplate.convertAndSend(
                Constants.WORKSPACE_STATUS_TOPIC_PREFIX + workspaceId,
                statusEvent
            )

            logger.debug(
                "Broadcasted status change: user={}, device={}, status={}",
                userId, deviceId, status
            )
        } catch (e: Exception) {
            logger.error("Error broadcasting status change", e)
        }
    }

    private fun extractDeviceName(accessor: StompHeaderAccessor): String? {
        // Try to extract device name from headers
        val userAgent = accessor.getFirstNativeHeader("User-Agent")
        return userAgent?.let { parseUserAgent(it) }
    }

    private fun parseUserAgent(userAgent: String): String {
        return when {
            userAgent.contains("Mobile") -> "Mobile App"
            userAgent.contains("Chrome") -> "Chrome Browser"
            userAgent.contains("Firefox") -> "Firefox Browser"
            userAgent.contains("Safari") -> "Safari Browser"
            userAgent.contains("Edge") -> "Edge Browser"
            else -> "Web Browser"
        }
    }
}
