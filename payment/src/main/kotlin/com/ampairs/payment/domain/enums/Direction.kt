package com.ampairs.payment.domain.enums

/**
 * Posting direction of a [LedgerEntry]. The single source of truth for balance math:
 * `signedAmount = if (direction == DR) +amount else -amount`.
 *
 * - `DR` (debit)  ⇒ +amount ⇒ increases the receivable ("to receive").
 * - `CR` (credit) ⇒ −amount ⇒ increases the payable ("to pay").
 */
enum class Direction {
    DR,
    CR,
}
