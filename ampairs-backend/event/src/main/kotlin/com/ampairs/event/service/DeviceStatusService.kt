package com.ampairs.event.service

import com.ampairs.event.config.Constants
import com.ampairs.event.domain.WebSocketSession
import com.ampairs.event.domain.DeviceStatus
import com.ampairs.event.domain.dto.UserStatusEvent
import com.ampairs.event.repository.WebSocketSessionRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class DeviceStatusService(
    private val webSocketSessionRepository: WebSocketSessionRepository,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = LoggerFactory.getLogger(DeviceStatusService::class.java)

    /**
     * Scheduled job to detect and mark stale/away sessions
     * Runs every 30 seconds
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    @Transactional
    fun detectStaleAndAwaySessions() {
        try {
            val now = LocalDateTime.now()

            // Find sessions that haven't sent heartbeat for 30 seconds
            val idleSessions = webSocketSessionRepository.findByStatusAndLastHeartbeatBefore(
                DeviceStatus.ONLINE,
                now.minusSeconds(30)
            )

            idleSessions.forEach { session ->
                if (session.isStale(2)) {
                    // No heartbeat for > 2 minutes - mark as OFFLINE
                    session.markOffline()
                    webSocketSessionRepository.save(session)

                    broadcastStatusChange(
                        workspaceId = session.workspaceId,
                        userId = session.userId,
                        deviceId = session.deviceId,
                        status = DeviceStatus.OFFLINE,
                        deviceName = session.deviceName
                    )

                    logger.info(
                        "Marked session as OFFLINE due to inactivity: user={}, device={}",
                        session.userId, session.deviceId
                    )
                } else if (session.isIdle(30)) {
                    // No heartbeat for 30s-2min - mark as AWAY
                    session.markAway()
                    webSocketSessionRepository.save(session)

                    broadcastStatusChange(
                        workspaceId = session.workspaceId,
                        userId = session.userId,
                        deviceId = session.deviceId,
                        status = DeviceStatus.AWAY,
                        deviceName = session.deviceName
                    )

                    logger.debug(
                        "Marked session as AWAY due to idle: user={}, device={}",
                        session.userId, session.deviceId
                    )
                }
            }

            if (idleSessions.isNotEmpty()) {
                logger.debug("Processed {} idle/stale sessions", idleSessions.size)
            }

        } catch (e: Exception) {
            logger.error("Error detecting stale sessions", e)
        }
    }

    /**
     * Get active sessions for a workspace
     */
    fun getActiveSessions(workspaceId: String, pageable: Pageable): Page<WebSocketSession> {
        return webSocketSessionRepository.findActiveSessions(workspaceId, pageable)
    }

    /**
     * Get active sessions for a specific user
     */
    fun getActiveSessionsByUser(workspaceId: String, userId: String, pageable: Pageable): Page<WebSocketSession> {
        return webSocketSessionRepository.findActiveSessionsByUser(workspaceId, userId, pageable)
    }

    /**
     * Count active sessions for a workspace
     */
    fun countActiveSessions(workspaceId: String): Long {
        return webSocketSessionRepository.countActiveSessions(workspaceId)
    }

    /**
     * Update heartbeat for a session
     */
    @Transactional
    fun updateHeartbeat(sessionId: String): Boolean {
        val session = webSocketSessionRepository.findBySessionId(sessionId)
        if (session != null) {
            val previousStatus = session.status
            session.updateHeartbeat()
            webSocketSessionRepository.save(session)

            // If status changed from AWAY to ONLINE, broadcast
            if (previousStatus == DeviceStatus.AWAY && session.status == DeviceStatus.ONLINE) {
                broadcastStatusChange(
                    workspaceId = session.workspaceId,
                    userId = session.userId,
                    deviceId = session.deviceId,
                    status = DeviceStatus.ONLINE,
                    deviceName = session.deviceName
                )
            }
            return true
        }
        return false
    }

    /**
     * Mark session as offline
     */
    @Transactional
    fun markOffline(sessionId: String) {
        val session = webSocketSessionRepository.findBySessionId(sessionId)
        if (session != null) {
            session.markOffline()
            webSocketSessionRepository.save(session)

            broadcastStatusChange(
                workspaceId = session.workspaceId,
                userId = session.userId,
                deviceId = session.deviceId,
                status = DeviceStatus.OFFLINE,
                deviceName = session.deviceName
            )
        }
    }

    /**
     * Mark session as online
     */
    @Transactional
    fun markOnline(
        workspaceId: String,
        userId: String,
        deviceId: String,
        sessionId: String,
        deviceName: String? = null
    ): WebSocketSession {
        val session = webSocketSessionRepository.findByWorkspaceIdAndUserIdAndDeviceId(
            workspaceId, userId, deviceId
        ) ?: WebSocketSession().apply {
            this.workspaceId = workspaceId
            this.userId = userId
            this.deviceId = deviceId
            this.connectedAt = LocalDateTime.now()
        }

        session.apply {
            this.sessionId = sessionId
            this.deviceName = deviceName ?: this.deviceName
            markOnline()
        }

        return webSocketSessionRepository.save(session)
    }

    /**
     * Cleanup old offline sessions (older than 7 days)
     */
    @Scheduled(cron = "0 0 3 * * *") // Daily at 3 AM
    @Transactional
    fun cleanupOldSessions() {
        try {
            val cutoffDate = LocalDateTime.now().minusDays(7)
            val deletedCount = webSocketSessionRepository.deleteOfflineSessionsBefore(cutoffDate)

            if (deletedCount > 0) {
                logger.info("Cleaned up {} old offline sessions", deletedCount)
            }
        } catch (e: Exception) {
            logger.error("Error cleaning up old sessions", e)
        }
    }

    /**
     * Broadcast status change to workspace
     */
    fun broadcastStatusChange(
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
                "Broadcasted status change: workspace={}, user={}, device={}, status={}",
                workspaceId, userId, deviceId, status
            )
        } catch (e: Exception) {
            logger.error("Error broadcasting status change", e)
        }
    }
}
