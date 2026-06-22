package com.ampairs.inventory.service

import com.ampairs.inventory.config.Constants
import com.ampairs.inventory.config.InventorySettingDefinitions
import com.ampairs.inventory.domain.model.InventoryItem
import com.ampairs.inventory.domain.model.InventoryTransaction
import com.ampairs.inventory.exception.InsufficientStockException
import com.ampairs.inventory.repository.InventoryItemRepository
import com.ampairs.inventory.repository.InventoryTransactionRepository
import com.ampairs.inventory.repository.WarehouseRepository
import com.ampairs.core.setting.InstalledModulesProvider
import com.ampairs.setting.service.SettingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Default implementation of [InventoryStockService] (spec 014, US1).
 *
 * Applies/reverses stock for sales, honoring the workspace inventory policy read from the central
 * settings registry, computing `balanceAfter` under a per-item transaction, and guaranteeing
 * idempotency per (sourceType, sourceId, sourceLineUid).
 */
@Service
class InventoryStockServiceImpl(
    private val inventoryItemRepository: InventoryItemRepository,
    private val inventoryTransactionRepository: InventoryTransactionRepository,
    private val warehouseRepository: WarehouseRepository,
    private val settingService: SettingService,
    private val installedModulesProvider: InstalledModulesProvider,
) : InventoryStockService {

    private val log = LoggerFactory.getLogger(InventoryStockServiceImpl::class.java)

    @Transactional
    override fun applySale(command: StockMutationCommand) {
        if (!inventoryModuleInstalled()) return       // inventory-management not installed → no-op
        if (!autoDeductEnabled()) {
            log.debug("Auto-deduct disabled; skipping applySale for {} {}", command.sourceType, command.sourceId)
            return
        }
        val blockWhenOutOfStock = settingBool(InventorySettingDefinitions.KEY_BLOCK_ORDERS_WHEN_OUT_OF_STOCK)
        val allowNegative = settingBool(InventorySettingDefinitions.KEY_ALLOW_NEGATIVE_STOCK)

        for (line in command.lines) {
            val item = resolveItem(line) ?: continue            // FR-004: untracked line → skip
            if (alreadyApplied(command.sourceType.name, command.sourceId, line.sourceLineUid)) continue

            val newStock = item.currentStock.subtract(line.quantity)
            if (newStock < BigDecimal.ZERO && !allowNegative && blockWhenOutOfStock) {
                throw InsufficientStockException(
                    itemId = item.uid,
                    itemName = item.name,
                    available = item.currentStock.toPlainString(),
                    requested = line.quantity.toPlainString(),
                )
            }

            applyDelta(
                item = item,
                signedDelta = line.quantity.negate(),
                txnType = Constants.TXN_TYPE_STOCK_OUT,
                reason = Constants.REASON_SALE,
                sourceType = command.sourceType.name,
                command = command,
                line = line,
            )
        }
    }

    @Transactional
    override fun reverseSale(command: StockMutationCommand) {
        if (!inventoryModuleInstalled()) return       // inventory-management not installed → no-op
        for (line in command.lines) {
            val item = resolveItem(line) ?: continue
            // Reversals are recorded under a RETURN source type so their idempotency key never
            // collides with the original applySale row for the same document line.
            if (alreadyApplied(StockSourceType.RETURN.name, command.sourceId, line.sourceLineUid)) continue

            applyDelta(
                item = item,
                signedDelta = line.quantity,
                txnType = Constants.TXN_TYPE_STOCK_IN,
                reason = Constants.REASON_RETURN,
                sourceType = StockSourceType.RETURN.name,
                command = command,
                line = line,
            )
        }
    }

    // ------------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------------

    private fun applyDelta(
        item: InventoryItem,
        signedDelta: BigDecimal,
        txnType: String,
        reason: String,
        sourceType: String,
        command: StockMutationCommand,
        line: StockLine,
    ) {
        item.currentStock = item.currentStock.add(signedDelta)
        item.recalculateAvailableStock()
        inventoryItemRepository.save(item)

        val unitCost = line.unitCost ?: item.costPrice
        val txn = InventoryTransaction().apply {
            this.transactionNumber = generateTransactionNumber()
            this.transactionType = txnType
            this.transactionReason = reason
            this.inventoryItemId = item.uid
            this.warehouseId = item.warehouseId
            this.quantity = line.quantity
            this.balanceAfter = item.currentStock
            this.unitCost = unitCost
            this.referenceType = sourceType
            this.referenceId = command.sourceId
            this.referenceNumber = command.referenceNumber
            this.sourceType = sourceType
            this.sourceId = command.sourceId
            this.sourceLineUid = line.sourceLineUid
            this.transactionDate = Instant.now()
            this.performedBy = command.performedBy
            calculateTotalCost()
        }
        inventoryTransactionRepository.save(txn)
    }

    /** Idempotency guard (also the authoritative guard on MySQL where the partial index is unavailable). */
    private fun alreadyApplied(sourceType: String, sourceId: String, sourceLineUid: String): Boolean =
        inventoryTransactionRepository
            .findBySourceTypeAndSourceIdAndSourceLineUid(sourceType, sourceId, sourceLineUid) != null

    /** Resolve a sale line to a tracked inventory item at the default warehouse (single-warehouse, R1). */
    private fun resolveItem(line: StockLine): InventoryItem? {
        val productId = line.productId ?: return null
        val defaultWarehouseId = warehouseRepository.findByIsDefaultTrue()?.uid
        val variantId = line.productVariantId
        if (defaultWarehouseId != null) {
            val byWarehouse = if (variantId != null) {
                inventoryItemRepository.findByProductVariantIdAndWarehouseId(variantId, defaultWarehouseId)
            } else {
                inventoryItemRepository.findByProductIdAndWarehouseId(productId, defaultWarehouseId)
            }
            if (byWarehouse != null) return byWarehouse
        }
        // Fallback: first tracked item for the product across locations (single-warehouse setups).
        return inventoryItemRepository.findByProductId(productId).firstOrNull()
    }

    /** Stock actions only run when the workspace has the inventory-management module installed. */
    private fun inventoryModuleInstalled(): Boolean =
        InventorySettingDefinitions.MODULE_CODE in installedModulesProvider.enabledModuleCodes()

    private fun autoDeductEnabled(): Boolean =
        settingBool(InventorySettingDefinitions.KEY_AUTO_DEDUCT_ON_ORDER)

    private fun settingBool(key: String): Boolean =
        settingService.getBoolean(InventorySettingDefinitions.MODULE, key)

    private fun generateTransactionNumber(): String {
        val today = LocalDate.now()
        val dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val startOfDay = today.atStartOfDay(ZoneId.of("UTC")).toInstant()
        val count = inventoryTransactionRepository.countTransactionsToday(startOfDay)
        return "TXN-$dateStr-" + (count + 1).toString().padStart(4, '0')
    }
}
