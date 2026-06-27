package com.ampairs.analytics.service

import com.ampairs.analytics.domain.dto.KpiResponse
import com.ampairs.analytics.domain.dto.KpiValueResponse
import com.ampairs.analytics.domain.dto.TrendPointResponse
import com.ampairs.analytics.domain.enums.MetricGroup
import com.ampairs.analytics.domain.enums.Period
import com.ampairs.analytics.domain.model.KpiDailySummary
import com.ampairs.analytics.repository.KpiDailySummaryRepository
import com.ampairs.business.service.BusinessService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

/**
 * Serves dashboard reads from the materialized [KpiDailySummary] (R1). P1 currently exposes the SALES
 * group (the only group the coarse event roll-up populates today — see [KpiRollupService]); the read
 * shape already matches the dashboard contract so adding groups is additive once their buckets exist.
 */
@Service
@Transactional(readOnly = true)
class DashboardReadService(
    private val summaryRepository: KpiDailySummaryRepository,
    private val businessService: BusinessService,
) {

    fun kpis(group: MetricGroup, from: LocalDate, to: LocalDate, period: Period): KpiResponse {
        val rows = summaryRepository.findByMetricGroupAndBusinessDateBetween(group, from, to)
        val values = when (group) {
            MetricGroup.SALES -> salesValues(rows)
            else -> emptyList() // other groups populated once their roll-up/reconcile lands
        }
        return KpiResponse(
            metricGroup = group.name,
            period = period.name,
            fromDate = from,
            toDate = to,
            currencyCode = currencyCode(),
            values = values,
            computedFrom = rows.mapNotNull { it.recomputedAt }.maxOrNull(),
        )
    }

    fun trend(metricId: String, from: LocalDate, to: LocalDate, period: Period): List<TrendPointResponse> {
        // P1: only sales.* metrics are backed; default to gross for any sales metric id.
        val rows = summaryRepository.findByMetricGroupAndBusinessDateBetween(MetricGroup.SALES, from, to)
        val measure: (KpiDailySummary) -> BigDecimal = when (metricId) {
            "sales.count" -> { r -> BigDecimal.valueOf(r.docCount.toLong()) }
            else -> { r -> r.grossAmount }
        }
        return rows
            .groupBy { bucketStart(it.businessDate, period) }
            .toSortedMap()
            .map { (start, group) ->
                TrendPointResponse(
                    bucketStart = start,
                    bucketLabel = bucketLabel(start, period),
                    value = group.fold(BigDecimal.ZERO) { acc, r -> acc.add(measure(r)) },
                )
            }
    }

    private fun salesValues(rows: List<KpiDailySummary>): List<KpiValueResponse> {
        val gross = rows.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.grossAmount) }
        val count = rows.sumOf { it.docCount }
        val aov = if (count > 0) gross.divide(BigDecimal.valueOf(count.toLong()), 2, RoundingMode.HALF_UP)
        else BigDecimal.ZERO
        return listOf(
            KpiValueResponse("sales.gross", "MONEY", gross),
            KpiValueResponse("sales.count", "COUNT", BigDecimal.valueOf(count.toLong())),
            KpiValueResponse("sales.aov", "MONEY", aov),
        )
    }

    private fun bucketStart(date: LocalDate, period: Period): LocalDate = when (period) {
        Period.DAY -> date
        Period.WEEK -> date.with(IsoFields.DAY_OF_WEEK, 1) // Monday of that ISO week
        Period.MONTH -> date.with(TemporalAdjusters.firstDayOfMonth())
    }

    private fun bucketLabel(start: LocalDate, period: Period): String = when (period) {
        Period.DAY -> start.toString()
        Period.WEEK -> "W${start.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)} ${start.year}"
        Period.MONTH -> "${start.month.name.take(3)} ${start.year}"
    }

    private fun currencyCode(): String =
        try {
            businessService.getBusinessProfile().currency.ifBlank { "INR" }
        } catch (e: Exception) {
            "INR"
        }
}
