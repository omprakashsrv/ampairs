package com.ampairs.event.domain.dto

import com.ampairs.event.domain.EventType
import com.ampairs.event.domain.WorkspaceEvent
import java.time.LocalDateTime

/**
 * Response DTO for WorkspaceEvent
 */
data class WorkspaceEventResponse(
    val uid: String,
    val eventType: EventType,
    val entityType: String,
    val entityId: String,
    val payload: Map<String, Any>,
    val deviceId: String,
    val userId: String,
    val sequenceNumber: Long,
    val consumed: Boolean,
    val workspaceId: String,
    val createdAt: LocalDateTime?
)

/**
 * Extension function to convert WorkspaceEvent to WorkspaceEventResponse
 */
fun WorkspaceEvent.asWorkspaceEventResponse(): WorkspaceEventResponse {
    return WorkspaceEventResponse(
        uid = this.uid,
        eventType = this.eventType,
        entityType = this.entityType,
        entityId = this.entityId,
        payload = this.getPayloadMap(),
        deviceId = this.deviceId,
        userId = this.userId,
        sequenceNumber = this.sequenceNumber,
        consumed = this.consumed,
        workspaceId = this.workspaceId,
        createdAt = this.createdAt
    )
}

/**
 * Extension function to convert list of WorkspaceEvent to list of WorkspaceEventResponse
 */
fun List<WorkspaceEvent>.asWorkspaceEventResponses(): List<WorkspaceEventResponse> {
    return this.map { it.asWorkspaceEventResponse() }
}
