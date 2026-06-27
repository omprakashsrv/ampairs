package com.ampairs.communication.controller

import com.ampairs.communication.domain.dto.BindingRequest
import com.ampairs.communication.domain.dto.BindingResponse
import com.ampairs.communication.service.trigger.BindingService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

/** Standard offline-sync for event→template bindings. */
@RestController
@RequestMapping("/communication/v1/bindings")
@Validated
class BindingController(
    private val bindingService: BindingService,
) {

    @GetMapping("/sync")
    fun sync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<BindingResponse>> {
        val property = if (sortBy in setOf("eventType", "createdAt", "updatedAt")) sortBy else "updatedAt"
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), property))
        return ApiResponse.success(PageResponse.from(bindingService.getAfterSync(lastSync, pageable)))
    }

    @PostMapping("/sync")
    fun push(@RequestBody requests: List<@Valid BindingRequest>): ApiResponse<List<BindingResponse>> =
        ApiResponse.success(bindingService.bulkUpsert(requests))
}
