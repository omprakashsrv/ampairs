package com.ampairs.communication.controller

import com.ampairs.communication.domain.dto.CampaignRequest
import com.ampairs.communication.domain.dto.CampaignResponse
import com.ampairs.communication.service.campaign.CampaignService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

/** Campaign authoring (`/sync`) + lifecycle actions. */
@RestController
@RequestMapping("/communication/v1/campaigns")
@Validated
class CampaignController(
    private val campaignService: CampaignService,
) {

    @GetMapping("/sync")
    fun sync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<CampaignResponse>> {
        val property = if (sortBy in setOf("name", "status", "createdAt", "updatedAt")) sortBy else "updatedAt"
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), property))
        return ApiResponse.success(PageResponse.from(campaignService.getAfterSync(lastSync, pageable)))
    }

    @PostMapping("/sync")
    fun push(@RequestBody requests: List<@Valid CampaignRequest>): ApiResponse<List<CampaignResponse>> =
        ApiResponse.success(campaignService.bulkUpsert(requests))

    @PostMapping("/{uid}/start")
    fun start(@PathVariable uid: String): ApiResponse<CampaignResponse> = ApiResponse.success(campaignService.start(uid))

    @PostMapping("/{uid}/pause")
    fun pause(@PathVariable uid: String): ApiResponse<CampaignResponse> = ApiResponse.success(campaignService.pause(uid))

    @PostMapping("/{uid}/resume")
    fun resume(@PathVariable uid: String): ApiResponse<CampaignResponse> = ApiResponse.success(campaignService.resume(uid))
}
