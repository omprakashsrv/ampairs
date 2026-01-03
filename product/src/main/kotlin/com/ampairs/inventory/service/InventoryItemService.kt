package com.ampairs.inventory.service

import com.ampairs.inventory.config.Constants
import com.ampairs.inventory.domain.model.InventoryItem
import com.ampairs.inventory.repository.InventoryItemRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * Inventory Item Service
 *
 * Business logic layer for inventory item management.
 * Handles CRUD operations, stock calculations, and alert detection.
 *
 * Key Responsibilities:
 * - Create, read, update, delete inventory items
 * - Validate SKU uniqueness
 * - Calculate available stock (current - reserved)
 * - Detect low stock and overstock conditions
 * - Link inventory with products (optional)
 */
@Service
class InventoryItemService @Autowired constructor(
    private val inventoryItemRepository: InventoryItemRepository,
    private val warehouseService: WarehouseService
) {

    /**
     * Create a new inventory item
     *
     * @param item InventoryItem entity to create
     * @return Created inventory item
     * @throws IllegalArgumentException if SKU exists or warehouse not found
     */
    @Transactional
    fun createInventoryItem(item: InventoryItem): InventoryItem {
        // Validate SKU uniqueness
        if (inventoryItemRepository.existsBySku(item.sku)) {
            throw IllegalArgumentException("SKU already exists: ${item.sku}")
        }

        // Validate warehouse exists
        val warehouse = warehouseService.getWarehouseByUid(item.warehouseId)
            ?: throw IllegalArgumentException(Constants.ERROR_WAREHOUSE_NOT_FOUND + ": ${item.warehouseId}")

        if (!warehouse.isActive) {
            throw IllegalArgumentException("Cannot add inventory to inactive warehouse: ${item.warehouseId}")
        }

        // Recalculate available stock
        item.recalculateAvailableStock()

        return inventoryItemRepository.save(item)
    }

    /**
     * Update an existing inventory item
     *
     * @param uid Inventory item UID
     * @param updates InventoryItem with updated fields
     * @return Updated inventory item
     * @throws IllegalArgumentException if item not found or SKU conflict
     */
    @Transactional
    fun updateInventoryItem(uid: String, updates: InventoryItem): InventoryItem {
        val existing = getInventoryItemByUid(uid)
            ?: throw IllegalArgumentException(Constants.ERROR_INVENTORY_ITEM_NOT_FOUND + ": $uid")

        // Check SKU uniqueness if changed
        if (updates.sku != existing.sku && inventoryItemRepository.existsBySku(updates.sku)) {
            throw IllegalArgumentException("SKU already exists: ${updates.sku}")
        }

        // Update fields
        existing.sku = updates.sku
        existing.name = updates.name
        existing.description = updates.description
        existing.productId = updates.productId
        existing.productVariantId = updates.productVariantId
        existing.warehouseId = updates.warehouseId
        existing.reorderLevel = updates.reorderLevel
        existing.maxStockLevel = updates.maxStockLevel
        existing.unitId = updates.unitId
        existing.costPrice = updates.costPrice
        existing.sellingPrice = updates.sellingPrice
        existing.mrp = updates.mrp
        existing.batchTrackingEnabled = updates.batchTrackingEnabled
        existing.serialTrackingEnabled = updates.serialTrackingEnabled
        existing.expiryTrackingEnabled = updates.expiryTrackingEnabled
        existing.isActive = updates.isActive
        existing.attributes = updates.attributes

        // Note: currentStock and reservedStock are updated via transactions, not direct updates

        return inventoryItemRepository.save(existing)
    }

    /**
     * Get inventory item by UID
     *
     * @param uid Inventory item UID
     * @return InventoryItem
     * @throws IllegalArgumentException if item not found
     */
    fun getInventoryItemByUid(uid: String): InventoryItem {
        return inventoryItemRepository.findByUid(uid)
            ?: throw IllegalArgumentException("Inventory item not found: $uid")
    }

    /**
     * Find inventory item by inventory item ID and warehouse ID
     * Used for stock transfers between warehouses
     *
     * @param inventoryItemId Inventory item UID (this is the item's UID, not productId)
     * @param warehouseId Warehouse UID
     * @return InventoryItem if found, null otherwise
     */
    fun findByInventoryItemIdAndWarehouseId(inventoryItemId: String, warehouseId: String): InventoryItem? {
        // Since inventoryItemId is the UID and we want to find same item in different warehouse
        // We need to find by the item's product/variant and warehouse
        val item = inventoryItemRepository.findByUid(inventoryItemId)
        if (item == null) return null

        // If item has productId, find by productId and warehouse
        if (item.productId != null) {
            return if (item.productVariantId != null) {
                inventoryItemRepository.findByProductVariantIdAndWarehouseId(
                    item.productVariantId!!,
                    warehouseId
                )
            } else {
                inventoryItemRepository.findByProductIdAndWarehouseId(item.productId!!, warehouseId)
            }
        }

        // For standalone items (no product link), if same item uid and warehouse, return it
        if (item.warehouseId == warehouseId) {
            return item
        }

        return null
    }

    /**
     * Get inventory item by SKU
     *
     * @param sku Stock Keeping Unit
     * @return InventoryItem if found, null otherwise
     */
    fun getInventoryItemBySku(sku: String): InventoryItem? {
        return inventoryItemRepository.findBySku(sku)
    }

    /**
     * Get inventory item for product at specific warehouse
     *
     * @param productId Product UID
     * @param warehouseId Warehouse UID
     * @return InventoryItem if found, null otherwise
     */
    fun getInventoryByProductAndWarehouse(productId: String, warehouseId: String): InventoryItem? {
        return inventoryItemRepository.findByProductIdAndWarehouseId(productId, warehouseId)
    }

    /**
     * Get inventory item for product variant at specific warehouse
     *
     * @param variantId ProductVariant UID
     * @param warehouseId Warehouse UID
     * @return InventoryItem if found, null otherwise
     */
    fun getInventoryByVariantAndWarehouse(variantId: String, warehouseId: String): InventoryItem? {
        return inventoryItemRepository.findByProductVariantIdAndWarehouseId(variantId, warehouseId)
    }

    /**
     * Get all inventory items in a warehouse
     *
     * @param warehouseId Warehouse UID
     * @param activeOnly Filter by active status
     * @return List of inventory items
     */
    fun getInventoryByWarehouse(warehouseId: String, activeOnly: Boolean = false): List<InventoryItem> {
        return if (activeOnly) {
            inventoryItemRepository.findByWarehouseIdAndIsActive(warehouseId, true)
        } else {
            inventoryItemRepository.findByWarehouseId(warehouseId)
        }
    }

    /**
     * Get all inventory items for a product across all warehouses
     *
     * @param productId Product UID
     * @return List of inventory items
     */
    fun getInventoryByProduct(productId: String): List<InventoryItem> {
        return inventoryItemRepository.findByProductId(productId)
    }

    /**
     * Get all active inventory items with pagination
     *
     * @param pageable Pagination parameters
     * @return Page of inventory items
     */
    fun getAllInventoryItems(pageable: Pageable, activeOnly: Boolean = false): Page<InventoryItem> {
        return if (activeOnly) {
            inventoryItemRepository.findByIsActive(true, pageable)
        } else {
            inventoryItemRepository.findAll(pageable)
        }
    }

    /**
     * Search inventory items by name or SKU
     *
     * @param searchTerm Search term
     * @param pageable Pagination parameters
     * @return Page of matching inventory items
     */
    fun searchInventoryItems(searchTerm: String, pageable: Pageable): Page<InventoryItem> {
        return inventoryItemRepository.searchItems(searchTerm, pageable)
    }

    /**
     * Get low stock items
     *
     * Items where current stock <= reorder level
     *
     * @param warehouseId Optional warehouse filter
     * @return List of low stock items
     */
    fun getLowStockItems(warehouseId: String? = null): List<InventoryItem> {
        return if (warehouseId != null) {
            inventoryItemRepository.findLowStockItemsByWarehouse(warehouseId)
        } else {
            inventoryItemRepository.findLowStockItems()
        }
    }

    /**
     * Get overstock items
     *
     * Items where current stock > max stock level
     *
     * @return List of overstock items
     */
    fun getOverstockItems(): List<InventoryItem> {
        return inventoryItemRepository.findOverstockItems()
    }

    /**
     * Update stock quantities for an inventory item
     *
     * This method should typically be called by InventoryTransactionService
     * after creating transactions, not directly
     *
     * @param uid Inventory item UID
     * @param currentStock New current stock
     * @param reservedStock New reserved stock
     * @return Updated inventory item
     */
    @Transactional
    fun updateStockQuantities(uid: String, currentStock: BigDecimal, reservedStock: BigDecimal): InventoryItem {
        val item = getInventoryItemByUid(uid)
            ?: throw IllegalArgumentException(Constants.ERROR_INVENTORY_ITEM_NOT_FOUND + ": $uid")

        item.currentStock = currentStock
        item.reservedStock = reservedStock
        item.recalculateAvailableStock()

        return inventoryItemRepository.save(item)
    }

    /**
     * Reserve stock for an order
     *
     * Increases reserved stock, decreases available stock
     *
     * @param uid Inventory item UID
     * @param quantity Quantity to reserve
     * @return Updated inventory item
     * @throws IllegalArgumentException if insufficient stock
     */
    @Transactional
    fun reserveStock(uid: String, quantity: BigDecimal): InventoryItem {
        val item = getInventoryItemByUid(uid)
            ?: throw IllegalArgumentException(Constants.ERROR_INVENTORY_ITEM_NOT_FOUND + ": $uid")

        if (item.availableStock < quantity) {
            throw IllegalArgumentException(Constants.ERROR_INSUFFICIENT_STOCK +
                ": Available=${item.availableStock}, Requested=$quantity")
        }

        item.reservedStock = item.reservedStock.add(quantity)
        item.recalculateAvailableStock()

        return inventoryItemRepository.save(item)
    }

    /**
     * Release reserved stock
     *
     * Decreases reserved stock, increases available stock
     *
     * @param uid Inventory item UID
     * @param quantity Quantity to release
     * @return Updated inventory item
     */
    @Transactional
    fun releaseReservedStock(uid: String, quantity: BigDecimal): InventoryItem {
        val item = getInventoryItemByUid(uid)
            ?: throw IllegalArgumentException(Constants.ERROR_INVENTORY_ITEM_NOT_FOUND + ": $uid")

        item.reservedStock = item.reservedStock.subtract(quantity)
        if (item.reservedStock < BigDecimal.ZERO) {
            item.reservedStock = BigDecimal.ZERO
        }
        item.recalculateAvailableStock()

        return inventoryItemRepository.save(item)
    }

    /**
     * Delete inventory item (soft delete)
     *
     * Sets isActive = false
     *
     * @param uid Inventory item UID
     */
    @Transactional
    fun deleteInventoryItem(uid: String) {
        val item = getInventoryItemByUid(uid)
            ?: throw IllegalArgumentException(Constants.ERROR_INVENTORY_ITEM_NOT_FOUND + ": $uid")

        item.isActive = false
        inventoryItemRepository.save(item)
    }

    /**
     * Count inventory items by warehouse
     *
     * @param warehouseId Warehouse UID
     * @return Count of inventory items
     */
    fun countByWarehouse(warehouseId: String): Long {
        return inventoryItemRepository.countByWarehouseId(warehouseId)
    }

    /**
     * Count active inventory items
     *
     * @return Count of active items
     */
    fun countActiveItems(): Long {
        return inventoryItemRepository.countByIsActive(true)
    }

    // ============================================================================
    // Low Stock Alert Methods
    // ============================================================================

    /**
     * Get items with low stock (at or below reorder level)
     *
     * @return List of items needing reorder
     */
    fun getLowStockItems(): List<InventoryItem> {
        return inventoryItemRepository.findLowStockItems()
    }

    /**
     * Get items with low stock for a specific warehouse
     *
     * @param warehouseId Warehouse UID
     * @return List of items needing reorder
     */
    fun getLowStockItemsByWarehouse(warehouseId: String): List<InventoryItem> {
        return inventoryItemRepository.findByWarehouseId(warehouseId).filter {
            it.isActive && it.currentStock <= it.reorderLevel
        }
    }

    /**
     * Check if an item is at or below reorder level
     *
     * @param uid Inventory item UID
     * @return true if low stock, false otherwise
     */
    fun isLowStock(uid: String): Boolean {
        val item = getInventoryItemByUid(uid) ?: return false
        return item.currentStock <= item.reorderLevel
    }

    /**
     * Get count of items with low stock
     *
     * @return Count of low stock items
     */
    fun countLowStockItems(): Long {
        return getLowStockItems().size.toLong()
    }

    /**
     * Get items with zero or negative stock
     *
     * @return List of items with no stock
     */
    fun getOutOfStockItems(): List<InventoryItem> {
        return inventoryItemRepository.findAll().filter {
            it.isActive && it.currentStock <= BigDecimal.ZERO
        }.toList()
    }
}
