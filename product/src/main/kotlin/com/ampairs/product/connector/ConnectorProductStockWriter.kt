package com.ampairs.product.connector

import com.ampairs.core.connector.ConnectorEntityWriter
import com.ampairs.core.connector.WriteOutcome
import com.ampairs.core.connector.WriteResult
import com.ampairs.product.repository.ProductRepository
import org.springframework.stereotype.Component

/**
 * Tally closing-balance → Product stock (spec 013), for the `stock_balance` connector entity.
 *
 * Status: **SKIPPED / not yet applied.** The `main` merge replaced the old single-scalar stock with a
 * multi-warehouse, ledger-backed inventory model (`com.ampairs.inventory`, spec 014): stock is a
 * per-(product, warehouse) `InventoryItem` reconciled through **transactions** (a physical COUNT sets
 * on-hand and records an `InventoryTransaction`, keeping `quantityOnHand` and the ledger in sync).
 * Tally sends an *absolute closing balance*, which does NOT map mechanically onto that model — a
 * correct integration must (1) resolve a per-workspace **default warehouse**, (2) resolve/auto-create
 * the product's `InventoryItem`, and (3) apply the balance as an **idempotent physical COUNT**
 * (`InventoryTransactionService.physicalCount`, skipping when the counted qty equals current on-hand,
 * via `WarehouseService.getDefaultWarehouse()`). Those are policy + correctness decisions (auto-create
 * scope, SKU uniqueness, no-default-warehouse behaviour) that warrant their own tests.
 *
 * Until that lands, this writer **explicitly SKIPs** every row (matching the product only for context)
 * rather than persisting a no-op "UPDATED" — so connector run history reflects reality instead of a
 * phantom success, and no stock data is silently discarded-as-applied. Follow-up: spec 013 §stock_balance.
 */
@Component
class ConnectorProductStockWriter(
    private val productRepository: ProductRepository,
) : ConnectorEntityWriter {

    override val entityType: String = "stock_balance"

    override fun applySparse(refId: String?, uid: String?, presentColumns: Map<String, Any?>): WriteResult {
        val product = refId?.takeIf { it.isNotBlank() }?.let { productRepository.findByRefId(it) }
            ?: uid?.takeIf { it.isNotBlank() }?.let { productRepository.findByUid(it) }
        return WriteResult(
            outcome = WriteOutcome.SKIPPED,
            ampairsUid = product?.uid,
            appliedColumns = emptyList(),
            message = "stock_balance not applied — pending inventory (spec 014) physical-count integration",
        )
    }
}
