package com.ampairs.business.model.enums

/**
 * Enumeration of business types supported by the Ampairs platform.
 * Used to categorize businesses for reporting, analytics, and customization.
 */
enum class BusinessType(val description: String) {
    /**
     * Retail business (B2C) - Direct sales to consumers
     */
    RETAIL("Retail - Business to Consumer"),

    /**
     * Wholesale business (B2B) - Bulk sales to other businesses
     */
    WHOLESALE("Wholesale - Business to Business"),

    /**
     * Manufacturing/production business
     */
    MANUFACTURING("Manufacturing/Production"),

    /**
     * Service-based business (consulting, maintenance, etc.)
     */
    SERVICE("Service-Based Business"),

    /**
     * Restaurant or food service business
     */
    RESTAURANT("Restaurant/Food Service"),

    /**
     * E-commerce/online retail business
     */
    ECOMMERCE("E-commerce/Online Retail"),

    /**
     * Healthcare services (clinics, hospitals, pharmacies)
     */
    HEALTHCARE("Healthcare Services"),

    /**
     * Educational institution (schools, training centers)
     */
    EDUCATION("Educational Institution"),

    /**
     * Real estate business (sales, rentals, property management)
     */
    REAL_ESTATE("Real Estate"),

    /**
     * Logistics and transportation services
     */
    LOGISTICS("Logistics/Transportation"),

    /**
     * Other type of business not listed above
     */
    OTHER("Other");

    companion object {
        /**
         * Get BusinessType from string value (case-insensitive)
         */
        fun fromString(value: String): BusinessType {
            return values().find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid business type: $value")
        }

        /**
         * Check if a string is a valid business type
         */
        fun isValid(value: String): Boolean {
            return values().any { it.name.equals(value, ignoreCase = true) }
        }
    }
}
