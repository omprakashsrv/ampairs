package com.ampairs.pricing.domain.model

import com.ampairs.core.domain.enums.SalesChannel
import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.pricing.config.Constants
import com.ampairs.pricing.domain.enums.PriceListStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * A channel- and segment-scoped price list. Targeting dimensions are all optional; a list with no
 * dimension set applies to everyone in its channel. `attributePredicatesJson` holds the optional,
 * lowest-precedence [AttributePredicate] list.
 */
@Entity(name = "price_list")
@Table(
    indexes = [
        Index(name = "idx_price_list_uid", columnList = "uid", unique = true),
        Index(name = "idx_price_list_owner", columnList = "owner_id"),
        Index(name = "idx_price_list_channel", columnList = "channel"),
        Index(name = "idx_price_list_customer_group", columnList = "customer_group_id"),
        Index(name = "idx_price_list_brand", columnList = "brand_id"),
    ]
)
class PriceList : OwnableBaseDomain() {

    @Column(name = "name", length = 200, nullable = false)
    var name: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 30, nullable = false)
    var channel: SalesChannel = SalesChannel.RETAIL

    // ── Structured targeting dimensions (all optional) ────────────────────────────────
    @Column(name = "customer_group_id", length = 200)
    var customerGroupId: String? = null

    @Column(name = "customer_type", length = 100)
    var customerType: String? = null

    @Column(name = "customer_id", length = 200)
    var customerId: String? = null

    @Column(name = "brand_id", length = 200)
    var brandId: String? = null

    @Column(name = "category_id", length = 200)
    var categoryId: String? = null

    @Column(name = "product_group_id", length = 200)
    var productGroupId: String? = null

    @Column(name = "geo_zone_id", length = 200)
    var geoZoneId: String? = null

    /** JSON array of [AttributePredicate] — lowest-precedence match. */
    @Column(name = "attribute_predicates_json", columnDefinition = "TEXT")
    var attributePredicatesJson: String? = null

    @Column(name = "currency", length = 3, nullable = false)
    var currency: String = Constants.DEFAULT_CURRENCY

    @Column(name = "priority", nullable = false)
    var priority: Int = 0

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    var status: PriceListStatus = PriceListStatus.DRAFT

    @Column(name = "starts_at")
    var startsAt: Instant? = null

    @Column(name = "ends_at")
    var endsAt: Instant? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.PRICE_LIST_PREFIX
}
