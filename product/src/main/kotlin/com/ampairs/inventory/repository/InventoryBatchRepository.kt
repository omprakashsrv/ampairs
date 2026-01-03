package com.ampairs.inventory.repository

import com.ampairs.inventory.domain.model.InventoryBatch
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Inventory Batch Repository
 *
 * Data access layer for InventoryBatch entities with multi-tenant support.
 * Provides specialized queries for batch allocation strategies (FIFO/FEFO/LIFO).
 *
 * Key Features:
 * - @EntityGraph optimization for relationship loading
 * - Automatic tenant filtering via @TenantId
 * - FIFO (First-In-First-Out) queries
 * - FEFO (First-Expired-First-Out) queries
 * - LIFO (Last-In-First-Out) queries
 * - Expiry date management
 * - Batch availability queries
 */
@Repository
interface InventoryBatchRepository : CrudRepository<InventoryBatch, Long> {

    // ============================================================================
    // Basic Queries
    // ============================================================================

    /**
     * Find batch by unique identifier (UID)
     *
     * @param uid Batch unique identifier
     * @return InventoryBatch if found, null otherwise
     */
    @EntityGraph("InventoryBatch.withItem")
    fun findByUid(uid: String): InventoryBatch?

    /**
     * Find batch by batch number
     * Note: Batch numbers are unique per item per warehouse per tenant
     *
     * @param batchNumber Batch number
     * @return InventoryBatch if found, null otherwise
     */
    @EntityGraph("InventoryBatch.withItem")
    fun findByBatchNumber(batchNumber: String): InventoryBatch?

    /**
     * Find batch by batch number, item, and warehouse
     * Most specific lookup for batches
     *
     * @param batchNumber Batch number
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return InventoryBatch if found, null otherwise
     */
    @EntityGraph("InventoryBatch.withItem")
    fun findByBatchNumberAndInventoryItemIdAndWarehouseId(
        batchNumber: String,
        inventoryItemId: String,
        warehouseId: String
    ): InventoryBatch?

    // ============================================================================
    // Item and Warehouse Queries
    // ============================================================================

    /**
     * Find all active batches for an inventory item at a warehouse
     *
     * @param itemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return List of active batches
     */
    @EntityGraph("InventoryBatch.withItem")
    fun findByInventoryItemIdAndWarehouseIdAndIsActiveTrue(
        itemId: String,
        warehouseId: String
    ): List<InventoryBatch>

    /**
     * Find all batches for an inventory item (all warehouses)
     *
     * @param itemId Inventory item UID
     * @return List of batches
     */
    @EntityGraph("InventoryBatch.withItem")
    fun findByInventoryItemIdAndIsActiveTrue(itemId: String): List<InventoryBatch>

    /**
     * Find all batches in a warehouse
     *
     * @param warehouseId Warehouse UID
     * @return List of batches
     */
    @EntityGraph("InventoryBatch.withItem")
    fun findByWarehouseIdAndIsActiveTrue(warehouseId: String): List<InventoryBatch>

    // ============================================================================
    // Availability Queries
    // ============================================================================

    /**
     * Find batches with available stock
     *
     * @param itemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return List of batches with available stock
     */
    @EntityGraph("InventoryBatch.withItem")
    @Query("""
        SELECT b FROM inventory_batch b
        WHERE b.inventoryItemId = :itemId
        AND b.warehouseId = :warehouseId
        AND b.availableQuantity > 0
        AND b.isActive = true
        AND b.isExpired = false
    """)
    fun findAvailableBatches(
        @Param("itemId") itemId: String,
        @Param("warehouseId") warehouseId: String
    ): List<InventoryBatch>

    // ============================================================================
    // FIFO Queries (First-In-First-Out)
    // ============================================================================

    /**
     * Find available batches in FIFO order (oldest received first)
     *
     * Used when stockConsumptionStrategy = FIFO
     *
     * @param itemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return List of batches ordered by received date (oldest first)
     */
    @EntityGraph("InventoryBatch.withItem")
    @Query("""
        SELECT b FROM inventory_batch b
        WHERE b.inventoryItemId = :itemId
        AND b.warehouseId = :warehouseId
        AND b.availableQuantity > 0
        AND b.isActive = true
        AND b.isExpired = false
        ORDER BY b.receivedDate ASC, b.createdAt ASC
    """)
    fun findAvailableBatchesFIFO(
        @Param("itemId") itemId: String,
        @Param("warehouseId") warehouseId: String
    ): List<InventoryBatch>

    // ============================================================================
    // FEFO Queries (First-Expired-First-Out)
    // ============================================================================

    /**
     * Find available batches in FEFO order (earliest expiry first)
     *
     * Used when stockConsumptionStrategy = FEFO
     * Prioritizes batches with expiry dates, then falls back to FIFO
     *
     * @param itemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return List of batches ordered by expiry date (earliest first), then received date
     */
    @EntityGraph("InventoryBatch.withItem")
    @Query("""
        SELECT b FROM inventory_batch b
        WHERE b.inventoryItemId = :itemId
        AND b.warehouseId = :warehouseId
        AND b.availableQuantity > 0
        AND b.isActive = true
        AND b.isExpired = false
        ORDER BY
            CASE WHEN b.expiryDate IS NULL THEN 1 ELSE 0 END,
            b.expiryDate ASC,
            b.receivedDate ASC,
            b.createdAt ASC
    """)
    fun findAvailableBatchesFEFO(
        @Param("itemId") itemId: String,
        @Param("warehouseId") warehouseId: String
    ): List<InventoryBatch>

    // ============================================================================
    // LIFO Queries (Last-In-First-Out)
    // ============================================================================

    /**
     * Find available batches in LIFO order (newest received first)
     *
     * Used when stockConsumptionStrategy = LIFO
     *
     * @param itemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return List of batches ordered by received date (newest first)
     */
    @EntityGraph("InventoryBatch.withItem")
    @Query("""
        SELECT b FROM inventory_batch b
        WHERE b.inventoryItemId = :itemId
        AND b.warehouseId = :warehouseId
        AND b.availableQuantity > 0
        AND b.isActive = true
        AND b.isExpired = false
        ORDER BY b.receivedDate DESC, b.createdAt DESC
    """)
    fun findAvailableBatchesLIFO(
        @Param("itemId") itemId: String,
        @Param("warehouseId") warehouseId: String
    ): List<InventoryBatch>

    // ============================================================================
    // Expiry Management Queries
    // ============================================================================

    /**
     * Find batches expiring before a given date
     *
     * Used for expiry alerts and automatic expiry processing
     *
     * @param alertDate Cutoff date for expiry alert
     * @return List of batches expiring before the alert date
     */
    @EntityGraph("InventoryBatch.withItem")
    @Query("""
        SELECT b FROM inventory_batch b
        WHERE b.expiryDate IS NOT NULL
        AND b.expiryDate <= :alertDate
        AND b.isActive = true
        AND b.isExpired = false
        AND b.availableQuantity > 0
        ORDER BY b.expiryDate ASC
    """)
    fun findExpiringBatches(@Param("alertDate") alertDate: Instant): List<InventoryBatch>

    /**
     * Find batches that have already expired
     *
     * Used for scheduled job to mark batches as expired
     *
     * @return List of expired batches
     */
    @EntityGraph("InventoryBatch.withItem")
    @Query("""
        SELECT b FROM inventory_batch b
        WHERE b.expiryDate IS NOT NULL
        AND b.expiryDate < CURRENT_TIMESTAMP
        AND b.isExpired = false
    """)
    fun findExpiredBatches(): List<InventoryBatch>

    /**
     * Find batches expiring soon (within N days) for a specific warehouse
     *
     * @param warehouseId Warehouse UID
     * @param alertDate Cutoff date
     * @return List of batches expiring soon
     */
    @EntityGraph("InventoryBatch.withItem")
    @Query("""
        SELECT b FROM inventory_batch b
        WHERE b.warehouseId = :warehouseId
        AND b.expiryDate IS NOT NULL
        AND b.expiryDate <= :alertDate
        AND b.isActive = true
        AND b.isExpired = false
        AND b.availableQuantity > 0
        ORDER BY b.expiryDate ASC
    """)
    fun findExpiringBatchesByWarehouse(
        @Param("warehouseId") warehouseId: String,
        @Param("alertDate") alertDate: Instant
    ): List<InventoryBatch>

    // ============================================================================
    // Statistics Queries
    // ============================================================================

    /**
     * Count batches for an inventory item
     *
     * @param inventoryItemId Inventory item UID
     * @return Batch count
     */
    fun countByInventoryItemIdAndIsActiveTrue(inventoryItemId: String): Long

    /**
     * Count batches with available stock
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return Count of batches with available stock
     */
    @Query("""
        SELECT COUNT(b) FROM inventory_batch b
        WHERE b.inventoryItemId = :itemId
        AND b.warehouseId = :warehouseId
        AND b.availableQuantity > 0
        AND b.isActive = true
        AND b.isExpired = false
    """)
    fun countAvailableBatches(
        @Param("itemId") itemId: String,
        @Param("warehouseId") warehouseId: String
    ): Long

    /**
     * Check if batch number exists for item and warehouse
     *
     * @param batchNumber Batch number
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return true if exists, false otherwise
     */
    fun existsByBatchNumberAndInventoryItemIdAndWarehouseId(
        batchNumber: String,
        inventoryItemId: String,
        warehouseId: String
    ): Boolean
}
