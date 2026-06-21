package com.ampairs.payment.domain.enums

/** Provenance of a [LedgerEntry] — which kind of document produced it. */
enum class SourceType {
    INVOICE,
    PAYMENT,
    ADJUSTMENT,
    PURCHASE,
    MANUAL,
}
