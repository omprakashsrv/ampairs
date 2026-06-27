package com.ampairs.analytics.controller

import com.ampairs.analytics.domain.dto.DemandForecastResponse
import com.ampairs.analytics.service.DemandForecastReadService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Pull-only `DemandForecast` `/sync` feed (canonical offline-sync GET; no POST — forecasts are
 * server-generated). Workspace scope via `SessionUserFilter`/`@TenantId`.
 */
@RestController
@RequestMapping("/analytics/v1/forecasts")
class DemandForecastController(
    private val readService: DemandForecastReadService,
) {

    @GetMapping("/sync")
    fun sync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<DemandForecastResponse>> {
        val checkpoint = lastSync?.takeIf { it.isNotBlank() }?.let { Instant.parse(it) }
        return ApiResponse.success(readService.syncFeed(checkpoint, page, size))
    }
}
