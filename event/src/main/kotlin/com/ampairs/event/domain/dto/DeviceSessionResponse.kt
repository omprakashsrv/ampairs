package com.ampairs.event.domain.dto

import com.ampairs.event.domain.WebSocketSession
import com.ampairs.event.domain.DeviceStatus
import java.time.LocalDateTime

/**
 * Response DTO for DeviceSession
 */
data class DeviceSessionResponse(
    val uid: String,
    val workspaceId: String,
    val userId: String,
    val deviceId: String,
    val deviceName: String?,
    val sessionId: String,
    val status: DeviceStatus,
    val lastHeartbeat: LocalDateTime,
    val connectedAt: LocalDateTime,
    val disconnectedAt: LocalDateTime?
)

/**
 * Extension function to convert WebSocketSession to DeviceSessionResponse
 */
fun WebSocketSession.asDeviceSessionResponse(): DeviceSessionResponse {
    return DeviceSessionResponse(
        uid = this.uid,
        workspaceId = this.workspaceId,
        userId = this.userId,
        deviceId = this.deviceId,
        deviceName = this.deviceName,
        sessionId = this.sessionId,
        status = this.status,
        lastHeartbeat = this.lastHeartbeat,
        connectedAt = this.connectedAt,
        disconnectedAt = this.disconnectedAt
    )
}

/**
 * Extension function to convert list of WebSocketSession to list of DeviceSessionResponse
 */
fun List<WebSocketSession>.asDeviceSessionResponses(): List<DeviceSessionResponse> {
    return this.map { it.asDeviceSessionResponse() }
}
