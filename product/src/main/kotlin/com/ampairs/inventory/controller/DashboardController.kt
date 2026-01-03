package com.ampairs.inventory.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.inventory.domain.dto.InventoryItemResponse
import com.ampairs.inventory.domain.dto.asInventoryItemResponses
import com.ampairs.inventory.service.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Dashboard Controller
 *
 * REST API endpoints for inventory dashboard, reports, and analytics.
 * Provides aggregated data and insights for inventory management.
 *
 * Base path: /inventory/v1/dashboard
 */
@RestController
@RequestMapping("/inventory/v1/dashboard")
class DashboardController @Autowired constructor(
    private val inventoryItemService: InventoryItemService,
    private val inventoryLedgerService: InventoryLedgerService,
    private val inventoryBatchService: InventoryBatchService,
    private val inventorySerialService: InventorySerialService,
    private val warehouseService: WarehouseService
) {

    // ============================================================================
    // Dashboard Summary Endpoints
    // ============================================================================

    /**
     * Get overall inventory dashboard summary
     *
     * GET /inventory/v1/dashboard/summary
     *
     * Returns key metrics for the entire inventory system
     *
     * @return Dashboard summary with key metrics
     */
    @GetMapping("/summary")
    fun getDashboardSummary(): ApiResponse<Map<String, Any>> {
        val lowStockItems = inventoryItemService.getLowStockItems()
        val outOfStockItems = inventoryItemService.getOutOfStockItems()
        val activeItems = inventoryItemService.countActiveItems()
        val activeWarehouses = warehouseService.getActiveWarehouses().size

        val summary = mapOf(
            "total_active_items" to activeItems,
            "total_warehouses" to activeWarehouses,
            "low_stock_count" to lowStockItems.size,
            "out_of_stock_count" to outOfStockItems.size,
            "low_stock_items" to lowStockItems.take(10).map { it.uid to it.name },
            "requires_attention" to (lowStockItems.size + outOfStockItems.size)
        )

        return ApiResponse.success(summary)
    }

    /**
     * Get warehouse dashboard summary
     *
     * GET /inventory/v1/dashboard/warehouse/{warehouseId}
     *
     * Returns metrics for a specific warehouse
     *
     * @param warehouseId Warehouse UID
     * @return Warehouse dashboard summary
     */
    @GetMapping("/warehouse/{warehouseId}")
    fun getWarehouseDashboard(@PathVariable warehouseId: String): ApiResponse<Map<String, Any>> {
        val warehouse = warehouseService.getWarehouseByUid(warehouseId)
        val totalItems = inventoryItemService.countByWarehouse(warehouseId)
        val lowStockItems = inventoryItemService.getLowStockItemsByWarehouse(warehouseId)

        val summary = mapOf(
            "warehouse_id" to warehouseId,
            "warehouse_name" to (warehouse?.name ?: "Unknown"),
            "warehouse_code" to (warehouse?.code ?: "N/A"),
            "total_items" to totalItems,
            "low_stock_count" to lowStockItems.size,
            "low_stock_items" to lowStockItems.take(10).map {
                mapOf(
                    "uid" to it.uid,
                    "name" to it.name,
                    "sku" to it.sku,
                    "current_stock" to it.currentStock,
                    "reorder_level" to it.reorderLevel
                )
            }
        )

        return ApiResponse.success(summary)
    }

    // ============================================================================
    // Alert Endpoints
    // ============================================================================

    /**
     * Get all active alerts
     *
     * GET /inventory/v1/dashboard/alerts
     *
     * Returns all alerts: low stock, out of stock, expiring batches, expiring warranties
     *
     * @return List of alerts
     */
    @GetMapping("/alerts")
    fun getAlerts(): ApiResponse<Map<String, Any>> {
        val lowStockItems = inventoryItemService.getLowStockItems()
        val outOfStockItems = inventoryItemService.getOutOfStockItems()
        val expiringBatches = inventoryBatchService.getExpiringBatches(30)
        val expiringWarranties = inventorySerialService.getSerialsWithExpiringWarranty(30)

        val alerts = mapOf(
            "low_stock" to mapOf(
                "count" to lowStockItems.size,
                "severity" to "warning",
                "items" to lowStockItems.take(5).map {
                    mapOf(
                        "item_id" to it.uid,
                        "name" to it.name,
                        "current_stock" to it.currentStock,
                        "reorder_level" to it.reorderLevel
                    )
                }
            ),
            "out_of_stock" to mapOf(
                "count" to outOfStockItems.size,
                "severity" to "critical",
                "items" to outOfStockItems.take(5).map {
                    mapOf(
                        "item_id" to it.uid,
                        "name" to it.name,
                        "current_stock" to it.currentStock
                    )
                }
            ),
            "expiring_batches" to mapOf(
                "count" to expiringBatches.size,
                "severity" to "warning",
                "days_threshold" to 30,
                "batches" to expiringBatches.take(5).map {
                    mapOf(
                        "batch_id" to it.uid,
                        "batch_number" to it.batchNumber,
                        "expiry_date" to it.expiryDate,
                        "available_quantity" to it.availableQuantity
                    )
                }
            ),
            "expiring_warranties" to mapOf(
                "count" to expiringWarranties.size,
                "severity" to "info",
                "days_threshold" to 30,
                "serials" to expiringWarranties.take(5).map {
                    mapOf(
                        "serial_number" to it.serialNumber,
                        "customer_id" to it.customerId,
                        "warranty_expiry" to it.warrantyExpiryDate
                    )
                }
            ),
            "total_alerts" to (lowStockItems.size + outOfStockItems.size + expiringBatches.size)
        )

        return ApiResponse.success(alerts)
    }

    // ============================================================================
    // Stock Valuation Endpoints
    // ============================================================================

    /**
     * Get total stock valuation
     *
     * GET /inventory/v1/dashboard/valuation
     *
     * Returns total stock value across all warehouses
     *
     * @param date Optional date for historical valuation (YYYY-MM-DD)
     * @return Stock valuation summary
     */
    @GetMapping("/valuation")
    fun getStockValuation(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?
    ): ApiResponse<Map<String, Any>> {
        val valuationDate = date ?: LocalDate.now().minusDays(1)
        val ledgerDate = com.ampairs.inventory.domain.model.InventoryLedger.ledgerDateFromLocalDate(valuationDate)

        val warehouses = warehouseService.getActiveWarehouses()
        val warehouseValuations = warehouses.map { warehouse ->
            val value = inventoryLedgerService.getWarehouseStockValue(warehouse.uid, ledgerDate)
            val quantity = inventoryLedgerService.getWarehouseStockQuantity(warehouse.uid, ledgerDate)

            mapOf(
                "warehouse_id" to warehouse.uid,
                "warehouse_name" to warehouse.name,
                "stock_value" to value,
                "stock_quantity" to quantity
            )
        }

        val totalValue = warehouseValuations.sumOf { (it["stock_value"] as BigDecimal?) ?: BigDecimal.ZERO }
        val totalQuantity = warehouseValuations.sumOf { (it["stock_quantity"] as BigDecimal?) ?: BigDecimal.ZERO }

        val valuation = mapOf(
            "valuation_date" to valuationDate,
            "total_stock_value" to totalValue,
            "total_stock_quantity" to totalQuantity,
            "warehouse_breakdown" to warehouseValuations
        )

        return ApiResponse.success(valuation)
    }

    /**
     * Get warehouse stock valuation
     *
     * GET /inventory/v1/dashboard/warehouse/{warehouseId}/valuation
     *
     * @param warehouseId Warehouse UID
     * @param date Optional date for historical valuation (YYYY-MM-DD)
     * @return Warehouse stock valuation
     */
    @GetMapping("/warehouse/{warehouseId}/valuation")
    fun getWarehouseValuation(
        @PathVariable warehouseId: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?
    ): ApiResponse<Map<String, Any>> {
        val warehouse = warehouseService.getWarehouseByUid(warehouseId)
        val valuationDate = date ?: LocalDate.now().minusDays(1)
        val ledgerDate = com.ampairs.inventory.domain.model.InventoryLedger.ledgerDateFromLocalDate(valuationDate)

        val value = inventoryLedgerService.getWarehouseStockValue(warehouseId, ledgerDate)
        val quantity = inventoryLedgerService.getWarehouseStockQuantity(warehouseId, ledgerDate)

        val valuation = mapOf(
            "warehouse_id" to warehouseId,
            "warehouse_name" to (warehouse?.name ?: "Unknown"),
            "valuation_date" to valuationDate,
            "stock_value" to value,
            "stock_quantity" to quantity
        )

        return ApiResponse.success(valuation)
    }

    // ============================================================================
    // Movement Analytics Endpoints
    // ============================================================================

    /**
     * Get stock movement summary for a date
     *
     * GET /inventory/v1/dashboard/movements/{date}
     *
     * Shows items with stock movements on a specific date
     *
     * @param date Date to analyze (YYYY-MM-DD)
     * @return Movement summary
     */
    @GetMapping("/movements/{date}")
    fun getMovementSummary(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ApiResponse<Map<String, Any>> {
        val ledgerDate = com.ampairs.inventory.domain.model.InventoryLedger.ledgerDateFromLocalDate(date)
        val ledgers = inventoryLedgerService.getItemsWithMovement(ledgerDate)

        val summary = mapOf(
            "date" to date,
            "items_with_movement" to ledgers.size,
            "total_stock_in" to ledgers.sumOf { it.stockIn },
            "total_stock_out" to ledgers.sumOf { it.stockOut },
            "total_transfers_in" to ledgers.sumOf { it.transferIn },
            "total_transfers_out" to ledgers.sumOf { it.transferOut },
            "total_adjustments_in" to ledgers.sumOf { it.adjustmentIn },
            "total_adjustments_out" to ledgers.sumOf { it.adjustmentOut },
            "top_movements" to ledgers.sortedByDescending { it.getNetMovement().abs() }.take(10).map {
                mapOf(
                    "item_id" to it.inventoryItemId,
                    "warehouse_id" to it.warehouseId,
                    "net_movement" to it.getNetMovement(),
                    "closing_stock" to it.closingStock
                )
            }
        )

        return ApiResponse.success(summary)
    }

    // ============================================================================
    // Batch & Serial Analytics
    // ============================================================================

    /**
     * Get batch expiry summary
     *
     * GET /inventory/v1/dashboard/batch-expiry
     *
     * @param days Days threshold for expiry (default: 30)
     * @return Batch expiry summary
     */
    @GetMapping("/batch-expiry")
    fun getBatchExpirySummary(
        @RequestParam(defaultValue = "30") days: Int
    ): ApiResponse<Map<String, Any>> {
        val expiringBatches = inventoryBatchService.getExpiringBatches(days)
        val totalQuantity = expiringBatches.sumOf { it.availableQuantity }
        val totalValue = expiringBatches.sumOf {
            it.availableQuantity.multiply(it.costPerUnit)
        }

        val summary = mapOf(
            "days_threshold" to days,
            "expiring_batches_count" to expiringBatches.size,
            "total_expiring_quantity" to totalQuantity,
            "total_expiring_value" to totalValue,
            "batches" to expiringBatches.take(10).map {
                mapOf(
                    "batch_number" to it.batchNumber,
                    "item_id" to it.inventoryItemId,
                    "expiry_date" to it.expiryDate,
                    "available_quantity" to it.availableQuantity,
                    "value" to it.availableQuantity.multiply(it.costPerUnit)
                )
            }
        )

        return ApiResponse.success(summary)
    }

    /**
     * Get serial tracking summary
     *
     * GET /inventory/v1/dashboard/serial-summary
     *
     * @return Serial tracking statistics
     */
    @GetMapping("/serial-summary")
    fun getSerialSummary(): ApiResponse<Map<String, Any>> {
        // This would require aggregating across all items
        // For now, return a placeholder that can be enhanced with actual data
        val summary = mapOf(
            "message" to "Serial summary endpoint - aggregate data across all items",
            "note" to "Use /inventory/v1/serials/item/{itemId}/warehouse/{warehouseId}/summary for specific item summaries"
        )

        return ApiResponse.success(summary)
    }
}
