package com.ampairs.inventory.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.inventory.domain.dto.InventoryLedgerResponse
import com.ampairs.inventory.domain.dto.asInventoryLedgerResponse
import com.ampairs.inventory.domain.dto.asInventoryLedgerResponses
import com.ampairs.inventory.domain.model.InventoryLedger
import com.ampairs.inventory.service.InventoryLedgerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Ledger Controller
 *
 * REST API endpoints for inventory ledger queries and reporting.
 * Provides access to daily stock snapshots and historical analysis.
 *
 * Base path: /inventory/v1/ledger
 */
@RestController
@RequestMapping("/inventory/v1/ledger")
class LedgerController @Autowired constructor(
    private val inventoryLedgerService: InventoryLedgerService
) {

    // ============================================================================
    // Ledger Query Endpoints
    // ============================================================================

    /**
     * Get ledger entry by UID
     *
     * GET /inventory/v1/ledger/{uid}
     *
     * @param uid Ledger UID
     * @return Ledger entry
     */
    @GetMapping("/{uid}")
    fun getLedger(@PathVariable uid: String): ApiResponse<InventoryLedgerResponse> {
        val ledger = inventoryLedgerService.getLedgerByUid(uid)
        return ApiResponse.success(ledger.asInventoryLedgerResponse())
    }

    /**
     * Get ledger for specific item, warehouse, and date
     *
     * GET /inventory/v1/ledger/item/{itemId}/warehouse/{warehouseId}/date/{date}
     *
     * @param itemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @param date Ledger date (YYYY-MM-DD)
     * @return Ledger entry if found
     */
    @GetMapping("/item/{itemId}/warehouse/{warehouseId}/date/{date}")
    fun getLedgerByItemWarehouseDate(
        @PathVariable itemId: String,
        @PathVariable warehouseId: String,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ApiResponse<InventoryLedgerResponse?> {
        val ledgerDate = InventoryLedger.ledgerDateFromLocalDate(date)
        val ledger = inventoryLedgerService.getLedger(itemId, warehouseId, ledgerDate)
        return ApiResponse.success(ledger?.asInventoryLedgerResponse())
    }

    /**
     * Get ledger history for an item at a warehouse
     *
     * GET /inventory/v1/ledger/item/{itemId}/warehouse/{warehouseId}/history
     *
     * @param itemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @param startDate Start date (YYYY-MM-DD)
     * @param endDate End date (YYYY-MM-DD)
     * @return List of ledger entries
     */
    @GetMapping("/item/{itemId}/warehouse/{warehouseId}/history")
    fun getLedgerHistory(
        @PathVariable itemId: String,
        @PathVariable warehouseId: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ApiResponse<List<InventoryLedgerResponse>> {
        val start = InventoryLedger.ledgerDateFromLocalDate(startDate)
        val end = InventoryLedger.ledgerDateFromLocalDate(endDate)
        val ledgers = inventoryLedgerService.getLedgersByDateRange(itemId, warehouseId, start, end)
        return ApiResponse.success(ledgers.asInventoryLedgerResponses())
    }

    // ============================================================================
    // Daily Ledger Endpoints
    // ============================================================================

    /**
     * Get all ledger entries for a specific date
     *
     * GET /inventory/v1/ledger/daily/{date}
     *
     * Returns ledger for all items across all warehouses
     *
     * @param date Ledger date (YYYY-MM-DD)
     * @return List of all ledger entries for the date
     */
    @GetMapping("/daily/{date}")
    fun getDailyLedger(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ApiResponse<List<InventoryLedgerResponse>> {
        val ledgerDate = InventoryLedger.ledgerDateFromLocalDate(date)
        val ledgers = inventoryLedgerService.getDailyLedger(ledgerDate)
        return ApiResponse.success(ledgers.asInventoryLedgerResponses())
    }

    /**
     * Get warehouse ledger for a specific date
     *
     * GET /inventory/v1/ledger/warehouse/{warehouseId}/date/{date}
     *
     * Returns ledger for all items in the warehouse
     *
     * @param warehouseId Warehouse UID
     * @param date Ledger date (YYYY-MM-DD)
     * @return List of ledger entries for the warehouse
     */
    @GetMapping("/warehouse/{warehouseId}/date/{date}")
    fun getWarehouseLedger(
        @PathVariable warehouseId: String,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ApiResponse<List<InventoryLedgerResponse>> {
        val ledgerDate = InventoryLedger.ledgerDateFromLocalDate(date)
        val ledgers = inventoryLedgerService.getWarehouseLedger(warehouseId, ledgerDate)
        return ApiResponse.success(ledgers.asInventoryLedgerResponses())
    }

    // ============================================================================
    // Ledger Generation Endpoints
    // ============================================================================

    /**
     * Generate ledger for a specific date
     *
     * POST /inventory/v1/ledger/generate/date/{date}
     *
     * Manually trigger ledger generation for a date
     *
     * @param date Date to generate ledger for (YYYY-MM-DD)
     * @return Number of ledger entries created/updated
     */
    @PostMapping("/generate/date/{date}")
    fun generateLedgerForDate(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ApiResponse<Int> {
        val count = inventoryLedgerService.generateDailyLedgerForDate(date)
        return ApiResponse.success(count)
    }

    /**
     * Generate ledger for a date range
     *
     * POST /inventory/v1/ledger/generate/range
     *
     * Backfill ledger entries for a date range
     *
     * @param startDate Start date (YYYY-MM-DD)
     * @param endDate End date (YYYY-MM-DD)
     * @return Total number of ledger entries created/updated
     */
    @PostMapping("/generate/range")
    fun generateLedgerForRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ApiResponse<Int> {
        val count = inventoryLedgerService.generateLedgerForDateRange(startDate, endDate)
        return ApiResponse.success(count)
    }

    /**
     * Generate yesterday's ledger
     *
     * POST /inventory/v1/ledger/generate/yesterday
     *
     * Manually trigger generation of yesterday's ledger
     *
     * @return Number of ledger entries created/updated
     */
    @PostMapping("/generate/yesterday")
    fun generateYesterdayLedger(): ApiResponse<Int> {
        val count = inventoryLedgerService.generateDailyLedgerForAllItems()
        return ApiResponse.success(count)
    }

    // ============================================================================
    // Statistics Endpoints
    // ============================================================================

    /**
     * Get warehouse stock value on a date
     *
     * GET /inventory/v1/ledger/warehouse/{warehouseId}/date/{date}/value
     *
     * @param warehouseId Warehouse UID
     * @param date Ledger date (YYYY-MM-DD)
     * @return Total stock value
     */
    @GetMapping("/warehouse/{warehouseId}/date/{date}/value")
    fun getWarehouseStockValue(
        @PathVariable warehouseId: String,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ApiResponse<BigDecimal> {
        val ledgerDate = InventoryLedger.ledgerDateFromLocalDate(date)
        val value = inventoryLedgerService.getWarehouseStockValue(warehouseId, ledgerDate)
        return ApiResponse.success(value)
    }

    /**
     * Get warehouse stock quantity on a date
     *
     * GET /inventory/v1/ledger/warehouse/{warehouseId}/date/{date}/quantity
     *
     * @param warehouseId Warehouse UID
     * @param date Ledger date (YYYY-MM-DD)
     * @return Total stock quantity
     */
    @GetMapping("/warehouse/{warehouseId}/date/{date}/quantity")
    fun getWarehouseStockQuantity(
        @PathVariable warehouseId: String,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ApiResponse<BigDecimal> {
        val ledgerDate = InventoryLedger.ledgerDateFromLocalDate(date)
        val quantity = inventoryLedgerService.getWarehouseStockQuantity(warehouseId, ledgerDate)
        return ApiResponse.success(quantity)
    }

    /**
     * Get items with movement on a date
     *
     * GET /inventory/v1/ledger/date/{date}/movements
     *
     * Returns items that had stock movements (in/out/transfers/adjustments)
     *
     * @param date Ledger date (YYYY-MM-DD)
     * @return List of ledger entries with movements
     */
    @GetMapping("/date/{date}/movements")
    fun getItemsWithMovement(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ApiResponse<List<InventoryLedgerResponse>> {
        val ledgerDate = InventoryLedger.ledgerDateFromLocalDate(date)
        val ledgers = inventoryLedgerService.getItemsWithMovement(ledgerDate)
        return ApiResponse.success(ledgers.asInventoryLedgerResponses())
    }
}
