package com.ampairs.inventory.service

import com.ampairs.inventory.config.Constants
import com.ampairs.inventory.domain.dto.*
import com.ampairs.inventory.domain.model.InventorySerial
import com.ampairs.inventory.repository.InventorySerialRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

/**
 * Inventory Serial Service
 *
 * Business logic layer for serial number tracking and lifecycle management.
 * Handles individual unit tracking from receipt to sale/disposal.
 *
 * Key Responsibilities:
 * - Create and manage serial numbers
 * - Track serial lifecycle (AVAILABLE → RESERVED → SOLD)
 * - Allocate serials for sales (FIFO by received date)
 * - Handle returns and damages
 * - Warranty tracking and alerts
 * - Serial number validation (uniqueness)
 *
 * Integration Points:
 * - InventoryItemService for item validation
 * - WarehouseService for warehouse validation
 * - InventoryBatchService for batch association (optional)
 */
@Service
class InventorySerialService @Autowired constructor(
    private val inventorySerialRepository: InventorySerialRepository,
    private val inventoryItemService: InventoryItemService,
    private val warehouseService: WarehouseService
) {

    // ============================================================================
    // Query Methods
    // ============================================================================

    /**
     * Get serial by UID
     *
     * @param uid Serial UID
     * @return InventorySerial
     * @throws IllegalArgumentException if serial not found
     */
    fun getSerialByUid(uid: String): InventorySerial {
        return inventorySerialRepository.findByUid(uid)
            ?: throw IllegalArgumentException("Serial not found: $uid")
    }

    /**
     * Get serial by serial number
     *
     * @param serialNumber Serial number
     * @return InventorySerial
     * @throws IllegalArgumentException if serial not found
     */
    fun getSerialByNumber(serialNumber: String): InventorySerial {
        return inventorySerialRepository.findBySerialNumber(serialNumber)
            ?: throw IllegalArgumentException("Serial number not found: $serialNumber")
    }

    /**
     * Find serial by serial number (returns null if not found)
     *
     * @param serialNumber Serial number
     * @return InventorySerial if found, null otherwise
     */
    fun findSerialByNumber(serialNumber: String): InventorySerial? {
        return inventorySerialRepository.findBySerialNumber(serialNumber)
    }

    /**
     * Check if serial number exists
     *
     * @param serialNumber Serial number
     * @return true if exists, false otherwise
     */
    fun serialNumberExists(serialNumber: String): Boolean {
        return inventorySerialRepository.existsBySerialNumber(serialNumber)
    }

    /**
     * Get all serials for an inventory item
     *
     * @param inventoryItemId Inventory item UID
     * @return List of serials
     */
    fun getSerialsByItem(inventoryItemId: String): List<InventorySerial> {
        return inventorySerialRepository.findByInventoryItemId(inventoryItemId)
    }

    /**
     * Get serials for an item at a warehouse
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return List of serials
     */
    fun getSerialsByItemAndWarehouse(
        inventoryItemId: String,
        warehouseId: String
    ): List<InventorySerial> {
        return inventorySerialRepository.findByInventoryItemIdAndWarehouseId(
            inventoryItemId,
            warehouseId
        )
    }

    /**
     * Get available serials for an item at a warehouse
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return List of available serials (FIFO order)
     */
    fun getAvailableSerials(
        inventoryItemId: String,
        warehouseId: String
    ): List<InventorySerial> {
        return inventorySerialRepository.findAvailableSerials(inventoryItemId, warehouseId)
    }

    /**
     * Get serials sold to a customer
     *
     * @param customerId Customer UID
     * @return List of serials
     */
    fun getSerialsByCustomer(customerId: String): List<InventorySerial> {
        return inventorySerialRepository.findByCustomerId(customerId)
    }

    // ============================================================================
    // CRUD Operations
    // ============================================================================

    /**
     * Create a single serial number
     *
     * @param request Serial creation request
     * @return Created serial
     * @throws IllegalArgumentException if serial number exists or item/warehouse not found
     */
    @Transactional
    fun createSerial(request: SerialRequest): InventorySerial {
        // Validate inventory item exists
        inventoryItemService.getInventoryItemByUid(request.inventoryItemId)

        // Validate warehouse exists
        warehouseService.getWarehouseByUid(request.warehouseId)

        // Check if serial number already exists
        if (inventorySerialRepository.existsBySerialNumber(request.serialNumber)) {
            throw IllegalArgumentException(
                "Serial number ${request.serialNumber} already exists"
            )
        }

        // Create serial entity
        val serial = request.toInventorySerial()

        return inventorySerialRepository.save(serial)
    }

    /**
     * Create multiple serial numbers in bulk
     *
     * @param request Bulk serial creation request
     * @return List of created serials
     * @throws IllegalArgumentException if any serial exists or item/warehouse not found
     */
    @Transactional
    fun createBulkSerials(request: BulkSerialRequest): List<InventorySerial> {
        // Validate inventory item exists
        inventoryItemService.getInventoryItemByUid(request.inventoryItemId)

        // Validate warehouse exists
        warehouseService.getWarehouseByUid(request.warehouseId)

        // Check if any serial numbers already exist
        val existingSerials = inventorySerialRepository.findBySerialNumberIn(request.serialNumbers)
        if (existingSerials.isNotEmpty()) {
            val existingNumbers = existingSerials.map { it.serialNumber }
            throw IllegalArgumentException(
                "The following serial numbers already exist: ${existingNumbers.joinToString(", ")}"
            )
        }

        // Create serial entities
        val serials = request.serialNumbers.map { serialNumber ->
            InventorySerial().apply {
                this.serialNumber = serialNumber
                this.inventoryItemId = request.inventoryItemId
                this.warehouseId = request.warehouseId
                this.batchId = request.batchId
                this.costPrice = request.costPrice
                this.sellingPrice = request.sellingPrice
                this.receivedDate = request.receivedDate
                this.warrantyExpiryDate = request.warrantyExpiryDate
                this.notes = request.notes
                this.attributes = request.attributes
                this.status = Constants.SERIAL_AVAILABLE
            }
        }

        return inventorySerialRepository.saveAll(serials).toList()
    }

    /**
     * Update serial information
     *
     * @param uid Serial UID
     * @param request Update request
     * @return Updated serial
     * @throws IllegalArgumentException if serial not found
     */
    @Transactional
    fun updateSerial(uid: String, request: SerialUpdateRequest): InventorySerial {
        val serial = getSerialByUid(uid)

        // Update fields if provided
        request.batchId?.let { serial.batchId = it }
        request.costPrice?.let { serial.costPrice = it }
        request.sellingPrice?.let { serial.sellingPrice = it }
        request.warrantyExpiryDate?.let { serial.warrantyExpiryDate = it }
        request.notes?.let { serial.notes = it }
        request.attributes?.let { serial.attributes = it }

        return inventorySerialRepository.save(serial)
    }

    /**
     * Delete serial (only if AVAILABLE status)
     *
     * @param uid Serial UID
     * @throws IllegalArgumentException if serial not found
     * @throws IllegalStateException if serial is not available
     */
    @Transactional
    fun deleteSerial(uid: String) {
        val serial = getSerialByUid(uid)

        if (!serial.isAvailable()) {
            throw IllegalStateException(
                "Cannot delete serial ${serial.serialNumber} with status ${serial.status}. " +
                "Only AVAILABLE serials can be deleted."
            )
        }

        inventorySerialRepository.delete(serial)
    }

    // ============================================================================
    // Serial Allocation Methods
    // ============================================================================

    /**
     * Allocate serials for sale/consumption
     *
     * Finds and allocates the required quantity of available serials using FIFO
     * (First In, First Out) based on received date.
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @param quantity Quantity needed
     * @return List of allocated serial numbers
     * @throws IllegalStateException if insufficient serials available
     */
    @Transactional
    fun allocateSerials(
        inventoryItemId: String,
        warehouseId: String,
        quantity: Int
    ): List<String> {
        val availableSerials = inventorySerialRepository.findAvailableSerials(
            inventoryItemId,
            warehouseId
        )

        if (availableSerials.size < quantity) {
            throw IllegalStateException(
                "Insufficient available serials. Required: $quantity, Available: ${availableSerials.size}"
            )
        }

        // Take first N serials (FIFO - oldest received first)
        return availableSerials.take(quantity).map { it.serialNumber }
    }

    /**
     * Reserve serials for an order
     *
     * @param serialNumbers List of serial numbers to reserve
     * @throws IllegalStateException if any serial is not available
     */
    @Transactional
    fun reserveSerials(serialNumbers: List<String>) {
        val serials = inventorySerialRepository.findBySerialNumberIn(serialNumbers)

        if (serials.size != serialNumbers.size) {
            val found = serials.map { it.serialNumber }
            val missing = serialNumbers.filterNot { it in found }
            throw IllegalArgumentException("Serial numbers not found: ${missing.joinToString(", ")}")
        }

        for (serial in serials) {
            serial.reserve()
            inventorySerialRepository.save(serial)
        }
    }

    /**
     * Release reserved serials
     *
     * @param serialNumbers List of serial numbers to release
     */
    @Transactional
    fun releaseReservations(serialNumbers: List<String>) {
        val serials = inventorySerialRepository.findBySerialNumberIn(serialNumbers)

        for (serial in serials) {
            if (serial.isReserved()) {
                serial.releaseReservation()
                inventorySerialRepository.save(serial)
            }
        }
    }

    // ============================================================================
    // Lifecycle Management Methods
    // ============================================================================

    /**
     * Mark serial as sold
     *
     * @param request Sale request
     * @return Updated serial
     * @throws IllegalArgumentException if serial not found
     * @throws IllegalStateException if serial cannot be sold
     */
    @Transactional
    fun markSerialAsSold(request: SerialSaleRequest): InventorySerial {
        val serial = getSerialByNumber(request.serialNumber)

        // Update selling price if provided
        request.sellingPrice?.let { serial.sellingPrice = it }

        // Mark as sold
        serial.markAsSold(
            referenceType = request.referenceType,
            referenceId = request.referenceId,
            referenceNumber = request.referenceNumber,
            customerId = request.customerId,
            customerName = request.customerName
        )

        return inventorySerialRepository.save(serial)
    }

    /**
     * Mark multiple serials as sold
     *
     * @param serialNumbers List of serial numbers
     * @param referenceType Reference type (ORDER, INVOICE)
     * @param referenceId Reference UID
     * @param referenceNumber Human-readable reference
     * @param customerId Customer UID
     * @param customerName Customer name
     * @return List of updated serials
     */
    @Transactional
    fun markSerialsAsSold(
        serialNumbers: List<String>,
        referenceType: String,
        referenceId: String,
        referenceNumber: String? = null,
        customerId: String? = null,
        customerName: String? = null
    ): List<InventorySerial> {
        val serials = inventorySerialRepository.findBySerialNumberIn(serialNumbers)

        if (serials.size != serialNumbers.size) {
            val found = serials.map { it.serialNumber }
            val missing = serialNumbers.filterNot { it in found }
            throw IllegalArgumentException("Serial numbers not found: ${missing.joinToString(", ")}")
        }

        for (serial in serials) {
            serial.markAsSold(
                referenceType = referenceType,
                referenceId = referenceId,
                referenceNumber = referenceNumber,
                customerId = customerId,
                customerName = customerName
            )
        }

        return inventorySerialRepository.saveAll(serials).toList()
    }

    /**
     * Mark serial as returned
     *
     * @param request Return request
     * @return Updated serial
     * @throws IllegalArgumentException if serial not found
     * @throws IllegalStateException if serial is not sold
     */
    @Transactional
    fun markSerialAsReturned(request: SerialReturnRequest): InventorySerial {
        val serial = getSerialByNumber(request.serialNumber)

        serial.markAsReturned(
            referenceType = request.referenceType,
            referenceId = request.referenceId,
            notes = request.notes
        )

        return inventorySerialRepository.save(serial)
    }

    /**
     * Mark serial as damaged
     *
     * @param serialNumber Serial number
     * @param notes Damage notes
     * @return Updated serial
     * @throws IllegalArgumentException if serial not found
     */
    @Transactional
    fun markSerialAsDamaged(serialNumber: String, notes: String? = null): InventorySerial {
        val serial = getSerialByNumber(serialNumber)
        serial.markAsDamaged(notes)
        return inventorySerialRepository.save(serial)
    }

    /**
     * Make serial available again (for returns, repairs, etc.)
     *
     * @param serialNumber Serial number
     * @return Updated serial
     * @throws IllegalArgumentException if serial not found
     * @throws IllegalStateException if serial cannot be made available
     */
    @Transactional
    fun makeSerialAvailable(serialNumber: String): InventorySerial {
        val serial = getSerialByNumber(serialNumber)
        serial.makeAvailable()
        return inventorySerialRepository.save(serial)
    }

    // ============================================================================
    // Warranty Management
    // ============================================================================

    /**
     * Get serials with expiring warranty
     *
     * @param days Days until expiry
     * @return List of serials
     */
    fun getSerialsWithExpiringWarranty(days: Int): List<InventorySerial> {
        val alertDate = Instant.now().plusSeconds((days * 24 * 60 * 60).toLong())
        return inventorySerialRepository.findSerialsWithExpiringWarranty(alertDate)
    }

    /**
     * Get customer's serials with active warranty
     *
     * @param customerId Customer UID
     * @return List of serials
     */
    fun getCustomerSerialsWithActiveWarranty(customerId: String): List<InventorySerial> {
        return inventorySerialRepository.findCustomerSerialsWithActiveWarranty(customerId)
    }

    // ============================================================================
    // Statistics and Summary
    // ============================================================================

    /**
     * Get serial status summary for an item
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return Status summary
     */
    fun getSerialStatusSummary(
        inventoryItemId: String,
        warehouseId: String
    ): SerialStatusSummary {
        val totalSerials = inventorySerialRepository.countByInventoryItemIdAndWarehouseId(
            inventoryItemId,
            warehouseId
        )

        val availableSerials = inventorySerialRepository.countByInventoryItemIdAndWarehouseIdAndStatus(
            inventoryItemId,
            warehouseId,
            Constants.SERIAL_AVAILABLE
        )

        val reservedSerials = inventorySerialRepository.countByInventoryItemIdAndWarehouseIdAndStatus(
            inventoryItemId,
            warehouseId,
            Constants.SERIAL_RESERVED
        )

        val soldSerials = inventorySerialRepository.countByInventoryItemIdAndWarehouseIdAndStatus(
            inventoryItemId,
            warehouseId,
            Constants.SERIAL_SOLD
        )

        val damagedSerials = inventorySerialRepository.countByInventoryItemIdAndWarehouseIdAndStatus(
            inventoryItemId,
            warehouseId,
            Constants.SERIAL_DAMAGED
        )

        val returnedSerials = inventorySerialRepository.countByInventoryItemIdAndWarehouseIdAndStatus(
            inventoryItemId,
            warehouseId,
            "RETURNED"
        )

        return SerialStatusSummary(
            inventoryItemId = inventoryItemId,
            warehouseId = warehouseId,
            totalSerials = totalSerials,
            availableSerials = availableSerials,
            reservedSerials = reservedSerials,
            soldSerials = soldSerials,
            damagedSerials = damagedSerials,
            returnedSerials = returnedSerials
        )
    }

    /**
     * Count available serials for an item at a warehouse
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return Count of available serials
     */
    fun countAvailableSerials(inventoryItemId: String, warehouseId: String): Long {
        return inventorySerialRepository.countAvailableSerials(inventoryItemId, warehouseId)
    }

    // ============================================================================
    // Helper Methods for Transaction Integration
    // ============================================================================

    /**
     * Find serials by serial numbers list
     *
     * @param serialNumbers List of serial numbers
     * @return List of serials
     */
    fun findSerialsByNumbers(serialNumbers: List<String>): List<InventorySerial> {
        return inventorySerialRepository.findBySerialNumberIn(serialNumbers)
    }

    /**
     * Update serial's warehouse location
     *
     * Used during stock transfers
     *
     * @param uid Serial UID
     * @param warehouseId New warehouse UID
     * @return Updated serial
     */
    @Transactional
    fun updateSerialWarehouse(uid: String, warehouseId: String): InventorySerial {
        val serial = getSerialByUid(uid)
        serial.warehouseId = warehouseId
        return inventorySerialRepository.save(serial)
    }
}
