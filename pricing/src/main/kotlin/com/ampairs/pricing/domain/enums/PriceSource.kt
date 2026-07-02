package com.ampairs.pricing.domain.enums

/** Where a resolved effective price came from. */
enum class PriceSource {
    /** Matched an active price list (possibly via a quantity tier). */
    PRICE_LIST,

    /** No list matched — fell back to the product/variant catalog selling price. */
    CATALOG_FALLBACK,
}
