package com.ampairs.pricing.domain.model

import com.ampairs.core.domain.enums.SalesChannel
import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.pricing.config.Constants
import com.ampairs.pricing.domain.enums.OfferConditionType
import com.ampairs.pricing.domain.enums.OfferRewardType
import com.ampairs.pricing.domain.enums.OfferStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * A promotion/offer that applies on top of resolved prices. Channel- and segment-scoped, with an
 * optional trigger condition (cart minimum, quantity, coupon) and a reward (percent / flat / BOGO).
 * Targeting dimensions are all optional; an offer with no dimension set applies to everyone in its
 * channel. Money fields are minor units (paise/cents), consistent with [PriceListItem].
 */
@Entity(name = "offer")
@Table(
    indexes = [
        Index(name = "idx_offer_uid", columnList = "uid", unique = true),
        Index(name = "idx_offer_owner", columnList = "owner_id"),
        Index(name = "idx_offer_channel", columnList = "channel"),
        Index(name = "idx_offer_brand", columnList = "brand_id"),
        Index(name = "idx_offer_geo_zone", columnList = "geo_zone_id"),
    ]
)
class Offer : OwnableBaseDomain() {

    @Column(name = "name", length = 200, nullable = false)
    var name: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 30, nullable = false)
    var channel: SalesChannel = SalesChannel.RETAIL

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    var status: OfferStatus = OfferStatus.DRAFT

    @Column(name = "priority", nullable = false)
    var priority: Int = 0

    @Column(name = "starts_at")
    var startsAt: Instant? = null

    @Column(name = "ends_at")
    var endsAt: Instant? = null

    // ── Structured targeting dimensions (all optional) ────────────────────────────────
    @Column(name = "customer_group_id", length = 200)
    var customerGroupId: String? = null

    @Column(name = "customer_type", length = 100)
    var customerType: String? = null

    @Column(name = "brand_id", length = 200)
    var brandId: String? = null

    @Column(name = "category_id", length = 200)
    var categoryId: String? = null

    @Column(name = "geo_zone_id", length = 200)
    var geoZoneId: String? = null

    // ── Trigger / condition ───────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", length = 20, nullable = false)
    var conditionType: OfferConditionType = OfferConditionType.NONE

    @Column(name = "cart_min_minor")
    var cartMinMinor: Long? = null

    @Column(name = "quantity_min")
    var quantityMin: Double? = null

    @Column(name = "coupon_code", length = 100)
    var couponCode: String? = null

    @Column(name = "coupon_limit")
    var couponLimit: Int? = null

    // ── Reward ──────────────────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", length = 20, nullable = false)
    var rewardType: OfferRewardType = OfferRewardType.PERCENT

    @Column(name = "reward_percent")
    var rewardPercent: Double? = null

    @Column(name = "reward_flat_minor")
    var rewardFlatMinor: Long? = null

    @Column(name = "reward_cap_minor")
    var rewardCapMinor: Long? = null

    @Column(name = "bogo_buy_qty")
    var bogoBuyQty: Int? = null

    @Column(name = "bogo_get_qty")
    var bogoGetQty: Int? = null

    // ── Stacking & limits ─────────────────────────────────────────────────────────────
    @Column(name = "stackable", nullable = false)
    var stackable: Boolean = false

    @Column(name = "exclusive", nullable = false)
    var exclusive: Boolean = false

    @Column(name = "per_customer_limit")
    var perCustomerLimit: Int? = null

    @Column(name = "total_limit")
    var totalLimit: Int? = null

    @Column(name = "used_count", nullable = false)
    var usedCount: Int = 0

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.OFFER_PREFIX
}
