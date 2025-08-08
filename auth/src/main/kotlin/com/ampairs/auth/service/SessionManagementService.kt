package com.ampairs.auth.service

import com.ampairs.auth.model.DeviceSession
import com.ampairs.auth.repository.DeviceSessionRepository
import com.ampairs.core.config.ApplicationProperties
import com.ampairs.core.service.SecurityAuditService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Service for managing session timeouts, concurrent session limits, and session cleanup
 */
@Service
class SessionManagementService(
    private val deviceSessionRepository: DeviceSessionRepository,
    private val applicationProperties: ApplicationProperties,
    private val securityAuditService: SecurityAuditService,
) {

    private val logger = LoggerFactory.getLogger(SessionManagementService::class.java)

    /**
     * Check if a device session is still valid (not expired by timeout rules)
     */
    fun isSessionValid(deviceSession: DeviceSession): Boolean {
        val now = LocalDateTime.now()
        val sessionConfig = applicationProperties.security.sessionManagement

        // Check if session has exceeded maximum lifetime
        val maxLifetime = sessionConfig.deviceSessionTimeout
        val sessionAge = java.time.Duration.between(deviceSession.loginTime, now)
        if (sessionAge > maxLifetime) {
            logger.debug(
                "Session {} exceeded maximum lifetime: {} > {}",
                deviceSession.deviceId, sessionAge, maxLifetime
            )
            return false
        }

        // Check if session has been idle too long
        val idleTimeout = sessionConfig.idleSessionTimeout
        val idleTime = java.time.Duration.between(deviceSession.lastActivity, now)
        if (idleTime > idleTimeout) {
            logger.debug(
                "Session {} exceeded idle timeout: {} > {}",
                deviceSession.deviceId, idleTime, idleTimeout
            )
            return false
        }

        return true
    }

    /**
     * Expire a device session due to timeout
     */
    @Transactional
    fun expireSession(deviceSession: DeviceSession, reason: String) {
        deviceSession.isActive = false
        deviceSession.expiredAt = LocalDateTime.now()
        deviceSessionRepository.save(deviceSession)

        logger.info(
            "Session expired: userId={}, deviceId={}, reason={}",
            deviceSession.userId, deviceSession.deviceId, reason
        )

        // Log security event
        securityAuditService.logDeviceSessionEvent(
            eventType = SecurityAuditService.DeviceSessionEventType.EXPIRED,
            userId = deviceSession.userId,
            deviceId = deviceSession.deviceId,
            deviceName = deviceSession.deviceName
        )
    }

    /**
     * Check concurrent session limits and deactivate oldest sessions if needed
     */
    @Transactional
    fun enforceConcurrentSessionLimits(userId: String): List<DeviceSession> {
        val sessionConfig = applicationProperties.security.sessionManagement
        val maxSessionsPerUser = sessionConfig.maxConcurrentSessionsPerUser
        val maxSessionsPerDeviceType = sessionConfig.maxConcurrentSessionsPerDeviceType

        // Get all active sessions for user, ordered by last activity (oldest first)
        val activeSessions = deviceSessionRepository
            .findByUserIdAndIsActiveTrueOrderByLastActivityAsc(userId)

        val expiredSessions = mutableListOf<DeviceSession>()

        // Enforce per-user limit
        if (activeSessions.size > maxSessionsPerUser) {
            val sessionsToExpire = activeSessions.size - maxSessionsPerUser
            val oldestSessions = activeSessions.take(sessionsToExpire)

            oldestSessions.forEach { session ->
                expireSession(session, "Exceeded concurrent session limit per user ($maxSessionsPerUser)")
                expiredSessions.add(session)
            }

            logger.info(
                "Expired {} old sessions for user {} due to concurrent session limit",
                sessionsToExpire, userId
            )
        }

        // Enforce per-device-type limit
        val sessionsByDeviceType = activeSessions
            .filter { it.isActive } // Only consider sessions that weren't just expired
            .groupBy { it.deviceType ?: "Unknown" }

        sessionsByDeviceType.forEach { (deviceType, sessions) ->
            if (sessions.size > maxSessionsPerDeviceType) {
                val sessionsToExpire = sessions.size - maxSessionsPerDeviceType
                val oldestSessionsOfType = sessions
                    .sortedBy { it.lastActivity }
                    .take(sessionsToExpire)

                oldestSessionsOfType.forEach { session ->
                    if (session.isActive) { // Don't double-expire
                        expireSession(
                            session,
                            "Exceeded concurrent session limit per device type ($maxSessionsPerDeviceType)"
                        )
                        expiredSessions.add(session)
                    }
                }

                logger.info(
                    "Expired {} old {} sessions for user {} due to device type limit",
                    sessionsToExpire, deviceType, userId
                )
            }
        }

        return expiredSessions
    }

    /**
     * Validate session and expire if needed
     */
    @Transactional
    fun validateAndExpireIfNeeded(deviceSession: DeviceSession): Boolean {
        if (!isSessionValid(deviceSession)) {
            val now = LocalDateTime.now()
            val sessionConfig = applicationProperties.security.sessionManagement

            // Determine expiry reason
            val sessionAge = java.time.Duration.between(deviceSession.loginTime, now)
            val idleTime = java.time.Duration.between(deviceSession.lastActivity, now)

            val reason = when {
                sessionAge > sessionConfig.deviceSessionTimeout -> "Session exceeded maximum lifetime"
                idleTime > sessionConfig.idleSessionTimeout -> "Session exceeded idle timeout"
                else -> "Session invalid"
            }

            expireSession(deviceSession, reason)
            return false
        }
        return true
    }

    /**
     * Update session activity timestamp
     */
    @Transactional
    fun updateSessionActivity(deviceSession: DeviceSession, request: HttpServletRequest? = null) {
        deviceSession.lastActivity = LocalDateTime.now()

        // Optionally update IP address if it changed
        if (request != null) {
            val currentIp = getClientIp(request)
            if (deviceSession.ipAddress != currentIp) {
                logger.info(
                    "IP address changed for session {}: {} -> {}",
                    deviceSession.deviceId, deviceSession.ipAddress, currentIp
                )
                deviceSession.ipAddress = currentIp

                // Log potential security event for IP change
                securityAuditService.logSuspiciousActivity(
                    activityType = "IP_ADDRESS_CHANGE",
                    description = "Session IP address changed during active session",
                    riskLevel = SecurityAuditService.RiskLevel.MEDIUM,
                    request = request,
                    additionalDetails = mapOf(
                        "device_id" to deviceSession.deviceId,
                        "old_ip" to (deviceSession.ipAddress ?: "unknown"),
                        "new_ip" to currentIp
                    )
                )
            }
        }

        deviceSessionRepository.save(deviceSession)
    }

    /**
     * Scheduled job to clean up expired sessions
     */
    @Scheduled(cron = "\${application.security.session-management.expired-session-cleanup.cron:0 */30 * * * ?}")
    @Transactional
    fun cleanupExpiredSessions() {
        val sessionConfig = applicationProperties.security.sessionManagement.expiredSessionCleanup

        if (!sessionConfig.enabled) {
            logger.debug("Session cleanup is disabled")
            return
        }

        val batchSize = sessionConfig.batchSize
        logger.info("Starting expired session cleanup (batch size: {})", batchSize)

        var totalExpired = 0
        var processed = 0

        do {
            // Get active sessions that need to be checked for expiry
            val activeSessions = deviceSessionRepository.findActiveSessionsForCleanup(batchSize)
            processed = activeSessions.size

            var expiredInBatch = 0
            activeSessions.forEach { session ->
                if (!isSessionValid(session)) {
                    val now = LocalDateTime.now()
                    val sessionConfig = applicationProperties.security.sessionManagement

                    val sessionAge = java.time.Duration.between(session.loginTime, now)
                    val idleTime = java.time.Duration.between(session.lastActivity, now)

                    val reason = when {
                        sessionAge > sessionConfig.deviceSessionTimeout -> "Maximum lifetime exceeded"
                        idleTime > sessionConfig.idleSessionTimeout -> "Idle timeout exceeded"
                        else -> "Session invalid"
                    }

                    expireSession(session, reason)
                    expiredInBatch++
                }
            }

            totalExpired += expiredInBatch
            logger.debug("Expired {} sessions in current batch", expiredInBatch)

        } while (processed == batchSize) // Continue if we got a full batch

        if (totalExpired > 0) {
            logger.info("Session cleanup completed: {} sessions expired", totalExpired)
        } else {
            logger.debug("Session cleanup completed: no sessions expired")
        }
    }

    /**
     * Get session statistics for monitoring
     */
    fun getSessionStatistics(): Map<String, Any> {
        val totalActiveSessions = deviceSessionRepository.countByIsActiveTrue()
        val sessionsByDeviceType = deviceSessionRepository.countActiveSessionsByDeviceType()
        val oldestActiveSession = deviceSessionRepository.findOldestActiveSession()

        return mapOf(
            "total_active_sessions" to totalActiveSessions,
            "sessions_by_device_type" to sessionsByDeviceType.associate {
                (it[0] as String? ?: "Unknown") to (it[1] as Long)
            },
            "oldest_active_session_age_hours" to (oldestActiveSession?.let {
                java.time.Duration.between(it.loginTime, LocalDateTime.now()).toHours()
            } ?: 0)
        )
    }

    /**
     * Extract client IP address from request
     */
    private fun getClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",")[0].trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }

        return request.remoteAddr ?: "unknown"
    }
}