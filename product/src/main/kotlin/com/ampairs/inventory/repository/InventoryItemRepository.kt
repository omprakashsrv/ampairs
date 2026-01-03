package com.ampairs.inventory.repository

import com.ampairs.inventory.domain.model.InventoryItem
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository

/**
 * Inventory Item Repository
 *
 * Data access layer for InventoryItem entities with multi-tenant support.
 * Provides CRUD operations, search, filtering, and stock alert queries.
 *
 * Key Features:
 * - Automatic tenant filtering via @TenantId
 * - @EntityGraph for efficient loading with warehouse and unit
 * - Product and variant linkage queries
 * - Low stock and overstock detection
 * - Search functionality
 */
@Repository
interface InventoryItemRepository : CrudRepository<InventoryItem, Long>,
                                     PagingAndSortingRepository<InventoryItem, Long> {

    /**
     * Find inventory item by unique identifier (UID)
     *
     * @param uid Inventory item unique identifier
     * @return InventoryItem if found, null otherwise
     */
    @EntityGraph("InventoryItem.withRelations")
    fun findByUid(uid: String): InventoryItem?

    /**
     * Find inventory item by SKU
     *
     * SKU is unique per tenant
     *
     * @param sku Stock Keeping Unit
     * @return InventoryItem if found, null otherwise
     */
    @EntityGraph("InventoryItem.withRelations")
    fun findBySku(sku: String): InventoryItem?

    /**
     * Find inventory item by product and warehouse
     *
     * Used to get inventory for a specific product at a specific location
     *
     * @param productId Product UID
     * @param warehouseId Warehouse UID
     * @return InventoryItem if found, null otherwise
     */
    @EntityGraph("InventoryItem.withRelations")
    fun findByProductIdAndWarehouseId(productId: String, warehouseId: String): InventoryItem?

    /**
     * Find inventory item by product variant and warehouse
     *
     * Used for variant-based inventory tracking
     *
     * @param variantId ProductVariant UID
     * @param warehouseId Warehouse UID
     * @return InventoryItem if found, null otherwise
     */
    @EntityGraph("InventoryItem.withRelations")
    fun findByProductVariantIdAndWarehouseId(variantId: String, warehouseId: String): InventoryItem?

    /**
     * Find all inventory items in a specific warehouse
     *
     * @param warehouseId Warehouse UID
     * @return List of inventory items
     */
    @EntityGraph("InventoryItem.withRelations")
    fun findByWarehouseId(warehouseId: String): List<InventoryItem>

    /**
     * Find all active inventory items in a warehouse
     *
     * @param warehouseId Warehouse UID
     * @param isActive Active status filter
     * @return List of active inventory items
     */
    @EntityGraph("InventoryItem.withRelations")
    fun findByWarehouseIdAndIsActive(warehouseId: String, isActive: Boolean): List<InventoryItem>

    /**
     * Find inventory items by product ID across all warehouses
     *
     * @param productId Product UID
     * @return List of inventory items for the product
     */
    @EntityGraph("InventoryItem.withRelations")
    fun findByProductId(productId: String): List<InventoryItem>

    /**
     * Find inventory items by product variant ID
     *
     * @param variantId ProductVariant UID
     * @return List of inventory items for the variant
     */
    @EntityGraph("InventoryItem.withRelations")
    fun findByProductVariantId(variantId: String): List<InventoryItem>

    /**
     * Find low stock items (current stock <= reorder level)
     *
     * Only includes active items with reorder level > 0
     *
     * @return List of low stock inventory items
     */
    @EntityGraph("InventoryItem.withRelations")
    @Query("""
        SELECT i FROM inventory_item i
        WHERE i.currentStock <= i.reorderLevel
        AND i.reorderLevel > 0
        AND i.isActive = true
    """)
    fun findLowStockItems(): List<InventoryItem>

    /**
     * Find low stock items in a specific warehouse
     *
     * @param warehouseId Warehouse UID
     * @return List of low stock items
     */
    @EntityGraph("InventoryItem.withRelations")
    @Query("""
        SELECT i FROM inventory_item i
        WHERE i.warehouseId = :warehouseId
        AND i.currentStock <= i.reorderLevel
        AND i.reorderLevel > 0
        AND i.isActive = true
    """)
    fun findLowStockItemsByWarehouse(warehouseId: String): List<InventoryItem>

    /**
     * Find overstock items (current stock > max stock level)
     *
     * @return List of overstock inventory items
     */
    @EntityGraph("InventoryItem.withRelations")
    @Query("""
        SELECT i FROM inventory_item i
        WHERE i.currentStock > i.maxStockLevel
        AND i.maxStockLevel > 0
        AND i.isActive = true
    """)
    fun findOverstockItems(): List<InventoryItem>

    /**
     * Search inventory items by name or SKU
     *
     * Case-insensitive search with ILIKE
     *
     * @param searchTerm Search term
     * @param pageable Pagination parameters
     * @return Page of matching inventory items
     */
    @EntityGraph("InventoryItem.withRelations")
    @Query("""
        SELECT i FROM inventory_item i
        WHERE (i.name ILIKE %:searchTerm% OR i.sku ILIKE %:searchTerm%)
        AND i.isActive = true
    """)
    fun searchItems(searchTerm: String, pageable: Pageable): Page<InventoryItem>

    /**
     * Find all active inventory items with pagination
     *
     * @param isActive Active status filter
     * @param pageable Pagination parameters
     * @return Page of inventory items
     */
    @EntityGraph("InventoryItem.withRelations")
    fun findByIsActive(isActive: Boolean, pageable: Pageable): Page<InventoryItem>

    /**
     * Find all inventory items with pagination
     *
     * @param pageable Pagination parameters
     * @return Page of inventory items
     */
    @EntityGraph("InventoryItem.withRelations")
    override fun findAll(pageable: Pageable): Page<InventoryItem>

    /**
     * Check if SKU already exists for current tenant
     *
     * @param sku Stock Keeping Unit to check
     * @return true if SKU exists, false otherwise
     */
    fun existsBySku(sku: String): Boolean

    /**
     * Count inventory items by warehouse
     *
     * @param warehouseId Warehouse UID
     * @return Count of inventory items in warehouse
     */
    fun countByWarehouseId(warehouseId: String): Long

    /**
     * Count active inventory items
     *
     * @param isActive Active status filter
     * @return Count of active inventory items
     */
    fun countByIsActive(isActive: Boolean): Long
}
