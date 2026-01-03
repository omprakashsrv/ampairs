package com.ampairs.inventory.repository

import com.ampairs.inventory.domain.model.InventoryLedger
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Inventory Ledger Repository
 *
 * Data access layer for InventoryLedger entities with multi-tenant support.
 * Provides queries for daily ledger entries and historical stock analysis.
 *
 * Key Features:
 * - Automatic tenant filtering via @TenantId
 * - Date-based queries
 * - Item and warehouse filtering
 * - Historical stock tracking
 * - Opening balance retrieval
 */
@Repository
interface InventoryLedgerRepository : CrudRepository<InventoryLedger, Long> {

    // ============================================================================
    // Basic Queries
    // ============================================================================

    /**
     * Find ledger by UID
     *
     * @param uid Ledger UID
     * @return InventoryLedger if found, null otherwise
     */
    fun findByUid(uid: String): InventoryLedger?

    /**
     * Find ledger entry for specific item, warehouse, and date
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @param ledgerDate Ledger date
     * @return InventoryLedger if found, null otherwise
     */
    fun findByInventoryItemIdAndWarehouseIdAndLedgerDate(
        inventoryItemId: String,
        warehouseId: String,
        ledgerDate: Instant
    ): InventoryLedger?

    /**
     * Check if ledger entry exists for item, warehouse, and date
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @param ledgerDate Ledger date
     * @return true if exists, false otherwise
     */
    fun existsByInventoryItemIdAndWarehouseIdAndLedgerDate(
        inventoryItemId: String,
        warehouseId: String,
        ledgerDate: Instant
    ): Boolean

    // ============================================================================
    // Date Range Queries
    // ============================================================================

    /**
     * Find ledger entries for an item at a warehouse within date range
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of ledger entries ordered by date
     */
    @Query("""
        SELECT l FROM inventory_ledger l
        WHERE l.inventoryItemId = :itemId
        AND l.warehouseId = :warehouseId
        AND l.ledgerDate >= :startDate
        AND l.ledgerDate <= :endDate
        ORDER BY l.ledgerDate ASC
    """)
    fun findByItemAndWarehouseAndDateRange(
        @Param("itemId") itemId: String,
        @Param("warehouseId") warehouseId: String,
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant
    ): List<InventoryLedger>

    /**
     * Find all ledger entries for a warehouse on a specific date
     *
     * Used for warehouse-level stock reports
     *
     * @param warehouseId Warehouse UID
     * @param ledgerDate Ledger date
     * @return List of ledger entries for all items in the warehouse
     */
    @Query("""
        SELECT l FROM inventory_ledger l
        WHERE l.warehouseId = :warehouseId
        AND l.ledgerDate = :date
        ORDER BY l.inventoryItemId ASC
    """)
    fun findByWarehouseAndDate(
        @Param("warehouseId") warehouseId: String,
        @Param("date") date: Instant
    ): List<InventoryLedger>

    /**
     * Find all ledger entries for a specific date
     *
     * Used for daily stock reports across all warehouses
     *
     * @param ledgerDate Ledger date
     * @return List of all ledger entries for the date
     */
    @Query("""
        SELECT l FROM inventory_ledger l
        WHERE l.ledgerDate = :date
        ORDER BY l.warehouseId ASC, l.inventoryItemId ASC
    """)
    fun findByDate(@Param("date") date: Instant): List<InventoryLedger>

    /**
     * Find ledger entries for an item across all warehouses on a date
     *
     * @param inventoryItemId Inventory item UID
     * @param ledgerDate Ledger date
     * @return List of ledger entries
     */
    @Query("""
        SELECT l FROM inventory_ledger l
        WHERE l.inventoryItemId = :itemId
        AND l.ledgerDate = :date
        ORDER BY l.warehouseId ASC
    """)
    fun findByItemAndDate(
        @Param("itemId") itemId: String,
        @Param("date") date: Instant
    ): List<InventoryLedger>

    // ============================================================================
    // Opening Balance Queries
    // ============================================================================

    /**
     * Find most recent ledger entry before a given date
     *
     * Used to get opening balance for a new ledger entry
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @param beforeDate Date to search before
     * @return Most recent ledger entry, or null if none found
     */
    @Query("""
        SELECT l FROM inventory_ledger l
        WHERE l.inventoryItemId = :itemId
        AND l.warehouseId = :warehouseId
        AND l.ledgerDate < :beforeDate
        ORDER BY l.ledgerDate DESC
        LIMIT 1
    """)
    fun findMostRecentBefore(
        @Param("itemId") itemId: String,
        @Param("warehouseId") warehouseId: String,
        @Param("beforeDate") beforeDate: Instant
    ): InventoryLedger?

    /**
     * Find latest ledger entry for an item at a warehouse
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return Latest ledger entry, or null if none found
     */
    @Query("""
        SELECT l FROM inventory_ledger l
        WHERE l.inventoryItemId = :itemId
        AND l.warehouseId = :warehouseId
        ORDER BY l.ledgerDate DESC
        LIMIT 1
    """)
    fun findLatestByItemAndWarehouse(
        @Param("itemId") itemId: String,
        @Param("warehouseId") warehouseId: String
    ): InventoryLedger?

    // ============================================================================
    // Statistics and Aggregation Queries
    // ============================================================================

    /**
     * Calculate total stock value for a warehouse on a date
     *
     * @param warehouseId Warehouse UID
     * @param ledgerDate Ledger date
     * @return Total closing value
     */
    @Query("""
        SELECT SUM(l.closingValue) FROM inventory_ledger l
        WHERE l.warehouseId = :warehouseId
        AND l.ledgerDate = :date
    """)
    fun calculateWarehouseValueOnDate(
        @Param("warehouseId") warehouseId: String,
        @Param("date") date: Instant
    ): java.math.BigDecimal?

    /**
     * Calculate total stock quantity for a warehouse on a date
     *
     * @param warehouseId Warehouse UID
     * @param ledgerDate Ledger date
     * @return Total closing stock
     */
    @Query("""
        SELECT SUM(l.closingStock) FROM inventory_ledger l
        WHERE l.warehouseId = :warehouseId
        AND l.ledgerDate = :date
    """)
    fun calculateWarehouseStockOnDate(
        @Param("warehouseId") warehouseId: String,
        @Param("date") date: Instant
    ): java.math.BigDecimal?

    /**
     * Find items with movement (inflow or outflow) on a date
     *
     * @param ledgerDate Ledger date
     * @return List of ledger entries with movements
     */
    @Query("""
        SELECT l FROM inventory_ledger l
        WHERE l.ledgerDate = :date
        AND (l.stockIn > 0 OR l.stockOut > 0
             OR l.transferIn > 0 OR l.transferOut > 0
             OR l.adjustmentIn > 0 OR l.adjustmentOut > 0)
        ORDER BY l.warehouseId ASC, l.inventoryItemId ASC
    """)
    fun findEntriesWithMovementOnDate(@Param("date") date: Instant): List<InventoryLedger>

    /**
     * Count ledger entries for a date
     *
     * @param ledgerDate Ledger date
     * @return Count of entries
     */
    fun countByLedgerDate(ledgerDate: Instant): Long

    // ============================================================================
    // Item-Level Queries
    // ============================================================================

    /**
     * Find all ledger entries for an inventory item (all warehouses, all dates)
     *
     * @param inventoryItemId Inventory item UID
     * @return List of ledger entries ordered by date and warehouse
     */
    @Query("""
        SELECT l FROM inventory_ledger l
        WHERE l.inventoryItemId = :itemId
        ORDER BY l.ledgerDate DESC, l.warehouseId ASC
    """)
    fun findAllByItem(@Param("itemId") itemId: String): List<InventoryLedger>

    /**
     * Find all ledger entries for a warehouse (all items, all dates)
     *
     * @param warehouseId Warehouse UID
     * @return List of ledger entries ordered by date and item
     */
    @Query("""
        SELECT l FROM inventory_ledger l
        WHERE l.warehouseId = :warehouseId
        ORDER BY l.ledgerDate DESC, l.inventoryItemId ASC
    """)
    fun findAllByWarehouse(@Param("warehouseId") warehouseId: String): List<InventoryLedger>
}
