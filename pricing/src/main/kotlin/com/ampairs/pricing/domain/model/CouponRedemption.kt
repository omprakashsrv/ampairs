package com.ampairs.pricing.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.pricing.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

/**
 * One recorded use of a coupon (a COUPON-condition [Offer]). The `(owner_id, coupon_offer_uid,
 * order_ref)` unique constraint makes redemption idempotent per order and is the atomic guard against
 * double-spend: a concurrent second insert for the same order fails at the DB. Per-customer and global
 * caps are enforced by counting rows for the coupon (optionally by customer).
 */
@Entity(name = "coupon_redemption")
@Table(
    uniqueConstraints = [
        UniqueConstraint(name = "uq_coupon_redemption_order", columnNames = ["owner_id", "coupon_offer_uid", "order_ref"]),
    ],
    indexes = [
        Index(name = "idx_coupon_redemption_uid", columnList = "uid", unique = true),
        Index(name = "idx_coupon_redemption_offer", columnList = "coupon_offer_uid"),
        Index(name = "idx_coupon_redemption_customer", columnList = "coupon_offer_uid, customer_id"),
    ]
)
class CouponRedemption : OwnableBaseDomain() {

    @Column(name = "coupon_offer_uid", length = 200, nullable = false)
    var couponOfferUid: String = ""

    @Column(name = "coupon_code", length = 100, nullable = false)
    var couponCode: String = ""

    @Column(name = "customer_id", length = 200)
    var customerId: String? = null

    @Column(name = "order_ref", length = 200, nullable = false)
    var orderRef: String = ""

    @Column(name = "redeemed_at", nullable = false)
    var redeemedAt: Instant = Instant.now()

    override fun obtainSeqIdPrefix(): String = Constants.COUPON_REDEMPTION_PREFIX
}
