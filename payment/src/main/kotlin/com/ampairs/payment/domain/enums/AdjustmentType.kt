package com.ampairs.payment.domain.enums

/**
 * Non-payment movements entered as an [AdjustmentVoucher]. Each maps to an [EntryType]
 * (which in turn carries the natural [Direction]) when posting its ledger entry.
 */
enum class AdjustmentType {
    SALES_RETURN,
    CREDIT_NOTE,
    PURCHASE_BILL,
    PURCHASE_RETURN,
    DEBIT_NOTE,
    DISCOUNT_ALLOWED,
    WRITE_OFF,
    ADJUSTMENT;

    /** The ledger entry type this adjustment posts. */
    fun toEntryType(): EntryType = when (this) {
        SALES_RETURN -> EntryType.SALES_RETURN
        CREDIT_NOTE -> EntryType.CREDIT_NOTE
        PURCHASE_BILL -> EntryType.PURCHASE_BILL
        PURCHASE_RETURN -> EntryType.PURCHASE_RETURN
        DEBIT_NOTE -> EntryType.DEBIT_NOTE
        DISCOUNT_ALLOWED -> EntryType.DISCOUNT_ALLOWED
        WRITE_OFF -> EntryType.WRITE_OFF
        ADJUSTMENT -> EntryType.ADJUSTMENT
    }

    /** Natural posting direction for this adjustment, derived from its entry type. */
    fun direction(): Direction = toEntryType().naturalDirection()
}
