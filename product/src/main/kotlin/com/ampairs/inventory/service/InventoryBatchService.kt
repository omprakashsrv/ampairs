package com.ampairs.inventory.service

import com.ampairs.inventory.config.Constants
import com.ampairs.inventory.domain.dto.BatchUpdateRequest
import com.ampairs.inventory.domain.dto.InventoryBatchRequest
import com.ampairs.inventory.domain.model.InventoryBatch
import com.ampairs.inventory.repository.InventoryBatchRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

/**
 * Inventory Batch Service
 *
 * Business logic layer for batch/lot tracking management.
 * Handles batch allocation using FIFO/FEFO/LIFO strategies.
 *
 * Key Responsibilities:
 * - Create and manage batches
 * - Allocate stock from batches (FIFO/FEFO/LIFO)
 * - Track batch expiry dates
 * - Reserve and release batch stock
 * - Provide batch availability information
 * - Mark expired batches
 *
 * Integration Points:
 * - InventoryConfigService for consumption strategy
 * - InventoryItemService for item validation
 * - WarehouseService for warehouse validation
 */
@Service
class InventoryBatchService @Autowired constructor(
    private val inventoryBatchRepository: InventoryBatchRepository,
    private val inventoryItemService: InventoryItemService,
    private val warehouseService: WarehouseService,
    private val inventoryConfigService: InventoryConfigService
) {

    // ============================================================================
    // Query Methods
    // ============================================================================

    /**
     * Get batch by UID
     *
     * @param uid Batch UID
     * @return InventoryBatch
     * @throws IllegalArgumentException if batch not found
     */
    fun getBatchByUid(uid: String): InventoryBatch {
        return inventoryBatchRepository.findByUid(uid)
            ?: throw IllegalArgumentException("Batch not found: $uid")
    }

    /**
     * Get batch by batch number, item, and warehouse
     *
     * @param batchNumber Batch number
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return InventoryBatch if found, null otherwise
     */
    fun getBatchByNumber(
        batchNumber: String,
        inventoryItemId: String,
        warehouseId: String
    ): InventoryBatch? {
        return inventoryBatchRepository.findByBatchNumberAndInventoryItemIdAndWarehouseId(
            batchNumber,
            inventoryItemId,
            warehouseId
        )
    }

    /**
     * Get all active batches for an inventory item at a warehouse
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return List of active batches
     */
    fun getBatchesByItemAndWarehouse(
        inventoryItemId: String,
        warehouseId: String
    ): List<InventoryBatch> {
        return inventoryBatchRepository.findByInventoryItemIdAndWarehouseIdAndIsActiveTrue(
            inventoryItemId,
            warehouseId
        )
    }

    /**
     * Get all active batches for an inventory item (all warehouses)
     *
     * @param inventoryItemId Inventory item UID
     * @return List of active batches
     */
    fun getBatchesByItem(inventoryItemId: String): List<InventoryBatch> {
        return inventoryBatchRepository.findByInventoryItemIdAndIsActiveTrue(inventoryItemId)
    }

    /**
     * Get batches expiring within specified days
     *
     * @param days Days until expiry
     * @return List of expiring batches
     */
    fun getExpiringBatches(days: Int): List<InventoryBatch> {
        val alertDate = Instant.now().plusSeconds((days * 24 * 60 * 60).toLong())
        return inventoryBatchRepository.findExpiringBatches(alertDate)
    }

    // ============================================================================
    // CRUD Operations
    // ============================================================================

    /**
     * Create a new batch
     *
     * @param request Batch creation request
     * @return Created batch
     * @throws IllegalArgumentException if batch number exists or item/warehouse not found
     */
    @Transactional
    fun createBatch(request: InventoryBatchRequest): InventoryBatch {
        // Validate inventory item exists
        inventoryItemService.getInventoryItemByUid(request.inventoryItemId)

        // Validate warehouse exists
        warehouseService.getWarehouseByUid(request.warehouseId)

        // Check if batch number already exists for this item and warehouse
        if (inventoryBatchRepository.existsByBatchNumberAndInventoryItemIdAndWarehouseId(
                request.batchNumber,
                request.inventoryItemId,
                request.warehouseId
            )
        ) {
            throw IllegalArgumentException(
                "Batch number ${request.batchNumber} already exists for this item and warehouse"
            )
        }

        // Create batch entity
        val batch = InventoryBatch().apply {
            this.batchNumber = request.batchNumber
            this.lotNumber = request.lotNumber
            this.inventoryItemId = request.inventoryItemId
            this.warehouseId = request.warehouseId
            this.totalQuantity = request.quantity
            this.availableQuantity = request.quantity
            this.reservedQuantity = BigDecimal.ZERO
            this.manufacturingDate = request.manufacturingDate
            this.expiryDate = request.expiryDate
            this.receivedDate = request.receivedDate
            this.supplierId = request.supplierId
            this.supplierName = request.supplierName
            this.purchaseOrderNumber = request.purchaseOrderNumber
            this.costPerUnit = request.costPerUnit
            this.attributes = request.attributes
            this.isActive = true
            this.isExpired = false
        }

        // Check if already expired
        if (batch.hasExpired()) {
            batch.isExpired = true
        }

        return inventoryBatchRepository.save(batch)
    }

    /**
     * Update batch information
     *
     * @param uid Batch UID
     * @param request Update request
     * @return Updated batch
     * @throws IllegalArgumentException if batch not found
     */
    @Transactional
    fun updateBatch(uid: String, request: BatchUpdateRequest): InventoryBatch {
        val batch = getBatchByUid(uid)

        // Update fields if provided
        request.lotNumber?.let { batch.lotNumber = it }
        request.manufacturingDate?.let { batch.manufacturingDate = it }
        request.expiryDate?.let { batch.expiryDate = it }
        request.supplierId?.let { batch.supplierId = it }
        request.supplierName?.let { batch.supplierName = it }
        request.purchaseOrderNumber?.let { batch.purchaseOrderNumber = it }
        request.costPerUnit?.let { batch.costPerUnit = it }
        request.isActive?.let { batch.isActive = it }
        request.attributes?.let { batch.attributes = it }

        // Recheck expiry status
        if (batch.hasExpired() && !batch.isExpired) {
            batch.isExpired = true
        }

        return inventoryBatchRepository.save(batch)
    }

    /**
     * Soft delete batch (mark as inactive)
     *
     * @param uid Batch UID
     * @throws IllegalArgumentException if batch not found
     * @throws IllegalStateException if batch has available stock
     */
    @Transactional
    fun deleteBatch(uid: String) {
        val batch = getBatchByUid(uid)

        if (batch.availableQuantity > BigDecimal.ZERO || batch.reservedQuantity > BigDecimal.ZERO) {
            throw IllegalStateException(
                "Cannot delete batch with available or reserved stock. " +
                "Available: ${batch.availableQuantity}, Reserved: ${batch.reservedQuantity}"
            )
        }

        batch.isActive = false
        inventoryBatchRepository.save(batch)
    }

    // ============================================================================
    // Batch Allocation Methods
    // ============================================================================

    /**
     * Allocate stock from batches based on configured strategy
     *
     * This method finds and allocates the required quantity from available batches
     * using the tenant's configured consumption strategy (FIFO/FEFO/LIFO/MANUAL)
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @param quantity Quantity to allocate
     * @param strategy Optional strategy override (defaults to tenant config)
     * @return List of batch allocations (batchUid -> allocated quantity)
     * @throws IllegalStateException if insufficient stock across all batches
     */
    @Transactional
    fun allocateBatches(
        inventoryItemId: String,
        warehouseId: String,
        quantity: BigDecimal,
        strategy: String? = null
    ): List<Pair<String, BigDecimal>> {
        // Get consumption strategy
        val consumptionStrategy = strategy ?: inventoryConfigService.getStockConsumptionStrategy()

        // Get available batches based on strategy
        val batches = when (consumptionStrategy) {
            Constants.STRATEGY_FIFO -> inventoryBatchRepository.findAvailableBatchesFIFO(
                inventoryItemId,
                warehouseId
            )
            Constants.STRATEGY_FEFO -> inventoryBatchRepository.findAvailableBatchesFEFO(
                inventoryItemId,
                warehouseId
            )
            Constants.STRATEGY_LIFO -> inventoryBatchRepository.findAvailableBatchesLIFO(
                inventoryItemId,
                warehouseId
            )
            else -> inventoryBatchRepository.findAvailableBatchesFIFO(
                inventoryItemId,
                warehouseId
            ) // Default to FIFO
        }

        if (batches.isEmpty()) {
            throw IllegalStateException("No available batches found for item $inventoryItemId")
        }

        // Allocate across batches
        val allocations = mutableListOf<Pair<String, BigDecimal>>()
        var remaining = quantity

        for (batch in batches) {
            if (remaining <= BigDecimal.ZERO) break

            val allocateQty = remaining.min(batch.availableQuantity)
            batch.consume(allocateQty, fromReserved = false)
            inventoryBatchRepository.save(batch)

            allocations.add(Pair(batch.uid, allocateQty))
            remaining = remaining.subtract(allocateQty)
        }

        if (remaining > BigDecimal.ZERO) {
            throw IllegalStateException(
                "Insufficient stock across all batches. " +
                "Required: $quantity, Available: ${quantity.subtract(remaining)}"
            )
        }

        return allocations
    }

    /**
     * Reserve stock from batches
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @param quantity Quantity to reserve
     * @param strategy Optional strategy override
     * @return List of batch reservations (batchUid -> reserved quantity)
     */
    @Transactional
    fun reserveBatches(
        inventoryItemId: String,
        warehouseId: String,
        quantity: BigDecimal,
        strategy: String? = null
    ): List<Pair<String, BigDecimal>> {
        // Get consumption strategy
        val consumptionStrategy = strategy ?: inventoryConfigService.getStockConsumptionStrategy()

        // Get available batches based on strategy
        val batches = when (consumptionStrategy) {
            Constants.STRATEGY_FIFO -> inventoryBatchRepository.findAvailableBatchesFIFO(
                inventoryItemId,
                warehouseId
            )
            Constants.STRATEGY_FEFO -> inventoryBatchRepository.findAvailableBatchesFEFO(
                inventoryItemId,
                warehouseId
            )
            Constants.STRATEGY_LIFO -> inventoryBatchRepository.findAvailableBatchesLIFO(
                inventoryItemId,
                warehouseId
            )
            else -> inventoryBatchRepository.findAvailableBatchesFIFO(
                inventoryItemId,
                warehouseId
            )
        }

        val reservations = mutableListOf<Pair<String, BigDecimal>>()
        var remaining = quantity

        for (batch in batches) {
            if (remaining <= BigDecimal.ZERO) break

            val reserveQty = remaining.min(batch.availableQuantity)
            batch.reserve(reserveQty)
            inventoryBatchRepository.save(batch)

            reservations.add(Pair(batch.uid, reserveQty))
            remaining = remaining.subtract(reserveQty)
        }

        if (remaining > BigDecimal.ZERO) {
            throw IllegalStateException(
                "Insufficient stock to reserve. " +
                "Required: $quantity, Available: ${quantity.subtract(remaining)}"
            )
        }

        return reservations
    }

    /**
     * Release reserved stock from batches
     *
     * @param reservations List of batch reservations to release
     */
    @Transactional
    fun releaseReservations(reservations: List<Pair<String, BigDecimal>>) {
        for ((batchUid, quantity) in reservations) {
            val batch = getBatchByUid(batchUid)
            batch.releaseReserved(quantity)
            inventoryBatchRepository.save(batch)
        }
    }

    // ============================================================================
    // Expiry Management
    // ============================================================================

    /**
     * Mark expired batches
     *
     * Scheduled job to automatically mark batches as expired
     *
     * @return Number of batches marked as expired
     */
    @Transactional
    fun markExpiredBatches(): Int {
        val expiredBatches = inventoryBatchRepository.findExpiredBatches()
        var count = 0

        for (batch in expiredBatches) {
            batch.isExpired = true
            inventoryBatchRepository.save(batch)
            count++
        }

        return count
    }

    /**
     * Get expiry alert for batches expiring soon
     *
     * @param days Days threshold for expiry alert
     * @return List of batches expiring within the threshold
     */
    fun getExpiryAlerts(days: Int): List<InventoryBatch> {
        return getExpiringBatches(days)
    }
}
