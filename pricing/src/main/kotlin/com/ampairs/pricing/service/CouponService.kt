package com.ampairs.pricing.service

import com.ampairs.pricing.domain.dto.CouponApplyRequest
import com.ampairs.pricing.domain.dto.CouponRedeemRequest
import com.ampairs.pricing.domain.dto.CouponResponse

/**
 * Coupon validation + atomic redemption. A coupon is a COUPON-condition [com.ampairs.pricing.domain.model.Offer]
 * with a code; redemptions are tracked in `coupon_redemption` and bounded by the offer's per-customer
 * and global caps. Coupons are online-only — the merchant app rejects offline entry and calls these
 * server-side endpoints when connected; the ecom checkout calls them too.
 */
interface CouponService {
    /** Cart-time check — no side effects. */
    fun validate(request: CouponApplyRequest): CouponResponse

    /** Record a redemption against an order (idempotent per orderRef); re-checks caps atomically. */
    fun redeem(request: CouponRedeemRequest): CouponResponse
}
