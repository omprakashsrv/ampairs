package com.ampairs.inventory.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.inventory.config.Constants
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * Inventory Configuration Entity
 *
 * Tenant-level configuration for inventory management behavior.
 * Only one configuration per tenant (enforced by unique constraint on owner_id).
 *
 * Key Features:
 * - Auto inventory deduction settings for order integration
 * - Stock consumption strategy (FIFO, FEFO, LIFO, MANUAL)
 * - Default tracking enablement (batch, serial, expiry)
 * - Alert configuration (low stock, expiry warnings)
 * - Negative stock policy
 * - Default warehouse assignment
 */
@Entity(name = "inventory_config")
@Table(
    name = "inventory_config",
    indexes = [
        Index(name = "idx_inventory_config_uid", columnList = "uid"),
        Index(name = "idx_inventory_config_owner_id", columnList = "owner_id")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_inventory_config_owner", columnNames = ["owner_id"])
    ]
)
class InventoryConfig : OwnableBaseDomain() {

    // ========================================
    // Auto Inventory Deduction (Order Integration)
    // ========================================

    /**
     * Automatically deduct inventory when orders are created/confirmed
     */
    @Column(name = "auto_deduct_on_order", nullable = false)
    var autoDeductOnOrder: Boolean = false

    /**
     * Allow manual override of auto-deduction
     * If true, users can manually adjust inventory even with auto-deduct enabled
     */
    @Column(name = "allow_manual_override", nullable = false)
    var allowManualOverride: Boolean = true

    /**
     * Block order creation when stock is insufficient
     */
    @Column(name = "block_orders_when_out_of_stock", nullable = false)
    var blockOrdersWhenOutOfStock: Boolean = false

    // ========================================
    // Stock Consumption Strategy
    // ========================================

    /**
     * Stock consumption strategy for batch allocation
     * Values: FIFO (First In First Out), FEFO (First Expiry First Out),
     *         LIFO (Last In First Out), MANUAL (Manual selection)
     */
    @Column(name = "stock_consumption_strategy", nullable = false, length = 20)
    var stockConsumptionStrategy: String = Constants.STRATEGY_FIFO

    // ========================================
    // Tracking Defaults (Applied to new inventory items)
    // ========================================

    /**
     * Enable batch/lot tracking by default for new items
     */
    @Column(name = "enable_batch_tracking_by_default", nullable = false)
    var enableBatchTrackingByDefault: Boolean = false

    /**
     * Enable serial number tracking by default for new items
     */
    @Column(name = "enable_serial_tracking_by_default", nullable = false)
    var enableSerialTrackingByDefault: Boolean = false

    /**
     * Enable expiry date tracking by default for new items
     */
    @Column(name = "enable_expiry_tracking_by_default", nullable = false)
    var enableExpiryTrackingByDefault: Boolean = false

    // ========================================
    // Alerts & Notifications
    // ========================================

    /**
     * Enable low stock alert notifications
     */
    @Column(name = "enable_low_stock_alerts", nullable = false)
    var enableLowStockAlerts: Boolean = true

    /**
     * Enable expiry date alert notifications
     */
    @Column(name = "enable_expiry_alerts", nullable = false)
    var enableExpiryAlerts: Boolean = true

    /**
     * Number of days before expiry to trigger alerts
     * E.g., 30 = alert when items expire within 30 days
     */
    @Column(name = "expiry_alert_days", nullable = false)
    var expiryAlertDays: Int = 30

    /**
     * Enable overstock alert notifications
     */
    @Column(name = "enable_overstock_alerts", nullable = false)
    var enableOverstockAlerts: Boolean = false

    // ========================================
    // Stock Policies
    // ========================================

    /**
     * Allow negative stock levels
     * If false, transactions that would result in negative stock will be blocked
     */
    @Column(name = "allow_negative_stock", nullable = false)
    var allowNegativeStock: Boolean = false

    /**
     * Require approval for stock adjustments
     */
    @Column(name = "require_approval_for_adjustments", nullable = false)
    var requireApprovalForAdjustments: Boolean = false

    // ========================================
    // Multi-location Defaults
    // ========================================

    /**
     * Default warehouse UID for new inventory items
     * If null, users must explicitly select warehouse
     */
    @Column(name = "default_warehouse_id", length = 200)
    var defaultWarehouseId: String? = null

    // ========================================
    // Ledger & Reporting
    // ========================================

    /**
     * Auto-generate daily ledger
     * If enabled, scheduled job will create daily ledger entries
     */
    @Column(name = "auto_generate_daily_ledger", nullable = false)
    var autoGenerateDailyLedger: Boolean = true

    /**
     * Ledger generation time (hour of day, 0-23)
     */
    @Column(name = "ledger_generation_hour", nullable = false)
    var ledgerGenerationHour: Int = 1  // 1 AM by default

    // ========================================
    // Extensible Attributes
    // ========================================

    /**
     * Additional flexible configuration stored as JSON
     * Can include custom settings, thresholds, integrations, etc.
     */
    @Column(name = "attributes", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    var attributes: Map<String, Any>? = null

    /**
     * Obtain the sequence ID prefix for UID generation
     */
    override fun obtainSeqIdPrefix(): String {
        return Constants.CONFIG_PREFIX
    }

    override fun toString(): String {
        return "InventoryConfig(uid='$uid', autoDeductOnOrder=$autoDeductOnOrder, " +
               "stockConsumptionStrategy='$stockConsumptionStrategy', " +
               "enableLowStockAlerts=$enableLowStockAlerts)"
    }
}
