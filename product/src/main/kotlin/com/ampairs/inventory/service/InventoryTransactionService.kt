package com.ampairs.inventory.service

import com.ampairs.inventory.config.Constants
import com.ampairs.inventory.domain.dto.*
import com.ampairs.inventory.domain.model.InventoryTransaction
import com.ampairs.inventory.repository.InventoryTransactionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Inventory Transaction Service
 *
 * Business logic layer for inventory transaction management.
 * Handles all stock movements:
 * - Stock-in (purchases, returns, opening stock)
 * - Stock-out (sales, damages, losses)
 * - Stock transfers (between warehouses)
 * - Stock adjustments (corrections)
 * - Physical counts (inventory reconciliation)
 *
 * Key Responsibilities:
 * - Process transactions and update inventory items
 * - Generate transaction numbers
 * - Validate stock availability
 * - Calculate balances
 * - Handle batch/serial tracking
 * - Maintain audit trail
 *
 * Integration Points:
 * - InventoryItemService for stock updates
 * - WarehouseService for warehouse validation
 * - InventoryConfigService for business rules
 */
@Service
class InventoryTransactionService @Autowired constructor(
    private val inventoryTransactionRepository: InventoryTransactionRepository,
    private val inventoryItemService: InventoryItemService,
    private val warehouseService: WarehouseService,
    private val inventoryConfigService: InventoryConfigService,
    private val inventorySerialService: InventorySerialService
) {

    // ============================================================================
    // Query Methods
    // ============================================================================

    /**
     * Get transaction by UID
     *
     * @param uid Transaction UID
     * @return InventoryTransaction if found
     * @throws IllegalArgumentException if transaction not found
     */
    fun getTransactionByUid(uid: String): InventoryTransaction {
        return inventoryTransactionRepository.findByUid(uid)
            ?: throw IllegalArgumentException("Transaction not found: $uid")
    }

    /**
     * Get transaction by transaction number
     *
     * @param transactionNumber Transaction number
     * @return InventoryTransaction if found
     * @throws IllegalArgumentException if transaction not found
     */
    fun getTransactionByNumber(transactionNumber: String): InventoryTransaction {
        return inventoryTransactionRepository.findByTransactionNumber(transactionNumber)
            ?: throw IllegalArgumentException("Transaction not found: $transactionNumber")
    }

    /**
     * Get transactions for an inventory item
     *
     * @param inventoryItemId Item UID
     * @param pageable Pagination parameters
     * @return Page of transactions
     */
    fun getTransactionsByItem(inventoryItemId: String, pageable: Pageable): Page<InventoryTransaction> {
        return inventoryTransactionRepository.findByInventoryItemIdOrderByTransactionDateDesc(
            inventoryItemId,
            pageable
        )
    }

    /**
     * Get transactions for a warehouse
     *
     * @param warehouseId Warehouse UID
     * @param pageable Pagination parameters
     * @return Page of transactions
     */
    fun getTransactionsByWarehouse(warehouseId: String, pageable: Pageable): Page<InventoryTransaction> {
        return inventoryTransactionRepository.findByWarehouseIdOrderByTransactionDateDesc(
            warehouseId,
            pageable
        )
    }

    /**
     * Get transactions by reference document
     *
     * @param referenceType Type of reference (ORDER, INVOICE, PURCHASE)
     * @param referenceId Reference UID
     * @return List of transactions
     */
    fun getTransactionsByReference(referenceType: String, referenceId: String): List<InventoryTransaction> {
        return inventoryTransactionRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId)
    }

    /**
     * Get transactions for item within date range
     *
     * @param itemId Item UID
     * @param startDate Start date
     * @param endDate End date
     * @return List of transactions
     */
    fun getTransactionsByItemAndDateRange(
        itemId: String,
        startDate: Instant,
        endDate: Instant
    ): List<InventoryTransaction> {
        return inventoryTransactionRepository.findByItemAndDateRange(itemId, startDate, endDate)
    }

    // ============================================================================
    // Stock-In Operations
    // ============================================================================

    /**
     * Process stock-in transaction
     *
     * Adds stock to inventory (purchases, returns, opening stock)
     *
     * @param request Stock-in request
     * @return Created transaction
     * @throws IllegalArgumentException if item or warehouse not found
     */
    @Transactional
    fun stockIn(request: StockInRequest): InventoryTransaction {
        // Validate inventory item exists
        val item = inventoryItemService.getInventoryItemByUid(request.inventoryItemId)

        // Validate warehouse exists
        warehouseService.getWarehouseByUid(request.warehouseId)

        // Generate transaction number
        val transactionNumber = generateTransactionNumber()

        // Create transaction entity
        val transaction = InventoryTransaction().apply {
            this.transactionNumber = transactionNumber
            this.transactionType = Constants.TXN_TYPE_STOCK_IN
            this.transactionReason = request.reason
            this.inventoryItemId = request.inventoryItemId
            this.warehouseId = request.warehouseId
            this.quantity = request.quantity
            this.unitCost = request.unitCost
            this.batchId = null  // Will be set in Phase 3
            this.serialNumbers = request.serialNumbers
            this.referenceType = request.referenceType
            this.referenceId = request.referenceId
            this.referenceNumber = request.referenceNumber
            this.transactionDate = request.transactionDate
            this.notes = request.notes
            this.performedBy = request.performedBy
            calculateTotalCost()
        }

        // Update inventory item stock
        val newStock = item.currentStock.add(request.quantity)
        inventoryItemService.updateStockQuantities(
            item.uid,
            currentStock = newStock,
            reservedStock = item.reservedStock
        )

        // Set balance after transaction
        transaction.balanceAfter = newStock

        // Save transaction
        val savedTransaction = inventoryTransactionRepository.save(transaction)

        // Create serial numbers if provided
        if (!request.serialNumbers.isNullOrEmpty()) {
            val bulkSerialRequest = BulkSerialRequest(
                inventoryItemId = request.inventoryItemId,
                warehouseId = request.warehouseId,
                batchId = request.batchNumber,  // Link to batch if provided
                serialNumbers = request.serialNumbers!!,
                costPrice = request.unitCost,
                sellingPrice = BigDecimal.ZERO,
                receivedDate = request.transactionDate,
                warrantyExpiryDate = null,
                notes = request.notes,
                attributes = null
            )
            inventorySerialService.createBulkSerials(bulkSerialRequest)
        }

        return savedTransaction
    }

    // ============================================================================
    // Stock-Out Operations
    // ============================================================================

    /**
     * Process stock-out transaction
     *
     * Removes stock from inventory (sales, damages, losses)
     *
     * @param request Stock-out request
     * @return Created transaction
     * @throws IllegalArgumentException if item or warehouse not found
     * @throws IllegalStateException if insufficient stock
     */
    @Transactional
    fun stockOut(request: StockOutRequest): InventoryTransaction {
        // Validate inventory item exists
        val item = inventoryItemService.getInventoryItemByUid(request.inventoryItemId)

        // Validate warehouse exists
        warehouseService.getWarehouseByUid(request.warehouseId)

        // Check stock availability
        val config = inventoryConfigService.getOrCreateConfig()
        val availableStock = item.currentStock.subtract(item.reservedStock)

        if (!config.allowNegativeStock && availableStock < request.quantity) {
            throw IllegalStateException(
                "Insufficient stock for item ${item.sku}. " +
                "Available: $availableStock, Requested: ${request.quantity}"
            )
        }

        // Generate transaction number
        val transactionNumber = generateTransactionNumber()

        // Create transaction entity
        val transaction = InventoryTransaction().apply {
            this.transactionNumber = transactionNumber
            this.transactionType = Constants.TXN_TYPE_STOCK_OUT
            this.transactionReason = request.reason
            this.inventoryItemId = request.inventoryItemId
            this.warehouseId = request.warehouseId
            this.quantity = request.quantity
            this.unitCost = item.costPrice  // Use item's cost price
            this.batchId = request.batchId
            this.serialNumbers = request.serialNumbers
            this.referenceType = request.referenceType
            this.referenceId = request.referenceId
            this.referenceNumber = request.referenceNumber
            this.transactionDate = request.transactionDate
            this.notes = request.notes
            this.performedBy = request.performedBy
            calculateTotalCost()
        }

        // Update inventory item stock
        val newStock = item.currentStock.subtract(request.quantity)
        inventoryItemService.updateStockQuantities(
            item.uid,
            currentStock = newStock,
            reservedStock = item.reservedStock
        )

        // Set balance after transaction
        transaction.balanceAfter = newStock

        // Save transaction
        val savedTransaction = inventoryTransactionRepository.save(transaction)

        // Mark serial numbers as sold if provided
        if (!request.serialNumbers.isNullOrEmpty() && request.referenceType != null && request.referenceId != null) {
            inventorySerialService.markSerialsAsSold(
                serialNumbers = request.serialNumbers!!,
                referenceType = request.referenceType!!,
                referenceId = request.referenceId!!,
                referenceNumber = request.referenceNumber,
                customerId = null,  // Can be populated from order/invoice if available
                customerName = null
            )
        }

        return savedTransaction
    }

    // ============================================================================
    // Stock Transfer Operations
    // ============================================================================

    /**
     * Process stock transfer between warehouses
     *
     * Creates two transactions:
     * 1. Stock-out from source warehouse
     * 2. Stock-in to destination warehouse
     *
     * @param request Stock transfer request
     * @return List of created transactions (out, in)
     * @throws IllegalArgumentException if items or warehouses not found
     * @throws IllegalStateException if insufficient stock
     */
    @Transactional
    fun transferStock(request: StockTransferRequest): List<InventoryTransaction> {
        // Validate source and destination are different
        if (request.fromWarehouseId == request.toWarehouseId) {
            throw IllegalArgumentException("Source and destination warehouses must be different")
        }

        // Get item from source warehouse
        val fromItem = inventoryItemService.findByInventoryItemIdAndWarehouseId(
            request.inventoryItemId,
            request.fromWarehouseId
        ) ?: throw IllegalArgumentException(
            "Item not found in source warehouse: ${request.fromWarehouseId}"
        )

        // Check if item exists in destination warehouse
        val toItem = inventoryItemService.findByInventoryItemIdAndWarehouseId(
            request.inventoryItemId,
            request.toWarehouseId
        ) ?: throw IllegalArgumentException(
            "Item does not exist in destination warehouse: ${request.toWarehouseId}. " +
            "Please create item in destination warehouse first."
        )

        // Check stock availability in source
        val config = inventoryConfigService.getOrCreateConfig()
        if (!config.allowNegativeStock && fromItem.availableStock < request.quantity) {
            throw IllegalStateException(
                "Insufficient stock in source warehouse. " +
                "Available: ${fromItem.availableStock}, Requested: ${request.quantity}"
            )
        }

        val transactions = mutableListOf<InventoryTransaction>()

        // 1. Stock-out from source warehouse
        val outTransaction = InventoryTransaction().apply {
            this.transactionNumber = generateTransactionNumber()
            this.transactionType = Constants.TXN_TYPE_TRANSFER
            this.transactionReason = "TRANSFER"
            this.inventoryItemId = request.inventoryItemId
            this.warehouseId = request.fromWarehouseId
            this.toWarehouseId = request.toWarehouseId
            this.quantity = request.quantity
            this.unitCost = fromItem.costPrice
            this.batchId = request.batchId
            this.serialNumbers = request.serialNumbers
            this.transactionDate = request.transactionDate
            this.notes = "Transfer to ${request.toWarehouseId}: ${request.notes ?: ""}"
            this.performedBy = request.performedBy
            calculateTotalCost()
        }

        // Update source warehouse stock
        val newFromStock = fromItem.currentStock.subtract(request.quantity)
        inventoryItemService.updateStockQuantities(
            fromItem.uid,
            currentStock = newFromStock,
            reservedStock = fromItem.reservedStock
        )
        outTransaction.balanceAfter = newFromStock
        transactions.add(inventoryTransactionRepository.save(outTransaction))

        // 2. Stock-in to destination warehouse
        val inTransaction = InventoryTransaction().apply {
            this.transactionNumber = generateTransactionNumber()
            this.transactionType = Constants.TXN_TYPE_TRANSFER
            this.transactionReason = "TRANSFER"
            this.inventoryItemId = request.inventoryItemId
            this.warehouseId = request.toWarehouseId
            this.toWarehouseId = null  // Receiving warehouse
            this.quantity = request.quantity
            this.unitCost = fromItem.costPrice
            this.batchId = request.batchId
            this.serialNumbers = request.serialNumbers
            this.transactionDate = request.transactionDate
            this.notes = "Transfer from ${request.fromWarehouseId}: ${request.notes ?: ""}"
            this.performedBy = request.performedBy
            calculateTotalCost()
        }

        // Update destination warehouse stock
        val newToStock = toItem.currentStock.add(request.quantity)
        inventoryItemService.updateStockQuantities(
            toItem.uid,
            currentStock = newToStock,
            reservedStock = toItem.reservedStock
        )
        inTransaction.balanceAfter = newToStock
        transactions.add(inventoryTransactionRepository.save(inTransaction))

        // Update serial warehouse locations if serials provided
        if (!request.serialNumbers.isNullOrEmpty()) {
            val serials = inventorySerialService.findSerialsByNumbers(request.serialNumbers!!)
            serials.forEach { serial ->
                inventorySerialService.updateSerialWarehouse(serial.uid, request.toWarehouseId)
            }
        }

        return transactions
    }

    // ============================================================================
    // Stock Adjustment Operations
    // ============================================================================

    /**
     * Process stock adjustment
     *
     * Manual correction of stock levels (positive or negative)
     *
     * @param request Stock adjustment request
     * @return Created transaction
     * @throws IllegalArgumentException if item or warehouse not found
     */
    @Transactional
    fun adjustStock(request: StockAdjustmentRequest): InventoryTransaction {
        // Validate inventory item exists
        val item = inventoryItemService.getInventoryItemByUid(request.inventoryItemId)

        // Validate warehouse exists
        warehouseService.getWarehouseByUid(request.warehouseId)

        // Determine transaction type based on adjustment direction
        val transactionType = if (request.adjustmentQuantity >= BigDecimal.ZERO) {
            Constants.TXN_TYPE_ADJUSTMENT  // Positive adjustment
        } else {
            Constants.TXN_TYPE_ADJUSTMENT  // Negative adjustment
        }

        // Generate transaction number
        val transactionNumber = generateTransactionNumber()

        // Create transaction entity
        val transaction = InventoryTransaction().apply {
            this.transactionNumber = transactionNumber
            this.transactionType = transactionType
            this.transactionReason = request.reason
            this.inventoryItemId = request.inventoryItemId
            this.warehouseId = request.warehouseId
            this.quantity = request.adjustmentQuantity.abs()  // Store as positive
            this.unitCost = if (request.adjustmentQuantity >= BigDecimal.ZERO) {
                item.costPrice
            } else {
                BigDecimal.ZERO
            }
            this.transactionDate = request.transactionDate
            this.notes = request.notes
            this.performedBy = request.performedBy
            calculateTotalCost()
        }

        // Update inventory item stock
        val newStock = item.currentStock.add(request.adjustmentQuantity)
        inventoryItemService.updateStockQuantities(
            item.uid,
            currentStock = newStock,
            reservedStock = item.reservedStock
        )

        // Set balance after transaction
        transaction.balanceAfter = newStock

        // Save transaction
        return inventoryTransactionRepository.save(transaction)
    }

    // ============================================================================
    // Physical Count Operations
    // ============================================================================

    /**
     * Process physical count
     *
     * Reconcile physical count with system stock
     * Creates adjustment transaction for the difference
     *
     * @param request Physical count request
     * @return Created transaction
     * @throws IllegalArgumentException if item or warehouse not found
     */
    @Transactional
    fun physicalCount(request: PhysicalCountRequest): InventoryTransaction {
        // Validate inventory item exists
        val item = inventoryItemService.getInventoryItemByUid(request.inventoryItemId)

        // Validate warehouse exists
        warehouseService.getWarehouseByUid(request.warehouseId)

        // Calculate difference
        val difference = request.countedQuantity.subtract(item.currentStock)

        // Generate transaction number
        val transactionNumber = generateTransactionNumber()

        // Create transaction entity
        val transaction = InventoryTransaction().apply {
            this.transactionNumber = transactionNumber
            this.transactionType = Constants.TXN_TYPE_COUNT
            this.transactionReason = "COUNT_ADJUSTMENT"
            this.inventoryItemId = request.inventoryItemId
            this.warehouseId = request.warehouseId
            this.quantity = difference.abs()  // Store absolute difference
            this.unitCost = if (difference >= BigDecimal.ZERO) {
                item.costPrice
            } else {
                BigDecimal.ZERO
            }
            this.transactionDate = request.transactionDate
            this.notes = buildString {
                append("Physical count reconciliation. ")
                append("System: ${item.currentStock}, ")
                append("Counted: ${request.countedQuantity}, ")
                append("Difference: $difference")
                if (!request.notes.isNullOrBlank()) {
                    append(". ${request.notes}")
                }
            }
            this.performedBy = request.performedBy
            calculateTotalCost()
        }

        // Update inventory item stock to counted quantity
        inventoryItemService.updateStockQuantities(
            item.uid,
            currentStock = request.countedQuantity,
            reservedStock = item.reservedStock
        )

        // Set balance after transaction
        transaction.balanceAfter = request.countedQuantity

        // Save transaction
        return inventoryTransactionRepository.save(transaction)
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    /**
     * Generate unique transaction number
     *
     * Format: TXN-YYYYMMDD-NNNN
     * Example: TXN-20250119-0001
     *
     * @return Generated transaction number
     */
    private fun generateTransactionNumber(): String {
        val today = LocalDate.now()
        val dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val prefix = "TXN-$dateStr-"

        // Get start of day in UTC
        val startOfDay = today.atStartOfDay(ZoneId.of("UTC")).toInstant()

        // Count transactions created today
        val count = inventoryTransactionRepository.countTransactionsToday(startOfDay)
        val sequence = String.format("%04d", count + 1)

        val transactionNumber = "$prefix$sequence"

        // Ensure uniqueness (rare collision case)
        if (inventoryTransactionRepository.existsByTransactionNumber(transactionNumber)) {
            // Fallback: append timestamp
            val timestamp = System.currentTimeMillis()
            return "$prefix$sequence-$timestamp"
        }

        return transactionNumber
    }
}
