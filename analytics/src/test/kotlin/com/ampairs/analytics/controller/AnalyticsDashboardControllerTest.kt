package com.ampairs.analytics.controller

import com.ampairs.analytics.domain.dto.AgingBucketResponse
import com.ampairs.analytics.domain.dto.AgingResponse
import com.ampairs.analytics.domain.dto.GstSummaryResponse
import com.ampairs.analytics.domain.dto.InterSplit
import com.ampairs.analytics.domain.dto.IntraSplit
import com.ampairs.analytics.domain.dto.KpiResponse
import com.ampairs.analytics.domain.dto.KpiValueResponse
import com.ampairs.analytics.domain.dto.TopEntryResponse
import com.ampairs.analytics.domain.dto.TrendPointResponse
import com.ampairs.analytics.domain.enums.MetricGroup
import com.ampairs.analytics.service.AnalyticsExportService
import com.ampairs.analytics.service.DashboardReadService
import com.ampairs.analytics.service.KpiRollupService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal
import java.time.LocalDate

/** Contract tests for the dashboard read endpoints (feature 022, T017). */
class AnalyticsDashboardControllerTest {

    private val read = mock<DashboardReadService>()
    private val rollup = mock<KpiRollupService>()
    private val export = mock<AnalyticsExportService>()
    private lateinit var mockMvc: MockMvc

    private val from = LocalDate.parse("2026-07-01")
    private val to = LocalDate.parse("2026-07-31")

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(AnalyticsController(read, rollup, export)).build()
    }

    @Test
    fun `kpis returns the metric values for a group`() {
        whenever(read.kpis(any(), any(), any(), any())).thenReturn(
            KpiResponse(
                MetricGroup.SALES.name, "MONTH", from, to, "INR",
                listOf(KpiValueResponse("sales.gross", "MONEY", BigDecimal("920710.50"))), null,
            ),
        )
        mockMvc.perform(
            get("/analytics/v1/dashboard/kpis")
                .param("from_date", "2026-07-01").param("to_date", "2026-07-31")
                .param("metric_group", "SALES"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.values.length()").value(1))
    }

    @Test
    fun `trend returns a daily series`() {
        whenever(read.trend(any(), any(), any(), any())).thenReturn(
            listOf(
                TrendPointResponse(from, "2026-07-01", BigDecimal("100.00")),
                TrendPointResponse(from.plusDays(1), "2026-07-02", BigDecimal("150.00")),
            ),
        )
        mockMvc.perform(
            get("/analytics/v1/dashboard/trend")
                .param("from_date", "2026-07-01").param("to_date", "2026-07-31"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(2))
    }

    @Test
    fun `aging returns bucketed outstanding`() {
        whenever(read.aging(any())).thenReturn(
            AgingResponse(
                to, "INR", BigDecimal("500.00"),
                listOf(AgingBucketResponse("CURRENT", BigDecimal("500.00"), 2)),
            ),
        )
        mockMvc.perform(get("/analytics/v1/dashboard/aging").param("as_of_date", "2026-07-31"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.buckets.length()").value(1))
    }

    @Test
    fun `gst summary returns intra and inter splits`() {
        whenever(read.gstSummary(any(), any())).thenReturn(
            GstSummaryResponse(
                from, to, "INR", BigDecimal("1000.00"), BigDecimal("230.00"),
                IntraSplit(BigDecimal("90.00"), BigDecimal("90.00")), InterSplit(BigDecimal("50.00")), emptyList(),
            ),
        )
        mockMvc.perform(
            get("/analytics/v1/dashboard/gst-summary")
                .param("from_date", "2026-07-01").param("to_date", "2026-07-31"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").exists())
    }

    @Test
    fun `top returns ranked entries for a dimension`() {
        whenever(read.top(any(), any(), any(), any())).thenReturn(
            listOf(TopEntryResponse(1, "PRD1", "Widget", BigDecimal("210400.00"), BigDecimal("5"), 3)),
        )
        mockMvc.perform(
            get("/analytics/v1/dashboard/top")
                .param("from_date", "2026-07-01").param("to_date", "2026-07-31")
                .param("dimension", "product").param("limit", "5"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
    }
}
