package com.ampairs.event.controller

import com.ampairs.event.repository.WebSocketSessionRepository
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.stereotype.Controller
import java.time.LocalDateTime

@Controller
class HeartbeatController(
    private val webSocketSessionRepository: WebSocketSessionRepository
) {

    private val logger = LoggerFactory.getLogger(HeartbeatController::class.java)

    /**
     * Handle heartbeat messages from clients
     * Clients should send heartbeat every 30 seconds to /app/heartbeat
     */
    @MessageMapping("/heartbeat")
    fun handleHeartbeat(@Header("simpSessionId") sessionId: String) {
        try {
            val session = webSocketSessionRepository.findBySessionId(sessionId)

            if (session != null) {
                session.updateHeartbeat()
                webSocketSessionRepository.save(session)

                logger.debug(
                    "Heartbeat received: session={}, user={}, device={}",
                    sessionId, session.userId, session.deviceId
                )
            } else {
                logger.warn("Heartbeat received for unknown session: {}", sessionId)
            }
        } catch (e: Exception) {
            logger.error("Error processing heartbeat for session: {}", sessionId, e)
        }
    }

    /**
     * Alternative heartbeat handler using SimpMessageHeaderAccessor
     */
    @MessageMapping("/ping")
    fun handlePing(headerAccessor: SimpMessageHeaderAccessor) {
        val sessionId = headerAccessor.sessionId
        if (sessionId != null) {
            handleHeartbeat(sessionId)
        }
    }
}
