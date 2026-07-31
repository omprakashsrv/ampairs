package com.ampairs.payment.domain.dto

import java.math.BigDecimal

/**
 * Entity-free collections/aging snapshot for the `analytics` module (Principle IX). Exposes only what
 * the dashboard needs — total open receivable and the aging-bucket breakdown — not the full
 * [AgingSummaryResponse] (credit-limit alerts, payables, etc.).
 */
data class CollectionsAgingProjection(
    val totalOutstanding: BigDecimal,
    val buckets: List<AgingSlice>,
)

data class AgingSlice(
    val label: String,
    val amount: BigDecimal,
)
