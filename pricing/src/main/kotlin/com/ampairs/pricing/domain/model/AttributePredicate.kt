package com.ampairs.pricing.domain.model

/**
 * Optional, lowest-precedence targeting rule over a customer/product attribute
 * (e.g. `customer.attributes.tier EQ GOLD`). Evaluated only after all structured-dimension
 * matches. Stored as a JSON array on `price_list.attribute_predicates_json`.
 */
data class AttributePredicate(
    val field: String,
    val operator: Operator,
    val value: String,
) {
    enum class Operator { EQ, NEQ, IN, GT, LT, GTE, LTE }
}
