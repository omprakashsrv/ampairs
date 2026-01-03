package com.ampairs.inventory.repository

import com.ampairs.inventory.domain.model.InventoryTransaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant

/**
 * Inventory Transaction Repository
 *
 * Data access layer for InventoryTransaction entities with multi-tenant support.
 * Provides specialized queries for:
 * - Transaction history by item
 * - Date range queries
 * - Reference document lookups (orders, invoices)
 * - Stock movement summaries
 * - Audit trail queries
 *
 * Key Features:
 * - @EntityGraph optimization for relationship loading
 * - Automatic tenant filtering via @TenantId
 * - Pagination support for large transaction histories
 * - Aggregation queries for reporting
 */
@Repository
interface InventoryTransactionRepository : CrudRepository<InventoryTransaction, Long>,
                                            PagingAndSortingRepository<InventoryTransaction, Long> {

    // ============================================================================
    // Basic Queries
    // ============================================================================

    /**
     * Find transaction by unique identifier (UID)
     *
     * @param uid Transaction unique identifier
     * @return InventoryTransaction if found, null otherwise
     */
    @EntityGraph("InventoryTransaction.full")
    fun findByUid(uid: String): InventoryTransaction?

    /**
     * Find transaction by transaction number
     *
     * @param transactionNumber Unique transaction number (e.g., TXN-20250119-0001)
     * @return InventoryTransaction if found, null otherwise
     */
    @EntityGraph("InventoryTransaction.full")
    fun findByTransactionNumber(transactionNumber: String): InventoryTransaction?

    // ============================================================================
    // Item-based Queries
    // ============================================================================

    /**
     * Find all transactions for a specific inventory item
     * Ordered by transaction date (most recent first)
     *
     * @param inventoryItemId Inventory item UID
     * @param pageable Pagination parameters
     * @return Page of transactions
     */
    @EntityGraph("InventoryTransaction.full")
    fun findByInventoryItemIdOrderByTransactionDateDesc(
        inventoryItemId: String,
        pageable: Pageable
    ): Page<InventoryTransaction>

    /**
     * Find transactions for an item within a date range
     *
     * @param itemId Inventory item UID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of transactions ordered by date descending
     */
    @EntityGraph("InventoryTransaction.full")
    @Query("""
        SELECT t FROM inventory_transaction t
        WHERE t.inventoryItemId = :itemId
        AND t.transactionDate >= :startDate
        AND t.transactionDate <= :endDate
        ORDER BY t.transactionDate DESC
    """)
    fun findByItemAndDateRange(
        @Param("itemId") itemId: String,
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant
    ): List<InventoryTransaction>

    /**
     * Find transactions for an item by transaction type
     *
     * @param itemId Inventory item UID
     * @param transactionType Transaction type (STOCK_IN, STOCK_OUT, etc.)
     * @param pageable Pagination parameters
     * @return Page of transactions
     */
    @EntityGraph("InventoryTransaction.full")
    fun findByInventoryItemIdAndTransactionTypeOrderByTransactionDateDesc(
        inventoryItemId: String,
        transactionType: String,
        pageable: Pageable
    ): Page<InventoryTransaction>

    // ============================================================================
    // Warehouse-based Queries
    // ============================================================================

    /**
     * Find all transactions for a specific warehouse
     *
     * @param warehouseId Warehouse UID
     * @param pageable Pagination parameters
     * @return Page of transactions
     */
    @EntityGraph("InventoryTransaction.full")
    fun findByWarehouseIdOrderByTransactionDateDesc(
        warehouseId: String,
        pageable: Pageable
    ): Page<InventoryTransaction>

    /**
     * Find transactions for a warehouse within a date range
     *
     * @param warehouseId Warehouse UID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of transactions ordered by date descending
     */
    @EntityGraph("InventoryTransaction.full")
    @Query("""
        SELECT t FROM inventory_transaction t
        WHERE t.warehouseId = :warehouseId
        AND t.transactionDate >= :startDate
        AND t.transactionDate <= :endDate
        ORDER BY t.transactionDate DESC
    """)
    fun findByWarehouseAndDateRange(
        @Param("warehouseId") warehouseId: String,
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant
    ): List<InventoryTransaction>

    // ============================================================================
    // Reference Document Queries
    // ============================================================================

    /**
     * Find transactions linked to a reference document
     * Used to lookup transactions for orders, invoices, purchases, etc.
     *
     * @param referenceType Type of reference (ORDER, INVOICE, PURCHASE, COUNT)
     * @param referenceId UID of reference document
     * @return List of transactions for the reference
     */
    @EntityGraph("InventoryTransaction.full")
    fun findByReferenceTypeAndReferenceId(
        referenceType: String,
        referenceId: String
    ): List<InventoryTransaction>

    /**
     * Find transactions by reference type only
     *
     * @param referenceType Type of reference (ORDER, INVOICE, PURCHASE, COUNT)
     * @param pageable Pagination parameters
     * @return Page of transactions
     */
    @EntityGraph("InventoryTransaction.full")
    fun findByReferenceTypeOrderByTransactionDateDesc(
        referenceType: String,
        pageable: Pageable
    ): Page<InventoryTransaction>

    // ============================================================================
    // Date Range Queries (All Transactions)
    // ============================================================================

    /**
     * Find all transactions within a date range
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param pageable Pagination parameters
     * @return Page of transactions
     */
    @EntityGraph("InventoryTransaction.full")
    @Query("""
        SELECT t FROM inventory_transaction t
        WHERE t.transactionDate >= :startDate
        AND t.transactionDate <= :endDate
        ORDER BY t.transactionDate DESC
    """)
    fun findByDateRange(
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant,
        pageable: Pageable
    ): Page<InventoryTransaction>

    // ============================================================================
    // Aggregation Queries
    // ============================================================================

    /**
     * Sum quantities by transaction type for an item
     * Useful for calculating total stock-in, stock-out, etc.
     *
     * @param itemId Inventory item UID
     * @param type Transaction type (STOCK_IN, STOCK_OUT, etc.)
     * @param startDate Start date (inclusive)
     * @return Total quantity (null if no transactions)
     */
    @Query("""
        SELECT SUM(t.quantity)
        FROM inventory_transaction t
        WHERE t.inventoryItemId = :itemId
        AND t.transactionType = :type
        AND t.transactionDate >= :startDate
    """)
    fun sumQuantityByTypeAndDate(
        @Param("itemId") itemId: String,
        @Param("type") type: String,
        @Param("startDate") startDate: Instant
    ): BigDecimal?

    /**
     * Sum quantities for an item within a date range
     *
     * @param itemId Inventory item UID
     * @param transactionType Transaction type
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Total quantity (null if no transactions)
     */
    @Query("""
        SELECT SUM(t.quantity)
        FROM inventory_transaction t
        WHERE t.inventoryItemId = :itemId
        AND t.transactionType = :type
        AND t.transactionDate >= :startDate
        AND t.transactionDate <= :endDate
    """)
    fun sumQuantityByTypeAndDateRange(
        @Param("itemId") itemId: String,
        @Param("type") type: String,
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant
    ): BigDecimal?

    /**
     * Calculate total cost for transactions
     *
     * @param itemId Inventory item UID
     * @param transactionType Transaction type
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Total cost (null if no transactions)
     */
    @Query("""
        SELECT SUM(t.totalCost)
        FROM inventory_transaction t
        WHERE t.inventoryItemId = :itemId
        AND t.transactionType = :type
        AND t.transactionDate >= :startDate
        AND t.transactionDate <= :endDate
    """)
    fun sumCostByTypeAndDateRange(
        @Param("itemId") itemId: String,
        @Param("type") type: String,
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant
    ): BigDecimal?

    // ============================================================================
    // Batch and Serial Queries
    // ============================================================================

    /**
     * Find transactions for a specific batch
     *
     * @param batchId Batch UID
     * @return List of transactions for the batch
     */
    @EntityGraph("InventoryTransaction.full")
    fun findByBatchIdOrderByTransactionDateDesc(batchId: String): List<InventoryTransaction>

    /**
     * Find transactions containing specific serial numbers
     * Note: This requires JSON search, may not be efficient for large datasets
     *
     * @param serialNumber Serial number to search for
     * @return List of transactions containing the serial number
     */
    @EntityGraph("InventoryTransaction.full")
    @Query("""
        SELECT t FROM inventory_transaction t
        WHERE JSON_CONTAINS(t.serialNumbers, :serialNumber) = 1
    """, nativeQuery = false)
    fun findBySerialNumber(@Param("serialNumber") serialNumber: String): List<InventoryTransaction>

    // ============================================================================
    // Statistics & Reporting
    // ============================================================================

    /**
     * Count transactions for an item
     *
     * @param inventoryItemId Inventory item UID
     * @return Transaction count
     */
    fun countByInventoryItemId(inventoryItemId: String): Long

    /**
     * Count transactions by type for an item
     *
     * @param inventoryItemId Inventory item UID
     * @param transactionType Transaction type
     * @return Transaction count
     */
    fun countByInventoryItemIdAndTransactionType(
        inventoryItemId: String,
        transactionType: String
    ): Long

    /**
     * Get latest transaction for an item
     *
     * @param inventoryItemId Inventory item UID
     * @return Latest transaction or null
     */
    @EntityGraph("InventoryTransaction.full")
    fun findFirstByInventoryItemIdOrderByTransactionDateDesc(
        inventoryItemId: String
    ): InventoryTransaction?

    /**
     * Get latest transaction for an item and warehouse
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return Latest transaction or null
     */
    @EntityGraph("InventoryTransaction.full")
    fun findFirstByInventoryItemIdAndWarehouseIdOrderByTransactionDateDesc(
        inventoryItemId: String,
        warehouseId: String
    ): InventoryTransaction?

    // ============================================================================
    // Transaction Number Generation Support
    // ============================================================================

    /**
     * Count transactions created today
     * Used for generating sequential transaction numbers
     *
     * @param startOfDay Start of current day
     * @return Count of transactions created today
     */
    @Query("""
        SELECT COUNT(t)
        FROM inventory_transaction t
        WHERE t.createdAt >= :startOfDay
    """)
    fun countTransactionsToday(@Param("startOfDay") startOfDay: Instant): Long

    /**
     * Check if transaction number already exists
     *
     * @param transactionNumber Transaction number to check
     * @return true if exists, false otherwise
     */
    fun existsByTransactionNumber(transactionNumber: String): Boolean

    // ============================================================================
    // Ledger Generation Support
    // ============================================================================

    /**
     * Find all transactions within a date range
     *
     * Used for daily ledger generation
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (exclusive)
     * @return List of all transactions in the range
     */
    @EntityGraph("InventoryTransaction.full")
    fun findByTransactionDateBetween(
        startDate: Instant,
        endDate: Instant
    ): List<InventoryTransaction>
}
