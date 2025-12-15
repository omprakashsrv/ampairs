package com.ampairs.product.domain.model

enum class ProductType {
    RETAIL,      // Physical retail products
    WHOLESALE,   // Bulk/wholesale items
    SERVICE;     // Service-based offerings

    companion object {
        fun fromString(value: String?): ProductType? {
            return value?.let {
                try {
                    valueOf(it.uppercase())
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }
    }
}
