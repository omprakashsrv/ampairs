package com.ampairs.inventory.service

import com.ampairs.inventory.config.Constants
import com.ampairs.inventory.domain.model.InventoryLedger
import com.ampairs.inventory.repository.InventoryItemRepository
import com.ampairs.inventory.repository.InventoryLedgerRepository
import com.ampairs.inventory.repository.InventoryTransactionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Inventory Ledger Service
 *
 * Business logic layer for daily stock ledger management.
 * Handles aggregation of transactions into daily ledger entries.
 *
 * Key Responsibilities:
 * - Generate daily ledger entries from transactions
 * - Aggregate stock movements by day
 * - Calculate opening/closing balances
 * - Compute average costs and stock valuation
 * - Provide historical stock analysis
 *
 * Ledger Generation Process:
 * 1. Get previous day's closing balance (becomes opening balance)
 * 2. Aggregate all transactions for the day by type
 * 3. Calculate closing balance
 * 4. Compute average cost (weighted)
 * 5. Calculate closing value
 *
 * Integration Points:
 * - InventoryTransactionRepository for transaction data
 * - InventoryItemRepository for current stock levels
 * - Scheduled jobs for automatic daily generation
 */
@Service
class InventoryLedgerService @Autowired constructor(
    private val inventoryLedgerRepository: InventoryLedgerRepository,
    private val inventoryTransactionRepository: InventoryTransactionRepository,
    private val inventoryItemRepository: InventoryItemRepository
) {

    // ============================================================================
    // Query Methods
    // ============================================================================

    /**
     * Get ledger by UID
     *
     * @param uid Ledger UID
     * @return InventoryLedger
     * @throws IllegalArgumentException if ledger not found
     */
    fun getLedgerByUid(uid: String): InventoryLedger {
        return inventoryLedgerRepository.findByUid(uid)
            ?: throw IllegalArgumentException("Ledger not found: $uid")
    }

    /**
     * Get ledger for specific item, warehouse, and date
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @param ledgerDate Ledger date
     * @return InventoryLedger if found, null otherwise
     */
    fun getLedger(
        inventoryItemId: String,
        warehouseId: String,
        ledgerDate: Instant
    ): InventoryLedger? {
        return inventoryLedgerRepository.findByInventoryItemIdAndWarehouseIdAndLedgerDate(
            inventoryItemId,
            warehouseId,
            ledgerDate
        )
    }

    /**
     * Get ledger entries for an item within date range
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @param startDate Start date
     * @param endDate End date
     * @return List of ledger entries
     */
    fun getLedgersByDateRange(
        inventoryItemId: String,
        warehouseId: String,
        startDate: Instant,
        endDate: Instant
    ): List<InventoryLedger> {
        return inventoryLedgerRepository.findByItemAndWarehouseAndDateRange(
            inventoryItemId,
            warehouseId,
            startDate,
            endDate
        )
    }

    /**
     * Get all ledger entries for a warehouse on a specific date
     *
     * @param warehouseId Warehouse UID
     * @param ledgerDate Ledger date
     * @return List of ledger entries
     */
    fun getWarehouseLedger(warehouseId: String, ledgerDate: Instant): List<InventoryLedger> {
        return inventoryLedgerRepository.findByWarehouseAndDate(warehouseId, ledgerDate)
    }

    /**
     * Get all ledger entries for a specific date
     *
     * @param ledgerDate Ledger date
     * @return List of all ledger entries
     */
    fun getDailyLedger(ledgerDate: Instant): List<InventoryLedger> {
        return inventoryLedgerRepository.findByDate(ledgerDate)
    }

    // ============================================================================
    // Ledger Generation Methods
    // ============================================================================

    /**
     * Generate daily ledger for all items that had transactions on a date
     *
     * This is the main ledger generation method, typically called by scheduled job
     *
     * @param date Date to generate ledger for (LocalDate)
     * @return Number of ledger entries created/updated
     */
    @Transactional
    fun generateDailyLedgerForDate(date: LocalDate): Int {
        val ledgerDate = InventoryLedger.ledgerDateFromLocalDate(date)
        val startOfDay = date.atStartOfDay(ZoneId.of("UTC")).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant()

        // Get all transactions for the day
        val transactions = inventoryTransactionRepository.findByTransactionDateBetween(
            startOfDay,
            endOfDay
        )

        if (transactions.isEmpty()) {
            return 0
        }

        // Group transactions by item and warehouse
        val groupedTransactions = transactions.groupBy {
            Pair(it.inventoryItemId, it.warehouseId)
        }

        var ledgerCount = 0

        // Generate ledger entry for each item/warehouse combination
        for ((key, txns) in groupedTransactions) {
            val (itemId, warehouseId) = key
            generateLedgerEntry(itemId, warehouseId, ledgerDate, txns)
            ledgerCount++
        }

        return ledgerCount
    }

    /**
     * Generate ledger entry for a specific item at a warehouse for a date
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @param ledgerDate Ledger date
     * @param transactions Transactions for this item/warehouse/date
     * @return Created or updated ledger entry
     */
    @Transactional
    fun generateLedgerEntry(
        inventoryItemId: String,
        warehouseId: String,
        ledgerDate: Instant,
        transactions: List<com.ampairs.inventory.domain.model.InventoryTransaction>
    ): InventoryLedger {
        // Check if ledger entry already exists
        val existingLedger = inventoryLedgerRepository.findByInventoryItemIdAndWarehouseIdAndLedgerDate(
            inventoryItemId,
            warehouseId,
            ledgerDate
        )

        val ledger = existingLedger ?: InventoryLedger().apply {
            this.inventoryItemId = inventoryItemId
            this.warehouseId = warehouseId
            this.ledgerDate = ledgerDate
        }

        // Get opening balance from previous day's closing
        if (existingLedger == null) {
            ledger.openingStock = getPreviousClosingBalance(inventoryItemId, warehouseId, ledgerDate)
        }

        // Reset movement fields
        ledger.stockIn = BigDecimal.ZERO
        ledger.transferIn = BigDecimal.ZERO
        ledger.adjustmentIn = BigDecimal.ZERO
        ledger.stockOut = BigDecimal.ZERO
        ledger.transferOut = BigDecimal.ZERO
        ledger.adjustmentOut = BigDecimal.ZERO

        // Aggregate transactions
        for (txn in transactions) {
            when (txn.transactionType) {
                Constants.TXN_TYPE_STOCK_IN -> {
                    ledger.stockIn = ledger.stockIn.add(txn.quantity)
                }
                Constants.TXN_TYPE_STOCK_OUT -> {
                    ledger.stockOut = ledger.stockOut.add(txn.quantity)
                }
                Constants.TXN_TYPE_TRANSFER -> {
                    // Determine if this is transfer in or transfer out
                    if (txn.warehouseId == warehouseId) {
                        // Source warehouse (outgoing)
                        ledger.transferOut = ledger.transferOut.add(txn.quantity)
                    } else if (txn.toWarehouseId == warehouseId) {
                        // Destination warehouse (incoming)
                        ledger.transferIn = ledger.transferIn.add(txn.quantity)
                    }
                }
                Constants.TXN_TYPE_ADJUSTMENT -> {
                    // Positive or negative adjustment
                    if (txn.quantity > BigDecimal.ZERO) {
                        ledger.adjustmentIn = ledger.adjustmentIn.add(txn.quantity)
                    } else {
                        ledger.adjustmentOut = ledger.adjustmentOut.add(txn.quantity.abs())
                    }
                }
                Constants.TXN_TYPE_COUNT -> {
                    // Physical count adjustments
                    if (txn.quantity > BigDecimal.ZERO) {
                        ledger.adjustmentIn = ledger.adjustmentIn.add(txn.quantity)
                    } else {
                        ledger.adjustmentOut = ledger.adjustmentOut.add(txn.quantity.abs())
                    }
                }
            }
        }

        // Calculate closing stock
        ledger.calculateClosingStock()

        // Calculate average cost
        ledger.averageCost = calculateAverageCost(inventoryItemId, warehouseId, transactions)

        // Calculate closing value
        ledger.calculateClosingValue()

        return inventoryLedgerRepository.save(ledger)
    }

    /**
     * Get previous day's closing balance
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @param currentDate Current ledger date
     * @return Previous closing balance, or ZERO if no previous entry
     */
    private fun getPreviousClosingBalance(
        inventoryItemId: String,
        warehouseId: String,
        currentDate: Instant
    ): BigDecimal {
        val previousLedger = inventoryLedgerRepository.findMostRecentBefore(
            inventoryItemId,
            warehouseId,
            currentDate
        )
        return previousLedger?.closingStock ?: BigDecimal.ZERO
    }

    /**
     * Calculate average cost for an item
     *
     * Uses weighted average method based on stock-in transactions
     *
     * @param inventoryItemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @param transactions Transactions for the period
     * @return Average cost per unit
     */
    private fun calculateAverageCost(
        inventoryItemId: String,
        warehouseId: String,
        transactions: List<com.ampairs.inventory.domain.model.InventoryTransaction>
    ): BigDecimal {
        // Get stock-in transactions with cost
        val stockInTransactions = transactions.filter {
            it.transactionType == Constants.TXN_TYPE_STOCK_IN && it.unitCost > BigDecimal.ZERO
        }

        if (stockInTransactions.isEmpty()) {
            // Use item's current cost price
            val item = inventoryItemRepository.findByUid(inventoryItemId)
            return item?.costPrice ?: BigDecimal.ZERO
        }

        // Calculate weighted average
        var totalValue = BigDecimal.ZERO
        var totalQuantity = BigDecimal.ZERO

        for (txn in stockInTransactions) {
            totalValue = totalValue.add(txn.totalCost)
            totalQuantity = totalQuantity.add(txn.quantity)
        }

        return if (totalQuantity > BigDecimal.ZERO) {
            totalValue.divide(totalQuantity, 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
    }

    // ============================================================================
    // Bulk Generation Methods
    // ============================================================================

    /**
     * Generate daily ledger for all items (for scheduled job)
     *
     * Called daily by scheduler to generate previous day's ledger
     *
     * @return Number of ledger entries created/updated
     */
    @Transactional
    fun generateDailyLedgerForAllItems(): Int {
        // Generate ledger for yesterday (as today's transactions may still be coming in)
        val yesterday = LocalDate.now(ZoneId.of("UTC")).minusDays(1)
        return generateDailyLedgerForDate(yesterday)
    }

    /**
     * Generate ledger for a date range (backfill)
     *
     * Used to generate historical ledger entries
     *
     * @param startDate Start date
     * @param endDate End date
     * @return Total number of ledger entries created/updated
     */
    @Transactional
    fun generateLedgerForDateRange(startDate: LocalDate, endDate: LocalDate): Int {
        var totalCount = 0
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            totalCount += generateDailyLedgerForDate(currentDate)
            currentDate = currentDate.plusDays(1)
        }

        return totalCount
    }

    // ============================================================================
    // Statistics Methods
    // ============================================================================

    /**
     * Calculate total warehouse stock value on a date
     *
     * @param warehouseId Warehouse UID
     * @param ledgerDate Ledger date
     * @return Total stock value
     */
    fun getWarehouseStockValue(warehouseId: String, ledgerDate: Instant): BigDecimal {
        return inventoryLedgerRepository.calculateWarehouseValueOnDate(warehouseId, ledgerDate)
            ?: BigDecimal.ZERO
    }

    /**
     * Calculate total warehouse stock quantity on a date
     *
     * @param warehouseId Warehouse UID
     * @param ledgerDate Ledger date
     * @return Total stock quantity
     */
    fun getWarehouseStockQuantity(warehouseId: String, ledgerDate: Instant): BigDecimal {
        return inventoryLedgerRepository.calculateWarehouseStockOnDate(warehouseId, ledgerDate)
            ?: BigDecimal.ZERO
    }

    /**
     * Get items with movement on a specific date
     *
     * @param ledgerDate Ledger date
     * @return List of ledger entries with movements
     */
    fun getItemsWithMovement(ledgerDate: Instant): List<InventoryLedger> {
        return inventoryLedgerRepository.findEntriesWithMovementOnDate(ledgerDate)
    }
}
