package com.ampairs.payment.domain.enums

/** How a payment was made. The workspace setting `enabled_payment_modes` may restrict which apply. */
enum class PaymentMode {
    CASH,
    CHEQUE,
    UPI,
    NEFT,
    RTGS,
    IMPS,
    NET_BANKING,
    CARD,
    BANK_TRANSFER,
    WALLET,
    OTHER;

    /** True for modes that settle instantly and default to CLEARED (no clearance step). */
    fun isInstant(): Boolean = this == CASH
}
