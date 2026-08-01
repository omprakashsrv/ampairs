package com.ampairs.inventory.service

import com.ampairs.inventory.repository.InventoryItemRepository
import com.ampairs.inventory.repository.InventoryTransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

/**
 * Public read-only port exposing inventory KPI aggregates to the `analytics` module (Principle II/IX —
 * no entity leakage). All figures are live (current stock is point-in-time); `@TenantId` scopes them to
 * the workspace.
 */
@Service
class InventoryAnalyticsQueryService(
    private val inventoryItemRepository: InventoryItemRepository,
    private val inventoryTransactionRepository: InventoryTransactionRepository,
) {

    /** Σ(currentStock × costPrice) over active items — stock value at current cost. */
    @Transactional(readOnly = true)
    fun totalStockValue(): BigDecimal = inventoryItemRepository.sumStockValue()

    /** Count of active items at/below their (non-zero) reorder level. */
    @Transactional(readOnly = true)
    fun lowStockCount(): Long = inventoryItemRepository.countLowStock()

    /**
     * Workspace inventory turns over [fromInclusive, toExclusive): total units sold-out in the window
     * divided by current units on hand. Returns 0 when there is no stock on hand. This is a
     * stock-quantity-based approximation (not COGS/avg-inventory), sufficient for the dashboard tile.
     */
    @Transactional(readOnly = true)
    fun inventoryTurns(fromInclusive: Instant, toExclusive: Instant): BigDecimal {
        val out = inventoryTransactionRepository.sumStockOutInWindow(fromInclusive, toExclusive)
        val onHand = inventoryItemRepository.sumCurrentStock()
        return if (onHand.signum() > 0) out.divide(onHand, 3, RoundingMode.HALF_UP) else BigDecimal.ZERO
    }
}
