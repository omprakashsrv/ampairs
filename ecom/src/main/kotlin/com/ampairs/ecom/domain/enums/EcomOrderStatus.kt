package com.ampairs.ecom.domain.enums

enum class EcomOrderStatus {
    PLACED,
    PENDING_MERCHANT_REVIEW,
    CONFIRMED,
    PROCESSING,
    DISPATCHED,
    DELIVERED,
    CANCELLED,
}
