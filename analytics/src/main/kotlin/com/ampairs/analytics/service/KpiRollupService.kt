package com.ampairs.analytics.service

import com.ampairs.analytics.domain.enums.MetricGroup
import com.ampairs.analytics.domain.enums.Period
import com.ampairs.analytics.domain.enums.TaxKind
import com.ampairs.analytics.domain.model.KpiDailySummary
import com.ampairs.analytics.repository.KpiDailySummaryRepository
import com.ampairs.invoice.domain.dto.FinalizedInvoiceProjection
import com.ampairs.invoice.service.InvoiceAnalyticsQueryService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Maintains the materialized [KpiDailySummary] read model (R1/R2) from invoice source data via the
 * `invoice` module's public read service (Principle IX — never the entity/repository).
 *
 * The unit of work is a **range reconcile**: delete the invoice-derived buckets in a date range and
 * rebuild them from the finalized invoices in that range. This is **idempotent** (re-running yields
 * identical rows — SC-004), self-heals against missed events and backdated edits, and excludes
 * drafts/de-finalized invoices by construction (FR-013/FR-014). The event listener reconciles the one
 * affected business day; the nightly batch reconciles a trailing window.
 *
 * Groups rebuilt from invoices: SALES (gross/net/tax/count), GST_SUMMARY (per rate × intra/inter),
 * TOP_CUSTOMER (gross/count per customer). Collections/aging (payment), inventory, and top-PRODUCT
 * (needs invoice items) are added by their own reconcile sources in later increments.
 */
@Service
class KpiRollupService(
    private val summaryRepository: KpiDailySummaryRepository,
    private val timeZoneProvider: BusinessTimeZoneProvider,
    private val invoiceQueryService: InvoiceAnalyticsQueryService,
) {
    private val log = LoggerFactory.getLogger(KpiRollupService::class.java)

    /** Groups owned by the invoice reconcile (cleared + rebuilt together). */
    private val invoiceGroups = listOf(MetricGroup.SALES, MetricGroup.GST_SUMMARY, MetricGroup.TOP_CUSTOMER)

    /** Reconcile the single business day a finalized invoice belongs to (called by the event listener). */
    @Transactional
    fun reconcileDayOf(invoiceDateEpochMillis: Long) {
        val day = businessDateOf(invoiceDateEpochMillis)
        reconcile(day, day)
    }

    /**
     * Rebuild invoice-derived KPI buckets for [fromDate, toDate] (inclusive) from source invoices.
     * @return number of summary rows written.
     */
    @Transactional
    fun reconcile(fromDate: LocalDate, toDate: LocalDate): Int {
        val zone = timeZoneProvider.currentZone()
        val windowStart = fromDate.atStartOfDay(zone).toInstant()
        val windowEnd = toDate.plusDays(1).atStartOfDay(zone).toInstant()

        summaryRepository.deleteByMetricGroupInAndBusinessDateBetween(invoiceGroups, fromDate, toDate)

        val invoices = invoiceQueryService.finalizedBetween(windowStart, windowEnd)
        val buckets = HashMap<BucketKey, KpiDailySummary>()
        val now = Instant.now()

        for (inv in invoices) {
            val day = Instant.ofEpochMilli(inv.invoiceDateEpochMillis).atZone(zone).toLocalDate()
            accumulateSales(buckets, day, inv, now)
            accumulateTopCustomer(buckets, day, inv, now)
            accumulateGst(buckets, day, inv, now)
        }

        val saved = summaryRepository.saveAll(buckets.values).count()
        log.debug("Reconciled {}..{}: {} invoices → {} buckets", fromDate, toDate, invoices.size, saved)
        return saved
    }

    private fun accumulateSales(
        buckets: MutableMap<BucketKey, KpiDailySummary>, day: LocalDate,
        inv: FinalizedInvoiceProjection, now: Instant,
    ) {
        val b = buckets.getOrPut(BucketKey(MetricGroup.SALES, day, "", "", null, null)) {
            newBucket(MetricGroup.SALES, day, now)
        }
        b.grossAmount = b.grossAmount.add(BigDecimal.valueOf(inv.gross))
        b.netAmount = b.netAmount.add(BigDecimal.valueOf(inv.net))
        b.taxAmount = b.taxAmount.add(BigDecimal.valueOf(inv.tax))
        b.docCount += 1
    }

    private fun accumulateTopCustomer(
        buckets: MutableMap<BucketKey, KpiDailySummary>, day: LocalDate,
        inv: FinalizedInvoiceProjection, now: Instant,
    ) {
        if (inv.customerId.isBlank()) return
        val b = buckets.getOrPut(BucketKey(MetricGroup.TOP_CUSTOMER, day, "", inv.customerId, null, null)) {
            newBucket(MetricGroup.TOP_CUSTOMER, day, now).apply { dimCustomerId = inv.customerId }
        }
        b.grossAmount = b.grossAmount.add(BigDecimal.valueOf(inv.gross))
        b.docCount += 1
    }

    private fun accumulateGst(
        buckets: MutableMap<BucketKey, KpiDailySummary>, day: LocalDate,
        inv: FinalizedInvoiceProjection, now: Instant,
    ) {
        val kind = if (inv.intraState) TaxKind.INTRA else TaxKind.INTER
        for (line in inv.taxLines) {
            val rate = BigDecimal.valueOf(line.rate)
            val b = buckets.getOrPut(BucketKey(MetricGroup.GST_SUMMARY, day, "", "", rate, kind)) {
                newBucket(MetricGroup.GST_SUMMARY, day, now).apply { taxRate = rate; taxKind = kind }
            }
            // taxable value implied by the rate; tax amount taken from the invoice snapshot.
            val taxable = if (line.rate != 0.0) line.taxValue / (line.rate / 100.0) else 0.0
            b.grossAmount = b.grossAmount.add(BigDecimal.valueOf(taxable))
            b.taxAmount = b.taxAmount.add(BigDecimal.valueOf(line.taxValue))
        }
    }

    private fun newBucket(group: MetricGroup, day: LocalDate, now: Instant) = KpiDailySummary().apply {
        metricGroup = group
        businessDate = day
        period = Period.DAY
        dimProductId = ""
        dimCustomerId = ""
        recomputedAt = now
    }

    /** Bucket a document instant into the workspace business day (R7), never device/UTC. */
    fun businessDateOf(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(timeZoneProvider.currentZone()).toLocalDate()

    private data class BucketKey(
        val group: MetricGroup,
        val day: LocalDate,
        val productId: String,
        val customerId: String,
        val rate: BigDecimal?,
        val kind: TaxKind?,
    )
}
