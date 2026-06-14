package com.ampairs.event.service

import com.ampairs.core.multitenancy.DeviceContextHolder
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.event.domain.WorkspaceEvent
import com.ampairs.event.repository.WorkspaceEventRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class WorkspaceEventService(
    private val eventRepository: WorkspaceEventRepository,
) {

    private val logger = LoggerFactory.getLogger(WorkspaceEventService::class.java)

    fun getEventsSince(
        sinceSequence: Long,
        limit: Int,
        excludeDeviceId: String? = null,
    ): List<WorkspaceEvent> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context")

        val deviceId = excludeDeviceId ?: DeviceContextHolder.getCurrentDevice() ?: "unknown"

        val pageable = Pageable.ofSize(limit)
        val events = eventRepository.findEventsSinceSequence(
            workspaceId = workspaceId,
            sinceSequence = sinceSequence,
            excludeDeviceId = deviceId,
            pageable = pageable,
        )

        logger.debug(
            "Retrieved {} watermarks since sequence {} for workspace {}",
            events.content.size, sinceSequence, workspaceId,
        )

        return events.content
    }

    fun getAllEvents(pageable: Pageable): Page<WorkspaceEvent> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("No workspace context")

        return eventRepository.findByWorkspaceIdOrderBySequenceNumberAsc(workspaceId, pageable)
    }

    fun getEventByUid(uid: String): WorkspaceEvent? = eventRepository.findByUid(uid)
}
