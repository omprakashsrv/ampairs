package com.ampairs.analytics.domain.dto

import com.ampairs.analytics.domain.enums.AgingBucket
import com.ampairs.analytics.domain.model.DemandForecast
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Response DTOs for the analytics dashboard read API. Entities are never exposed directly
 * (Principle II); fields serialize as snake_case via the global Jackson strategy (Principle III).
 */

data class KpiValueResponse(
    val metricId: String,
    val unit: String,
    val value: BigDecimal,
)

data class KpiResponse(
    val metricGroup: String,
    val period: String,
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val currencyCode: String,
    val values: List<KpiValueResponse>,
    val computedFrom: Instant?,
)

data class TrendPointResponse(
    val bucketStart: LocalDate,
    val bucketLabel: String,
    val value: BigDecimal,
)

data class AgingBucketResponse(
    val bucket: String,
    val amount: BigDecimal,
    val docCount: Int,
)

data class AgingResponse(
    val asOfDate: LocalDate,
    val currencyCode: String,
    val totalOutstanding: BigDecimal,
    val buckets: List<AgingBucketResponse>,
)

data class IntraSplit(val cgst: BigDecimal, val sgst: BigDecimal)
data class InterSplit(val igst: BigDecimal)
data class GstRateRow(
    val taxRate: BigDecimal,
    val taxableValue: BigDecimal,
    val taxAmount: BigDecimal,
    val kind: String,
)

data class GstSummaryResponse(
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val currencyCode: String,
    val taxableValue: BigDecimal,
    val totalTax: BigDecimal,
    val intraState: IntraSplit,
    val interState: InterSplit,
    val byRate: List<GstRateRow>,
)

data class TopEntryResponse(
    val rank: Int,
    val id: String,
    val name: String,
    val grossAmount: BigDecimal,
    val qty: BigDecimal,
    val docCount: Int,
)

data class RecomputeResultResponse(
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val daysRecomputed: Int,
    val rowsUpserted: Int,
    val durationMs: Long,
)

data class DemandForecastResponse(
    val uid: String,
    val productId: String,
    val periodStart: LocalDate,
    val horizon: Int,
    val meanQty: BigDecimal,
    val stdDevQty: BigDecimal,
    val method: String,
    val confidence: String,
    val generatedAt: Instant,
    val updatedAt: Instant?,
    val active: Boolean,
)

fun DemandForecast.asResponse(): DemandForecastResponse =
    DemandForecastResponse(
        uid = uid,
        productId = productId,
        periodStart = periodStart,
        horizon = horizon,
        meanQty = meanQty,
        stdDevQty = stdDevQty,
        method = method.name,
        confidence = confidence.name,
        generatedAt = generatedAt,
        updatedAt = updatedAt,
        active = true,
    )

/** Ordered, fixed set of aging buckets so the response always lists all five (zero-filled). */
val ALL_AGING_BUCKETS: List<AgingBucket> = listOf(
    AgingBucket.CURRENT,
    AgingBucket.D1_30,
    AgingBucket.D31_60,
    AgingBucket.D61_90,
    AgingBucket.D90_PLUS,
)
