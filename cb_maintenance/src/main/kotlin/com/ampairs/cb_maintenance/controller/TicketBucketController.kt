package com.ampairs.cb_maintenance.controller

import com.ampairs.cb_maintenance.domain.dto.TicketBucketRequest
import com.ampairs.cb_maintenance.domain.dto.TicketBucketResponse
import com.ampairs.cb_maintenance.service.TicketBucketService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/cb_maintenance/v1/ticket-buckets")
@Validated
class TicketBucketController(
    private val service: TicketBucketService,
) {

    @GetMapping("/sync")
    fun getSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<TicketBucketResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), "updatedAt"))
        return ApiResponse.success(PageResponse.from(service.getAfterSync(lastSync, pageable)))
    }

    @PostMapping("/sync")
    fun bulkUpsert(
        @RequestBody requests: List<@Valid TicketBucketRequest>,
    ): ApiResponse<List<TicketBucketResponse>> =
        ApiResponse.success(service.bulkUpsert(requests))
}
