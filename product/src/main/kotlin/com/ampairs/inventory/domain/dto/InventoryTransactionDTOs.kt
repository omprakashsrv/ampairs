package com.ampairs.inventory.domain.dto

import com.ampairs.inventory.domain.model.InventoryTransaction
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant

// ============================================================================
// Request DTOs
// ============================================================================

/**
 * Stock-In Request DTO
 *
 * Used for receiving stock (purchases, returns, opening stock, etc.)
 */
data class StockInRequest(

    @field:NotBlank(message = "Inventory item ID is required")
    var inventoryItemId: String = "",

    @field:NotBlank(message = "Warehouse ID is required")
    var warehouseId: String = "",

    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    var quantity: BigDecimal = BigDecimal.ZERO,

    @field:NotNull(message = "Unit cost is required")
    var unitCost: BigDecimal = BigDecimal.ZERO,

    @field:NotBlank(message = "Reason is required")
    @field:Size(max = 50, message = "Reason must not exceed 50 characters")
    var reason: String = "PURCHASE",  // PURCHASE, RETURN, OPENING, CORRECTION

    // Optional batch tracking
    var batchNumber: String? = null,
    var lotNumber: String? = null,
    var manufacturingDate: Instant? = null,
    var expiryDate: Instant? = null,

    // Optional serial tracking
    var serialNumbers: List<String>? = null,

    // Optional reference
    var referenceType: String? = null,  // PURCHASE, INVOICE
    var referenceId: String? = null,
    var referenceNumber: String? = null,

    // Transaction metadata
    var transactionDate: Instant = Instant.now(),
    var notes: String? = null,
    var performedBy: String? = null
)

/**
 * Stock-Out Request DTO
 *
 * Used for issuing stock (sales, damages, losses, etc.)
 */
data class StockOutRequest(

    @field:NotBlank(message = "Inventory item ID is required")
    var inventoryItemId: String = "",

    @field:NotBlank(message = "Warehouse ID is required")
    var warehouseId: String = "",

    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    var quantity: BigDecimal = BigDecimal.ZERO,

    @field:NotBlank(message = "Reason is required")
    @field:Size(max = 50, message = "Reason must not exceed 50 characters")
    var reason: String = "SALE",  // SALE, DAMAGE, LOSS, RETURN

    // Optional batch/serial selection
    var batchId: String? = null,
    var serialNumbers: List<String>? = null,

    // Optional reference
    var referenceType: String? = null,  // ORDER, INVOICE
    var referenceId: String? = null,
    var referenceNumber: String? = null,

    // Transaction metadata
    var transactionDate: Instant = Instant.now(),
    var notes: String? = null,
    var performedBy: String? = null
)

/**
 * Stock Transfer Request DTO
 *
 * Used for transferring stock between warehouses
 */
data class StockTransferRequest(

    @field:NotBlank(message = "Inventory item ID is required")
    var inventoryItemId: String = "",

    @field:NotBlank(message = "Source warehouse ID is required")
    var fromWarehouseId: String = "",

    @field:NotBlank(message = "Destination warehouse ID is required")
    var toWarehouseId: String = "",

    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    var quantity: BigDecimal = BigDecimal.ZERO,

    // Optional batch/serial selection
    var batchId: String? = null,
    var serialNumbers: List<String>? = null,

    // Transaction metadata
    var transactionDate: Instant = Instant.now(),
    var notes: String? = null,
    var performedBy: String? = null
)

/**
 * Stock Adjustment Request DTO
 *
 * Used for manual stock corrections (write-offs, corrections, etc.)
 */
data class StockAdjustmentRequest(

    @field:NotBlank(message = "Inventory item ID is required")
    var inventoryItemId: String = "",

    @field:NotBlank(message = "Warehouse ID is required")
    var warehouseId: String = "",

    @field:NotNull(message = "Adjustment quantity is required")
    var adjustmentQuantity: BigDecimal = BigDecimal.ZERO,  // Can be positive or negative

    @field:NotBlank(message = "Reason is required")
    @field:Size(max = 50, message = "Reason must not exceed 50 characters")
    var reason: String = "CORRECTION",  // CORRECTION, DAMAGE, LOSS, FOUND

    // Transaction metadata
    var transactionDate: Instant = Instant.now(),
    var notes: String? = null,
    var performedBy: String? = null
)

/**
 * Physical Count Request DTO
 *
 * Used for inventory physical count reconciliation
 */
data class PhysicalCountRequest(

    @field:NotBlank(message = "Inventory item ID is required")
    var inventoryItemId: String = "",

    @field:NotBlank(message = "Warehouse ID is required")
    var warehouseId: String = "",

    @field:NotNull(message = "Counted quantity is required")
    var countedQuantity: BigDecimal = BigDecimal.ZERO,  // Physical count result

    // Transaction metadata
    var transactionDate: Instant = Instant.now(),
    var notes: String? = null,
    var performedBy: String? = null
)

// ============================================================================
// Response DTO
// ============================================================================

/**
 * Inventory Transaction Response DTO
 *
 * Used for API responses. Exposes transaction details.
 */
data class InventoryTransactionResponse(
    val uid: String,
    val transactionNumber: String,
    val transactionType: String,
    val transactionReason: String,

    // Item and Warehouse
    val inventoryItemId: String,
    val inventoryItemName: String?,
    val inventoryItemSku: String?,
    val warehouseId: String,
    val warehouseName: String?,
    val toWarehouseId: String?,
    val toWarehouseName: String?,

    // Quantities and Cost
    val quantity: BigDecimal,
    val balanceAfter: BigDecimal,
    val unitCost: BigDecimal,
    val totalCost: BigDecimal,

    // Batch/Serial
    val batchId: String?,
    val batchNumber: String?,
    val serialNumbers: List<String>?,

    // Reference
    val referenceType: String?,
    val referenceId: String?,
    val referenceNumber: String?,

    // Metadata
    val transactionDate: Instant,
    val notes: String?,
    val performedBy: String?,
    val attributes: Map<String, Any>?,

    // Audit
    val createdAt: Instant?,
    val updatedAt: Instant?
)

// ============================================================================
// Extension Functions
// ============================================================================

/**
 * Convert InventoryTransaction entity to InventoryTransactionResponse DTO
 */
fun InventoryTransaction.asInventoryTransactionResponse(): InventoryTransactionResponse {
    return InventoryTransactionResponse(
        uid = this.uid,
        transactionNumber = this.transactionNumber,
        transactionType = this.transactionType,
        transactionReason = this.transactionReason,

        // Item and Warehouse (with lazy-loaded names if available)
        inventoryItemId = this.inventoryItemId,
        inventoryItemName = this.inventoryItem?.name,
        inventoryItemSku = this.inventoryItem?.sku,
        warehouseId = this.warehouseId,
        warehouseName = this.warehouse?.name,
        toWarehouseId = this.toWarehouseId,
        toWarehouseName = null,  // Would need separate query

        // Quantities and Cost
        quantity = this.quantity,
        balanceAfter = this.balanceAfter,
        unitCost = this.unitCost,
        totalCost = this.totalCost,

        // Batch/Serial
        batchId = this.batchId,
        batchNumber = null,  // Would need batch entity loaded
        serialNumbers = this.serialNumbers,

        // Reference
        referenceType = this.referenceType,
        referenceId = this.referenceId,
        referenceNumber = this.referenceNumber,

        // Metadata
        transactionDate = this.transactionDate,
        notes = this.notes,
        performedBy = this.performedBy,
        attributes = this.attributes,

        // Audit
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

/**
 * Convert list of InventoryTransaction to list of InventoryTransactionResponse
 */
fun List<InventoryTransaction>.asInventoryTransactionResponses(): List<InventoryTransactionResponse> {
    return this.map { it.asInventoryTransactionResponse() }
}

// ============================================================================
// Helper DTOs
// ============================================================================

/**
 * Batch Info for Stock-In
 *
 * Optional batch information when receiving stock
 */
data class BatchInfo(
    var batchNumber: String = "",
    var lotNumber: String? = null,
    var manufacturingDate: Instant? = null,
    var expiryDate: Instant? = null,
    var supplierId: String? = null,
    var supplierName: String? = null
)

/**
 * Transaction Summary Response
 *
 * Summary of transactions for an item
 */
data class TransactionSummaryResponse(
    val inventoryItemId: String,
    val inventoryItemName: String,
    val warehouseId: String,
    val warehouseName: String,

    // Transaction counts
    val totalTransactions: Long,
    val stockInCount: Long,
    val stockOutCount: Long,
    val transferCount: Long,
    val adjustmentCount: Long,

    // Quantity totals
    val totalStockIn: BigDecimal,
    val totalStockOut: BigDecimal,
    val netChange: BigDecimal,  // stockIn - stockOut

    // Cost totals
    val totalCostIn: BigDecimal,
    val totalCostOut: BigDecimal,
    val averageCost: BigDecimal,

    // Date range
    val fromDate: Instant,
    val toDate: Instant
)
