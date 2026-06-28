package com.ampairs.analytics.service

import com.ampairs.analytics.domain.forecast.DemandForecasting
import com.ampairs.analytics.domain.model.DemandForecast
import com.ampairs.analytics.repository.DemandForecastRepository
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.event.domain.events.DemandForecastUpdatedEvent
import com.ampairs.invoice.service.InvoiceAnalyticsQueryService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlin.math.sqrt

/**
 * Produces per-product [DemandForecast] rows (R5/P2) from the daily sales series of finalized invoices.
 * Runs per workspace (tenant context set by the caller). Persisted forecasts are exposed read-only to
 * mobile via the `/forecasts/sync` feed.
 *
 * Trigger: today this is invoked on demand (`POST /analytics/v1/forecasts/recompute`, per-request
 * tenant). A nightly cross-tenant `@Scheduled` batch is deferred along with T025 (both need a workspace
 * enumerator + per-tenant context loop).
 */
@Service
class ForecastService(
    private val invoiceQueryService: InvoiceAnalyticsQueryService,
    private val forecastRepository: DemandForecastRepository,
    private val timeZoneProvider: BusinessTimeZoneProvider,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(ForecastService::class.java)

    private val season = 7 // weekly seasonality

    /**
     * Fit a forecast for every product sold in the trailing [lookbackDays] and upsert a horizon-day
     * demand row starting tomorrow. Returns the number of products forecast.
     */
    @Transactional
    fun recompute(lookbackDays: Int = 90, horizonDays: Int = 7): Int {
        val zone = timeZoneProvider.currentZone()
        val today = Instant.now().atZone(zone).toLocalDate()
        val from = today.minusDays((lookbackDays - 1).toLong())
        val windowStart = from.atStartOfDay(zone).toInstant()
        val windowEnd = today.plusDays(1).atStartOfDay(zone).toInstant()

        // product → (business day → qty sold)
        val perProductDaily = HashMap<String, HashMap<LocalDate, Double>>()
        for (inv in invoiceQueryService.finalizedBetween(windowStart, windowEnd)) {
            val day = Instant.ofEpochMilli(inv.invoiceDateEpochMillis).atZone(zone).toLocalDate()
            for (line in inv.lines) {
                if (line.productId.isBlank()) continue
                val byDay = perProductDaily.getOrPut(line.productId) { HashMap() }
                byDay[day] = (byDay[day] ?: 0.0) + line.qty
            }
        }

        val periodStart = today.plusDays(1)
        val now = Instant.now()
        var count = 0
        for ((productId, byDay) in perProductDaily) {
            val series = (0 until lookbackDays).map { i -> byDay[from.plusDays(i.toLong())] ?: 0.0 }
            val f = DemandForecasting.summarize(series, season, horizonDays)
            val meanTotal = f.meanDaily * horizonDays
            val stdTotal = f.stdDevDaily * sqrt(horizonDays.toDouble()) // variance adds over the horizon
            upsert(productId, periodStart, horizonDays, meanTotal, stdTotal, f, now)
            count++
        }
        if (count > 0) {
            // Signal replenishment/inventory (FR-018) — they pull per-product figures from
            // DemandSignalService. workspaceId read (not set) from the active tenant context.
            val workspaceId = TenantContextHolder.getCurrentTenant() ?: ""
            eventPublisher.publishEvent(
                DemandForecastUpdatedEvent(
                    source = this,
                    workspaceId = workspaceId,
                    entityId = workspaceId,
                    userId = "system",
                    deviceId = "",
                    productCount = count,
                    generatedAtEpochMillis = now.toEpochMilli(),
                ),
            )
        }
        log.debug("Forecast recompute: {} products over {}d lookback", count, lookbackDays)
        return count
    }

    private fun upsert(
        productId: String, periodStart: LocalDate, horizon: Int,
        meanTotal: Double, stdTotal: Double, f: DemandForecasting.Forecast, now: Instant,
    ) {
        val entity = forecastRepository.findByProductIdAndPeriodStartAndHorizon(productId, periodStart, horizon)
            ?: DemandForecast().apply {
                this.productId = productId
                this.periodStart = periodStart
                this.horizon = horizon
            }
        entity.meanQty = BigDecimal.valueOf(meanTotal)
        entity.stdDevQty = BigDecimal.valueOf(stdTotal)
        entity.method = f.method
        entity.confidence = f.confidence
        entity.generatedAt = now
        forecastRepository.save(entity)
    }
}
