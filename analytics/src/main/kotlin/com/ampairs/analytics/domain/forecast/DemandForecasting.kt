package com.ampairs.analytics.domain.forecast

import com.ampairs.analytics.domain.enums.Confidence
import com.ampairs.analytics.domain.enums.ForecastMethod
import kotlin.math.sqrt

/**
 * Pure, dependency-free demand forecasting (R5). Additive Holt-Winters (level + trend + additive weekly
 * seasonality) once there are at least two seasonal cycles of history; otherwise a moving-average
 * fallback with lower stated confidence. Additive (not multiplicative) seasonality is used because
 * retail daily series are zero-heavy and multiplicative smoothing divides by zero.
 *
 * Deterministic and side-effect-free so it can be unit-tested against a synthetic series.
 */
object DemandForecasting {

    private const val ALPHA = 0.3 // level smoothing
    private const val BETA = 0.05 // trend smoothing
    private const val GAMMA = 0.3 // seasonal smoothing

    data class Forecast(
        val meanDaily: Double,   // expected demand per day over the horizon
        val stdDevDaily: Double, // residual variability per day
        val method: ForecastMethod,
        val confidence: Confidence,
    )

    /**
     * Forecast the next [steps] days from a contiguous daily [series] (oldest → newest, zeros filled).
     * Returns clamped-non-negative point forecasts; empty when the series is empty.
     */
    fun forecastNext(series: List<Double>, season: Int, steps: Int): List<Double> {
        if (series.isEmpty() || steps <= 0) return emptyList()
        if (series.size < 2 * season) {
            // Moving-average fallback: flat forecast at the recent mean.
            val window = series.takeLast(season.coerceAtMost(series.size))
            val avg = window.average()
            return List(steps) { avg.coerceAtLeast(0.0) }
        }
        return holtWinters(series, season, steps).map { it.coerceAtLeast(0.0) }
    }

    /** Summarize a forecast: average daily demand + residual std, with method/confidence. */
    fun summarize(series: List<Double>, season: Int, horizon: Int): Forecast {
        val method = if (series.size >= 2 * season) ForecastMethod.HOLT_WINTERS else ForecastMethod.MOVING_AVG
        val confidence = when {
            series.size >= 2 * season -> Confidence.HIGH
            series.size >= season -> Confidence.MEDIUM
            else -> Confidence.LOW
        }
        val next = forecastNext(series, season, horizon)
        val meanDaily = if (next.isEmpty()) 0.0 else next.average()
        val stdDevDaily = residualStdDev(series, season)
        return Forecast(meanDaily, stdDevDaily, method, confidence)
    }

    /** Additive Holt-Winters; returns [steps] point forecasts past the end of [series]. */
    private fun holtWinters(series: List<Double>, season: Int, steps: Int): List<Double> {
        val n = series.size
        var level = series.take(season).average()
        var trend = (series.slice(season until 2 * season).average() - level) / season
        val seasonal = DoubleArray(season) { series[it] - level }

        for (t in season until n) {
            val s = seasonal[t % season]
            val prevLevel = level
            level = ALPHA * (series[t] - s) + (1 - ALPHA) * (level + trend)
            trend = BETA * (level - prevLevel) + (1 - BETA) * trend
            seasonal[t % season] = GAMMA * (series[t] - level) + (1 - GAMMA) * s
        }
        return (1..steps).map { h -> level + h * trend + seasonal[(n - 1 + h) % season] }
    }

    /** Std-dev of in-sample one-step-ahead residuals (the level+trend+seasonal fit). */
    private fun residualStdDev(series: List<Double>, season: Int): Double {
        if (series.size < 2 * season) {
            val window = series.takeLast(season.coerceAtMost(series.size))
            return sampleStdDev(window)
        }
        val n = series.size
        var level = series.take(season).average()
        var trend = (series.slice(season until 2 * season).average() - level) / season
        val seasonal = DoubleArray(season) { series[it] - level }
        val residuals = ArrayList<Double>(n)
        for (t in season until n) {
            val s = seasonal[t % season]
            val forecast = level + trend + s
            residuals.add(series[t] - forecast)
            val prevLevel = level
            level = ALPHA * (series[t] - s) + (1 - ALPHA) * (level + trend)
            trend = BETA * (level - prevLevel) + (1 - BETA) * trend
            seasonal[t % season] = GAMMA * (series[t] - level) + (1 - GAMMA) * s
        }
        return sampleStdDev(residuals)
    }

    private fun sampleStdDev(xs: List<Double>): Double {
        if (xs.size < 2) return 0.0
        val mean = xs.average()
        val variance = xs.sumOf { (it - mean) * (it - mean) } / (xs.size - 1)
        return sqrt(variance)
    }
}
