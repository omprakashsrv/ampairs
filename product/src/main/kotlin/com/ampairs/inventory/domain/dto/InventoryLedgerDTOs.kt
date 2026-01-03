package com.ampairs.inventory.domain.dto

import com.ampairs.inventory.domain.model.InventoryLedger
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

// ============================================================================
// Response DTOs
// ============================================================================

/**
 * Inventory Ledger Response
 *
 * Daily stock ledger entry with movements and balances
 */
data class InventoryLedgerResponse(
    val uid: String,

    // Date
    val ledgerDate: Instant,
    val ledgerLocalDate: LocalDate,

    // References
    val inventoryItemId: String,
    val inventoryItemName: String?,
    val inventoryItemSku: String?,
    val warehouseId: String,
    val warehouseName: String?,

    // Opening Balance
    val openingStock: BigDecimal,

    // Additions (Inflows)
    val stockIn: BigDecimal,
    val transferIn: BigDecimal,
    val adjustmentIn: BigDecimal,
    val totalInflows: BigDecimal,

    // Deductions (Outflows)
    val stockOut: BigDecimal,
    val transferOut: BigDecimal,
    val adjustmentOut: BigDecimal,
    val totalOutflows: BigDecimal,

    // Net Movement
    val netMovement: BigDecimal,

    // Closing Balance
    val closingStock: BigDecimal,

    // Valuation
    val averageCost: BigDecimal,
    val closingValue: BigDecimal,

    // Audit
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Ledger Summary Response
 *
 * Simplified ledger information for listings
 */
data class LedgerSummaryResponse(
    val uid: String,
    val ledgerDate: Instant,
    val ledgerLocalDate: LocalDate,
    val inventoryItemId: String,
    val warehouseId: String,
    val openingStock: BigDecimal,
    val closingStock: BigDecimal,
    val netMovement: BigDecimal,
    val closingValue: BigDecimal
)

/**
 * Warehouse Stock Summary
 *
 * Aggregated stock summary for a warehouse on a date
 */
data class WarehouseStockSummary(
    val warehouseId: String,
    val warehouseName: String?,
    val ledgerDate: Instant,
    val ledgerLocalDate: LocalDate,
    val totalItems: Int,
    val totalStockQuantity: BigDecimal,
    val totalStockValue: BigDecimal,
    val itemsWithMovement: Int
)

// ============================================================================
// Extension Functions - Entity to DTO
// ============================================================================

/**
 * Convert InventoryLedger to InventoryLedgerResponse
 */
fun InventoryLedger.asInventoryLedgerResponse(): InventoryLedgerResponse {
    return InventoryLedgerResponse(
        uid = this.uid,

        // Date
        ledgerDate = this.ledgerDate,
        ledgerLocalDate = this.getLedgerLocalDate(),

        // References (will be populated by service layer if needed)
        inventoryItemId = this.inventoryItemId,
        inventoryItemName = null,
        inventoryItemSku = null,
        warehouseId = this.warehouseId,
        warehouseName = null,

        // Opening Balance
        openingStock = this.openingStock,

        // Additions
        stockIn = this.stockIn,
        transferIn = this.transferIn,
        adjustmentIn = this.adjustmentIn,
        totalInflows = this.getTotalInflows(),

        // Deductions
        stockOut = this.stockOut,
        transferOut = this.transferOut,
        adjustmentOut = this.adjustmentOut,
        totalOutflows = this.getTotalOutflows(),

        // Net Movement
        netMovement = this.getNetMovement(),

        // Closing Balance
        closingStock = this.closingStock,

        // Valuation
        averageCost = this.averageCost,
        closingValue = this.closingValue,

        // Audit
        createdAt = this.createdAt ?: Instant.now(),
        updatedAt = this.updatedAt ?: Instant.now()
    )
}

/**
 * Convert InventoryLedger to LedgerSummaryResponse
 */
fun InventoryLedger.asLedgerSummaryResponse(): LedgerSummaryResponse {
    return LedgerSummaryResponse(
        uid = this.uid,
        ledgerDate = this.ledgerDate,
        ledgerLocalDate = this.getLedgerLocalDate(),
        inventoryItemId = this.inventoryItemId,
        warehouseId = this.warehouseId,
        openingStock = this.openingStock,
        closingStock = this.closingStock,
        netMovement = this.getNetMovement(),
        closingValue = this.closingValue
    )
}

/**
 * Convert list of InventoryLedger to list of InventoryLedgerResponse
 */
fun List<InventoryLedger>.asInventoryLedgerResponses(): List<InventoryLedgerResponse> {
    return this.map { it.asInventoryLedgerResponse() }
}

/**
 * Convert list of InventoryLedger to list of LedgerSummaryResponse
 */
fun List<InventoryLedger>.asLedgerSummaryResponses(): List<LedgerSummaryResponse> {
    return this.map { it.asLedgerSummaryResponse() }
}
