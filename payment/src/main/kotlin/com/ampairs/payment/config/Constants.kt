package com.ampairs.payment.config

interface Constants {
    companion object {
        const val ID_LENGTH = 34

        // uid prefixes
        const val PARTY_BALANCE_PREFIX = "PBL"
        const val LEDGER_ENTRY_PREFIX = "LDG"
        const val PAYMENT_VOUCHER_PREFIX = "RCP"
        const val PAYMENT_ALLOCATION_PREFIX = "ALC"
        const val ADJUSTMENT_PREFIX = "ADJ"

        /** Deterministic uid for a ledger entry derived from a source document (1:1, dedup-safe). */
        const val LEDGER_SOURCE_PREFIX = "LDG_"

        /** Sequence-number series for voucher numbering (via the `sequence` module). */
        const val SERIES_RECEIPT = "RCP"
        const val SERIES_PAYMENT_OUT = "PAY"
        const val SERIES_CREDIT_NOTE = "CRN"
        const val SERIES_DEBIT_NOTE = "DBN"
        const val SERIES_ADJUSTMENT = "ADJ"

        /** Installed-module code that gates the payment-collection feature for a workspace. */
        const val MODULE_CODE = "payment-collection"

        const val MONEY_SCALE = 4
    }
}
