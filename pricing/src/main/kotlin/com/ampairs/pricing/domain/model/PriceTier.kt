package com.ampairs.pricing.domain.model

import java.math.BigDecimal

/**
 * A quantity break within a price-list item: at [minQty] or more units, charge [unitPrice].
 * Tiers within an item must be contiguous and non-overlapping (validated at save).
 * Stored as a JSON array on `price_list_item.tiers_json`.
 */
data class PriceTier(
    val minQty: BigDecimal,
    val unitPrice: BigDecimal,
)
