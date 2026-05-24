package com.ampairs.event.domain

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.event.config.Constants
import jakarta.persistence.*
import org.hibernate.annotations.TenantId
import java.time.Instant

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
    var deviceName: String? = null

    @Column(name = "session_id", nullable = false, length = 200, unique = true)
    var sessionId: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: DeviceStatus = DeviceStatus.ONLINE

    @Column(name = "last_heartbeat", nullable = false)
    var lastHeartbeat: Instant = Instant.now()

    @Column(name = "connected_at", nullable = false)
    var connectedAt: Instant = Instant.now()

    @Column(name = "disconnected_at")
    var disconnectedAt: Instant? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.DEVICE_SESSION_PREFIX
    }

    fun isStale(minutes: Long = 2): Boolean {
        return lastHeartbeat.isBefore(Instant.now().minusSeconds(minutes * 60))
    }

    fun isIdle(seconds: Long = 30): Boolean {
        return lastHeartbeat.isBefore(Instant.now().minusSeconds(seconds))
    }

    fun updateHeartbeat() {
        lastHeartbeat = Instant.now()
        if (status == DeviceStatus.AWAY) {
            status = DeviceStatus.ONLINE
        }
    }

    fun markAway() {
        status = DeviceStatus.AWAY
    }

    fun markOffline() {
        status = DeviceStatus.OFFLINE
        disconnectedAt = Instant.now()
    }

    fun markOnline() {
        status = DeviceStatus.ONLINE
        lastHeartbeat = Instant.now()
    }

    fun getSessionDuration(): Long {
        val endTime = disconnectedAt ?: Instant.now()
        return java.time.Duration.between(connectedAt, endTime).toMinutes()
    }

    fun isActive(): Boolean {
        return status == DeviceStatus.ONLINE || status == DeviceStatus.AWAY
    }
}
