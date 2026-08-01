package com.ampairs.analytics.domain.model

import com.ampairs.analytics.domain.enums.AgingBucket
import com.ampairs.analytics.domain.enums.MetricGroup
import com.ampairs.analytics.domain.enums.Period
import com.ampairs.analytics.domain.enums.TaxKind
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Materialized read-model row: one bucket per (workspace × businessDate × metricGroup × dimension).
 * Fully recomputable from source tables (R1/R2). Money is `DECIMAL(19,4)` in major currency units;
 * the mobile client mirrors it as `Long` minor units after the `/sync` boundary (data-model §money).
 *
 * `businessDate` is bucketed in the workspace business timezone (R7), never UTC/device.
 * Nullable dimension columns use sentinels (`''` for ids) so the unique key is enforceable.
 */
@Entity(name = "kpi_daily_summary")
@Table(
    name = "kpi_daily_summary",
    indexes = [
        Index(name = "idx_kpi_summary_uid", columnList = "uid", unique = true),
        Index(
            name = "ux_kpi_summary_key",
            columnList = "owner_id, business_date, metric_group, dim_product_id, dim_customer_id, tax_rate, tax_kind, aging_bucket",
            unique = true,
        ),
        Index(name = "ix_kpi_summary_read", columnList = "owner_id, metric_group, business_date"),
        Index(name = "ix_kpi_summary_dim_product", columnList = "owner_id, metric_group, dim_product_id, business_date"),
        Index(name = "ix_kpi_summary_dim_customer", columnList = "owner_id, metric_group, dim_customer_id, business_date"),
    ],
)
class KpiDailySummary : OwnableBaseDomain() {

    @Column(name = "business_date", nullable = false)
    var businessDate: LocalDate = LocalDate.EPOCH

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_group", nullable = false, length = 32)
    var metricGroup: MetricGroup = MetricGroup.SALES

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 8)
    var period: Period = Period.DAY

    /** Dimension keys — sentinel `""` when not applicable so the unique index stays enforceable. */
    @Column(name = "dim_product_id", nullable = false, length = 64)
    var dimProductId: String = ""

    @Column(name = "dim_customer_id", nullable = false, length = 64)
    var dimCustomerId: String = ""

    @Column(name = "tax_rate", precision = 7, scale = 4)
    var taxRate: BigDecimal? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_kind", length = 8)
    var taxKind: TaxKind? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "aging_bucket", length = 16)
    var agingBucket: AgingBucket? = null

    @Column(name = "gross_amount", nullable = false, precision = 19, scale = 4)
    var grossAmount: BigDecimal = BigDecimal.ZERO

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 4)
    var netAmount: BigDecimal = BigDecimal.ZERO

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    var taxAmount: BigDecimal = BigDecimal.ZERO

    @Column(name = "qty", nullable = false, precision = 19, scale = 3)
    var qty: BigDecimal = BigDecimal.ZERO

    @Column(name = "doc_count", nullable = false)
    var docCount: Int = 0

    @Column(name = "recomputed_at")
    var recomputedAt: Instant? = null

    override fun obtainSeqIdPrefix(): String = "KPI"
}
