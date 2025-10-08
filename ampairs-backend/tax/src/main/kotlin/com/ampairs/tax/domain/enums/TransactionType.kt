package com.ampairs.tax.domain.enums

enum class TransactionType(val displayName: String, val description: String) {
    INTRA_STATE("Intrastate", "Transaction within same state - CGST + SGST/UTGST applicable"),
    INTER_STATE("Interstate", "Transaction between different states - IGST applicable"),
    UNION_TERRITORY("Union Territory", "Transaction involving Union Territory"),
    EXPORT("Export", "Goods exported outside India - Zero rated"),
    IMPORT("Import", "Goods imported into India - IGST + Customs duty"),
    SEZ_UNIT("SEZ Unit", "Supply to SEZ unit"),
    SEZ_DEVELOPER("SEZ Developer", "Supply to SEZ developer"),
    DEEMED_EXPORT("Deemed Export", "Supplies treated as export for GST purposes")
}