package com.ampairs.product.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.product.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

/**
 * Effective-dated standard purchase cost for a product (optionally a variant) — the cost-side mirror
 * of sales price-list effective dating, equivalent to Tally's stock-item "Standard Cost" with an
 * "Applicable From" date.
 *
 * A purchase voucher auto-fills its line rate from the cost version effective as of the voucher date:
 * versions effective after that date are dormant, and the most-recently-effective version at the
 * voucher date wins. A null [effectiveFrom] means effective from the beginning (back-compat); a null
 * [effectiveTo] means open-ended (the current cost).
 */
@Entity(name = "product_standard_cost")
@Table(
    indexes = [
        Index(name = "idx_product_standard_cost_uid", columnList = "uid", unique = true),
        Index(name = "idx_product_standard_cost_owner", columnList = "owner_id"),
        Index(name = "idx_product_standard_cost_product", columnList = "product_id"),
    ]
)
class ProductStandardCost : OwnableBaseDomain() {

    @Column(name = "product_id", length = 200, nullable = false)
    var productId: String = ""

    @Column(name = "variant_sku", length = 200)
    var variantSku: String? = null

    @Column(name = "cost_price", nullable = false, precision = 15, scale = 2)
    var costPrice: BigDecimal = BigDecimal.ZERO

    @Column(name = "effective_from")
    var effectiveFrom: Instant? = null

    @Column(name = "effective_to")
    var effectiveTo: Instant? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.PRODUCT_STANDARD_COST_PREFIX
}
