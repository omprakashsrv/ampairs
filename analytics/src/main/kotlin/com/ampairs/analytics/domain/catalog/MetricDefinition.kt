package com.ampairs.analytics.domain.catalog

import com.ampairs.analytics.domain.enums.Aggregation
import com.ampairs.analytics.domain.enums.MetricGroup
import com.ampairs.analytics.domain.enums.MetricUnit
import com.ampairs.analytics.domain.enums.Period

/**
 * Declarative description of a single KPI. One source of truth that drives the backend summary
 * roll-up, the dashboard read API, and (on mobile) the on-device DAO query + agent NL mapping (R9).
 */
data class MetricDefinition(
    val id: String,
    val group: MetricGroup,
    val unit: MetricUnit,
    val aggregation: Aggregation,
    val sourceModule: String,
    val periods: Set<Period> = setOf(Period.DAY, Period.WEEK, Period.MONTH),
)

/**
 * P1 metric catalog. Kept in code (no DB table) so the same definitions are reused across layers.
 */
object MetricCatalog {

    val ALL: List<MetricDefinition> = listOf(
        // SALES
        MetricDefinition("sales.gross", MetricGroup.SALES, MetricUnit.MONEY, Aggregation.SUM, "invoice"),
        MetricDefinition("sales.net", MetricGroup.SALES, MetricUnit.MONEY, Aggregation.SUM, "invoice"),
        MetricDefinition("sales.tax", MetricGroup.SALES, MetricUnit.MONEY, Aggregation.SUM, "invoice"),
        MetricDefinition("sales.count", MetricGroup.SALES, MetricUnit.COUNT, Aggregation.COUNT, "invoice"),
        MetricDefinition("sales.aov", MetricGroup.SALES, MetricUnit.MONEY, Aggregation.RATIO, "invoice"),
        // COLLECTIONS
        MetricDefinition("collections.collected", MetricGroup.COLLECTIONS, MetricUnit.MONEY, Aggregation.SUM, "payment"),
        MetricDefinition("collections.outstanding", MetricGroup.COLLECTIONS, MetricUnit.MONEY, Aggregation.SUM, "payment"),
        MetricDefinition("collections.aging", MetricGroup.AGING, MetricUnit.MONEY, Aggregation.SUM, "payment"),
        // TOP-N
        MetricDefinition("top.product", MetricGroup.TOP_PRODUCT, MetricUnit.MONEY, Aggregation.SUM, "invoice"),
        MetricDefinition("top.customer", MetricGroup.TOP_CUSTOMER, MetricUnit.MONEY, Aggregation.SUM, "invoice"),
        // GST
        MetricDefinition("gst.output_by_rate", MetricGroup.GST_SUMMARY, MetricUnit.MONEY, Aggregation.SUM, "invoice"),
        MetricDefinition("gst.cgst_sgst", MetricGroup.GST_SUMMARY, MetricUnit.MONEY, Aggregation.SUM, "invoice"),
        MetricDefinition("gst.igst", MetricGroup.GST_SUMMARY, MetricUnit.MONEY, Aggregation.SUM, "invoice"),
        // INVENTORY
        MetricDefinition("inventory.stock_value", MetricGroup.INVENTORY, MetricUnit.MONEY, Aggregation.SUM, "inventory"),
        MetricDefinition("inventory.low_stock_count", MetricGroup.INVENTORY, MetricUnit.COUNT, Aggregation.COUNT, "inventory"),
        MetricDefinition("inventory.turns", MetricGroup.INVENTORY, MetricUnit.RATIO, Aggregation.RATIO, "inventory"),
    )

    private val byId: Map<String, MetricDefinition> = ALL.associateBy { it.id }

    fun byId(id: String): MetricDefinition? = byId[id]

    fun byGroup(group: MetricGroup): List<MetricDefinition> = ALL.filter { it.group == group }
}
