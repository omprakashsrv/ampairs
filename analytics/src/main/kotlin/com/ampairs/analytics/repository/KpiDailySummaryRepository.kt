package com.ampairs.analytics.repository

import com.ampairs.analytics.domain.enums.MetricGroup
import com.ampairs.analytics.domain.model.KpiDailySummary
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * Persistence for the materialized KPI read model. `@TenantId` on the entity scopes every query to
 * the current workspace automatically — no explicit `ownerId` parameter (multi-tenancy rule).
 */
@Repository
interface KpiDailySummaryRepository : JpaRepository<KpiDailySummary, Long> {

    /** Rows for a metric group within an inclusive business-date range (drives KPI/trend reads). */
    fun findByMetricGroupAndBusinessDateBetween(
        metricGroup: MetricGroup,
        fromDate: LocalDate,
        toDate: LocalDate,
    ): List<KpiDailySummary>

    /** All rows on a single business date — used by the reconcile sweep to recompute a day. */
    fun findByBusinessDate(businessDate: LocalDate): List<KpiDailySummary>

    /** Resolve a specific bucket by its full unique key for in-place upsert. */
    fun findByMetricGroupAndBusinessDateAndDimProductIdAndDimCustomerId(
        metricGroup: MetricGroup,
        businessDate: LocalDate,
        dimProductId: String,
        dimCustomerId: String,
    ): KpiDailySummary?

    /** Rows for several metric groups within an inclusive date range (drives top-N / GST reads). */
    fun findByMetricGroupInAndBusinessDateBetween(
        metricGroups: Collection<MetricGroup>,
        fromDate: LocalDate,
        toDate: LocalDate,
    ): List<KpiDailySummary>

    /** Clear the buckets a reconcile is about to rebuild, so the rebuild is idempotent. */
    fun deleteByMetricGroupInAndBusinessDateBetween(
        metricGroups: Collection<MetricGroup>,
        fromDate: LocalDate,
        toDate: LocalDate,
    )
}
