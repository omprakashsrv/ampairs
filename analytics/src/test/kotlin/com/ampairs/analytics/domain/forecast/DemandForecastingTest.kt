package com.ampairs.analytics.domain.forecast

import com.ampairs.analytics.domain.enums.Confidence
import com.ampairs.analytics.domain.enums.ForecastMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/**
 * Unit tests for [DemandForecasting] (R5 / T034). Uses a synthetic weekly-seasonal series with trend
 * and asserts Holt-Winters beats a naïve "same as last period" baseline by a clear margin (SC-007),
 * plus the method/confidence thresholds.
 */
class DemandForecastingTest {

    private val weeklyPattern = listOf(10.0, 12.0, 14.0, 16.0, 18.0, 25.0, 8.0) // Mon..Sun

    /** value(i) = weekly pattern + gentle upward trend — deterministic, noiseless. */
    private fun seriesOf(days: Int): List<Double> =
        (0 until days).map { i -> weeklyPattern[i % 7] + 0.1 * i }

    private fun mape(actual: List<Double>, forecast: List<Double>): Double =
        actual.indices.sumOf { abs(actual[it] - forecast[it]) / actual[it] } / actual.size

    @Test
    fun `holt-winters beats naive same-as-last-period by at least 20 percent (SC-007)`() {
        val full = seriesOf(56)      // 8 weeks
        val train = full.take(49)    // 7 weeks
        val actual = full.drop(49)   // held-out week 8

        val hw = DemandForecasting.forecastNext(train, season = 7, steps = 7)
        val naive = List(7) { train.last() } // "same as last period"

        val hwMape = mape(actual, hw)
        val naiveMape = mape(actual, naive)

        assertTrue(hwMape <= 0.8 * naiveMape, "HW MAPE=$hwMape should be ≤ 80% of naive MAPE=$naiveMape")
    }

    @Test
    fun `method and confidence track history depth`() {
        // ≥ 2 seasonal cycles → Holt-Winters / HIGH
        val long = DemandForecasting.summarize(seriesOf(56), season = 7, horizon = 7)
        assertEquals(ForecastMethod.HOLT_WINTERS, long.method)
        assertEquals(Confidence.HIGH, long.confidence)

        // 1–2 cycles → moving average / MEDIUM
        val mid = DemandForecasting.summarize(seriesOf(10), season = 7, horizon = 7)
        assertEquals(ForecastMethod.MOVING_AVG, mid.method)
        assertEquals(Confidence.MEDIUM, mid.confidence)

        // < 1 cycle → moving average / LOW
        val short = DemandForecasting.summarize(seriesOf(5), season = 7, horizon = 7)
        assertEquals(ForecastMethod.MOVING_AVG, short.method)
        assertEquals(Confidence.LOW, short.confidence)
    }

    @Test
    fun `forecasts are non-negative`() {
        val spiky = listOf(0.0, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0)
        val next = DemandForecasting.forecastNext(spiky, season = 7, steps = 7)
        assertTrue(next.all { it >= 0.0 }, "forecasts must be clamped to non-negative")
    }
}
