package com.ampairs.event.domain

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.event.config.Constants
import jakarta.persistence.*

@Entity(name = "workspace_events")
@NamedEntityGraph(
    name = "WorkspaceEvent.full",
    attributeNodes = []
)
class WorkspaceEvent : OwnableBaseDomain() {

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    var eventType: EventType = EventType.CUSTOMER_UPDATED

    @Column(name = "entity_type", nullable = false, length = 50)
    var entityType: String = "" // "customer", "product", "order", "invoice"

    @Column(name = "entity_id", nullable = false, length = 200)
    var entityId: String = "" // UID of the affected entity

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    var payload: String = "" // JSON payload with change details

    @Column(name = "device_id", nullable = false, length = 200)
    var deviceId: String = "" // Originating device ID

    @Column(name = "user_id", nullable = false, length = 200)
    var userId: String = "" // User who triggered the event

    @Column(name = "sequence_number", nullable = false)
    var sequenceNumber: Long = 0 // For ordering and catch-up

    @Column(name = "consumed", nullable = false)
    var consumed: Boolean = false // Mark when all devices synced

    @Column(name = "workspace_id", nullable = false, length = 200)
    var workspaceId: String = "" // Workspace context

    override fun obtainSeqIdPrefix(): String {
        return Constants.WORKSPACE_EVENT_PREFIX
    }

    /**
     * Check if event is recent (within last 24 hours)
     */
    fun isRecent(): Boolean {
        return createdAt?.isAfter(java.time.LocalDateTime.now().minusDays(1)) ?: false
    }

    /**
     * Mark event as consumed
     */
    fun markConsumed() {
        consumed = true
    }

    /**
     * Get payload as map
     */
    fun getPayloadMap(): Map<String, Any> {
        return if (payload.isBlank()) {
            emptyMap()
        } else {
            try {
                com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                    .readValue(payload, Map::class.java) as Map<String, Any>
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
}
