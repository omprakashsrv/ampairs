package com.ampairs.inventory.domain.dto

import com.ampairs.inventory.domain.model.InventoryBatch
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
 * Inventory Batch Request DTO
 *
 * Used for creating new batches during stock-in operations
 */
data class InventoryBatchRequest(

    @field:NotBlank(message = "Batch number is required")
    @field:Size(max = 100, message = "Batch number must not exceed 100 characters")
    var batchNumber: String = "",

    @field:Size(max = 100, message = "Lot number must not exceed 100 characters")
    var lotNumber: String? = null,

    @field:NotBlank(message = "Inventory item ID is required")
    var inventoryItemId: String = "",

    @field:NotBlank(message = "Warehouse ID is required")
    var warehouseId: String = "",

    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    var quantity: BigDecimal = BigDecimal.ZERO,

    // Dates
    var manufacturingDate: Instant? = null,
    var expiryDate: Instant? = null,
    var receivedDate: Instant = Instant.now(),

    // Supplier information
    var supplierId: String? = null,
    var supplierName: String? = null,
    var purchaseOrderNumber: String? = null,

    // Cost
    @field:NotNull(message = "Cost per unit is required")
    var costPerUnit: BigDecimal = BigDecimal.ZERO,

    // Extensible attributes
    var attributes: Map<String, Any>? = null
)

/**
 * Batch Update Request DTO
 *
 * Used for updating batch information
 */
data class BatchUpdateRequest(
    var lotNumber: String? = null,
    var manufacturingDate: Instant? = null,
    var expiryDate: Instant? = null,
    var supplierId: String? = null,
    var supplierName: String? = null,
    var purchaseOrderNumber: String? = null,
    var costPerUnit: BigDecimal? = null,
    var isActive: Boolean? = null,
    var attributes: Map<String, Any>? = null
)

// ============================================================================
// Response DTOs
// ============================================================================

/**
 * Inventory Batch Response DTO
 *
 * Used for API responses. Exposes batch details.
 */
data class InventoryBatchResponse(
    val uid: String,
    val batchNumber: String,
    val lotNumber: String?,

    // Inventory Item
    val inventoryItemId: String,
    val inventoryItemName: String?,
    val inventoryItemSku: String?,

    // Warehouse
    val warehouseId: String,
    val warehouseName: String?,

    // Stock Levels
    val totalQuantity: BigDecimal,
    val availableQuantity: BigDecimal,
    val reservedQuantity: BigDecimal,
    val consumedQuantity: BigDecimal,

    // Dates
    val manufacturingDate: Instant?,
    val expiryDate: Instant?,
    val receivedDate: Instant,

    // Supplier Information
    val supplierId: String?,
    val supplierName: String?,
    val purchaseOrderNumber: String?,

    // Cost
    val costPerUnit: BigDecimal,
    val batchValue: BigDecimal,  // availableQuantity * costPerUnit

    // Status
    val isActive: Boolean,
    val isExpired: Boolean,
    val hasExpired: Boolean,
    val daysUntilExpiry: Long?,

    // Extensible Attributes
    val attributes: Map<String, Any>?,

    // Audit
    val createdAt: Instant?,
    val updatedAt: Instant?
)

/**
 * Batch Summary Response
 *
 * Simplified batch information for listings
 */
data class BatchSummaryResponse(
    val uid: String,
    val batchNumber: String,
    val lotNumber: String?,
    val availableQuantity: BigDecimal,
    val expiryDate: Instant?,
    val daysUntilExpiry: Long?,
    val isExpired: Boolean,
    val costPerUnit: BigDecimal
)

// ============================================================================
// Extension Functions
// ============================================================================

/**
 * Convert InventoryBatchRequest to InventoryBatch entity
 */
fun InventoryBatchRequest.toInventoryBatch(): InventoryBatch {
    val batch = InventoryBatch()
    batch.batchNumber = this.batchNumber
    batch.lotNumber = this.lotNumber
    batch.inventoryItemId = this.inventoryItemId
    batch.warehouseId = this.warehouseId
    batch.totalQuantity = this.quantity
    batch.availableQuantity = this.quantity
    batch.reservedQuantity = BigDecimal.ZERO
    batch.manufacturingDate = this.manufacturingDate
    batch.expiryDate = this.expiryDate
    batch.receivedDate = this.receivedDate
    batch.supplierId = this.supplierId
    batch.supplierName = this.supplierName
    batch.purchaseOrderNumber = this.purchaseOrderNumber
    batch.costPerUnit = this.costPerUnit
    batch.attributes = this.attributes
    batch.isActive = true
    batch.isExpired = false
    return batch
}

/**
 * Convert InventoryBatch entity to InventoryBatchResponse DTO
 */
fun InventoryBatch.asInventoryBatchResponse(): InventoryBatchResponse {
    val daysUntilExpiry = if (expiryDate != null) {
        val now = Instant.now()
        val secondsUntilExpiry = expiryDate!!.epochSecond - now.epochSecond
        secondsUntilExpiry / (24 * 60 * 60) // Convert to days
    } else {
        null
    }

    return InventoryBatchResponse(
        uid = this.uid,
        batchNumber = this.batchNumber,
        lotNumber = this.lotNumber,

        // Inventory Item (with lazy-loaded names if available)
        inventoryItemId = this.inventoryItemId,
        inventoryItemName = this.inventoryItem?.name,
        inventoryItemSku = this.inventoryItem?.sku,

        // Warehouse
        warehouseId = this.warehouseId,
        warehouseName = null,  // Would need separate query

        // Stock Levels
        totalQuantity = this.totalQuantity,
        availableQuantity = this.availableQuantity,
        reservedQuantity = this.reservedQuantity,
        consumedQuantity = this.calculateConsumedQuantity(),

        // Dates
        manufacturingDate = this.manufacturingDate,
        expiryDate = this.expiryDate,
        receivedDate = this.receivedDate,

        // Supplier Information
        supplierId = this.supplierId,
        supplierName = this.supplierName,
        purchaseOrderNumber = this.purchaseOrderNumber,

        // Cost
        costPerUnit = this.costPerUnit,
        batchValue = this.availableQuantity.multiply(this.costPerUnit),

        // Status
        isActive = this.isActive,
        isExpired = this.isExpired,
        hasExpired = this.hasExpired(),
        daysUntilExpiry = daysUntilExpiry,

        // Extensible Attributes
        attributes = this.attributes,

        // Audit
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

/**
 * Convert InventoryBatch entity to BatchSummaryResponse DTO
 */
fun InventoryBatch.asBatchSummaryResponse(): BatchSummaryResponse {
    val daysUntilExpiry = if (expiryDate != null) {
        val now = Instant.now()
        val secondsUntilExpiry = expiryDate!!.epochSecond - now.epochSecond
        secondsUntilExpiry / (24 * 60 * 60) // Convert to days
    } else {
        null
    }

    return BatchSummaryResponse(
        uid = this.uid,
        batchNumber = this.batchNumber,
        lotNumber = this.lotNumber,
        availableQuantity = this.availableQuantity,
        expiryDate = this.expiryDate,
        daysUntilExpiry = daysUntilExpiry,
        isExpired = this.isExpired,
        costPerUnit = this.costPerUnit
    )
}

/**
 * Convert list of InventoryBatch to list of InventoryBatchResponse
 */
fun List<InventoryBatch>.asInventoryBatchResponses(): List<InventoryBatchResponse> {
    return this.map { it.asInventoryBatchResponse() }
}

/**
 * Convert list of InventoryBatch to list of BatchSummaryResponse
 */
fun List<InventoryBatch>.asBatchSummaryResponses(): List<BatchSummaryResponse> {
    return this.map { it.asBatchSummaryResponse() }
}
