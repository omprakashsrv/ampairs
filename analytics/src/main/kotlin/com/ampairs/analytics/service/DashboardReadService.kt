package com.ampairs.analytics.service

import com.ampairs.analytics.domain.dto.AgingBucketResponse
import com.ampairs.analytics.domain.dto.AgingResponse
import com.ampairs.analytics.domain.dto.GstRateRow
import com.ampairs.analytics.domain.dto.GstSummaryResponse
import com.ampairs.analytics.domain.dto.InterSplit
import com.ampairs.analytics.domain.dto.IntraSplit
import com.ampairs.analytics.domain.dto.KpiResponse
import com.ampairs.analytics.domain.dto.KpiValueResponse
import com.ampairs.analytics.domain.dto.TopEntryResponse
import com.ampairs.analytics.domain.dto.TrendPointResponse
import com.ampairs.analytics.domain.enums.MetricGroup
import com.ampairs.analytics.domain.enums.Period
import com.ampairs.analytics.domain.enums.TaxKind
import com.ampairs.analytics.domain.model.KpiDailySummary
import com.ampairs.analytics.repository.KpiDailySummaryRepository
import com.ampairs.business.service.BusinessService
import com.ampairs.inventory.service.InventoryAnalyticsQueryService
import com.ampairs.payment.service.PaymentAnalyticsQueryService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

/**
 * Serves dashboard reads from the materialized [KpiDailySummary] (R1). Backs the invoice-derived
 * groups (SALES, GST_SUMMARY, TOP_CUSTOMER) populated by [KpiRollupService]; other groups are added
 * as their reconcile sources land.
 */
@Service
@Transactional(readOnly = true)
class DashboardReadService(
    private val summaryRepository: KpiDailySummaryRepository,
    private val businessService: BusinessService,
    private val paymentQueryService: PaymentAnalyticsQueryService,
    private val inventoryQueryService: InventoryAnalyticsQueryService,
    private val timeZoneProvider: BusinessTimeZoneProvider,
) {

    fun kpis(group: MetricGroup, from: LocalDate, to: LocalDate, period: Period): KpiResponse {
        // COLLECTIONS and INVENTORY are read live from their owning modules (point-in-time), not the
        // materialized summary table.
        if (group == MetricGroup.COLLECTIONS || group == MetricGroup.INVENTORY) {
            val values = if (group == MetricGroup.COLLECTIONS) collectionsValues(from, to) else inventoryValues(from, to)
            return KpiResponse(
                metricGroup = group.name, period = period.name, fromDate = from, toDate = to,
                currencyCode = currencyCode(), values = values, computedFrom = null,
            )
        }
        val rows = summaryRepository.findByMetricGroupAndBusinessDateBetween(group, from, to)
        val values = when (group) {
            MetricGroup.SALES -> salesValues(rows)
            else -> emptyList()
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

    /** Receivables aging snapshot as of [asOf] (null → business "today"), live from the payment module. */
    fun aging(asOf: LocalDate?): AgingResponse {
        val zone = timeZoneProvider.currentZone()
        val day = asOf ?: Instant.now().atZone(zone).toLocalDate() // "today" in the business timezone
        val asOfInstant = day.plusDays(1).atStartOfDay(zone).toInstant() // end of the business day
        val snapshot = paymentQueryService.collectionsAging(asOfInstant)
        return AgingResponse(
            asOfDate = day,
            currencyCode = currencyCode(),
            totalOutstanding = snapshot.totalOutstanding,
            buckets = snapshot.buckets.map { AgingBucketResponse(bucket = it.label, amount = it.amount, docCount = 0) },
        )
    }

    private fun collectionsValues(from: LocalDate, to: LocalDate): List<KpiValueResponse> {
        val zone = timeZoneProvider.currentZone()
        val windowStart = from.atStartOfDay(zone).toInstant()
        val windowEnd = to.plusDays(1).atStartOfDay(zone).toInstant()
        val collected = paymentQueryService.collectedBetween(windowStart, windowEnd)
        val outstanding = paymentQueryService.collectionsAging(windowEnd).totalOutstanding
        return listOf(
            KpiValueResponse("collections.collected", "MONEY", collected),
            KpiValueResponse("collections.outstanding", "MONEY", outstanding),
        )
    }

    private fun inventoryValues(from: LocalDate, to: LocalDate): List<KpiValueResponse> {
        val zone = timeZoneProvider.currentZone()
        val windowStart = from.atStartOfDay(zone).toInstant()
        val windowEnd = to.plusDays(1).atStartOfDay(zone).toInstant()
        return listOf(
            KpiValueResponse("inventory.stock_value", "MONEY", inventoryQueryService.totalStockValue()),
            KpiValueResponse("inventory.low_stock_count", "COUNT", BigDecimal.valueOf(inventoryQueryService.lowStockCount())),
            KpiValueResponse("inventory.turns", "RATIO", inventoryQueryService.inventoryTurns(windowStart, windowEnd)),
        )
    }

    fun trend(metricId: String, from: LocalDate, to: LocalDate, period: Period): List<TrendPointResponse> {
        val rows = summaryRepository.findByMetricGroupAndBusinessDateBetween(MetricGroup.SALES, from, to)
        val measure: (KpiDailySummary) -> BigDecimal = when (metricId) {
            "sales.count" -> { r -> BigDecimal.valueOf(r.docCount.toLong()) }
            "sales.net" -> { r -> r.netAmount }
            "sales.tax" -> { r -> r.taxAmount }
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

    fun gstSummary(from: LocalDate, to: LocalDate): GstSummaryResponse {
        val rows = summaryRepository.findByMetricGroupAndBusinessDateBetween(MetricGroup.GST_SUMMARY, from, to)
        val taxable = rows.fold(BigDecimal.ZERO) { a, r -> a.add(r.grossAmount) }
        val totalTax = rows.fold(BigDecimal.ZERO) { a, r -> a.add(r.taxAmount) }
        val intraTax = rows.filter { it.taxKind == TaxKind.INTRA }.fold(BigDecimal.ZERO) { a, r -> a.add(r.taxAmount) }
        val interTax = rows.filter { it.taxKind == TaxKind.INTER }.fold(BigDecimal.ZERO) { a, r -> a.add(r.taxAmount) }
        val half = intraTax.divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP)
        val byRate = rows
            .groupBy { (it.taxRate ?: BigDecimal.ZERO) to (it.taxKind ?: TaxKind.INTER) }
            .map { (key, group) ->
                GstRateRow(
                    taxRate = key.first,
                    taxableValue = group.fold(BigDecimal.ZERO) { a, r -> a.add(r.grossAmount) },
                    taxAmount = group.fold(BigDecimal.ZERO) { a, r -> a.add(r.taxAmount) },
                    kind = key.second.name,
                )
            }
            .sortedBy { it.taxRate }
        return GstSummaryResponse(
            fromDate = from,
            toDate = to,
            currencyCode = currencyCode(),
            taxableValue = taxable,
            totalTax = totalTax,
            intraState = IntraSplit(cgst = half, sgst = half),
            interState = InterSplit(igst = interTax),
            byRate = byRate,
        )
    }

    /**
     * Top entities by gross revenue for the period. `dimension=customer` is backed today; the entry
     * `name` carries the id (human-readable names need a customer-module lookup, a later increment).
     */
    fun top(dimension: String, from: LocalDate, to: LocalDate, limit: Int): List<TopEntryResponse> {
        val group = if (dimension.equals("product", ignoreCase = true)) MetricGroup.TOP_PRODUCT else MetricGroup.TOP_CUSTOMER
        val rows = summaryRepository.findByMetricGroupAndBusinessDateBetween(group, from, to)
        val keyOf: (KpiDailySummary) -> String =
            if (group == MetricGroup.TOP_PRODUCT) { r -> r.dimProductId } else { r -> r.dimCustomerId }
        return rows
            .filter { keyOf(it).isNotBlank() }
            .groupBy(keyOf)
            .map { (id, g) ->
                Triple(
                    id,
                    g.fold(BigDecimal.ZERO) { a, r -> a.add(r.grossAmount) },
                    g.sumOf { it.docCount },
                )
            }
            .sortedByDescending { it.second }
            .take(limit.coerceIn(1, 50))
            .mapIndexed { i, (id, gross, count) ->
                TopEntryResponse(rank = i + 1, id = id, name = id, grossAmount = gross, qty = BigDecimal.ZERO, docCount = count)
            }
    }

    private fun salesValues(rows: List<KpiDailySummary>): List<KpiValueResponse> {
        val gross = rows.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.grossAmount) }
        val net = rows.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.netAmount) }
        val tax = rows.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.taxAmount) }
        val count = rows.sumOf { it.docCount }
        val aov = if (count > 0) gross.divide(BigDecimal.valueOf(count.toLong()), 2, RoundingMode.HALF_UP)
        else BigDecimal.ZERO
        return listOf(
            KpiValueResponse("sales.gross", "MONEY", gross),
            KpiValueResponse("sales.net", "MONEY", net),
            KpiValueResponse("sales.tax", "MONEY", tax),
            KpiValueResponse("sales.count", "COUNT", BigDecimal.valueOf(count.toLong())),
            KpiValueResponse("sales.aov", "MONEY", aov),
        )
    }

    private fun bucketStart(date: LocalDate, period: Period): LocalDate = when (period) {
        Period.DAY -> date
        Period.WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
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
