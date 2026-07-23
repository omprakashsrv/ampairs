package com.ampairs.pricing.domain.enums

/** Lifecycle of an offer/promotion. Only ACTIVE (and in-window) offers participate in resolution. */
enum class OfferStatus {
    DRAFT,
    SCHEDULED,
    ACTIVE,
    INACTIVE,
    EXPIRED,
}
