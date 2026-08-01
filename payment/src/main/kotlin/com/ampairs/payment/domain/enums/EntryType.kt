package com.ampairs.payment.domain.enums

/**
 * Type of a single ledger movement. Every entry type has a fixed *natural* [Direction]
 * (see [naturalDirection]); both are stored on the entry for safety and validated for
 * consistency on every upsert.
 *
 * Note: there is intentionally no `OPENING_BALANCE` type — the opening balance is an
 * attribute of `PartyBalance`, folded into the balance as `openingSigned`, never a row.
 */
enum class EntryType {
    SALES_INVOICE,
    SALES_RETURN,
    CREDIT_NOTE,
    PAYMENT_IN,
    PAYMENT_OUT,
    PURCHASE_BILL,
    PURCHASE_RETURN,
    DEBIT_NOTE,
    DISCOUNT_ALLOWED,
    WRITE_OFF,
    ROUND_OFF,
    ADJUSTMENT;

    /**
     * The natural posting direction for this entry type (receivable-positive convention).
     * `DR` increases what the party owes us; `CR` decreases it.
     */
    fun naturalDirection(): Direction = when (this) {
        // Increase receivable (party owes us more)
        SALES_INVOICE -> Direction.DR
        PURCHASE_RETURN -> Direction.DR
        DEBIT_NOTE -> Direction.DR
        PAYMENT_OUT -> Direction.DR
        // Decrease receivable (party owes us less / we owe the party)
        SALES_RETURN -> Direction.CR
        CREDIT_NOTE -> Direction.CR
        PURCHASE_BILL -> Direction.CR
        DISCOUNT_ALLOWED -> Direction.CR
        WRITE_OFF -> Direction.CR
        PAYMENT_IN -> Direction.CR
        // Sign carried explicitly by the entry's own direction
        ROUND_OFF -> Direction.DR
        ADJUSTMENT -> Direction.DR
    }
}
