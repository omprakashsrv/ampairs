package com.ampairs.analytics.service

import com.ampairs.analytics.domain.enums.MetricGroup
import com.ampairs.analytics.domain.enums.Period
import com.ampairs.analytics.domain.model.KpiDailySummary
import com.ampairs.analytics.repository.KpiDailySummaryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Maintains the materialized [KpiDailySummary] read model from domain events (R2).
 *
 * **Current scope (P1, Option B — coarse, event-driven SALES):** the platform's existing
 * `InvoiceFinalizedEvent` carries only a single `Double totalAmount` (no per-line net/tax split, no
 * per-product/customer dimension), so this service rolls up the **SALES headline bucket** (gross +
 * document count) for the invoice's business day. Net/tax, GST split, top-N and inventory buckets
 * require reading source rows from the owning modules and are intentionally deferred to a reconcile
 * pass backed by public read-service interfaces (see tasks T023–T028, "Option A").
 *
 * **Idempotence caveat:** the event path is at-least-once and additive, so it does not self-heal
 * against redelivery, backdated edits, or cancellations (the cancel event carries no amount). The
 * nightly reconcile (not yet implemented) is what makes the summary authoritative; until then SALES
 * figures track finalize events only.
 */
@Service
class KpiRollupService(
    private val summaryRepository: KpiDailySummaryRepository,
    private val timeZoneProvider: BusinessTimeZoneProvider,
) {
    private val log = LoggerFactory.getLogger(KpiRollupService::class.java)

    /**
     * Roll a finalized invoice into the SALES headline bucket for its business day.
     * Tenant context must already be set by the caller (the event listener).
     */
    @Transactional
    fun applyInvoiceFinalized(totalAmount: Double, invoiceDateEpochMillis: Long) {
        val businessDate = businessDateOf(invoiceDateEpochMillis)
        val bucket = upsertBucket(MetricGroup.SALES, businessDate)
        bucket.grossAmount = bucket.grossAmount.add(BigDecimal.valueOf(totalAmount))
        bucket.docCount += 1
        bucket.recomputedAt = Instant.now()
        summaryRepository.save(bucket)
        log.debug("Rolled invoice ({}) into SALES bucket {}", totalAmount, businessDate)
    }

    /** Bucket the document instant into the workspace business day (R7), never device/UTC. */
    fun businessDateOf(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(timeZoneProvider.currentZone()).toLocalDate()

    /** Find the headline (dimensionless) bucket for a group/day, or create a fresh one. */
    private fun upsertBucket(group: MetricGroup, date: LocalDate): KpiDailySummary =
        summaryRepository.findByMetricGroupAndBusinessDateAndDimProductIdAndDimCustomerId(
            group, date, "", "",
        ) ?: KpiDailySummary().apply {
            metricGroup = group
            businessDate = date
            period = Period.DAY
            dimProductId = ""
            dimCustomerId = ""
        }
}
