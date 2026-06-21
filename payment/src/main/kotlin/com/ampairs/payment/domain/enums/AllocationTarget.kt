package com.ampairs.payment.domain.enums

/** What a [PaymentAllocation] is matched against. Phase 1 uses INVOICE only. */
enum class AllocationTarget {
    INVOICE,
    PURCHASE,
    LEDGER_ENTRY,
}
