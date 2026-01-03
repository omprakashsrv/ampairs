package com.ampairs.inventory.domain.enums

/**
 * Warehouse Type Enum
 *
 * Defines different types of storage locations.
 * Used to categorize warehouses by their purpose and characteristics.
 *
 * @property value String value for database storage
 * @property description Human-readable description
 */
enum class WarehouseType(
    val value: String,
    val description: String
) {
    /**
     * Standard warehouse for bulk storage
     */
    WAREHOUSE(
        value = "WAREHOUSE",
        description = "Warehouse (Bulk Storage)"
    ),

    /**
     * Retail store location
     */
    STORE(
        value = "STORE",
        description = "Retail Store"
    ),

    /**
     * Godown/Storage facility
     */
    GODOWN(
        value = "GODOWN",
        description = "Godown (Storage Facility)"
    ),

    /**
     * Showroom for display and sales
     */
    SHOWROOM(
        value = "SHOWROOM",
        description = "Showroom/Display Center"
    ),

    /**
     * Factory or manufacturing facility
     */
    FACTORY(
        value = "FACTORY",
        description = "Factory/Manufacturing Unit"
    ),

    /**
     * Distribution center
     */
    DISTRIBUTION_CENTER(
        value = "DISTRIBUTION_CENTER",
        description = "Distribution Center"
    ),

    /**
     * Cross-dock facility
     */
    CROSS_DOCK(
        value = "CROSS_DOCK",
        description = "Cross-Dock Facility"
    ),

    /**
     * Service center
     */
    SERVICE_CENTER(
        value = "SERVICE_CENTER",
        description = "Service Center"
    ),

    /**
     * Virtual location (for tracking purposes)
     */
    VIRTUAL(
        value = "VIRTUAL",
        description = "Virtual Location"
    );

    companion object {
        /**
         * Get WarehouseType from string value
         *
         * @param value String value
         * @return WarehouseType enum, or null if not found
         */
        fun fromValue(value: String): WarehouseType? {
            return values().find { it.value == value }
        }

        /**
         * Get all warehouse type values as list
         */
        fun getAllValues(): List<String> {
            return values().map { it.value }
        }

        /**
         * Get warehouse types suitable for retail operations
         */
        fun getRetailTypes(): List<WarehouseType> {
            return listOf(STORE, SHOWROOM)
        }

        /**
         * Get warehouse types suitable for storage
         */
        fun getStorageTypes(): List<WarehouseType> {
            return listOf(WAREHOUSE, GODOWN, DISTRIBUTION_CENTER)
        }

        /**
         * Get warehouse types suitable for manufacturing
         */
        fun getManufacturingTypes(): List<WarehouseType> {
            return listOf(FACTORY)
        }
    }
}
