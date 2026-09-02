package com.ampairs.cb_maintenance.controller

import com.ampairs.cb_maintenance.domain.dto.CompletePmEntryRequest
import com.ampairs.cb_maintenance.domain.dto.PmEntryRequest
import com.ampairs.cb_maintenance.domain.dto.PmEntryResponse
import com.ampairs.cb_maintenance.domain.dto.ReassignRequest
import com.ampairs.cb_maintenance.service.MaintenanceAccessService
import com.ampairs.cb_maintenance.service.PmEntryService
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
@RequestMapping("/cb_maintenance/v1/pm-entries")
@Validated
class PmEntryController(
    private val pmEntryService: PmEntryService,
    private val accessService: MaintenanceAccessService,
) {

    /** Zone-scoped incremental pull — the caller's own zone only (LEADER sees all). */
    @GetMapping("/sync")
    fun getSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<PmEntryResponse>> {
        val caller = accessService.requireCurrentEmployee()
        val zoneFilter = accessService.effectiveZoneFilter(caller)
        val prop = when (sortBy) {
            "dueDate" -> "dueDate"
            "status" -> "status"
            "createdAt" -> "createdAt"
            else -> "updatedAt"
        }
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), prop))
        return ApiResponse.success(PageResponse.from(pmEntryService.getAfterSync(lastSync, zoneFilter, pageable)))
    }

    @PostMapping("/sync")
    fun bulkUpsert(@RequestBody requests: List<@Valid PmEntryRequest>): ApiResponse<List<PmEntryResponse>> =
        ApiResponse.success(pmEntryService.bulkUpsert(requests))

    @GetMapping("/{uid}")
    fun get(@PathVariable uid: String): ApiResponse<PmEntryResponse> {
        val caller = accessService.requireCurrentEmployee()
        return ApiResponse.success(pmEntryService.getForCaller(uid, caller))
    }

    @PostMapping("/{uid}/complete")
    fun complete(
        @PathVariable uid: String,
        @RequestBody request: CompletePmEntryRequest,
    ): ApiResponse<PmEntryResponse> {
        val caller = accessService.requireCurrentEmployee()
        return ApiResponse.success(pmEntryService.complete(uid, request.checklistResult, caller))
    }

    @PostMapping("/{uid}/assist")
    fun assist(@PathVariable uid: String): ApiResponse<PmEntryResponse> {
        val caller = accessService.requireCurrentEmployee()
        return ApiResponse.success(pmEntryService.assist(uid, caller))
    }

    @PostMapping("/{uid}/reassign")
    fun reassign(
        @PathVariable uid: String,
        @RequestBody @Valid request: ReassignRequest,
    ): ApiResponse<PmEntryResponse> {
        val caller = accessService.requireCurrentEmployee()
        return ApiResponse.success(pmEntryService.reassign(uid, request.newAssigneeId, caller))
    }
}
