package com.ampairs.analytics.service

import com.ampairs.analytics.repository.DemandForecastRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.sqrt

/**
 * Public demand signal for a product, derived from its most recent [com.ampairs.analytics.domain.model.DemandForecast]
 * (R6 / FR-018). Replenishment (feature 027) and inventory consume this to compute reorder point /
 * safety stock; analytics only measures and signals — it never writes their tables.
 *
 * The stored forecast is horizon-total (mean/std over the whole horizon); this exposes the
 * per-day figures replenishment math expects.
 */
@Service
@Transactional(readOnly = true)
class DemandSignalService(
    private val forecastRepository: DemandForecastRepository,
) {

    /** Expected average daily demand for [productId], or null if no forecast exists. */
    fun averageDailyDemand(productId: String): BigDecimal? {
        val f = forecastRepository.findFirstByProductIdOrderByPeriodStartDesc(productId) ?: return null
        if (f.horizon <= 0) return null
        return f.meanQty.divide(BigDecimal.valueOf(f.horizon.toLong()), 3, RoundingMode.HALF_UP)
    }

    /** Per-day demand variability (std dev) for [productId], or null if no forecast exists. */
    fun demandVariability(productId: String): BigDecimal? {
        val f = forecastRepository.findFirstByProductIdOrderByPeriodStartDesc(productId) ?: return null
        if (f.horizon <= 0) return null
        // Variance adds over the horizon, so daily std = horizon-total std / sqrt(horizon).
        val dailyStd = f.stdDevQty.toDouble() / sqrt(f.horizon.toDouble())
        return BigDecimal.valueOf(dailyStd).setScale(3, RoundingMode.HALF_UP)
    }
}
