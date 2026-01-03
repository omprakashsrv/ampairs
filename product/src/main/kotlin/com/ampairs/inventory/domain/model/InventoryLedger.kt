package com.ampairs.inventory.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.inventory.config.Constants
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Inventory Ledger Entity
 *
 * Daily stock ledger for each inventory item at each warehouse.
 * Provides historical record of stock movements and balances.
 *
 * Key Features:
 * - Daily snapshot of stock levels
 * - Aggregated movements (stock in/out, transfers, adjustments)
 * - Opening and closing balances
 * - Stock valuation (average cost)
 * - One ledger entry per item per warehouse per day
 *
 * Ledger Entry Flow:
 * Opening Stock + Stock In + Transfer In + Adjustment In
 * - Stock Out - Transfer Out - Adjustment Out
 * = Closing Stock
 *
 * @property ledgerDate Date of ledger entry (truncated to day)
 * @property inventoryItemId Inventory item UID
 * @property warehouseId Warehouse UID
 * @property openingStock Opening stock balance
 * @property stockIn Stock received (purchases, returns)
 * @property transferIn Stock transferred in from other warehouses
 * @property adjustmentIn Positive adjustments
 * @property stockOut Stock issued (sales, damages, losses)
 * @property transferOut Stock transferred out to other warehouses
 * @property adjustmentOut Negative adjustments
 * @property closingStock Closing stock balance
 * @property averageCost Average cost per unit
 * @property closingValue Total value (closingStock * averageCost)
 */
@Entity(name = "inventory_ledger")
@Table(
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_ledger_item_warehouse_date",
            columnNames = ["inventory_item_id", "warehouse_id", "ledger_date", "owner_id"]
        )
    ]
)
class InventoryLedger : OwnableBaseDomain() {

    // ============================================================================
    // Date
    // ============================================================================

    /**
     * Ledger date (UTC, truncated to day)
     *
     * Stored as Instant but represents the start of the day in UTC
     */
    @Column(name = "ledger_date", nullable = false)
    var ledgerDate: Instant = Instant.now()

    // ============================================================================
    // References
    // ============================================================================

    /**
     * Inventory item UID
     */
    @Column(name = "inventory_item_id", nullable = false, length = 200)
    var inventoryItemId: String = ""

    /**
     * Warehouse UID
     */
    @Column(name = "warehouse_id", nullable = false, length = 200)
    var warehouseId: String = ""

    // ============================================================================
    // Opening Balance
    // ============================================================================

    /**
     * Opening stock balance (previous day's closing balance)
     */
    @Column(name = "opening_stock", precision = 15, scale = 3, nullable = false)
    var openingStock: BigDecimal = BigDecimal.ZERO

    // ============================================================================
    // Additions (Inflows)
    // ============================================================================

    /**
     * Stock received (purchases, returns, opening stock)
     */
    @Column(name = "stock_in", precision = 15, scale = 3, nullable = false)
    var stockIn: BigDecimal = BigDecimal.ZERO

    /**
     * Stock transferred in from other warehouses
     */
    @Column(name = "transfer_in", precision = 15, scale = 3, nullable = false)
    var transferIn: BigDecimal = BigDecimal.ZERO

    /**
     * Positive stock adjustments
     */
    @Column(name = "adjustment_in", precision = 15, scale = 3, nullable = false)
    var adjustmentIn: BigDecimal = BigDecimal.ZERO

    // ============================================================================
    // Deductions (Outflows)
    // ============================================================================

    /**
     * Stock issued (sales, damages, losses)
     */
    @Column(name = "stock_out", precision = 15, scale = 3, nullable = false)
    var stockOut: BigDecimal = BigDecimal.ZERO

    /**
     * Stock transferred out to other warehouses
     */
    @Column(name = "transfer_out", precision = 15, scale = 3, nullable = false)
    var transferOut: BigDecimal = BigDecimal.ZERO

    /**
     * Negative stock adjustments
     */
    @Column(name = "adjustment_out", precision = 15, scale = 3, nullable = false)
    var adjustmentOut: BigDecimal = BigDecimal.ZERO

    // ============================================================================
    // Closing Balance
    // ============================================================================

    /**
     * Closing stock balance
     *
     * Calculated as:
     * openingStock + stockIn + transferIn + adjustmentIn
     * - stockOut - transferOut - adjustmentOut
     */
    @Column(name = "closing_stock", precision = 15, scale = 3, nullable = false)
    var closingStock: BigDecimal = BigDecimal.ZERO

    // ============================================================================
    // Valuation
    // ============================================================================

    /**
     * Average cost per unit on this date
     */
    @Column(name = "average_cost", precision = 15, scale = 2, nullable = false)
    var averageCost: BigDecimal = BigDecimal.ZERO

    /**
     * Total stock value (closingStock * averageCost)
     */
    @Column(name = "closing_value", precision = 15, scale = 2, nullable = false)
    var closingValue: BigDecimal = BigDecimal.ZERO

    // ============================================================================
    // Base Domain Methods
    // ============================================================================

    override fun obtainSeqIdPrefix(): String = Constants.LEDGER_PREFIX

    // ============================================================================
    // Calculation Methods
    // ============================================================================

    /**
     * Calculate closing stock from opening stock and movements
     */
    fun calculateClosingStock() {
        closingStock = openingStock
            .add(stockIn)
            .add(transferIn)
            .add(adjustmentIn)
            .subtract(stockOut)
            .subtract(transferOut)
            .subtract(adjustmentOut)
    }

    /**
     * Calculate closing value from closing stock and average cost
     */
    fun calculateClosingValue() {
        closingValue = closingStock.multiply(averageCost)
    }

    /**
     * Calculate total inflows
     */
    fun getTotalInflows(): BigDecimal {
        return stockIn.add(transferIn).add(adjustmentIn)
    }

    /**
     * Calculate total outflows
     */
    fun getTotalOutflows(): BigDecimal {
        return stockOut.add(transferOut).add(adjustmentOut)
    }

    /**
     * Calculate net movement (inflows - outflows)
     */
    fun getNetMovement(): BigDecimal {
        return getTotalInflows().subtract(getTotalOutflows())
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    /**
     * Get ledger date as LocalDate
     */
    fun getLedgerLocalDate(): LocalDate {
        return ledgerDate.atZone(ZoneId.of("UTC")).toLocalDate()
    }

    companion object {
        /**
         * Create ledger date Instant from LocalDate
         *
         * Converts LocalDate to start of day in UTC
         */
        fun ledgerDateFromLocalDate(localDate: LocalDate): Instant {
            return localDate.atStartOfDay(ZoneId.of("UTC")).toInstant()
        }

        /**
         * Get today's ledger date
         */
        fun todayLedgerDate(): Instant {
            val today = LocalDate.now(ZoneId.of("UTC"))
            return ledgerDateFromLocalDate(today)
        }
    }
}
