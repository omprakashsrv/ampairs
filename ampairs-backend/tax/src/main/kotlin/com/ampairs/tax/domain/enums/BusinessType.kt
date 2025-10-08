package com.ampairs.tax.domain.enums

enum class BusinessType(val displayName: String, val description: String) {
    B2B("Business to Business", "Regular business transactions between GST registered entities"),
    B2C("Business to Consumer", "Retail transactions with end consumers"),
    COMPOSITION("Composition Scheme", "Simplified GST scheme for small businesses"),
    EXPORT("Export", "Goods exported outside India"),
    IMPORT("Import", "Goods imported into India"),
    SEZ("Special Economic Zone", "Transactions within SEZ"),
    DEEMED_EXPORT("Deemed Export", "Supplies treated as exports"),
    KIRANA("Kirana Store", "Traditional grocery and convenience stores"),
    JEWELRY("Jewelry Business", "Gold, silver and precious metal trading"),
    HARDWARE("Hardware Store", "Construction and electrical materials"),
    TOBACCO("Tobacco Products", "Cigarettes, bidis and tobacco products"),
    AUTOMOBILE("Automobile", "Vehicles, parts and accessories"),
    TEXTILE("Textile", "Clothing, fabric and garments"),
    ELECTRONICS("Electronics", "Electronic goods and appliances"),
    PHARMACY("Pharmacy", "Medicines and healthcare products"),
    RESTAURANT("Restaurant", "Food service and catering"),
    ECOMMERCE("E-commerce", "Online marketplace transactions")
}