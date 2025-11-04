package com.ampairs.event.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.event.domain.EventType
import com.ampairs.event.domain.dto.WorkspaceEventResponse
import com.ampairs.event.domain.dto.asWorkspaceEventResponse
import com.ampairs.event.domain.dto.asWorkspaceEventResponses
import com.ampairs.event.service.WorkspaceEventService
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/events")
class WorkspaceEventController(
    private val eventService: WorkspaceEventService
) {

    /**
     * Get events since a specific sequence number
     * For offline sync and catching up on missed events
     */
    @GetMapping
    fun getEvents(
        @RequestParam(required = false, defaultValue = "0") sinceSequence: Long,
        @RequestParam(defaultValue = "100") limit: Int,
        @RequestHeader(value = "X-Device-ID", required = false) deviceId: String?
    ): ApiResponse<List<WorkspaceEventResponse>> {
        val events = eventService.getEventsSince(
            sinceSequence = sinceSequence,
            limit = limit,
            excludeDeviceId = deviceId
        )

        return ApiResponse.success(events.asWorkspaceEventResponses())
    }

    /**
     * Get all events with pagination
     */
    @GetMapping("/all")
    fun getAllEvents(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ApiResponse<List<WorkspaceEventResponse>> {
        val pageable = PageRequest.of(page, size)
        val events = eventService.getAllEvents(pageable)

        return ApiResponse.success(events.content.asWorkspaceEventResponses())
    }

    /**
     * Get unconsumed events
     */
    @GetMapping("/unconsumed")
    fun getUnconsumedEvents(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ApiResponse<List<WorkspaceEventResponse>> {
        val pageable = PageRequest.of(page, size)
        val events = eventService.getUnconsumedEvents(pageable)

        return ApiResponse.success(events.content.asWorkspaceEventResponses())
    }

    /**
     * Get events for a specific entity
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    fun getEventsByEntity(
        @PathVariable entityType: String,
        @PathVariable entityId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<List<WorkspaceEventResponse>> {
        val pageable = PageRequest.of(page, size)
        val events = eventService.getEventsByEntity(entityType, entityId, pageable)

        return ApiResponse.success(events.content.asWorkspaceEventResponses())
    }

    /**
     * Get events by event type
     */
    @GetMapping("/type/{eventType}")
    fun getEventsByType(
        @PathVariable eventType: EventType,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ApiResponse<List<WorkspaceEventResponse>> {
        val pageable = PageRequest.of(page, size)
        val events = eventService.getEventsByType(eventType, pageable)

        return ApiResponse.success(events.content.asWorkspaceEventResponses())
    }

    /**
     * Mark an event as consumed
     */
    @PostMapping("/{eventId}/acknowledge")
    fun acknowledgeEvent(@PathVariable eventId: String): ApiResponse<Unit> {
        eventService.markEventConsumed(eventId)
        return ApiResponse.success(Unit)
    }

    /**
     * Mark multiple events as consumed
     */
    @PostMapping("/acknowledge")
    fun acknowledgeEvents(@RequestBody eventIds: List<String>): ApiResponse<Unit> {
        eventService.markEventsConsumed(eventIds)
        return ApiResponse.success(Unit)
    }

    /**
     * Get count of unconsumed events
     */
    @GetMapping("/unconsumed/count")
    fun getUnconsumedCount(): ApiResponse<Long> {
        val count = eventService.countUnconsumedEvents()
        return ApiResponse.success(count)
    }

    /**
     * Get event by UID
     */
    @GetMapping("/{eventId}")
    fun getEvent(@PathVariable eventId: String): ApiResponse<WorkspaceEventResponse?> {
        val event = eventService.getEventByUid(eventId)
        return ApiResponse.success(event?.asWorkspaceEventResponse())
    }
}
