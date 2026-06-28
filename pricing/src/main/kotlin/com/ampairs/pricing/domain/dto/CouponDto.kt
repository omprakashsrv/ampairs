package com.ampairs.pricing.domain.dto

import com.ampairs.core.domain.enums.SalesChannel
import com.ampairs.pricing.config.Constants
import com.ampairs.pricing.domain.enums.CouponRejectionReason
import com.ampairs.pricing.domain.enums.OfferRewardType
import jakarta.validation.constraints.NotBlank

/** Validate a coupon code (cart-time check, no side effects). */
data class CouponApplyRequest(
    @field:NotBlank
    val code: String,
    val channel: SalesChannel = SalesChannel.RETAIL,
    val customerId: String? = null,
)

/** Atomically redeem a coupon against an order/invoice — idempotent per [orderRef]. */
data class CouponRedeemRequest(
    @field:NotBlank
    val code: String,
    val channel: SalesChannel = SalesChannel.RETAIL,
    val customerId: String? = null,
    @field:NotBlank
    val orderRef: String,
)

/** Result of validate/redeem — `valid = false` carries a [rejectionReason]; success carries the offer. */
data class CouponResponse(
    val valid: Boolean,
    val rejectionReason: CouponRejectionReason? = null,
    val offerUid: String? = null,
    val offerName: String? = null,
    val rewardType: OfferRewardType? = null,
    val rewardPercent: Double? = null,
    val rewardFlatMinor: Long? = null,
    val rewardCapMinor: Long? = null,
    val currency: String = Constants.DEFAULT_CURRENCY,
)
