package com.ampairs.inventory.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.inventory.config.Constants
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

/**
 * Inventory Transaction Entity
 *
 * Represents all stock movements including:
 * - Stock-in (purchases, returns, opening stock)
 * - Stock-out (sales, damages, losses)
 * - Transfers (between warehouses)
 * - Adjustments (corrections, write-offs)
 * - Physical counts (inventory reconciliation)
 *
 * Key Features:
 * - Complete audit trail with transaction number
 * - Quantity and cost tracking
 * - Batch and serial number support
 * - Reference to source documents (orders, invoices, etc.)
 * - Balance tracking after each transaction
 *
 * @property transactionNumber Auto-generated unique transaction number
 * @property transactionType Type of transaction (STOCK_IN, STOCK_OUT, TRANSFER, ADJUSTMENT, COUNT)
 * @property transactionReason Reason for transaction (PURCHASE, SALE, RETURN, DAMAGE, etc.)
 * @property inventoryItemId Reference to inventory item
 * @property warehouseId Source warehouse
 * @property toWarehouseId Destination warehouse (for transfers)
 * @property quantity Transaction quantity (always positive)
 * @property balanceAfter Stock balance after transaction
 * @property unitCost Cost per unit
 * @property totalCost Total transaction cost
 * @property batchId Optional batch reference
 * @property serialNumbers Optional serial numbers (JSON array)
 * @property referenceType Type of reference document (ORDER, INVOICE, PURCHASE)
 * @property referenceId UID of reference document
 * @property referenceNumber Human-readable reference number
 * @property transactionDate Date/time of transaction
 * @property notes Optional notes
 * @property performedBy User who performed the transaction
 */
@Entity(name = "inventory_transaction")
@NamedEntityGraph(
    name = "InventoryTransaction.full",
    attributeNodes = [
        NamedAttributeNode("inventoryItem"),
        NamedAttributeNode("warehouse")
    ]
)
class InventoryTransaction : OwnableBaseDomain() {

    // ============================================================================
    // Transaction Identification
    // ============================================================================

    /**
     * Auto-generated transaction number
     * Format: TXN-YYYYMMDD-NNNN
     * Example: TXN-20250119-0001
     */
    @Column(name = "transaction_number", unique = true, nullable = false, length = 50)
    var transactionNumber: String = ""

    /**
     * Transaction type
     * Values: STOCK_IN, STOCK_OUT, TRANSFER, ADJUSTMENT, COUNT
     */
    @Column(name = "transaction_type", nullable = false, length = 20)
    var transactionType: String = ""

    /**
     * Transaction reason
     * Values: PURCHASE, SALE, RETURN, DAMAGE, LOSS, OPENING, CORRECTION, TRANSFER, COUNT_ADJUSTMENT
     */
    @Column(name = "transaction_reason", nullable = false, length = 50)
    var transactionReason: String = ""

    // ============================================================================
    // Inventory Item Reference
    // ============================================================================

    /**
     * Inventory item UID
     */
    @Column(name = "inventory_item_id", nullable = false, length = 200)
    var inventoryItemId: String = ""

    /**
     * Inventory item entity (lazy loaded)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "inventory_item_id",
        referencedColumnName = "uid",
        updatable = false,
        insertable = false
    )
    var inventoryItem: InventoryItem? = null

    // ============================================================================
    // Warehouse/Location
    // ============================================================================

    /**
     * Source warehouse UID
     * For stock-in: receiving warehouse
     * For stock-out: issuing warehouse
     * For transfer: source warehouse
     */
    @Column(name = "warehouse_id", nullable = false, length = 200)
    var warehouseId: String = ""

    /**
     * Warehouse entity (lazy loaded)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "warehouse_id",
        referencedColumnName = "uid",
        updatable = false,
        insertable = false
    )
    var warehouse: Warehouse? = null

    /**
     * Destination warehouse UID (for transfers only)
     */
    @Column(name = "to_warehouse_id", length = 200)
    var toWarehouseId: String? = null

    // ============================================================================
    // Quantities
    // ============================================================================

    /**
     * Transaction quantity (always positive)
     * For stock-in: quantity added
     * For stock-out: quantity deducted
     * For adjustment: absolute adjustment amount
     */
    @Column(name = "quantity", nullable = false, precision = 15, scale = 3)
    var quantity: BigDecimal = BigDecimal.ZERO

    /**
     * Stock balance after this transaction
     * Calculated and stored for audit trail
     */
    @Column(name = "balance_after", precision = 15, scale = 3)
    var balanceAfter: BigDecimal = BigDecimal.ZERO

    // ============================================================================
    // Pricing
    // ============================================================================

    /**
     * Cost per unit
     */
    @Column(name = "unit_cost", precision = 15, scale = 2)
    var unitCost: BigDecimal = BigDecimal.ZERO

    /**
     * Total transaction cost
     * Calculated: quantity * unitCost
     */
    @Column(name = "total_cost", precision = 15, scale = 2)
    var totalCost: BigDecimal = BigDecimal.ZERO

    // ============================================================================
    // Batch/Serial Tracking
    // ============================================================================

    /**
     * Batch UID (optional - for batch-tracked items)
     */
    @Column(name = "batch_id", length = 200)
    var batchId: String? = null

    /**
     * Serial numbers (optional - for serial-tracked items)
     * Stored as JSON array: ["SN001", "SN002", "SN003"]
     */
    @Column(name = "serial_numbers", columnDefinition = "JSON")
    @Convert(converter = StringListJsonConverter::class)
    var serialNumbers: List<String>? = null

    // ============================================================================
    // References (Link to Source Documents)
    // ============================================================================

    /**
     * Type of reference document
     * Values: ORDER, INVOICE, PURCHASE, COUNT
     */
    @Column(name = "reference_type", length = 50)
    var referenceType: String? = null

    /**
     * UID of reference document
     */
    @Column(name = "reference_id", length = 200)
    var referenceId: String? = null

    /**
     * Human-readable reference number
     * Example: INV-2025-0001, PO-2025-0123
     */
    @Column(name = "reference_number", length = 100)
    var referenceNumber: String? = null

    // ============================================================================
    // Transaction Metadata
    // ============================================================================

    /**
     * Transaction date/time
     */
    @Column(name = "transaction_date", nullable = false)
    var transactionDate: Instant = Instant.now()

    /**
     * Optional notes/comments
     */
    @Column(name = "notes", length = 1000)
    var notes: String? = null

    /**
     * User who performed the transaction
     * Stores user UID
     */
    @Column(name = "performed_by", length = 200)
    var performedBy: String? = null

    /**
     * Extensible attributes (JSON)
     */
    @Column(name = "attributes", columnDefinition = "JSON")
    var attributes: Map<String, Any>? = null

    // ============================================================================
    // Base Domain Methods
    // ============================================================================

    override fun obtainSeqIdPrefix(): String = Constants.TRANSACTION_PREFIX

    // ============================================================================
    // Calculated Properties
    // ============================================================================

    /**
     * Check if this is a stock-in transaction
     */
    fun isStockIn(): Boolean {
        return transactionType == Constants.TXN_TYPE_STOCK_IN
    }

    /**
     * Check if this is a stock-out transaction
     */
    fun isStockOut(): Boolean {
        return transactionType == Constants.TXN_TYPE_STOCK_OUT
    }

    /**
     * Check if this is a transfer transaction
     */
    fun isTransfer(): Boolean {
        return transactionType == Constants.TXN_TYPE_TRANSFER
    }

    /**
     * Check if this is an adjustment transaction
     */
    fun isAdjustment(): Boolean {
        return transactionType == Constants.TXN_TYPE_ADJUSTMENT
    }

    /**
     * Check if this is a physical count transaction
     */
    fun isPhysicalCount(): Boolean {
        return transactionType == Constants.TXN_TYPE_COUNT
    }

    /**
     * Calculate total cost from quantity and unit cost
     */
    fun calculateTotalCost() {
        totalCost = quantity.multiply(unitCost)
    }
}

/**
 * JPA Converter for List<String> to JSON
 * Handles serialization/deserialization of serial numbers array
 */
@Converter
class StringListJsonConverter : jakarta.persistence.AttributeConverter<List<String>?, String?> {

    override fun convertToDatabaseColumn(attribute: List<String>?): String? {
        if (attribute.isNullOrEmpty()) return null

        // Simple JSON array serialization
        return attribute.joinToString(
            separator = ",",
            prefix = "[\"",
            postfix = "\"]",
            transform = { it.replace("\"", "\\\"") }
        )
    }

    override fun convertToEntityAttribute(dbData: String?): List<String>? {
        if (dbData.isNullOrBlank() || dbData == "null") return null

        // Simple JSON array deserialization
        val cleaned = dbData.trim().removeSurrounding("[", "]").trim()
        if (cleaned.isEmpty()) return emptyList()

        return cleaned
            .split("\",\"")
            .map { it.trim().removeSurrounding("\"").replace("\\\"", "\"") }
    }
}
