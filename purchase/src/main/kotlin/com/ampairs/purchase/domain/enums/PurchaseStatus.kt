package com.ampairs.purchase.domain.enums

/**
 * Lifecycle of a purchase document. RECEIVED triggers an inbound stock movement (stock-in);
 * CANCELLED reverses it.
 */
enum class PurchaseStatus {
    DRAFT, ORDERED, RECEIVED, CANCELLED
}
