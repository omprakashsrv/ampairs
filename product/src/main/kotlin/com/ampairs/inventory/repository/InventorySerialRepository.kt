package com.ampairs.inventory.repository

import com.ampairs.inventory.domain.model.InventorySerial
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Inventory Serial Repository
 *
 * Data access layer for InventorySerial entities with multi-tenant support.
 * Provides specialized queries for serial number tracking and lifecycle management.
 *
 * Key Features:
 * - Automatic tenant filtering via @TenantId
 * - Status-based queries
 * - Customer and warranty tracking
 * - Batch association queries
 * - Unique serial number validation
 */
@Repository
interface InventorySerialRepository : CrudRepository<InventorySerial, Long> {

    // ============================================================================
    // Basic Queries
    // ============================================================================

    /**
     * Find serial by unique identifier (UID)
     *
     * @param uid Serial UID
     * @return InventorySerial if found, null otherwise
     */
    fun findByUid(uid: String): InventorySerial?

    /**
     * Find serial by serial number
     * Serial numbers are unique per tenant
     *
     * @param serialNumber Serial number
     * @return InventorySerial if found, null otherwise
     */
    fun findBySerialNumber(serialNumber: String): InventorySerial?

    /**
     * Check if serial number exists
     *
     * @param serialNumber Serial number
     * @return true if exists, false otherwise
     */
    fun existsBySerialNumber(serialNumber: String): Boolean

    // ============================================================================
    // Item and Warehouse Queries
    // ============================================================================

    /**
     * Find all serials for an inventory item
     *
     * @param inventoryItemId Inventory item UID
     * @return List of serials
     */
    fun findByInventoryItemId(inventoryItemId: String): List<InventorySerial>

    /**
     * Find serials for an inventory item at a warehouse
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return List of serials
     */
    fun findByInventoryItemIdAndWarehouseId(
        inventoryItemId: String,
        warehouseId: String
    ): List<InventorySerial>

    /**
     * Find serials for a batch
     *
     * @param batchId Batch UID
     * @return List of serials in the batch
     */
    fun findByBatchId(batchId: String): List<InventorySerial>

    // ============================================================================
    // Status-Based Queries
    // ============================================================================

    /**
     * Find serials by status
     *
     * @param status Status (AVAILABLE, RESERVED, SOLD, DAMAGED, RETURNED)
     * @return List of serials with the specified status
     */
    fun findByStatus(status: String): List<InventorySerial>

    /**
     * Find serials by inventory item and status
     *
     * @param inventoryItemId Inventory item UID
     * @param status Status
     * @return List of serials
     */
    fun findByInventoryItemIdAndStatus(
        inventoryItemId: String,
        status: String
    ): List<InventorySerial>

    /**
     * Find serials by inventory item, warehouse, and status
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @param status Status
     * @return List of serials
     */
    fun findByInventoryItemIdAndWarehouseIdAndStatus(
        inventoryItemId: String,
        warehouseId: String,
        status: String
    ): List<InventorySerial>

    /**
     * Find available serials for an item at a warehouse
     *
     * Shortcut for finding AVAILABLE serials
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return List of available serials
     */
    @Query("""
        SELECT s FROM inventory_serial s
        WHERE s.inventoryItemId = :itemId
        AND s.warehouseId = :warehouseId
        AND s.status = 'AVAILABLE'
        ORDER BY s.receivedDate ASC
    """)
    fun findAvailableSerials(
        @Param("itemId") itemId: String,
        @Param("warehouseId") warehouseId: String
    ): List<InventorySerial>

    /**
     * Find reserved serials for an item at a warehouse
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return List of reserved serials
     */
    @Query("""
        SELECT s FROM inventory_serial s
        WHERE s.inventoryItemId = :itemId
        AND s.warehouseId = :warehouseId
        AND s.status = 'RESERVED'
        ORDER BY s.updatedAt DESC
    """)
    fun findReservedSerials(
        @Param("itemId") itemId: String,
        @Param("warehouseId") warehouseId: String
    ): List<InventorySerial>

    // ============================================================================
    // Customer Queries
    // ============================================================================

    /**
     * Find serials sold to a customer
     *
     * @param customerId Customer UID
     * @return List of serials sold to the customer
     */
    fun findByCustomerId(customerId: String): List<InventorySerial>

    /**
     * Find serials sold to a customer for a specific item
     *
     * @param customerId Customer UID
     * @param inventoryItemId Inventory item UID
     * @return List of serials
     */
    fun findByCustomerIdAndInventoryItemId(
        customerId: String,
        inventoryItemId: String
    ): List<InventorySerial>

    // ============================================================================
    // Reference Document Queries
    // ============================================================================

    /**
     * Find serials linked to a sale reference
     *
     * @param referenceType Type of reference (ORDER, INVOICE)
     * @param referenceId Reference UID
     * @return List of serials
     */
    fun findBySoldReferenceTypeAndSoldReferenceId(
        referenceType: String,
        referenceId: String
    ): List<InventorySerial>

    /**
     * Find serials linked to a return reference
     *
     * @param referenceType Type of return reference
     * @param referenceId Reference UID
     * @return List of returned serials
     */
    fun findByReturnReferenceTypeAndReturnReferenceId(
        referenceType: String,
        referenceId: String
    ): List<InventorySerial>

    // ============================================================================
    // Warranty Queries
    // ============================================================================

    /**
     * Find serials with warranty expiring before a given date
     *
     * @param expiryDate Cutoff date for warranty expiry
     * @return List of serials with expiring warranties
     */
    @Query("""
        SELECT s FROM inventory_serial s
        WHERE s.warrantyExpiryDate IS NOT NULL
        AND s.warrantyExpiryDate <= :expiryDate
        AND s.status = 'SOLD'
        ORDER BY s.warrantyExpiryDate ASC
    """)
    fun findSerialsWithExpiringWarranty(@Param("expiryDate") expiryDate: Instant): List<InventorySerial>

    /**
     * Find serials sold to a customer with active warranty
     *
     * @param customerId Customer UID
     * @return List of serials with active warranty
     */
    @Query("""
        SELECT s FROM inventory_serial s
        WHERE s.customerId = :customerId
        AND s.status = 'SOLD'
        AND (s.warrantyExpiryDate IS NULL OR s.warrantyExpiryDate > CURRENT_TIMESTAMP)
        ORDER BY s.soldDate DESC
    """)
    fun findCustomerSerialsWithActiveWarranty(@Param("customerId") customerId: String): List<InventorySerial>

    // ============================================================================
    // Statistics Queries
    // ============================================================================

    /**
     * Count serials by item and status
     *
     * @param inventoryItemId Inventory item UID
     * @param status Status
     * @return Count of serials
     */
    fun countByInventoryItemIdAndStatus(
        inventoryItemId: String,
        status: String
    ): Long

    /**
     * Count available serials for an item at a warehouse
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return Count of available serials
     */
    @Query("""
        SELECT COUNT(s) FROM inventory_serial s
        WHERE s.inventoryItemId = :itemId
        AND s.warehouseId = :warehouseId
        AND s.status = 'AVAILABLE'
    """)
    fun countAvailableSerials(
        @Param("itemId") itemId: String,
        @Param("warehouseId") warehouseId: String
    ): Long

    /**
     * Count serials sold within a date range
     *
     * @param inventoryItemId Inventory item UID
     * @param startDate Start date
     * @param endDate End date
     * @return Count of serials sold
     */
    @Query("""
        SELECT COUNT(s) FROM inventory_serial s
        WHERE s.inventoryItemId = :itemId
        AND s.soldDate >= :startDate
        AND s.soldDate <= :endDate
    """)
    fun countSerialsSoldInDateRange(
        @Param("itemId") itemId: String,
        @Param("startDate") startDate: Instant,
        @Param("endDate") endDate: Instant
    ): Long

    /**
     * Count damaged serials
     *
     * @param inventoryItemId Inventory item UID
     * @return Count of damaged serials
     */
    @Query("""
        SELECT COUNT(s) FROM inventory_serial s
        WHERE s.inventoryItemId = :itemId
        AND s.status = 'DAMAGED'
    """)
    fun countDamagedSerials(@Param("itemId") itemId: String): Long

    // ============================================================================
    // Bulk Operations Support
    // ============================================================================

    /**
     * Find serials by serial number list
     *
     * Used for bulk operations and validation
     *
     * @param serialNumbers List of serial numbers
     * @return List of serials
     */
    fun findBySerialNumberIn(serialNumbers: List<String>): List<InventorySerial>

    /**
     * Check if any serial numbers exist
     *
     * Used for bulk validation
     *
     * @param serialNumbers List of serial numbers to check
     * @return true if any exist, false otherwise
     */
    fun existsBySerialNumberIn(serialNumbers: List<String>): Boolean

    /**
     * Count how many serial numbers exist
     *
     * @param serialNumbers List of serial numbers
     * @return Count of existing serials
     */
    @Query("""
        SELECT COUNT(s) FROM inventory_serial s
        WHERE s.serialNumber IN :serialNumbers
    """)
    fun countBySerialNumberIn(@Param("serialNumbers") serialNumbers: List<String>): Long

    /**
     * Count all serials for an item at a warehouse
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return Total count
     */
    fun countByInventoryItemIdAndWarehouseId(
        inventoryItemId: String,
        warehouseId: String
    ): Long

    /**
     * Count serials by item, warehouse, and status
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @param status Status
     * @return Count
     */
    fun countByInventoryItemIdAndWarehouseIdAndStatus(
        inventoryItemId: String,
        warehouseId: String,
        status: String
    ): Long
}
