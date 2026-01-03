package com.ampairs.inventory.domain.dto

import com.ampairs.inventory.domain.model.InventorySerial
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.Instant

// ============================================================================
// Request DTOs
// ============================================================================

/**
 * Bulk Serial Creation Request
 *
 * Used for creating multiple serial numbers at once during stock-in operations
 */
data class BulkSerialRequest(
    @field:NotBlank(message = "Inventory item ID is required")
    var inventoryItemId: String = "",

    @field:NotBlank(message = "Warehouse ID is required")
    var warehouseId: String = "",

    var batchId: String? = null,

    @field:NotNull(message = "Serial numbers list is required")
    var serialNumbers: List<String> = emptyList(),

    @field:NotNull(message = "Cost price is required")
    @field:Positive(message = "Cost price must be positive")
    var costPrice: BigDecimal = BigDecimal.ZERO,

    var sellingPrice: BigDecimal = BigDecimal.ZERO,

    var receivedDate: Instant = Instant.now(),

    var warrantyExpiryDate: Instant? = null,

    var notes: String? = null,

    var attributes: Map<String, Any>? = null
)

/**
 * Individual Serial Creation Request
 */
data class SerialRequest(
    @field:NotBlank(message = "Serial number is required")
    var serialNumber: String = "",

    @field:NotBlank(message = "Inventory item ID is required")
    var inventoryItemId: String = "",

    @field:NotBlank(message = "Warehouse ID is required")
    var warehouseId: String = "",

    var batchId: String? = null,

    @field:NotNull(message = "Cost price is required")
    @field:Positive(message = "Cost price must be positive")
    var costPrice: BigDecimal = BigDecimal.ZERO,

    var sellingPrice: BigDecimal = BigDecimal.ZERO,

    var receivedDate: Instant = Instant.now(),

    var warrantyExpiryDate: Instant? = null,

    var notes: String? = null,

    var attributes: Map<String, Any>? = null
)

/**
 * Serial Update Request
 *
 * For updating serial information (not for status changes)
 */
data class SerialUpdateRequest(
    var batchId: String? = null,
    var costPrice: BigDecimal? = null,
    var sellingPrice: BigDecimal? = null,
    var warrantyExpiryDate: Instant? = null,
    var notes: String? = null,
    var attributes: Map<String, Any>? = null
)

/**
 * Serial Status Change Request
 *
 * For changing serial status (reserve, sell, damage, return)
 */
data class SerialStatusChangeRequest(
    @field:NotBlank(message = "Status is required")
    var status: String = "",

    var referenceType: String? = null,
    var referenceId: String? = null,
    var referenceNumber: String? = null,

    var customerId: String? = null,
    var customerName: String? = null,

    var notes: String? = null
)

/**
 * Serial Sale Request
 *
 * For marking serial as sold
 */
data class SerialSaleRequest(
    @field:NotBlank(message = "Serial number is required")
    var serialNumber: String = "",

    @field:NotBlank(message = "Reference type is required")
    var referenceType: String = "",

    @field:NotBlank(message = "Reference ID is required")
    var referenceId: String = "",

    var referenceNumber: String? = null,

    var customerId: String? = null,
    var customerName: String? = null,

    var sellingPrice: BigDecimal? = null
)

/**
 * Serial Return Request
 *
 * For marking serial as returned
 */
data class SerialReturnRequest(
    @field:NotBlank(message = "Serial number is required")
    var serialNumber: String = "",

    @field:NotBlank(message = "Reference type is required")
    var referenceType: String = "",

    @field:NotBlank(message = "Reference ID is required")
    var referenceId: String = "",

    var notes: String? = null
)

// ============================================================================
// Response DTOs
// ============================================================================

/**
 * Full Serial Response
 *
 * Complete serial information including lifecycle data
 */
data class InventorySerialResponse(
    val uid: String,
    val serialNumber: String,

    // References
    val inventoryItemId: String,
    val inventoryItemName: String?,
    val inventoryItemSku: String?,
    val warehouseId: String,
    val warehouseName: String?,
    val batchId: String?,
    val batchNumber: String?,

    // Status
    val status: String,
    val isAvailable: Boolean,
    val isReserved: Boolean,
    val isSold: Boolean,
    val isDamaged: Boolean,
    val isReturned: Boolean,

    // Dates
    val receivedDate: Instant,
    val soldDate: Instant?,
    val warrantyExpiryDate: Instant?,
    val returnedDate: Instant?,

    // Warranty Status
    val hasWarrantyExpired: Boolean,
    val daysUntilWarrantyExpiry: Long?,

    // References
    val soldReferenceType: String?,
    val soldReferenceId: String?,
    val soldReferenceNumber: String?,
    val returnReferenceType: String?,
    val returnReferenceId: String?,

    // Customer
    val customerId: String?,
    val customerName: String?,

    // Pricing
    val costPrice: BigDecimal,
    val sellingPrice: BigDecimal,

    // Additional
    val notes: String?,
    val attributes: Map<String, Any>?,

    // Audit
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Serial Summary Response
 *
 * Simplified serial information for listings
 */
data class SerialSummaryResponse(
    val uid: String,
    val serialNumber: String,
    val inventoryItemId: String,
    val inventoryItemName: String?,
    val warehouseId: String,
    val warehouseName: String?,
    val status: String,
    val receivedDate: Instant,
    val soldDate: Instant?,
    val customerId: String?,
    val customerName: String?
)

/**
 * Serial Status Summary
 *
 * Aggregated status counts for an inventory item
 */
data class SerialStatusSummary(
    val inventoryItemId: String,
    val warehouseId: String,
    val totalSerials: Long,
    val availableSerials: Long,
    val reservedSerials: Long,
    val soldSerials: Long,
    val damagedSerials: Long,
    val returnedSerials: Long
)

// ============================================================================
// Extension Functions - Entity to DTO
// ============================================================================

/**
 * Convert InventorySerial to InventorySerialResponse
 */
fun InventorySerial.asInventorySerialResponse(): InventorySerialResponse {
    val daysUntilExpiry = warrantyExpiryDate?.let { expiry ->
        val now = Instant.now()
        if (now.isBefore(expiry)) {
            java.time.Duration.between(now, expiry).toDays()
        } else {
            null
        }
    }

    return InventorySerialResponse(
        uid = this.uid,
        serialNumber = this.serialNumber,

        // References (will be populated by service layer if needed)
        inventoryItemId = this.inventoryItemId,
        inventoryItemName = null,
        inventoryItemSku = null,
        warehouseId = this.warehouseId,
        warehouseName = null,
        batchId = this.batchId,
        batchNumber = null,

        // Status
        status = this.status,
        isAvailable = this.isAvailable(),
        isReserved = this.isReserved(),
        isSold = this.isSold(),
        isDamaged = this.isDamaged(),
        isReturned = this.isReturned(),

        // Dates
        receivedDate = this.receivedDate,
        soldDate = this.soldDate,
        warrantyExpiryDate = this.warrantyExpiryDate,
        returnedDate = this.returnedDate,

        // Warranty
        hasWarrantyExpired = this.hasWarrantyExpired(),
        daysUntilWarrantyExpiry = daysUntilExpiry,

        // References
        soldReferenceType = this.soldReferenceType,
        soldReferenceId = this.soldReferenceId,
        soldReferenceNumber = this.soldReferenceNumber,
        returnReferenceType = this.returnReferenceType,
        returnReferenceId = this.returnReferenceId,

        // Customer
        customerId = this.customerId,
        customerName = this.customerName,

        // Pricing
        costPrice = this.costPrice,
        sellingPrice = this.sellingPrice,

        // Additional
        notes = this.notes,
        attributes = this.attributes,

        // Audit
        createdAt = this.createdAt ?: Instant.now(),
        updatedAt = this.updatedAt ?: Instant.now()
    )
}

/**
 * Convert InventorySerial to SerialSummaryResponse
 */
fun InventorySerial.asSerialSummaryResponse(): SerialSummaryResponse {
    return SerialSummaryResponse(
        uid = this.uid,
        serialNumber = this.serialNumber,
        inventoryItemId = this.inventoryItemId,
        inventoryItemName = null,
        warehouseId = this.warehouseId,
        warehouseName = null,
        status = this.status,
        receivedDate = this.receivedDate,
        soldDate = this.soldDate,
        customerId = this.customerId,
        customerName = this.customerName
    )
}

/**
 * Convert list of InventorySerial to list of InventorySerialResponse
 */
fun List<InventorySerial>.asInventorySerialResponses(): List<InventorySerialResponse> {
    return this.map { it.asInventorySerialResponse() }
}

/**
 * Convert list of InventorySerial to list of SerialSummaryResponse
 */
fun List<InventorySerial>.asSerialSummaryResponses(): List<SerialSummaryResponse> {
    return this.map { it.asSerialSummaryResponse() }
}

// ============================================================================
// Extension Functions - DTO to Entity
// ============================================================================

/**
 * Convert SerialRequest to InventorySerial
 */
fun SerialRequest.toInventorySerial(): InventorySerial {
    return InventorySerial().apply {
        this.serialNumber = this@toInventorySerial.serialNumber
        this.inventoryItemId = this@toInventorySerial.inventoryItemId
        this.warehouseId = this@toInventorySerial.warehouseId
        this.batchId = this@toInventorySerial.batchId
        this.costPrice = this@toInventorySerial.costPrice
        this.sellingPrice = this@toInventorySerial.sellingPrice
        this.receivedDate = this@toInventorySerial.receivedDate
        this.warrantyExpiryDate = this@toInventorySerial.warrantyExpiryDate
        this.notes = this@toInventorySerial.notes
        this.attributes = this@toInventorySerial.attributes
    }
}
