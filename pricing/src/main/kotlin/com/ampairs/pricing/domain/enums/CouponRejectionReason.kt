package com.ampairs.pricing.domain.enums

/** Why a coupon code could not be applied/redeemed. */
enum class CouponRejectionReason {
    NOT_FOUND,
    OUT_OF_WINDOW,
    GLOBAL_LIMIT_REACHED,
    PER_CUSTOMER_LIMIT_REACHED,
}
