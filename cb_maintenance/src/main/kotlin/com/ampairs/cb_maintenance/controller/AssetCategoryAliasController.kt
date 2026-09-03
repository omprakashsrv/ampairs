package com.ampairs.cb_maintenance.controller

import com.ampairs.cb_maintenance.domain.dto.AssetCategoryAliasRequest
import com.ampairs.cb_maintenance.domain.dto.AssetCategoryAliasResponse
import com.ampairs.cb_maintenance.service.AssetCategoryAliasService
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
@RequestMapping("/cb_maintenance/v1/asset-category-aliases")
@Validated
class AssetCategoryAliasController(
    private val service: AssetCategoryAliasService,
) {

    @GetMapping("/sync")
    fun getSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<AssetCategoryAliasResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), "updatedAt"))
        return ApiResponse.success(PageResponse.from(service.getAfterSync(lastSync, pageable)))
    }

    @PostMapping("/sync")
    fun bulkUpsert(
        @RequestBody requests: List<@Valid AssetCategoryAliasRequest>,
    ): ApiResponse<List<AssetCategoryAliasResponse>> =
        ApiResponse.success(service.bulkUpsert(requests))
}
