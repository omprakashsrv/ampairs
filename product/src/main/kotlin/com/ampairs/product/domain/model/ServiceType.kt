package com.ampairs.product.domain.model

enum class ServiceType {
    PHYSICAL,    // Physical goods
    SERVICE,     // Pure service
    DIGITAL;     // Digital products/services

    companion object {
        fun fromString(value: String?): ServiceType? {
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
