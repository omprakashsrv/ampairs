package com.ampairs.inventory.domain.dto

import com.ampairs.inventory.domain.model.InventoryItem
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.Instant

/**
 * Inventory Item Request DTO
 *
 * Used for creating and updating inventory items.
 * Supports both standalone items and product-linked items.
 */
data class InventoryItemRequest(

    @field:NotBlank(message = "SKU is required")
    @field:Size(min = 1, max = 100, message = "SKU must be between 1 and 100 characters")
    var sku: String = "",

    @field:NotBlank(message = "Item name is required")
    @field:Size(min = 2, max = 200, message = "Item name must be between 2 and 200 characters")
    var name: String = "",

    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    var description: String? = null,

    // Product Module Integration (Optional)
    var productId: String? = null,
    var productVariantId: String? = null,

    // Location
    @field:NotBlank(message = "Warehouse ID is required")
    var warehouseId: String = "",

    // Stock Levels
    @field:PositiveOrZero(message = "Current stock must be zero or positive")
    var currentStock: BigDecimal = BigDecimal.ZERO,

    @field:PositiveOrZero(message = "Reserved stock must be zero or positive")
    var reservedStock: BigDecimal = BigDecimal.ZERO,

    @field:PositiveOrZero(message = "Reorder level must be zero or positive")
    var reorderLevel: BigDecimal = BigDecimal.ZERO,

    @field:PositiveOrZero(message = "Max stock level must be zero or positive")
    var maxStockLevel: BigDecimal = BigDecimal.ZERO,

    // Unit
    var unitId: String? = null,

    // Pricing
    @field:PositiveOrZero(message = "Cost price must be zero or positive")
    var costPrice: BigDecimal = BigDecimal.ZERO,

    @field:PositiveOrZero(message = "Selling price must be zero or positive")
    var sellingPrice: BigDecimal = BigDecimal.ZERO,

    @field:PositiveOrZero(message = "MRP must be zero or positive")
    var mrp: BigDecimal = BigDecimal.ZERO,

    // Tracking Flags
    var batchTrackingEnabled: Boolean = false,
    var serialTrackingEnabled: Boolean = false,
    var expiryTrackingEnabled: Boolean = false,

    // Status
    var isActive: Boolean = true,

    // Extensible Attributes
    var attributes: Map<String, Any>? = null
)

/**
 * Inventory Item Response DTO
 *
 * Used for API responses. Includes computed fields and relationships.
 */
data class InventoryItemResponse(
    val uid: String,
    val sku: String,
    val name: String,
    val description: String?,

    // Product Integration
    val productId: String?,
    val productVariantId: String?,

    // Location
    val warehouseId: String,
    val warehouseName: String?,
    val warehouseCode: String?,

    // Stock Levels
    val currentStock: BigDecimal,
    val reservedStock: BigDecimal,
    val availableStock: BigDecimal,
    val reorderLevel: BigDecimal,
    val maxStockLevel: BigDecimal,

    // Stock Status Flags
    val isLowStock: Boolean,
    val isOverStock: Boolean,

    // Unit
    val unitId: String?,
    val unitName: String?,
    val unitSymbol: String?,

    // Pricing
    val costPrice: BigDecimal,
    val sellingPrice: BigDecimal,
    val mrp: BigDecimal,

    // Stock Value
    val stockValue: BigDecimal,  // currentStock * costPrice

    // Tracking Flags
    val batchTrackingEnabled: Boolean,
    val serialTrackingEnabled: Boolean,
    val expiryTrackingEnabled: Boolean,

    // Status
    val isActive: Boolean,

    // Extensible Attributes
    val attributes: Map<String, Any>?,

    // Timestamps
    val createdAt: Instant?,
    val updatedAt: Instant?
)

/**
 * Stock Summary Response DTO
 *
 * Lightweight DTO for stock level queries
 */
data class StockSummaryResponse(
    val itemUid: String,
    val sku: String,
    val name: String,
    val warehouseId: String,
    val warehouseName: String?,
    val currentStock: BigDecimal,
    val availableStock: BigDecimal,
    val reservedStock: BigDecimal,
    val isLowStock: Boolean,
    val reorderLevel: BigDecimal
)

// Extension Functions for DTO Conversion

/**
 * Convert InventoryItemRequest to InventoryItem entity
 */
fun InventoryItemRequest.toInventoryItem(): InventoryItem {
    val item = InventoryItem()
    item.sku = this.sku.trim().uppercase()  // Normalize SKU to uppercase
    item.name = this.name.trim()
    item.description = this.description?.trim()
    item.productId = this.productId
    item.productVariantId = this.productVariantId
    item.warehouseId = this.warehouseId
    item.currentStock = this.currentStock
    item.reservedStock = this.reservedStock
    item.reorderLevel = this.reorderLevel
    item.maxStockLevel = this.maxStockLevel
    item.unitId = this.unitId
    item.costPrice = this.costPrice
    item.sellingPrice = this.sellingPrice
    item.mrp = this.mrp
    item.batchTrackingEnabled = this.batchTrackingEnabled
    item.serialTrackingEnabled = this.serialTrackingEnabled
    item.expiryTrackingEnabled = this.expiryTrackingEnabled
    item.isActive = this.isActive
    item.attributes = this.attributes

    // Recalculate available stock
    item.recalculateAvailableStock()

    return item
}

/**
 * Convert InventoryItem entity to InventoryItemResponse DTO
 */
fun InventoryItem.asInventoryItemResponse(): InventoryItemResponse {
    return InventoryItemResponse(
        uid = this.uid,
        sku = this.sku,
        name = this.name,
        description = this.description,
        productId = this.productId,
        productVariantId = this.productVariantId,
        warehouseId = this.warehouseId,
        warehouseName = this.warehouse?.name,
        warehouseCode = this.warehouse?.code,
        currentStock = this.currentStock,
        reservedStock = this.reservedStock,
        availableStock = this.availableStock,
        reorderLevel = this.reorderLevel,
        maxStockLevel = this.maxStockLevel,
        isLowStock = this.isLowStock(),
        isOverStock = this.isOverStock(),
        unitId = this.unitId,
        unitName = this.unit?.name,
        unitSymbol = this.unit?.shortName,
        costPrice = this.costPrice,
        sellingPrice = this.sellingPrice,
        mrp = this.mrp,
        stockValue = this.currentStock.multiply(this.costPrice),
        batchTrackingEnabled = this.batchTrackingEnabled,
        serialTrackingEnabled = this.serialTrackingEnabled,
        expiryTrackingEnabled = this.expiryTrackingEnabled,
        isActive = this.isActive,
        attributes = this.attributes,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

/**
 * Convert InventoryItem entity to StockSummaryResponse DTO
 */
fun InventoryItem.asStockSummary(): StockSummaryResponse {
    return StockSummaryResponse(
        itemUid = this.uid,
        sku = this.sku,
        name = this.name,
        warehouseId = this.warehouseId,
        warehouseName = this.warehouse?.name,
        currentStock = this.currentStock,
        availableStock = this.availableStock,
        reservedStock = this.reservedStock,
        isLowStock = this.isLowStock(),
        reorderLevel = this.reorderLevel
    )
}

/**
 * Convert list of InventoryItem entities to list of InventoryItemResponse DTOs
 */
fun List<InventoryItem>.asInventoryItemResponses(): List<InventoryItemResponse> {
    return this.map { it.asInventoryItemResponse() }
}

/**
 * Convert list of InventoryItem entities to list of StockSummaryResponse DTOs
 */
fun List<InventoryItem>.asStockSummaries(): List<StockSummaryResponse> {
    return this.map { it.asStockSummary() }
}
