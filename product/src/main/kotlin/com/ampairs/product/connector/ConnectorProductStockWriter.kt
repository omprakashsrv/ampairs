package com.ampairs.product.connector

import com.ampairs.core.connector.ConnectorEntityWriter
import com.ampairs.core.connector.WriteOutcome
import com.ampairs.core.connector.WriteResult
import com.ampairs.inventory.domain.dto.PhysicalCountRequest
import com.ampairs.inventory.repository.InventoryItemRepository
import com.ampairs.inventory.service.InventoryTransactionService
import com.ampairs.inventory.service.WarehouseService
import com.ampairs.product.repository.ProductRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Tally closing-balance → Product stock (spec 013, `stock_balance` connector entity).
 *
 * Maps Tally's **absolute** closing balance onto the multi-warehouse, ledger-backed inventory model
 * (`com.ampairs.inventory`, spec 014) as an **idempotent, ledger-consistent physical COUNT**:
 *
 *  1. Match the product by `refId` (Tally GUID) or `uid`.
 *  2. Resolve the workspace's **default warehouse** ([WarehouseService.getDefaultWarehouse]); Tally has
 *     no warehouse dimension in this feed.
 *  3. Resolve the product's tracked [com.ampairs.inventory.domain.model.InventoryItem] in that
 *     warehouse. Inventory tracking is **opt-in per item** (FR-004) — this writer NEVER auto-creates an
 *     item (that would silently opt every Tally product into inventory tracking, and SKU is unique per
 *     tenant). Untracked products are SKIPped.
 *  4. Apply the balance via [InventoryTransactionService.physicalCount], which records an
 *     `InventoryTransaction` (COUNT) and reconciles on-hand — keeping `currentStock` and the ledger in
 *     sync. Only runs when the counted quantity **differs** from current on-hand, so re-syncing an
 *     unchanged balance is a 0-write no-op (idempotent).
 *
 * Cross-module note: this writer lives in the `product` module (which owns `com.ampairs.inventory`), so
 * it uses the inventory services directly — it does not reach across a module boundary.
 */
@Component
class ConnectorProductStockWriter(
    private val productRepository: ProductRepository,
    private val warehouseService: WarehouseService,
    private val inventoryItemRepository: InventoryItemRepository,
    private val inventoryTransactionService: InventoryTransactionService,
) : ConnectorEntityWriter {

    override val entityType: String = "stock_balance"

    override fun applySparse(refId: String?, uid: String?, presentColumns: Map<String, Any?>): WriteResult {
        val product = refId?.takeIf { it.isNotBlank() }?.let { productRepository.findByRefId(it) }
            ?: uid?.takeIf { it.isNotBlank() }?.let { productRepository.findByUid(it) }
            ?: return skip(null, "No matching product for refId=$refId / uid=$uid")

        // stockQuantity must be present in the row (already ∩ the mapping allowlist by the caller).
        if (!presentColumns.containsKey("stockQuantity")) {
            return skip(product.uid, "Row has no stockQuantity")
        }
        val qty = toQuantity(presentColumns["stockQuantity"])
            ?: return skip(product.uid, "stockQuantity is not a number: ${presentColumns["stockQuantity"]}")

        val warehouse = warehouseService.getDefaultWarehouse()
            ?: return skip(product.uid, "No default warehouse configured")

        // Opt-in tracking (FR-004): reconcile only products that already have a tracked InventoryItem.
        val item = inventoryItemRepository.findByProductIdAndWarehouseId(product.uid, warehouse.uid)
            ?: return skip(product.uid, "Product not tracked in inventory — stock not applied")

        // Idempotent: only reconcile on a real difference from current on-hand.
        if (item.currentStock.compareTo(qty) == 0) {
            return skip(product.uid, "Stock already $qty — no change")
        }

        inventoryTransactionService.physicalCount(
            PhysicalCountRequest(
                inventoryItemId = item.uid,
                warehouseId = warehouse.uid,
                countedQuantity = qty,
                notes = "Tally connector stock sync",
                performedBy = "connector:tally",
            ),
        )
        return WriteResult(WriteOutcome.UPDATED, product.uid, listOf("stockQuantity"))
    }

    private fun skip(uid: String?, message: String): WriteResult =
        WriteResult(WriteOutcome.SKIPPED, uid, emptyList(), message)

    /** Coerce a JSON scalar (Number or numeric String) to BigDecimal; null if not numeric. */
    private fun toQuantity(value: Any?): BigDecimal? =
        value?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.toBigDecimalOrNull()
}
