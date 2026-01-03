package com.ampairs.inventory.domain.enums

/**
 * Serial Status Enum
 *
 * Defines the lifecycle status of serial numbers.
 * Used to track individual units from receipt to sale/disposal.
 *
 * Lifecycle Flow:
 * AVAILABLE → RESERVED → SOLD
 * AVAILABLE → DAMAGED
 * SOLD → RETURNED → AVAILABLE (after inspection)
 *
 * @property value String value for database storage
 * @property description Human-readable description
 * @property allowedTransitions Valid next statuses
 */
enum class SerialStatus(
    val value: String,
    val description: String,
    val allowedTransitions: List<String>
) {
    /**
     * Serial is in stock and available for sale
     */
    AVAILABLE(
        value = "AVAILABLE",
        description = "In Stock - Available",
        allowedTransitions = listOf("RESERVED", "SOLD", "DAMAGED")
    ),

    /**
     * Serial is reserved for an order but not yet sold
     */
    RESERVED(
        value = "RESERVED",
        description = "Reserved for Order",
        allowedTransitions = listOf("AVAILABLE", "SOLD", "DAMAGED")
    ),

    /**
     * Serial has been sold to customer
     */
    SOLD(
        value = "SOLD",
        description = "Sold to Customer",
        allowedTransitions = listOf("RETURNED", "WARRANTY_CLAIM")
    ),

    /**
     * Serial is damaged or defective
     */
    DAMAGED(
        value = "DAMAGED",
        description = "Damaged/Defective",
        allowedTransitions = listOf("AVAILABLE") // After repair
    ),

    /**
     * Serial was returned by customer
     */
    RETURNED(
        value = "RETURNED",
        description = "Returned by Customer",
        allowedTransitions = listOf("AVAILABLE", "DAMAGED")
    ),

    /**
     * Serial is under warranty claim
     */
    WARRANTY_CLAIM(
        value = "WARRANTY_CLAIM",
        description = "Warranty Claim",
        allowedTransitions = listOf("RETURNED", "DAMAGED")
    );

    /**
     * Check if transition to another status is allowed
     *
     * @param toStatus Target status
     * @return true if transition is allowed
     */
    fun canTransitionTo(toStatus: SerialStatus): Boolean {
        return allowedTransitions.contains(toStatus.value)
    }

    companion object {
        /**
         * Get SerialStatus from string value
         *
         * @param value String value
         * @return SerialStatus enum, or null if not found
         */
        fun fromValue(value: String): SerialStatus? {
            return values().find { it.value == value }
        }

        /**
         * Get all serial status values as list
         */
        fun getAllValues(): List<String> {
            return values().map { it.value }
        }

        /**
         * Validate status transition
         *
         * @param from Current status
         * @param to Target status
         * @return true if transition is valid
         * @throws IllegalArgumentException if transition is invalid
         */
        fun validateTransition(from: SerialStatus, to: SerialStatus) {
            if (!from.canTransitionTo(to)) {
                throw IllegalArgumentException(
                    "Invalid serial status transition from ${from.value} to ${to.value}. " +
                    "Allowed transitions from ${from.value}: ${from.allowedTransitions.joinToString(", ")}"
                )
            }
        }
    }
}
