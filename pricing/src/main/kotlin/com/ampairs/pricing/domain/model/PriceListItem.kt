package com.ampairs.pricing.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.pricing.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

/**
 * A single product/variant entry within a [PriceList]: a base unit price, optional MOQ, and optional
 * ordered quantity tiers (`tiersJson`). A variant-targeted item (variantSku set) wins over a
 * base-product item within the same list.
 */
@Entity(name = "price_list_item")
@Table(
    indexes = [
        Index(name = "idx_price_list_item_uid", columnList = "uid", unique = true),
        Index(name = "idx_price_list_item_owner", columnList = "owner_id"),
        Index(name = "idx_price_list_item_list", columnList = "price_list_id"),
        Index(name = "idx_price_list_item_product", columnList = "product_id"),
    ]
)
class PriceListItem : OwnableBaseDomain() {

    @Column(name = "price_list_id", length = 200, nullable = false)
    var priceListId: String = ""

    @Column(name = "product_id", length = 200, nullable = false)
    var productId: String = ""

    @Column(name = "variant_sku", length = 200)
    var variantSku: String? = null

    @Column(name = "unit_price", precision = 19, scale = 4, nullable = false)
    var unitPrice: BigDecimal = BigDecimal.ZERO

    @Column(name = "moq", precision = 19, scale = 4)
    var moq: BigDecimal? = null

    /** JSON array of [PriceTier] (minQty, unitPrice), ascending by minQty. */
    @Column(name = "tiers_json", columnDefinition = "TEXT")
    var tiersJson: String? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    // ── effective dating (price-change history, Tally "Applicable From") ──────────────────────
    /** Effective from this instant; null = effective from the beginning (back-compat for legacy rows). */
    @Column(name = "effective_from")
    var effectiveFrom: Instant? = null

    /** Effective until this instant (exclusive); null = open-ended (the current price). */
    @Column(name = "effective_to")
    var effectiveTo: Instant? = null

    override fun obtainSeqIdPrefix(): String = Constants.PRICE_LIST_ITEM_PREFIX
}
