package com.ampairs.inventory.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.inventory.config.Constants
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

/**
 * Inventory Batch Entity
 *
 * Represents batches/lots for batch-tracked inventory items.
 * Supports FIFO/FEFO/LIFO consumption strategies.
 *
 * Key Features:
 * - Batch/lot number tracking
 * - Expiry date management
 * - Manufacturing date tracking
 * - Stock level tracking per batch
 * - Supplier information
 * - Cost tracking per batch
 * - FIFO/FEFO allocation support
 *
 * @property batchNumber Unique batch number
 * @property lotNumber Optional lot number
 * @property inventoryItemId Reference to inventory item
 * @property warehouseId Warehouse location
 * @property totalQuantity Total quantity in batch
 * @property availableQuantity Available quantity (total - reserved - consumed)
 * @property reservedQuantity Reserved for orders
 * @property manufacturingDate Manufacturing date
 * @property expiryDate Expiry date
 * @property receivedDate Date batch was received
 * @property supplierId Supplier reference
 * @property supplierName Supplier name
 * @property purchaseOrderNumber PO reference
 * @property costPerUnit Cost per unit for this batch
 * @property isActive Active status
 * @property isExpired Expired status (auto-calculated)
 */
@Entity(name = "inventory_batch")
@NamedEntityGraph(
    name = "InventoryBatch.withItem",
    attributeNodes = [NamedAttributeNode("inventoryItem")]
)
@Table(
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_batch_number_item_warehouse",
            columnNames = ["batch_number", "inventory_item_id", "warehouse_id", "owner_id"]
        )
    ]
)
class InventoryBatch : OwnableBaseDomain() {

    // ============================================================================
    // Batch Identification
    // ============================================================================

    /**
     * Batch number (unique per item per warehouse per tenant)
     */
    @Column(name = "batch_number", nullable = false, length = 100)
    var batchNumber: String = ""

    /**
     * Lot number (optional additional identifier)
     */
    @Column(name = "lot_number", length = 100)
    var lotNumber: String? = null

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
     * Warehouse UID where batch is stored
     */
    @Column(name = "warehouse_id", nullable = false, length = 200)
    var warehouseId: String = ""

    // ============================================================================
    // Stock Tracking
    // ============================================================================

    /**
     * Total quantity in this batch
     */
    @Column(name = "total_quantity", nullable = false, precision = 15, scale = 3)
    var totalQuantity: BigDecimal = BigDecimal.ZERO

    /**
     * Available quantity (not reserved or consumed)
     */
    @Column(name = "available_quantity", nullable = false, precision = 15, scale = 3)
    var availableQuantity: BigDecimal = BigDecimal.ZERO

    /**
     * Reserved quantity (allocated to orders but not yet consumed)
     */
    @Column(name = "reserved_quantity", nullable = false, precision = 15, scale = 3)
    var reservedQuantity: BigDecimal = BigDecimal.ZERO

    // ============================================================================
    // Dates
    // ============================================================================

    /**
     * Manufacturing/production date
     */
    @Column(name = "manufacturing_date")
    var manufacturingDate: Instant? = null

    /**
     * Expiry date
     */
    @Column(name = "expiry_date")
    var expiryDate: Instant? = null

    /**
     * Date batch was received into inventory
     */
    @Column(name = "received_date", nullable = false)
    var receivedDate: Instant = Instant.now()

    // ============================================================================
    // Supplier Information
    // ============================================================================

    /**
     * Supplier UID (reference to supplier/vendor)
     */
    @Column(name = "supplier_id", length = 200)
    var supplierId: String? = null

    /**
     * Supplier name (denormalized for quick access)
     */
    @Column(name = "supplier_name", length = 200)
    var supplierName: String? = null

    /**
     * Purchase order number reference
     */
    @Column(name = "purchase_order_number", length = 100)
    var purchaseOrderNumber: String? = null

    // ============================================================================
    // Cost
    // ============================================================================

    /**
     * Cost per unit for this batch
     * Used for weighted average cost calculation
     */
    @Column(name = "cost_per_unit", precision = 15, scale = 2)
    var costPerUnit: BigDecimal = BigDecimal.ZERO

    // ============================================================================
    // Status
    // ============================================================================

    /**
     * Active status
     * Inactive batches are excluded from allocation
     */
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    /**
     * Expired status
     * Auto-set by scheduled job checking expiry dates
     */
    @Column(name = "is_expired", nullable = false)
    var isExpired: Boolean = false

    /**
     * Extensible attributes (JSON)
     */
    @Column(name = "attributes", columnDefinition = "JSON")
    var attributes: Map<String, Any>? = null

    // ============================================================================
    // Base Domain Methods
    // ============================================================================

    override fun obtainSeqIdPrefix(): String = Constants.BATCH_PREFIX

    // ============================================================================
    // Calculated Properties
    // ============================================================================

    /**
     * Calculate consumed quantity
     * Consumed = Total - Available - Reserved
     */
    fun calculateConsumedQuantity(): BigDecimal {
        return totalQuantity.subtract(availableQuantity).subtract(reservedQuantity)
    }

    /**
     * Check if batch has expired
     */
    fun hasExpired(): Boolean {
        if (expiryDate == null) return false
        return Instant.now().isAfter(expiryDate)
    }

    /**
     * Check if batch is expiring soon (within given days)
     */
    fun isExpiringSoon(days: Int): Boolean {
        if (expiryDate == null) return false
        val threshold = Instant.now().plusSeconds((days * 24 * 60 * 60).toLong())
        return expiryDate!!.isBefore(threshold) && !hasExpired()
    }

    /**
     * Check if batch has available stock
     */
    fun hasAvailableStock(): Boolean {
        return availableQuantity > BigDecimal.ZERO && isActive && !isExpired
    }

    /**
     * Recalculate available quantity
     * Available = Total - Reserved - Consumed
     */
    fun recalculateAvailableQuantity() {
        val consumed = calculateConsumedQuantity()
        availableQuantity = totalQuantity.subtract(reservedQuantity).subtract(consumed)
        if (availableQuantity < BigDecimal.ZERO) {
            availableQuantity = BigDecimal.ZERO
        }
    }

    /**
     * Reserve stock from this batch
     *
     * @param quantity Quantity to reserve
     * @throws IllegalStateException if insufficient available stock
     */
    fun reserve(quantity: BigDecimal) {
        if (availableQuantity < quantity) {
            throw IllegalStateException(
                "Insufficient available stock in batch $batchNumber. " +
                "Available: $availableQuantity, Requested: $quantity"
            )
        }
        availableQuantity = availableQuantity.subtract(quantity)
        reservedQuantity = reservedQuantity.add(quantity)
    }

    /**
     * Release reserved stock
     *
     * @param quantity Quantity to release
     */
    fun releaseReserved(quantity: BigDecimal) {
        val releaseAmount = quantity.min(reservedQuantity)
        reservedQuantity = reservedQuantity.subtract(releaseAmount)
        availableQuantity = availableQuantity.add(releaseAmount)
    }

    /**
     * Consume stock from this batch
     *
     * @param quantity Quantity to consume
     * @param fromReserved Whether to consume from reserved stock
     * @throws IllegalStateException if insufficient stock
     */
    fun consume(quantity: BigDecimal, fromReserved: Boolean = true) {
        if (fromReserved) {
            // Consume from reserved stock first
            val fromReservedQty = quantity.min(reservedQuantity)
            reservedQuantity = reservedQuantity.subtract(fromReservedQty)

            val remaining = quantity.subtract(fromReservedQty)
            if (remaining > BigDecimal.ZERO) {
                // Consume remaining from available
                if (availableQuantity < remaining) {
                    throw IllegalStateException(
                        "Insufficient stock in batch $batchNumber. " +
                        "Available: $availableQuantity, Required: $remaining"
                    )
                }
                availableQuantity = availableQuantity.subtract(remaining)
            }
        } else {
            // Consume directly from available
            if (availableQuantity < quantity) {
                throw IllegalStateException(
                    "Insufficient available stock in batch $batchNumber. " +
                    "Available: $availableQuantity, Requested: $quantity"
                )
            }
            availableQuantity = availableQuantity.subtract(quantity)
        }
    }

    /**
     * Add stock to this batch
     *
     * @param quantity Quantity to add
     */
    fun addStock(quantity: BigDecimal) {
        totalQuantity = totalQuantity.add(quantity)
        availableQuantity = availableQuantity.add(quantity)
    }
}
