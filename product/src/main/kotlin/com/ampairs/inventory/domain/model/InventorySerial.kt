package com.ampairs.inventory.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.inventory.config.Constants
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

/**
 * Inventory Serial Entity
 *
 * Represents individual serial numbers for serial-tracked inventory items.
 * Tracks complete lifecycle from receiving to sale/disposal.
 *
 * Key Features:
 * - Unique serial number tracking
 * - Status lifecycle management (AVAILABLE → RESERVED → SOLD)
 * - Warranty tracking
 * - Cost and price tracking per unit
 * - Batch association (optional)
 * - Reference document linking
 * - Return handling
 *
 * Serial Number Lifecycle:
 * 1. AVAILABLE - In stock, ready for sale
 * 2. RESERVED - Allocated to an order but not yet sold
 * 3. SOLD - Sold to customer
 * 4. DAMAGED - Damaged/defective
 * 5. RETURNED - Returned by customer
 *
 * @property serialNumber Unique serial number
 * @property inventoryItemId Reference to inventory item
 * @property warehouseId Warehouse location
 * @property batchId Optional batch reference
 * @property status Current status (AVAILABLE, RESERVED, SOLD, DAMAGED, RETURNED)
 * @property receivedDate Date serial was received
 * @property soldDate Date serial was sold
 * @property warrantyExpiryDate Warranty expiry date
 * @property soldReferenceType Type of sale document (ORDER, INVOICE)
 * @property soldReferenceId UID of sale document
 * @property costPrice Cost price of this unit
 * @property sellingPrice Selling price of this unit
 */
@Entity(name = "inventory_serial")
@Table(
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_serial_number",
            columnNames = ["serial_number", "owner_id"]
        )
    ]
)
class InventorySerial : OwnableBaseDomain() {

    // ============================================================================
    // Serial Number Identification
    // ============================================================================

    /**
     * Serial number (unique per tenant)
     */
    @Column(name = "serial_number", unique = true, nullable = false, length = 100)
    var serialNumber: String = ""

    // ============================================================================
    // Inventory Item Reference
    // ============================================================================

    /**
     * Inventory item UID
     */
    @Column(name = "inventory_item_id", nullable = false, length = 200)
    var inventoryItemId: String = ""

    // ============================================================================
    // Warehouse/Location
    // ============================================================================

    /**
     * Warehouse UID where serial is located
     */
    @Column(name = "warehouse_id", nullable = false, length = 200)
    var warehouseId: String = ""

    // ============================================================================
    // Batch Association (Optional)
    // ============================================================================

    /**
     * Batch UID (if serial belongs to a batch)
     */
    @Column(name = "batch_id", length = 200)
    var batchId: String? = null

    // ============================================================================
    // Status Lifecycle
    // ============================================================================

    /**
     * Current status
     * Values: AVAILABLE, RESERVED, SOLD, DAMAGED, RETURNED
     */
    @Column(name = "status", nullable = false, length = 20)
    var status: String = Constants.SERIAL_AVAILABLE

    // ============================================================================
    // Dates
    // ============================================================================

    /**
     * Date serial was received into inventory
     */
    @Column(name = "received_date", nullable = false)
    var receivedDate: Instant = Instant.now()

    /**
     * Date serial was sold
     */
    @Column(name = "sold_date")
    var soldDate: Instant? = null

    /**
     * Warranty expiry date
     */
    @Column(name = "warranty_expiry_date")
    var warrantyExpiryDate: Instant? = null

    /**
     * Date serial was returned (if applicable)
     */
    @Column(name = "returned_date")
    var returnedDate: Instant? = null

    // ============================================================================
    // References (Link to Sale/Order Documents)
    // ============================================================================

    /**
     * Type of sale reference (ORDER, INVOICE)
     */
    @Column(name = "sold_reference_type", length = 50)
    var soldReferenceType: String? = null

    /**
     * UID of sale document
     */
    @Column(name = "sold_reference_id", length = 200)
    var soldReferenceId: String? = null

    /**
     * Human-readable sale reference number
     */
    @Column(name = "sold_reference_number", length = 100)
    var soldReferenceNumber: String? = null

    /**
     * Type of return reference (if returned)
     */
    @Column(name = "return_reference_type", length = 50)
    var returnReferenceType: String? = null

    /**
     * UID of return document (if returned)
     */
    @Column(name = "return_reference_id", length = 200)
    var returnReferenceId: String? = null

    // ============================================================================
    // Customer Information (Optional)
    // ============================================================================

    /**
     * Customer UID (who purchased this serial)
     */
    @Column(name = "customer_id", length = 200)
    var customerId: String? = null

    /**
     * Customer name (denormalized for quick access)
     */
    @Column(name = "customer_name", length = 200)
    var customerName: String? = null

    // ============================================================================
    // Pricing
    // ============================================================================

    /**
     * Cost price of this specific unit
     */
    @Column(name = "cost_price", precision = 15, scale = 2)
    var costPrice: BigDecimal = BigDecimal.ZERO

    /**
     * Selling price of this specific unit
     */
    @Column(name = "selling_price", precision = 15, scale = 2)
    var sellingPrice: BigDecimal = BigDecimal.ZERO

    // ============================================================================
    // Additional Information
    // ============================================================================

    /**
     * Notes about this serial (condition, issues, etc.)
     */
    @Column(name = "notes", length = 1000)
    var notes: String? = null

    /**
     * Extensible attributes (JSON)
     */
    @Column(name = "attributes", columnDefinition = "JSON")
    var attributes: Map<String, Any>? = null

    // ============================================================================
    // Base Domain Methods
    // ============================================================================

    override fun obtainSeqIdPrefix(): String = Constants.SERIAL_PREFIX

    // ============================================================================
    // Status Check Methods
    // ============================================================================

    /**
     * Check if serial is available for sale
     */
    fun isAvailable(): Boolean {
        return status == Constants.SERIAL_AVAILABLE
    }

    /**
     * Check if serial is reserved
     */
    fun isReserved(): Boolean {
        return status == Constants.SERIAL_RESERVED
    }

    /**
     * Check if serial has been sold
     */
    fun isSold(): Boolean {
        return status == Constants.SERIAL_SOLD
    }

    /**
     * Check if serial is damaged
     */
    fun isDamaged(): Boolean {
        return status == Constants.SERIAL_DAMAGED
    }

    /**
     * Check if serial was returned
     */
    fun isReturned(): Boolean {
        return status == "RETURNED"
    }

    /**
     * Check if warranty has expired
     */
    fun hasWarrantyExpired(): Boolean {
        if (warrantyExpiryDate == null) return false
        return Instant.now().isAfter(warrantyExpiryDate)
    }

    /**
     * Check if warranty is expiring soon (within given days)
     */
    fun isWarrantyExpiringSoon(days: Int): Boolean {
        if (warrantyExpiryDate == null) return false
        val threshold = Instant.now().plusSeconds((days * 24 * 60 * 60).toLong())
        return warrantyExpiryDate!!.isBefore(threshold) && !hasWarrantyExpired()
    }

    // ============================================================================
    // Status Transition Methods
    // ============================================================================

    /**
     * Reserve this serial for an order
     *
     * @throws IllegalStateException if serial is not available
     */
    fun reserve() {
        if (!isAvailable()) {
            throw IllegalStateException(
                "Cannot reserve serial $serialNumber. Current status: $status"
            )
        }
        status = Constants.SERIAL_RESERVED
    }

    /**
     * Release reservation
     *
     * @throws IllegalStateException if serial is not reserved
     */
    fun releaseReservation() {
        if (!isReserved()) {
            throw IllegalStateException(
                "Cannot release reservation for serial $serialNumber. Current status: $status"
            )
        }
        status = Constants.SERIAL_AVAILABLE
    }

    /**
     * Mark serial as sold
     *
     * @param referenceType Type of sale document
     * @param referenceId UID of sale document
     * @param referenceNumber Human-readable reference
     * @param customerId Customer UID
     * @param customerName Customer name
     * @throws IllegalStateException if serial cannot be sold
     */
    fun markAsSold(
        referenceType: String,
        referenceId: String,
        referenceNumber: String? = null,
        customerId: String? = null,
        customerName: String? = null
    ) {
        if (!isAvailable() && !isReserved()) {
            throw IllegalStateException(
                "Cannot sell serial $serialNumber. Current status: $status"
            )
        }

        status = Constants.SERIAL_SOLD
        soldDate = Instant.now()
        soldReferenceType = referenceType
        soldReferenceId = referenceId
        soldReferenceNumber = referenceNumber
        this.customerId = customerId
        this.customerName = customerName
    }

    /**
     * Mark serial as damaged
     *
     * @param notes Reason for damage
     */
    fun markAsDamaged(notes: String? = null) {
        status = Constants.SERIAL_DAMAGED
        this.notes = notes
    }

    /**
     * Mark serial as returned
     *
     * @param referenceType Type of return document
     * @param referenceId UID of return document
     * @param notes Return notes
     */
    fun markAsReturned(
        referenceType: String,
        referenceId: String,
        notes: String? = null
    ) {
        if (!isSold()) {
            throw IllegalStateException(
                "Cannot mark serial $serialNumber as returned. Must be sold first. Current status: $status"
            )
        }

        status = "RETURNED"
        returnedDate = Instant.now()
        returnReferenceType = referenceType
        returnReferenceId = referenceId
        this.notes = notes
    }

    /**
     * Make available again (for returns, repairs, etc.)
     *
     * @throws IllegalStateException if serial cannot be made available
     */
    fun makeAvailable() {
        if (isSold()) {
            throw IllegalStateException(
                "Cannot make sold serial $serialNumber available. Use return process instead."
            )
        }

        status = Constants.SERIAL_AVAILABLE
    }
}
