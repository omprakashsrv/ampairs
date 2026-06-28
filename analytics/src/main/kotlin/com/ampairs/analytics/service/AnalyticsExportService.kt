package com.ampairs.analytics.service

import com.ampairs.analytics.domain.enums.MetricGroup
import com.ampairs.analytics.domain.enums.Period
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Server-side CSV export of the dashboard KPIs for a period (P3 / T051 / R11), for history beyond the
 * device sync window. Reuses [DashboardReadService] so server and on-device exports agree. PDF is
 * deferred — CSV is sufficient for P1 and avoids a new dependency.
 */
@Service
@Transactional(readOnly = true)
class AnalyticsExportService(
    private val readService: DashboardReadService,
) {

    /** Build a flat `section,metric,value,currency` CSV across all P1 KPI groups for [from]..[to]. */
    fun exportCsv(from: LocalDate, to: LocalDate, period: Period): String {
        val sb = StringBuilder()
        sb.append("section,metric,value,currency\n")

        val sales = readService.kpis(MetricGroup.SALES, from, to, period)
        sales.values.forEach { row("SALES", it.metricId, it.value.toPlainString(), sales.currencyCode, sb) }

        val collections = readService.kpis(MetricGroup.COLLECTIONS, from, to, period)
        collections.values.forEach { row("COLLECTIONS", it.metricId, it.value.toPlainString(), collections.currencyCode, sb) }

        val inventory = readService.kpis(MetricGroup.INVENTORY, from, to, period)
        inventory.values.forEach { row("INVENTORY", it.metricId, it.value.toPlainString(), inventory.currencyCode, sb) }

        val gst = readService.gstSummary(from, to)
        row("GST", "taxable_value", gst.taxableValue.toPlainString(), gst.currencyCode, sb)
        row("GST", "total_tax", gst.totalTax.toPlainString(), gst.currencyCode, sb)
        row("GST", "cgst", gst.intraState.cgst.toPlainString(), gst.currencyCode, sb)
        row("GST", "sgst", gst.intraState.sgst.toPlainString(), gst.currencyCode, sb)
        row("GST", "igst", gst.interState.igst.toPlainString(), gst.currencyCode, sb)

        readService.top("product", from, to, 10).forEach {
            row("TOP_PRODUCT", it.id, it.grossAmount.toPlainString(), gst.currencyCode, sb)
        }
        readService.top("customer", from, to, 10).forEach {
            row("TOP_CUSTOMER", it.id, it.grossAmount.toPlainString(), gst.currencyCode, sb)
        }

        val aging = readService.aging(to)
        aging.buckets.forEach { row("AGING", it.bucket, it.amount.toPlainString(), aging.currencyCode, sb) }

        return sb.toString()
    }

    private fun row(section: String, metric: String, value: String, currency: String, sb: StringBuilder) {
        sb.append(escape(section)).append(',')
            .append(escape(metric)).append(',')
            .append(escape(value)).append(',')
            .append(escape(currency)).append('\n')
    }

    /** Minimal CSV escaping: quote fields containing a comma, quote, or newline. */
    private fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' }) "\"" + field.replace("\"", "\"\"") + "\"" else field
}
