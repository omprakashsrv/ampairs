package com.ampairs.analytics.controller

import com.ampairs.analytics.domain.dto.DemandForecastResponse
import com.ampairs.analytics.domain.dto.ForecastRecomputeResponse
import com.ampairs.analytics.service.DemandForecastReadService
import com.ampairs.analytics.service.ForecastService
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * `DemandForecast` pull-only `/sync` feed (canonical offline-sync GET; no client upserts — forecasts
 * are server-generated) + a manual recompute trigger. Workspace scope via `SessionUserFilter`/`@TenantId`.
 */
@RestController
@RequestMapping("/analytics/v1/forecasts")
class DemandForecastController(
    private val readService: DemandForecastReadService,
    private val forecastService: ForecastService,
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

    /** Recompute demand forecasts for the current workspace (per-request tenant). */
    @PostMapping("/recompute")
    fun recompute(
        @RequestParam("lookback_days", defaultValue = "90") lookbackDays: Int,
        @RequestParam("horizon_days", defaultValue = "7") horizonDays: Int,
    ): ApiResponse<ForecastRecomputeResponse> {
        val count = forecastService.recompute(lookbackDays, horizonDays)
        return ApiResponse.success(ForecastRecomputeResponse(productsForecast = count, generatedAt = Instant.now()))
    }
}
