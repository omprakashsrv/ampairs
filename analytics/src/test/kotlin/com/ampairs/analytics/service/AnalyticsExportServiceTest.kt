package com.ampairs.analytics.service

import com.ampairs.analytics.domain.dto.AgingBucketResponse
import com.ampairs.analytics.domain.dto.AgingResponse
import com.ampairs.analytics.domain.dto.GstSummaryResponse
import com.ampairs.analytics.domain.dto.InterSplit
import com.ampairs.analytics.domain.dto.IntraSplit
import com.ampairs.analytics.domain.dto.KpiResponse
import com.ampairs.analytics.domain.dto.KpiValueResponse
import com.ampairs.analytics.domain.dto.TopEntryResponse
import com.ampairs.analytics.domain.enums.MetricGroup
import com.ampairs.analytics.domain.enums.Period
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate

class AnalyticsExportServiceTest {

    private val read = mock<DashboardReadService>()
    private val service = AnalyticsExportService(read)

    private fun kpi(group: MetricGroup, vararg vs: KpiValueResponse) =
        KpiResponse(group.name, "MONTH", FROM, TO, "INR", vs.toList(), null)

    @Test
    fun `csv has a header and a row per KPI across groups`() {
        whenever(read.kpis(eq(MetricGroup.SALES), any(), any(), any()))
            .thenReturn(kpi(MetricGroup.SALES, KpiValueResponse("sales.gross", "MONEY", BigDecimal("920710.5000"))))
        whenever(read.kpis(eq(MetricGroup.COLLECTIONS), any(), any(), any()))
            .thenReturn(kpi(MetricGroup.COLLECTIONS, KpiValueResponse("collections.collected", "MONEY", BigDecimal("12000.0000"))))
        whenever(read.kpis(eq(MetricGroup.INVENTORY), any(), any(), any()))
            .thenReturn(kpi(MetricGroup.INVENTORY, KpiValueResponse("inventory.stock_value", "MONEY", BigDecimal("250000.0000"))))
        whenever(read.gstSummary(any(), any())).thenReturn(
            GstSummaryResponse(
                FROM, TO, "INR", BigDecimal("1000.0000"), BigDecimal("230.0000"),
                IntraSplit(BigDecimal("90.0000"), BigDecimal("90.0000")), InterSplit(BigDecimal("50.0000")), emptyList(),
            ),
        )
        whenever(read.top(eq("product"), any(), any(), any()))
            .thenReturn(listOf(TopEntryResponse(1, "PRD1", "PRD1", BigDecimal("210400.0000"), BigDecimal("5"), 3)))
        whenever(read.top(eq("customer"), any(), any(), any()))
            .thenReturn(listOf(TopEntryResponse(1, "CUST1", "CUST1", BigDecimal("188900.0000"), BigDecimal.ZERO, 2)))
        whenever(read.aging(any())).thenReturn(
            AgingResponse(TO, "INR", BigDecimal("45500.0000"), listOf(AgingBucketResponse("0-30", BigDecimal("18000.0000"), 0))),
        )

        val csv = service.exportCsv(FROM, TO, Period.MONTH)
        val lines = csv.trim().lines()

        assertTrue(lines.first() == "section,metric,value,currency", "header")
        assertTrue(lines.any { it == "SALES,sales.gross,920710.5000,INR" }, "sales row")
        assertTrue(lines.any { it == "COLLECTIONS,collections.collected,12000.0000,INR" }, "collections row")
        assertTrue(lines.any { it == "GST,total_tax,230.0000,INR" }, "gst row")
        assertTrue(lines.any { it == "TOP_PRODUCT,PRD1,210400.0000,INR" }, "top product row")
        assertTrue(lines.any { it == "AGING,0-30,18000.0000,INR" }, "aging row")
    }

    companion object {
        private val FROM = LocalDate.of(2026, 6, 1)
        private val TO = LocalDate.of(2026, 6, 30)
    }
}
