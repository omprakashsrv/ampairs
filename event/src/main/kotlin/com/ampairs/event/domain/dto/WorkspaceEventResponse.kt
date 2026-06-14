package com.ampairs.event.domain.dto

import com.ampairs.event.domain.EventType
import com.ampairs.event.domain.WorkspaceEvent
import java.time.Instant

data class WorkspaceEventResponse(
    val uid: String,
    val eventType: EventType,
    val entityType: String,
    val entityId: String,
    val payload: Map<String, Any>,
    // The entity's max(updatedAt) at change time. Clients compare this to their last-synced
    // watermark to decide whether to pull — no full record is carried on the wire.
    val lastUpdatedAt: Instant?,
    val deviceId: String,
    val userId: String,
    val sequenceNumber: Long,
    val workspaceId: String,
    val createdAt: Instant?,
)

fun WorkspaceEvent.asWorkspaceEventResponse(): WorkspaceEventResponse {
    val payloadMap = this.getPayloadMap()
    val lastUpdated = (payloadMap["last_updated_at"] as? String)
        ?.let { runCatching { Instant.parse(it) }.getOrNull() }
    return WorkspaceEventResponse(
        uid = this.uid,
        eventType = this.eventType,
        entityType = this.entityType,
        entityId = this.entityId,
        payload = payloadMap,
        lastUpdatedAt = lastUpdated,
        deviceId = this.deviceId,
        userId = this.userId,
        sequenceNumber = this.sequenceNumber,
        workspaceId = this.workspaceId,
        createdAt = this.createdAt,
    )
}

fun List<WorkspaceEvent>.asWorkspaceEventResponses(): List<WorkspaceEventResponse> {
    return this.map { it.asWorkspaceEventResponse() }
}
