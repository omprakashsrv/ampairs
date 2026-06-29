package com.ampairs.dms.domain.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.dms.config.Constants
import com.ampairs.dms.domain.enums.SnapshotGrain
import com.ampairs.dms.domain.enums.TargetTier
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

/**
 * A published, recomputable rollup of distributor→retailer sales **attributed to a brand** (Hop A,
 * point-in-time), itemized by the brand SKU where Hop B mapped else the aggregated "unmapped" bucket.
 * BaseDomain (cross-tenant): carries explicit brand + distributor workspace ids; brand reads are gated.
 */
@Entity
@Table(
    name = "secondary_sales_snapshots",
    indexes = [
        Index(name = "idx_sss_brand", columnList = "attributed_brand_workspace_id"),
        Index(name = "idx_sss_distributor", columnList = "distributor_workspace_id"),
        Index(name = "idx_sss_period", columnList = "period_key"),
    ],
)
class SecondarySalesSnapshot : BaseDomain() {

    @Column(name = "attributed_brand_workspace_id", nullable = false, length = 40)
    var attributedBrandWorkspaceId: String = ""

    @Column(name = "distributor_workspace_id", nullable = false, length = 40)
    var distributorWorkspaceId: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "grain", nullable = false, length = 20)
    var grain: SnapshotGrain = SnapshotGrain.SKU_PERIOD

    @Column(name = "period_key", nullable = false, length = 20)
    var periodKey: String = ""

    @Column(name = "area_code", length = 20)
    var areaCode: String? = null

    /** Null when the row is the aggregated "unmapped" (Hop-A attributed, not Hop-B SKU-mapped) bucket. */
    @Column(name = "brand_product_uid", length = 40)
    var brandProductUid: String? = null

    @Column(name = "brand_sku_code", length = 80)
    var brandSkuCode: String? = null

    @Column(name = "quantity", nullable = false)
    var quantity: Double = 0.0

    @Column(name = "value_amount", nullable = false, precision = 19, scale = 4)
    var valueAmount: BigDecimal = BigDecimal.ZERO

    @Column(name = "version", nullable = false)
    var version: Int = 1

    override fun obtainSeqIdPrefix(): String = Constants.SECONDARY_SALES_PREFIX
}

/** A published, point-in-time rollup of a distributor's on-hand stock, attributed to a brand. */
@Entity
@Table(
    name = "distributor_stock_snapshots",
    indexes = [
        Index(name = "idx_dss_brand", columnList = "attributed_brand_workspace_id"),
        Index(name = "idx_dss_distributor", columnList = "distributor_workspace_id"),
    ],
)
class DistributorStockSnapshot : BaseDomain() {

    @Column(name = "attributed_brand_workspace_id", nullable = false, length = 40)
    var attributedBrandWorkspaceId: String = ""

    @Column(name = "distributor_workspace_id", nullable = false, length = 40)
    var distributorWorkspaceId: String = ""

    @Column(name = "brand_product_uid", length = 40)
    var brandProductUid: String? = null

    @Column(name = "brand_sku_code", length = 80)
    var brandSkuCode: String? = null

    @Column(name = "on_hand_quantity", nullable = false)
    var onHandQuantity: Double = 0.0

    @Column(name = "as_of", nullable = false)
    var asOf: Instant = Instant.EPOCH

    @Column(name = "version", nullable = false)
    var version: Int = 1

    override fun obtainSeqIdPrefix(): String = Constants.DISTRIBUTOR_STOCK_PREFIX
}

/** A sales target by tier and grain; achievement is derived from snapshots (never stored). */
@Entity
@Table(
    name = "sales_targets",
    indexes = [
        Index(name = "idx_target_brand", columnList = "brand_workspace_id"),
        Index(name = "idx_target_distributor", columnList = "distributor_workspace_id"),
        Index(name = "idx_target_period", columnList = "period_key"),
    ],
)
class SalesTarget : BaseDomain() {

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    var tier: TargetTier = TargetTier.PRIMARY

    @Column(name = "brand_workspace_id", nullable = false, length = 40)
    var brandWorkspaceId: String = ""

    @Column(name = "distributor_workspace_id", length = 40)
    var distributorWorkspaceId: String? = null

    @Column(name = "rep_member_uid", length = 40)
    var repMemberUid: String? = null

    @Column(name = "period_key", nullable = false, length = 20)
    var periodKey: String = ""

    @Column(name = "brand_product_uid", length = 40)
    var brandProductUid: String? = null

    @Column(name = "area_code", length = 20)
    var areaCode: String? = null

    @Column(name = "target_quantity", nullable = false)
    var targetQuantity: Double = 0.0

    @Column(name = "target_value", nullable = false, precision = 19, scale = 4)
    var targetValue: BigDecimal = BigDecimal.ZERO

    override fun obtainSeqIdPrefix(): String = Constants.SALES_TARGET_PREFIX
}
