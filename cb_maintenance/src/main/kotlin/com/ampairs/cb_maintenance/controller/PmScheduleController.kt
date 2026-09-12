package com.ampairs.cb_maintenance.controller

import com.ampairs.cb_maintenance.domain.dto.PmScheduleRequest
import com.ampairs.cb_maintenance.domain.dto.PmScheduleResponse
import com.ampairs.cb_maintenance.service.MaintenanceNotFoundException
import com.ampairs.cb_maintenance.service.PmScheduleService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/cb_maintenance/v1/pm-schedules")
@Validated
class PmScheduleController(
    private val pmScheduleService: PmScheduleService,
) {

    @GetMapping("/sync")
    fun getSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<PmScheduleResponse>> {
        val prop = when (sortBy) {
            "assetCategory" -> "assetCategory"
            "taskName" -> "taskName"
            "createdAt" -> "createdAt"
            else -> "updatedAt"
        }
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), prop))
        return ApiResponse.success(PageResponse.from(pmScheduleService.getAfterSync(lastSync, pageable)))
    }

    @PostMapping("/sync")
    fun bulkUpsert(@RequestBody requests: List<@Valid PmScheduleRequest>): ApiResponse<List<PmScheduleResponse>> =
        ApiResponse.success(pmScheduleService.bulkUpsert(requests))

    @PostMapping
    fun create(@RequestBody @Valid request: PmScheduleRequest): ApiResponse<PmScheduleResponse> =
        ApiResponse.success(pmScheduleService.create(request))

    @GetMapping("/{uid}")
    fun get(@PathVariable uid: String): ApiResponse<PmScheduleResponse> =
        ApiResponse.success(
            pmScheduleService.findByUid(uid)
                ?: throw MaintenanceNotFoundException("PM schedule not found for uid: $uid"),
        )

    @DeleteMapping("/{uid}")
    fun delete(@PathVariable uid: String): ApiResponse<Unit> {
        pmScheduleService.delete(uid)
        return ApiResponse.success(Unit)
    }
}
