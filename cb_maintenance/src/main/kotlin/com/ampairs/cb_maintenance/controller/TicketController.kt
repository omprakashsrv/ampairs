package com.ampairs.cb_maintenance.controller

import com.ampairs.cb_maintenance.domain.dto.ReassignRequest
import com.ampairs.cb_maintenance.domain.dto.TicketRequest
import com.ampairs.cb_maintenance.domain.dto.TicketResponse
import com.ampairs.cb_maintenance.service.MaintenanceAccessService
import com.ampairs.cb_maintenance.service.TicketService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/cb_maintenance/v1/tickets")
@Validated
class TicketController(
    private val ticketService: TicketService,
    private val accessService: MaintenanceAccessService,
) {

    /**
     * Incremental pull — a zoned field employee sees only their own zone; a MAINTENANCE_LEADER or a
     * workspace admin/owner with no roster row sees all zones (HQ view). Not gated on being a
     * maintenance employee, so the owner can see tickets without being on the roster.
     */
    @GetMapping("/sync")
    fun getSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<TicketResponse>> {
        val zoneFilter = accessService.readZoneFilter()
        val prop = when (sortBy) {
            "status" -> "status"
            "raisedAt" -> "raisedAt"
            "createdAt" -> "createdAt"
            else -> "updatedAt"
        }
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), prop))
        return ApiResponse.success(PageResponse.from(ticketService.getAfterSync(lastSync, zoneFilter, pageable)))
    }

    @PostMapping("/sync")
    fun bulkUpsert(@RequestBody requests: List<@Valid TicketRequest>): ApiResponse<List<TicketResponse>> =
        ApiResponse.success(ticketService.bulkUpsert(requests))

    /** Raise a reactive ticket — auto-assigns by zone + load-balancing. */
    @PostMapping
    fun raise(@RequestBody @Valid request: TicketRequest): ApiResponse<TicketResponse> {
        val caller = accessService.requireCurrentEmployee()
        return ApiResponse.success(ticketService.raiseTicket(request, caller))
    }

    @GetMapping("/{uid}")
    fun get(@PathVariable uid: String): ApiResponse<TicketResponse> {
        val caller = accessService.requireCurrentEmployee()
        return ApiResponse.success(ticketService.getForCaller(uid, caller))
    }

    @PostMapping("/{uid}/reassign")
    fun reassign(
        @PathVariable uid: String,
        @RequestBody @Valid request: ReassignRequest,
    ): ApiResponse<TicketResponse> {
        val caller = accessService.requireCurrentEmployee()
        return ApiResponse.success(ticketService.reassign(uid, request.newAssigneeId, caller))
    }

    @PostMapping("/{uid}/assist")
    fun assist(@PathVariable uid: String): ApiResponse<TicketResponse> {
        val caller = accessService.requireCurrentEmployee()
        return ApiResponse.success(ticketService.assist(uid, caller))
    }
}
