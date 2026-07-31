package com.ampairs.analytics.domain.enums

/**
 * Metric groups materialized in [com.ampairs.analytics.domain.model.KpiDailySummary].
 * One row per (workspace × businessDate × metricGroup × dimension).
 */
enum class MetricGroup {
    SALES,
    COLLECTIONS,
    AGING,
    TOP_PRODUCT,
    TOP_CUSTOMER,
    GST_SUMMARY,
    INVENTORY,
}

/** Read-time roll-up grain. Storage grain is always DAY; WEEK/MONTH are GROUP BY at read time. */
enum class Period {
    DAY,
    WEEK,
    MONTH,
}

/** GST classification derived from place-of-supply vs seller place (R10). */
enum class TaxKind {
    INTRA, // CGST + SGST (same state)
    INTER, // IGST (different state)
}

/** Receivables aging buckets, computed against the business "today". */
enum class AgingBucket {
    CURRENT,
    D1_30,
    D31_60,
    D61_90,
    D90_PLUS,
}

/** Forecast algorithm used to produce a [com.ampairs.analytics.domain.model.DemandForecast] row. */
enum class ForecastMethod {
    HOLT_WINTERS,
    MOVING_AVG,
}

/** Forecast confidence, derived from the depth of available history. */
enum class Confidence {
    HIGH,
    MEDIUM,
    LOW,
}

/** Unit of a [com.ampairs.analytics.domain.catalog.MetricDefinition] value. */
enum class MetricUnit {
    MONEY,
    COUNT,
    QTY,
    RATIO,
    PERCENT,
}

/** How a metric aggregates its source rows. */
enum class Aggregation {
    SUM,
    COUNT,
    AVG,
    RATIO,
    LAST,
}
