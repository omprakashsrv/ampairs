package com.ampairs.analytics.controller

import com.ampairs.analytics.domain.dto.DemandForecastResponse
import com.ampairs.analytics.service.DemandForecastReadService
import com.ampairs.analytics.service.ForecastService
import com.ampairs.core.domain.dto.PageResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** Contract test for the pull-only demand-forecast /sync feed (feature 022, T035). */
class DemandForecastSyncControllerTest {

    private val read = mock<DemandForecastReadService>()
    private val forecast = mock<ForecastService>()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(DemandForecastController(read, forecast)).build()
    }

    private fun page(vararg rows: DemandForecastResponse) = PageResponse(
        content = rows.toList(),
        pageNumber = 0,
        pageSize = 100,
        totalElements = rows.size.toLong(),
        totalPages = 1,
        first = true,
        last = true,
        hasNext = false,
        hasPrevious = false,
        empty = rows.isEmpty(),
    )

    private fun row(uid: String, active: Boolean) = DemandForecastResponse(
        uid = uid,
        productId = "PRD1",
        periodStart = LocalDate.parse("2026-07-01"),
        horizon = 7,
        meanQty = BigDecimal("14.00"),
        stdDevQty = BigDecimal("2.00"),
        method = "HOLT_WINTERS",
        confidence = "HIGH",
        generatedAt = Instant.parse("2026-07-01T02:30:00Z"),
        updatedAt = Instant.parse("2026-07-01T02:30:00Z"),
        active = active,
    )

    @Test
    fun `first sync with no checkpoint returns the page (incl retired rows)`() {
        whenever(read.syncFeed(isNull(), eq(0), eq(100))).thenReturn(page(row("F1", true), row("F2", false)))

        mockMvc.perform(get("/analytics/v1/forecasts/sync"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.content.length()").value(2))
    }

    @Test
    fun `last_sync checkpoint is parsed and forwarded`() {
        whenever(read.syncFeed(eq(Instant.parse("2026-07-01T00:00:00Z")), any(), any())).thenReturn(page())

        mockMvc.perform(
            get("/analytics/v1/forecasts/sync")
                .param("last_sync", "2026-07-01T00:00:00Z")
                .param("page", "0")
                .param("size", "50"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.empty").value(true))
    }
}
