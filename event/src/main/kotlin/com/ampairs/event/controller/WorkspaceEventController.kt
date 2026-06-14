package com.ampairs.event.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.event.domain.dto.WorkspaceEventResponse
import com.ampairs.event.domain.dto.asWorkspaceEventResponse
import com.ampairs.event.domain.dto.asWorkspaceEventResponses
import com.ampairs.event.service.WorkspaceEventService
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/event/v1/events")
class WorkspaceEventController(
    private val eventService: WorkspaceEventService,
) {

    /**
     * Watermarks newer than `sinceSequence`. Returns at most one row per entity type;
     * the client compares each row's `last_updated_at` against its own sync state and
     * pulls the actual entity payload via the per-module `/sync` endpoint.
     */
    @GetMapping
    fun getEvents(
        @RequestParam(required = false, defaultValue = "0") sinceSequence: Long,
        @RequestParam(defaultValue = "100") limit: Int,
        @RequestHeader(value = "X-Device-ID", required = false) deviceId: String?,
    ): ApiResponse<List<WorkspaceEventResponse>> {
        val events = eventService.getEventsSince(
            sinceSequence = sinceSequence,
            limit = limit,
            excludeDeviceId = deviceId,
        )
        return ApiResponse.success(events.asWorkspaceEventResponses())
    }

    /**
     * All current watermarks for the workspace (at most one row per entity type).
     */
    @GetMapping("/all")
    fun getAllEvents(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): ApiResponse<List<WorkspaceEventResponse>> {
        val pageable = PageRequest.of(page, size)
        val events = eventService.getAllEvents(pageable)
        return ApiResponse.success(events.content.asWorkspaceEventResponses())
    }

    @GetMapping("/{eventId}")
    fun getEvent(@PathVariable eventId: String): ApiResponse<WorkspaceEventResponse?> {
        val event = eventService.getEventByUid(eventId)
        return ApiResponse.success(event?.asWorkspaceEventResponse())
    }
}
