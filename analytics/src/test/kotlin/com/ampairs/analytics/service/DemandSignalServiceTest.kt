package com.ampairs.analytics.service

import com.ampairs.analytics.domain.model.DemandForecast
import com.ampairs.analytics.repository.DemandForecastRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

/**
 * Unit tests for the demand signal: per-day mean/variability derived from the horizon-total forecast.
 */
class DemandSignalServiceTest {

    private val repo = mock<DemandForecastRepository>()
    private val service = DemandSignalService(repo)

    private fun forecast(mean: Double, std: Double, horizon: Int) = DemandForecast().apply {
        productId = "PRD1"
        meanQty = BigDecimal.valueOf(mean)
        stdDevQty = BigDecimal.valueOf(std)
        this.horizon = horizon
    }

    @Test
    fun `average daily demand is the horizon-total mean divided by the horizon`() {
        whenever(repo.findFirstByProductIdOrderByPeriodStartDesc(any())).thenReturn(forecast(70.0, 7.0, 7))
        assertEquals(0, BigDecimal.valueOf(10.0).compareTo(service.averageDailyDemand("PRD1")))
    }

    @Test
    fun `daily variability scales the horizon-total std by 1 over sqrt horizon`() {
        whenever(repo.findFirstByProductIdOrderByPeriodStartDesc(any())).thenReturn(forecast(70.0, 7.0, 7))
        // 7 / sqrt(7) = 2.6457... → 2.646 at scale 3
        assertEquals(0, BigDecimal.valueOf(2.646).compareTo(service.demandVariability("PRD1")))
    }

    @Test
    fun `no forecast yields null`() {
        whenever(repo.findFirstByProductIdOrderByPeriodStartDesc(any())).thenReturn(null)
        assertNull(service.averageDailyDemand("PRD1"))
        assertNull(service.demandVariability("PRD1"))
    }
}
