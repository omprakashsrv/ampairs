package com.ampairs.tax.domain.enums

enum class TaxComponentType(val displayName: String, val description: String, val isGstComponent: Boolean = true) {
    CGST("Central GST", "Central Goods and Services Tax", true),
    SGST("State GST", "State Goods and Services Tax", true),
    IGST("Integrated GST", "Integrated Goods and Services Tax", true),
    UTGST("Union Territory GST", "Union Territory Goods and Services Tax", true),
    CESS("Cess", "Additional cess on specific goods", false),
    COMPENSATION_CESS("Compensation Cess", "GST Compensation Cess", false),
    KRISHI_KALYAN_CESS("Krishi Kalyan Cess", "Agricultural welfare cess", false),
    SWACHH_BHARAT_CESS("Swachh Bharat Cess", "Cleanliness cess", false),
    TDS("Tax Deducted at Source", "TDS applicable", false),
    TCS("Tax Collected at Source", "TCS applicable", false),
    CUSTOMS_DUTY("Customs Duty", "Import duty", false),
    EXCISE_DUTY("Excise Duty", "Central excise duty (pre-GST)", false),
    VAT("Value Added Tax", "State VAT (pre-GST)", false),
    SERVICE_TAX("Service Tax", "Central service tax (pre-GST)", false)
}