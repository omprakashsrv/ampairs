package com.ampairs.communication.controller

import com.ampairs.communication.domain.dto.PreferenceRequest
import com.ampairs.communication.domain.dto.PreferenceResponse
import com.ampairs.communication.service.consent.PreferenceService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

/** Standard offline-sync for per-customer communication preferences. */
@RestController
@RequestMapping("/communication/v1/preferences")
@Validated
class PreferenceController(
    private val preferenceService: PreferenceService,
) {

    @GetMapping("/sync")
    fun sync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<PreferenceResponse>> {
        val property = if (sortBy in setOf("createdAt", "updatedAt")) sortBy else "updatedAt"
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), property))
        return ApiResponse.success(PageResponse.from(preferenceService.getAfterSync(lastSync, pageable)))
    }

    @PostMapping("/sync")
    fun push(@RequestBody requests: List<@Valid PreferenceRequest>): ApiResponse<List<PreferenceResponse>> =
        ApiResponse.success(preferenceService.bulkUpsert(requests))
}
