package com.ampairs.inventory.domain.enums

/**
 * Transaction Reason Enum
 *
 * Defines the reasons for inventory transactions.
 * Provides more detailed context than transaction type alone.
 *
 * @property value String value for database storage
 * @property description Human-readable description
 * @property applicableTypes Transaction types this reason applies to
 */
enum class TransactionReason(
    val value: String,
    val description: String,
    val applicableTypes: List<TransactionType>
) {
    /**
     * Stock received from purchase
     */
    PURCHASE(
        value = "PURCHASE",
        description = "Purchase from Supplier",
        applicableTypes = listOf(TransactionType.STOCK_IN)
    ),

    /**
     * Stock sold to customer
     */
    SALE(
        value = "SALE",
        description = "Sale to Customer",
        applicableTypes = listOf(TransactionType.STOCK_OUT)
    ),

    /**
     * Stock returned by customer
     */
    RETURN(
        value = "RETURN",
        description = "Customer Return",
        applicableTypes = listOf(TransactionType.STOCK_IN)
    ),

    /**
     * Stock damaged or defective
     */
    DAMAGE(
        value = "DAMAGE",
        description = "Damaged/Defective Stock",
        applicableTypes = listOf(TransactionType.STOCK_OUT)
    ),

    /**
     * Stock lost (theft, misplacement, etc.)
     */
    LOSS(
        value = "LOSS",
        description = "Stock Loss",
        applicableTypes = listOf(TransactionType.STOCK_OUT)
    ),

    /**
     * Stock theft
     */
    THEFT(
        value = "THEFT",
        description = "Stock Theft",
        applicableTypes = listOf(TransactionType.STOCK_OUT)
    ),

    /**
     * Opening stock entry
     */
    OPENING(
        value = "OPENING",
        description = "Opening Stock",
        applicableTypes = listOf(TransactionType.STOCK_IN)
    ),

    /**
     * Manual correction
     */
    CORRECTION(
        value = "CORRECTION",
        description = "Stock Correction",
        applicableTypes = listOf(TransactionType.ADJUSTMENT)
    ),

    /**
     * Inter-warehouse transfer
     */
    TRANSFER(
        value = "TRANSFER",
        description = "Warehouse Transfer",
        applicableTypes = listOf(TransactionType.TRANSFER)
    ),

    /**
     * Physical count adjustment
     */
    COUNT_ADJUSTMENT(
        value = "COUNT_ADJUSTMENT",
        description = "Physical Count Adjustment",
        applicableTypes = listOf(TransactionType.COUNT, TransactionType.ADJUSTMENT)
    ),

    /**
     * Stock expired and removed
     */
    EXPIRED(
        value = "EXPIRED",
        description = "Expired Stock Removal",
        applicableTypes = listOf(TransactionType.STOCK_OUT)
    ),

    /**
     * Stock used in production
     */
    PRODUCTION(
        value = "PRODUCTION",
        description = "Production Consumption",
        applicableTypes = listOf(TransactionType.STOCK_OUT)
    ),

    /**
     * Stock consumed for internal use
     */
    CONSUMPTION(
        value = "CONSUMPTION",
        description = "Internal Consumption",
        applicableTypes = listOf(TransactionType.STOCK_OUT)
    ),

    /**
     * Stock received from production
     */
    PRODUCTION_OUTPUT(
        value = "PRODUCTION_OUTPUT",
        description = "Production Output",
        applicableTypes = listOf(TransactionType.STOCK_IN)
    ),

    /**
     * Stock sample given to customer
     */
    SAMPLE(
        value = "SAMPLE",
        description = "Sample Distribution",
        applicableTypes = listOf(TransactionType.STOCK_OUT)
    ),

    /**
     * Stock promotional giveaway
     */
    PROMOTIONAL(
        value = "PROMOTIONAL",
        description = "Promotional Giveaway",
        applicableTypes = listOf(TransactionType.STOCK_OUT)
    );

    companion object {
        /**
         * Get TransactionReason from string value
         *
         * @param value String value
         * @return TransactionReason enum, or null if not found
         */
        fun fromValue(value: String): TransactionReason? {
            return values().find { it.value == value }
        }

        /**
         * Get all transaction reason values as list
         */
        fun getAllValues(): List<String> {
            return values().map { it.value }
        }

        /**
         * Get reasons applicable to a specific transaction type
         *
         * @param type Transaction type
         * @return List of applicable reasons
         */
        fun getReasonsForType(type: TransactionType): List<TransactionReason> {
            return values().filter { it.applicableTypes.contains(type) }
        }

        /**
         * Validate if a reason is valid for a transaction type
         *
         * @param reason Transaction reason
         * @param type Transaction type
         * @return true if valid combination, false otherwise
         */
        fun isValidForType(reason: TransactionReason, type: TransactionType): Boolean {
            return reason.applicableTypes.contains(type)
        }
    }
}
