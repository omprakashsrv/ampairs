package com.ampairs.communication.controller

import com.ampairs.communication.domain.dto.ScheduleRequest
import com.ampairs.communication.domain.dto.ScheduleResponse
import com.ampairs.communication.service.schedule.ScheduleService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

/** Standard offline-sync for recurring schedules. */
@RestController
@RequestMapping("/communication/v1/schedules")
@Validated
class ScheduleController(
    private val scheduleService: ScheduleService,
) {

    @GetMapping("/sync")
    fun sync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<ScheduleResponse>> {
        val property = if (sortBy in setOf("name", "createdAt", "updatedAt")) sortBy else "updatedAt"
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), property))
        return ApiResponse.success(PageResponse.from(scheduleService.getAfterSync(lastSync, pageable)))
    }

    @PostMapping("/sync")
    fun push(@RequestBody requests: List<@Valid ScheduleRequest>): ApiResponse<List<ScheduleResponse>> =
        ApiResponse.success(scheduleService.bulkUpsert(requests))
}
