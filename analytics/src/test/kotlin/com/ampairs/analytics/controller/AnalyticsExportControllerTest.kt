package com.ampairs.analytics.controller

import com.ampairs.analytics.service.AnalyticsExportService
import com.ampairs.analytics.service.DashboardReadService
import com.ampairs.analytics.service.KpiRollupService
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/** Contract test for the CSV export endpoint (feature 022, T047). */
class AnalyticsExportControllerTest {

    private val read = mock<DashboardReadService>()
    private val rollup = mock<KpiRollupService>()
    private val export = mock<AnalyticsExportService>()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(AnalyticsController(read, rollup, export)).build()
    }

    @Test
    fun `GET export streams a text-csv attachment for an out-of-window range`() {
        whenever(export.exportCsv(any(), any(), any()))
            .thenReturn("section,metric,value,currency\nSALES,sales.gross,920710.50,INR\n")

        mockMvc.perform(
            get("/analytics/v1/export")
                .param("from_date", "2020-01-01")
                .param("to_date", "2020-12-31"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith("text/csv"))
            .andExpect(
                header().string(
                    "Content-Disposition",
                    "attachment; filename=\"analytics_2020-01-01_2020-12-31.csv\"",
                ),
            )
            .andExpect(content().string(containsString("section,metric,value,currency")))
            .andExpect(content().string(containsString("SALES,sales.gross,920710.50,INR")))
    }
}
