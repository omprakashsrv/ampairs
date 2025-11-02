package com.ampairs.event.service

import com.ampairs.core.multitenancy.DeviceContextHolder
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.event.domain.EventType
import com.ampairs.event.domain.WorkspaceEvent
import com.ampairs.event.repository.WorkspaceEventRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class WorkspaceEventService(
    private val eventRepository: WorkspaceEventRepository
) {

    private val logger = LoggerFactory.getLogger(WorkspaceEventService::class.java)

    /**
     * Get events for a workspace since a specific sequence number
     * Excludes events from the current device
     */
    fun getEventsSince(
        sinceSequence: Long,
        limit: Int,
        excludeDeviceId: String? = null
    ): List<WorkspaceEvent> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context")

        val deviceId = excludeDeviceId ?: DeviceContextHolder.getCurrentDevice() ?: "unknown"

        val pageable = Pageable.ofSize(limit)
        val events = eventRepository.findEventsSinceSequence(
            workspaceId = workspaceId,
            sinceSequence = sinceSequence,
            excludeDeviceId = deviceId,
            pageable = pageable
        )

        logger.debug(
            "Retrieved {} events since sequence {} for workspace {}",
            events.content.size, sinceSequence, workspaceId
        )

        return events.content
    }

    /**
     * Get all events for a workspace
     */
    fun getAllEvents(pageable: Pageable): Page<WorkspaceEvent> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context")

        return eventRepository.findByWorkspaceIdOrderBySequenceNumberAsc(workspaceId, pageable)
    }

    /**
     * Get unconsumed events for a workspace
     */
    fun getUnconsumedEvents(pageable: Pageable): Page<WorkspaceEvent> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context")

        return eventRepository.findUnconsumedEvents(workspaceId, pageable)
    }

    /**
     * Get events by entity type and ID
     */
    fun getEventsByEntity(
        entityType: String,
        entityId: String,
        pageable: Pageable
    ): Page<WorkspaceEvent> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context")

        return eventRepository.findByWorkspaceIdAndEntityTypeAndEntityIdOrderBySequenceNumberDesc(
            workspaceId = workspaceId,
            entityType = entityType,
            entityId = entityId,
            pageable = pageable
        )
    }

    /**
     * Get events by event type
     */
    fun getEventsByType(
        eventType: EventType,
        pageable: Pageable
    ): Page<WorkspaceEvent> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context")

        return eventRepository.findByWorkspaceIdAndEventTypeOrderBySequenceNumberDesc(
            workspaceId = workspaceId,
            eventType = eventType,
            pageable = pageable
        )
    }

    /**
     * Mark an event as consumed
     */
    @Transactional
    fun markEventConsumed(eventId: String) {
        val event = eventRepository.findByUid(eventId)
            ?: throw IllegalArgumentException("Event not found: $eventId")

        event.markConsumed()
        eventRepository.save(event)

        logger.debug("Marked event as consumed: {}", eventId)
    }

    /**
     * Mark multiple events as consumed
     */
    @Transactional
    fun markEventsConsumed(eventIds: List<String>) {
        eventIds.forEach { eventId ->
            try {
                markEventConsumed(eventId)
            } catch (e: Exception) {
                logger.error("Error marking event as consumed: {}", eventId, e)
            }
        }

        logger.debug("Marked {} events as consumed", eventIds.size)
    }

    /**
     * Count unconsumed events for current workspace
     */
    fun countUnconsumedEvents(): Long {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context")

        return eventRepository.countUnconsumedEvents(workspaceId)
    }

    /**
     * Get next sequence number for workspace
     */
    fun getNextSequenceNumber(): Long {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context")

        return eventRepository.getNextSequenceNumber(workspaceId)
    }

    /**
     * Scheduled job to cleanup old consumed events
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    fun cleanupOldConsumedEvents() {
        try {
            val cutoffDate = LocalDateTime.now().minusDays(30)
            val deletedCount = eventRepository.deleteConsumedEventsBefore(cutoffDate)

            if (deletedCount > 0) {
                logger.info("Cleaned up {} old consumed events", deletedCount)
            }
        } catch (e: Exception) {
            logger.error("Error cleaning up old consumed events", e)
        }
    }

    /**
     * Get event by UID
     */
    fun getEventByUid(uid: String): WorkspaceEvent? {
        return eventRepository.findByUid(uid)
    }
}
