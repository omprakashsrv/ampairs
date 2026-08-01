package com.ampairs.pricing.domain.enums

/** Lifecycle of a price list. Only ACTIVE lists participate in resolution. */
enum class PriceListStatus {
    DRAFT,
    ACTIVE,
    INACTIVE,
}
