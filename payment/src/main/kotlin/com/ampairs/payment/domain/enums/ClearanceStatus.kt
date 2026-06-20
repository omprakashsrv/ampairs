package com.ampairs.payment.domain.enums

/**
 * Realisation lifecycle of a payment instrument.
 * `PENDING → CLEARED`, `PENDING → BOUNCED` (terminal), `PENDING|CLEARED → CANCELLED` (terminal).
 */
enum class ClearanceStatus {
    PENDING,
    CLEARED,
    BOUNCED,
    CANCELLED;

    /** Terminal states cannot transition further. */
    fun isTerminal(): Boolean = this == BOUNCED || this == CANCELLED
}
