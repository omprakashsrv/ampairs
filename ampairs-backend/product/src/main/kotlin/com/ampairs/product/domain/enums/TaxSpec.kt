package com.ampairs.product.domain.enums

/**
 * Tax specifications for Indian GST system
 */
enum class TaxSpec(val displayName: String, val description: String) {
    INTER("Interstate", "Interstate transaction - IGST applicable"),
    INTRA("Intrastate", "Intrastate transaction - SGST + CGST applicable"),
    EXPORT("Export", "Export transaction - 0% GST"),
    COMPOSITION("Composition", "Composition scheme - simplified GST"),
    EXEMPT("Exempt", "Exempt from GST"),
    NIL("Nil Rated", "Nil rated - 0% GST but not exempt")
}