package com.ampairs.inventory.domain.enums

/**
 * Transaction Type Enum
 *
 * Defines the types of inventory transactions that can occur.
 * Used to categorize stock movements for reporting and auditing.
 *
 * @property value String value for database storage
 * @property description Human-readable description
 */
enum class TransactionType(
    val value: String,
    val description: String
) {
    /**
     * Stock received into inventory
     * Examples: Purchases, returns from customers, opening stock
     */
    STOCK_IN(
        value = "STOCK_IN",
        description = "Stock In (Purchases, Returns, Opening Stock)"
    ),

    /**
     * Stock removed from inventory
     * Examples: Sales, damages, losses, consumption
     */
    STOCK_OUT(
        value = "STOCK_OUT",
        description = "Stock Out (Sales, Damages, Losses)"
    ),

    /**
     * Stock moved between warehouses
     */
    TRANSFER(
        value = "TRANSFER",
        description = "Inter-Warehouse Transfer"
    ),

    /**
     * Manual stock adjustment
     * Used for corrections and reconciliations
     */
    ADJUSTMENT(
        value = "ADJUSTMENT",
        description = "Stock Adjustment (Correction)"
    ),

    /**
     * Physical stock count
     * Result of inventory counting/reconciliation
     */
    COUNT(
        value = "COUNT",
        description = "Physical Stock Count"
    );

    companion object {
        /**
         * Get TransactionType from string value
         *
         * @param value String value
         * @return TransactionType enum, or null if not found
         */
        fun fromValue(value: String): TransactionType? {
            return values().find { it.value == value }
        }

        /**
         * Get all transaction type values as list
         */
        fun getAllValues(): List<String> {
            return values().map { it.value }
        }
    }
}
