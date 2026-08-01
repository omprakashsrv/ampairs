package com.ampairs.pricing.domain.enums

/** What has to happen for an offer to fire. */
enum class OfferConditionType {
    NONE,
    CART_MIN,
    QUANTITY_MIN,
    FIRST_ORDER,
    COUPON,
}
