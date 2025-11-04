package com.ampairs.event.domain

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.event.config.Constants
import jakarta.persistence.*
import org.hibernate.annotations.TenantId
import java.time.LocalDateTime

@Entity(name = "device_sessions")
@NamedEntityGraph(
    name = "WebSocketSession.full",
    attributeNodes = []
)
class WebSocketSession : BaseDomain() {

    @TenantId
    @Column(name = "workspace_id", nullable = false, length = 200)
    var workspaceId: String = ""

    @Column(name = "user_id", nullable = false, length = 200)
    var userId: String = ""

    @Column(name = "device_id", nullable = false, length = 200)
    var deviceId: String = ""

    @Column(name = "device_name", length = 255)
    var deviceName: String? = null // "Chrome - MacBook Pro", "Mobile App - iPhone"

    @Column(name = "session_id", nullable = false, length = 200, unique = true)
    var sessionId: String = "" // WebSocket session ID

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: DeviceStatus = DeviceStatus.ONLINE

    @Column(name = "last_heartbeat", nullable = false)
    var lastHeartbeat: LocalDateTime = LocalDateTime.now()

    @Column(name = "connected_at", nullable = false)
    var connectedAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "disconnected_at")
    var disconnectedAt: LocalDateTime? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.DEVICE_SESSION_PREFIX
    }

    /**
     * Check if session is stale (no heartbeat for more than specified minutes)
     */
    fun isStale(minutes: Long = 2): Boolean {
        return lastHeartbeat.isBefore(LocalDateTime.now().minusMinutes(minutes))
    }

    /**
     * Check if session is idle/away (no heartbeat for more than specified seconds)
     */
    fun isIdle(seconds: Long = 30): Boolean {
        return lastHeartbeat.isBefore(LocalDateTime.now().minusSeconds(seconds))
    }

    /**
     * Update heartbeat timestamp
     */
    fun updateHeartbeat() {
        lastHeartbeat = LocalDateTime.now()
        if (status == DeviceStatus.AWAY) {
            status = DeviceStatus.ONLINE
        }
    }

    /**
     * Mark session as away
     */
    fun markAway() {
        status = DeviceStatus.AWAY
    }

    /**
     * Mark session as offline
     */
    fun markOffline() {
        status = DeviceStatus.OFFLINE
        disconnectedAt = LocalDateTime.now()
    }

    /**
     * Mark session as online
     */
    fun markOnline() {
        status = DeviceStatus.ONLINE
        lastHeartbeat = LocalDateTime.now()
    }

    /**
     * Get session duration in minutes
     */
    fun getSessionDuration(): Long {
        val endTime = disconnectedAt ?: LocalDateTime.now()
        return java.time.Duration.between(connectedAt, endTime).toMinutes()
    }

    /**
     * Check if session is currently active (online or away)
     */
    fun isActive(): Boolean {
        return status == DeviceStatus.ONLINE || status == DeviceStatus.AWAY
    }
}
