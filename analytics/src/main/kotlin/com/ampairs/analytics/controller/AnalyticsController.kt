package com.ampairs.analytics.controller

import com.ampairs.analytics.domain.dto.KpiResponse
import com.ampairs.analytics.domain.dto.TrendPointResponse
import com.ampairs.analytics.domain.enums.MetricGroup
import com.ampairs.analytics.domain.enums.Period
import com.ampairs.analytics.service.DashboardReadService
import com.ampairs.core.domain.dto.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * Dashboard read API (R1). Workspace scope is established by `SessionUserFilter` from the
 * `X-Workspace-ID` header; `@TenantId` filters every query. Returns `ApiResponse<T>`; business
 * exceptions bubble to the global handler (no try/catch here).
 *
 * P1 backs the SALES group end-to-end (event roll-up → summary → read); other groups return an empty
 * value list until their roll-up/reconcile lands (Option A).
 */
@RestController
@RequestMapping("/analytics/v1/dashboard")
class AnalyticsController(
    private val readService: DashboardReadService,
) {

    @GetMapping("/kpis")
    fun kpis(
        @RequestParam("from_date") fromDate: LocalDate,
        @RequestParam("to_date") toDate: LocalDate,
        @RequestParam("period", defaultValue = "MONTH") period: String,
        @RequestParam("metric_group", defaultValue = "SALES") metricGroup: String,
    ): ApiResponse<KpiResponse> {
        val group = MetricGroup.valueOf(metricGroup.uppercase())
        val periodEnum = Period.valueOf(period.uppercase())
        return ApiResponse.success(readService.kpis(group, fromDate, toDate, periodEnum))
    }

    @GetMapping("/trend")
    fun trend(
        @RequestParam("from_date") fromDate: LocalDate,
        @RequestParam("to_date") toDate: LocalDate,
        @RequestParam("period", defaultValue = "MONTH") period: String,
        @RequestParam("metric_id", defaultValue = "sales.gross") metricId: String,
    ): ApiResponse<List<TrendPointResponse>> {
        val periodEnum = Period.valueOf(period.uppercase())
        return ApiResponse.success(readService.trend(metricId, fromDate, toDate, periodEnum))
    }
}
