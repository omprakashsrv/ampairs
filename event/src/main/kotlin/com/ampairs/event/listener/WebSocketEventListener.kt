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
import java.time.Instant

@Component
class WebSocketEventListener(
    private val webSocketSessionRepository: WebSocketSessionRepository,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = LoggerFactory.getLogger(WebSocketEventListener::class.java)

    @EventListener
    fun handleWebSocketConnectEvent(event: SessionConnectEvent) {
        // SessionConnectEvent wraps the CLIENT's inbound STOMP CONNECT frame.
        // Spring copies the WebSocket session attributes (set by HandshakeInterceptor)
        // into inbound message headers, so sessionAttributes is reliably non-null here.
        // SessionConnectedEvent wraps the SERVER's outbound CONNECTED frame and does NOT
        // carry session attributes in Spring WebSocket 6 — registration must happen here.
        val accessor = StompHeaderAccessor.wrap(event.message)
        val sessionAttributes = accessor.sessionAttributes ?: run {
            logger.warn("WebSocket CONNECT: no session attributes available")
            return
        }

        val sessionId = accessor.sessionId ?: return
        val userId = sessionAttributes["userId"] as? String ?: return
        val tenantAttr = sessionAttributes["tenantId"] as? String
        val workspaceAttr = sessionAttributes["workspaceId"] as? String
        val deviceId = sessionAttributes["deviceId"] as? String ?: return

        val workspaceIdCandidate = tenantAttr?.takeIf { it.isNotBlank() } ?: workspaceAttr
        if (workspaceIdCandidate.isNullOrBlank()) {
            logger.warn(
                "Skipping WebSocket CONNECT registration due to missing workspace. session={}, user={}, tenantAttr={}, workspaceAttr={}",
                sessionId, userId, tenantAttr, workspaceAttr
            )
            return
        }
        val workspaceId = workspaceIdCandidate

        logger.info(
            "WebSocket CONNECT: session={}, user={}, workspace={}, device={}",
            sessionId, userId, workspaceId, deviceId
        )

        TenantContextHolder.setCurrentTenant(workspaceId)
        var registeredDeviceName: String? = null
        try {
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
                lastHeartbeat = Instant.now()
                if (existingSession == null) {
                    connectedAt = Instant.now()
                    deviceName = extractDeviceName(accessor)
                } else {
                    disconnectedAt = null
                }
            }

            webSocketSessionRepository.save(deviceSession)
            registeredDeviceName = deviceSession.deviceName

            logger.info(
                "Registered device session: uid={}, workspace={}, user={}, device={}",
                deviceSession.uid, workspaceId, userId, deviceId
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to register device session: session={}, workspace={}, user={}, device={}",
                sessionId, workspaceId, userId, deviceId, e
            )
        } finally {
            TenantContextHolder.clearTenantContext()
        }

        broadcastStatusChange(
            workspaceId = workspaceId,
            userId = userId,
            deviceId = deviceId,
            status = DeviceStatus.ONLINE,
            deviceName = registeredDeviceName
        )
    }

    @EventListener
    fun handleWebSocketConnectedEvent(event: SessionConnectedEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        logger.debug("WebSocket CONNECTED: session={}", accessor.sessionId)
    }

    @EventListener
    fun handleWebSocketDisconnectEvent(event: SessionDisconnectEvent) {
        val sessionId = event.sessionId

        logger.info(
            "WebSocket DISCONNECT: session={}, closeStatus={}",
            sessionId,
            event.closeStatus.code
        )

        // Cross-tenant lookup: @TenantId on WebSocketSession means a plain findBySessionId()
        // would silently return null if no tenant is set in TenantContextHolder at this point.
        val session = webSocketSessionRepository.findBySessionIdCrossTenant(sessionId)
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
