package com.ampairs.inventory.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.inventory.config.Constants
import com.ampairs.unit.domain.model.Unit
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal

/**
 * Inventory Item Entity
 *
 * Represents a stock item in inventory with location-specific tracking.
 * Supports both standalone items and product-linked items for flexible inventory management.
 *
 * Key Features:
 * - Multi-tenant aware via @TenantId
 * - Multi-location stock tracking via warehouse association
 * - Product module integration (optional via productId/productVariantId)
 * - Precise stock quantities using BigDecimal
 * - Reserved stock tracking for order management
 * - Batch/Serial/Expiry tracking enablement flags
 * - Reorder level and max stock level alerts
 * - Extensible attributes via JSON storage
 */
@Entity(name = "inventory_item")
@Table(
    name = "inventory_item",
    indexes = [
        Index(name = "idx_inventory_item_uid", columnList = "uid"),
        Index(name = "idx_inventory_item_sku", columnList = "sku"),
        Index(name = "idx_inventory_item_product_id", columnList = "product_id"),
        Index(name = "idx_inventory_item_warehouse_id", columnList = "warehouse_id"),
        Index(name = "idx_inventory_item_owner_id", columnList = "owner_id"),
        Index(name = "idx_inventory_item_is_active", columnList = "is_active")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_inventory_item_sku", columnNames = ["sku", "owner_id"]),
        UniqueConstraint(name = "uk_inventory_item_product_warehouse",
                        columnNames = ["product_id", "product_variant_id", "warehouse_id", "owner_id"])
    ]
)
@NamedEntityGraph(
    name = "InventoryItem.withRelations",
    attributeNodes = [
        NamedAttributeNode("warehouse"),
        NamedAttributeNode("unit")
    ]
)
class InventoryItem : OwnableBaseDomain() {

    // ========================================
    // Identification
    // ========================================

    /**
     * Stock Keeping Unit (SKU) - unique identifier for inventory item
     * Unique per tenant
     */
    @Column(name = "sku", nullable = false, length = 100)
    var sku: String = ""

    /**
     * Item name (required for standalone items, optional for product-linked items)
     */
    @Column(name = "name", nullable = false, length = 200)
    var name: String = ""

    /**
     * Item description
     */
    @Column(name = "description", length = 500)
    var description: String? = null

    // ========================================
    // Product Module Integration (Optional)
    // ========================================

    /**
     * Link to Product entity (null if standalone inventory item)
     */
    @Column(name = "product_id", length = 200)
    var productId: String? = null

    /**
     * Link to ProductVariant entity (null if not variant-based)
     */
    @Column(name = "product_variant_id", length = 200)
    var productVariantId: String? = null

    // ========================================
    // Location
    // ========================================

    /**
     * Warehouse/location where this inventory is stored
     */
    @Column(name = "warehouse_id", nullable = false, length = 200)
    var warehouseId: String = ""

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", referencedColumnName = "uid",
                updatable = false, insertable = false)
    var warehouse: Warehouse? = null

    // ========================================
    // Stock Levels
    // ========================================

    /**
     * Current physical stock quantity
     */
    @Column(name = "current_stock", precision = 15, scale = 3, nullable = false)
    var currentStock: BigDecimal = BigDecimal.ZERO

    /**
     * Stock reserved for orders (not yet fulfilled)
     */
    @Column(name = "reserved_stock", precision = 15, scale = 3, nullable = false)
    var reservedStock: BigDecimal = BigDecimal.ZERO

    /**
     * Available stock for new orders (current - reserved)
     * Computed field, updated on transaction
     */
    @Column(name = "available_stock", precision = 15, scale = 3, nullable = false)
    var availableStock: BigDecimal = BigDecimal.ZERO

    /**
     * Reorder level - alert when stock falls below this threshold
     */
    @Column(name = "reorder_level", precision = 15, scale = 3, nullable = false)
    var reorderLevel: BigDecimal = BigDecimal.ZERO

    /**
     * Maximum stock level - alert when stock exceeds this threshold
     */
    @Column(name = "max_stock_level", precision = 15, scale = 3, nullable = false)
    var maxStockLevel: BigDecimal = BigDecimal.ZERO

    // ========================================
    // Unit of Measurement
    // ========================================

    /**
     * Base unit of measurement (e.g., "PCS", "KG", "LITER")
     */
    @Column(name = "unit_id", length = 200)
    var unitId: String? = null

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", referencedColumnName = "uid",
                updatable = false, insertable = false)
    var unit: Unit? = null

    // ========================================
    // Pricing
    // ========================================

    /**
     * Cost price (purchase price)
     */
    @Column(name = "cost_price", precision = 15, scale = 2, nullable = false)
    var costPrice: BigDecimal = BigDecimal.ZERO

    /**
     * Selling price (default selling price)
     */
    @Column(name = "selling_price", precision = 15, scale = 2, nullable = false)
    var sellingPrice: BigDecimal = BigDecimal.ZERO

    /**
     * Maximum Retail Price (MRP)
     */
    @Column(name = "mrp", precision = 15, scale = 2, nullable = false)
    var mrp: BigDecimal = BigDecimal.ZERO

    // ========================================
    // Tracking Flags
    // ========================================

    /**
     * Enable batch/lot tracking for this item
     * If true, all transactions must specify batch information
     */
    @Column(name = "batch_tracking_enabled", nullable = false)
    var batchTrackingEnabled: Boolean = false

    /**
     * Enable serial number tracking for this item
     * If true, all transactions must specify serial numbers
     */
    @Column(name = "serial_tracking_enabled", nullable = false)
    var serialTrackingEnabled: Boolean = false

    /**
     * Enable expiry date tracking for this item
     * If true, batch records must include expiry dates
     */
    @Column(name = "expiry_tracking_enabled", nullable = false)
    var expiryTrackingEnabled: Boolean = false

    // ========================================
    // Status
    // ========================================

    /**
     * Active status - inactive items cannot have new transactions
     */
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    // ========================================
    // Extensible Attributes
    // ========================================

    /**
     * Additional flexible attributes stored as JSON
     * Can include custom fields specific to business needs
     */
    @Column(name = "attributes", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    var attributes: Map<String, Any>? = null

    /**
     * Obtain the sequence ID prefix for UID generation
     */
    override fun obtainSeqIdPrefix(): String {
        return Constants.INVENTORY_ITEM_PREFIX
    }

    /**
     * Calculate available stock
     * Should be called after updating currentStock or reservedStock
     */
    fun recalculateAvailableStock() {
        availableStock = currentStock.subtract(reservedStock)
        if (availableStock < BigDecimal.ZERO) {
            availableStock = BigDecimal.ZERO
        }
    }

    /**
     * Check if stock is low (below reorder level)
     */
    fun isLowStock(): Boolean {
        return currentStock <= reorderLevel && reorderLevel > BigDecimal.ZERO
    }

    /**
     * Check if stock exceeds maximum level
     */
    fun isOverStock(): Boolean {
        return maxStockLevel > BigDecimal.ZERO && currentStock > maxStockLevel
    }

    override fun toString(): String {
        return "InventoryItem(uid='$uid', sku='$sku', name='$name', warehouse='$warehouseId', " +
               "currentStock=$currentStock, availableStock=$availableStock)"
    }
}
