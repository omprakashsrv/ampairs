package com.ampairs.event.controller

import com.ampairs.core.multitenancy.DeviceContextHolder
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.event.service.DeviceStatusService
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.stereotype.Controller

@Controller
class HeartbeatController(
    private val deviceStatusService: DeviceStatusService
) {

    private val logger = LoggerFactory.getLogger(HeartbeatController::class.java)

    /**
     * Handle heartbeat messages from clients
     * Clients should send heartbeat every 30 seconds to /app/heartbeat
     */
    @MessageMapping("/heartbeat")
    fun handleHeartbeat(headerAccessor: SimpMessageHeaderAccessor) {
        val sessionId = headerAccessor.sessionId
        if (sessionId.isNullOrBlank()) {
            logger.warn("Heartbeat received without session ID")
            return
        }

        val sessionAttributes = headerAccessor.sessionAttributes ?: emptyMap<String, Any>()
        val tenantAttr = sessionAttributes["tenantId"] as? String
        val workspaceAttr = sessionAttributes["workspaceId"] as? String
        val deviceAttr = sessionAttributes["deviceId"] as? String
        val effectiveTenant = tenantAttr?.takeIf { it.isNotBlank() } ?: workspaceAttr

        try {
            effectiveTenant?.let { TenantContextHolder.setCurrentTenant(it) }
            deviceAttr?.let { DeviceContextHolder.setCurrentDevice(it) }

            if (deviceStatusService.updateHeartbeat(sessionId)) {
                logger.debug("Heartbeat received: session={}, tenant={}, device={}", sessionId, effectiveTenant, deviceAttr)
            } else {
                logger.warn("Heartbeat received for unknown session: {}", sessionId)
            }
        } catch (e: Exception) {
            logger.error("Error processing heartbeat for session: {}", sessionId, e)
        } finally {
            TenantContextHolder.clearTenantContext()
            DeviceContextHolder.clearDeviceContext()
        }
    }

    /**
     * Alternative heartbeat handler using SimpMessageHeaderAccessor
     */
    @MessageMapping("/ping")
    fun handlePing(headerAccessor: SimpMessageHeaderAccessor) {
        handleHeartbeat(headerAccessor)
    }
}
