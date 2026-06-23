package com.ampairs.inventory.service

import java.math.BigDecimal

/**
 * Public, cross-module entry point for moving stock in response to a sale (spec 014, US1).
 *
 * The `order` and `invoice` modules call this interface (never the inventory repositories) to apply
 * or reverse stock for a confirmed/cancelled sale. All operations are **idempotent** per
 * (sourceType, sourceId, sourceLineUid): a retried or duplicated event is a no-op, so a belt-and-
 * suspenders event + direct-call combination is safe (R2 / SC-002).
 *
 * Policy (auto-deduct, block-on-out-of-stock, allow-negative) is read from the central settings
 * registry under the `inventory` namespace (R11). Lines whose product/variant has no tracked
 * inventory item are skipped — inventory tracking is opt-in per item (FR-004).
 */
interface InventoryStockService {

    /** Apply outbound stock for a confirmed sale. Idempotent per source line. */
    fun applySale(command: StockMutationCommand)

    /** Restore stock for a cancellation/return/void. Idempotent per source line. */
    fun reverseSale(command: StockMutationCommand)
}

/** The source document type that triggered a stock movement. */
enum class StockSourceType { ORDER, INVOICE, RETURN, MANUAL, COUNT }

/**
 * A request to apply/reverse stock for one source document.
 *
 * @property sourceType the document type (ORDER/INVOICE/RETURN)
 * @property sourceId the document uid
 * @property referenceNumber human-readable document number (optional, for the audit trail)
 * @property performedBy actor uid (optional)
 * @property lines the per-line quantities to move
 */
data class StockMutationCommand(
    val sourceType: StockSourceType,
    val sourceId: String,
    val referenceNumber: String? = null,
    val performedBy: String? = null,
    val lines: List<StockLine>,
)

/**
 * One line of a [StockMutationCommand].
 *
 * @property sourceLineUid stable per-line uid — the idempotency key
 * @property productId resolves to a tracked [com.ampairs.inventory.domain.model.InventoryItem]
 * @property productVariantId optional variant discriminator
 * @property quantity positive quantity to deduct (applySale) or restore (reverseSale)
 * @property unitCost optional cost for valuation of the movement
 */
data class StockLine(
    val sourceLineUid: String,
    val productId: String?,
    val productVariantId: String? = null,
    val quantity: BigDecimal,
    val unitCost: BigDecimal? = null,
)
