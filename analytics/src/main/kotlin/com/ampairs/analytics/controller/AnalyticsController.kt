package com.ampairs.analytics.controller

import com.ampairs.analytics.domain.dto.GstSummaryResponse
import com.ampairs.analytics.domain.dto.KpiResponse
import com.ampairs.analytics.domain.dto.RecomputeRequest
import com.ampairs.analytics.domain.dto.RecomputeResultResponse
import com.ampairs.analytics.domain.dto.TopEntryResponse
import com.ampairs.analytics.domain.dto.TrendPointResponse
import com.ampairs.analytics.domain.enums.MetricGroup
import com.ampairs.analytics.domain.enums.Period
import com.ampairs.analytics.service.DashboardReadService
import com.ampairs.analytics.service.KpiRollupService
import com.ampairs.core.domain.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * Dashboard read + recompute API (R1/R2). Workspace scope is established by `SessionUserFilter` from
 * the `X-Workspace-ID` header; `@TenantId` filters every query. Returns `ApiResponse<T>`; business
 * exceptions bubble to the global handler (no try/catch here).
 *
 * Backs the invoice-derived groups end-to-end (SALES, GST_SUMMARY, TOP_CUSTOMER); collections/aging,
 * inventory and top-PRODUCT land with their own reconcile sources.
 */
@RestController
@RequestMapping("/analytics/v1")
class AnalyticsController(
    private val readService: DashboardReadService,
    private val rollupService: KpiRollupService,
) {

    @GetMapping("/dashboard/kpis")
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

    @GetMapping("/dashboard/trend")
    fun trend(
        @RequestParam("from_date") fromDate: LocalDate,
        @RequestParam("to_date") toDate: LocalDate,
        @RequestParam("period", defaultValue = "MONTH") period: String,
        @RequestParam("metric_id", defaultValue = "sales.gross") metricId: String,
    ): ApiResponse<List<TrendPointResponse>> {
        val periodEnum = Period.valueOf(period.uppercase())
        return ApiResponse.success(readService.trend(metricId, fromDate, toDate, periodEnum))
    }

    @GetMapping("/dashboard/gst-summary")
    fun gstSummary(
        @RequestParam("from_date") fromDate: LocalDate,
        @RequestParam("to_date") toDate: LocalDate,
    ): ApiResponse<GstSummaryResponse> =
        ApiResponse.success(readService.gstSummary(fromDate, toDate))

    @GetMapping("/dashboard/top")
    fun top(
        @RequestParam("from_date") fromDate: LocalDate,
        @RequestParam("to_date") toDate: LocalDate,
        @RequestParam("dimension", defaultValue = "customer") dimension: String,
        @RequestParam("limit", defaultValue = "5") limit: Int,
    ): ApiResponse<List<TopEntryResponse>> =
        ApiResponse.success(readService.top(dimension, fromDate, toDate, limit))

    @PostMapping("/recompute")
    fun recompute(@RequestBody @Valid request: RecomputeRequest): ApiResponse<RecomputeResultResponse> {
        val from = request.fromDate!!
        val to = request.toDate!!
        val rows = rollupService.reconcile(from, to)
        val result = RecomputeResultResponse(
            fromDate = from,
            toDate = to,
            daysRecomputed = (java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1).toInt(),
            rowsUpserted = rows,
            durationMs = 0,
        )
        return ApiResponse.success(result)
    }
}
