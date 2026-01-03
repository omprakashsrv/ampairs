package com.ampairs.inventory.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.inventory.domain.dto.*
import com.ampairs.inventory.service.InventoryBatchService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

/**
 * Batch Controller
 *
 * REST API endpoints for batch/lot tracking management.
 * Handles batch creation, queries, and expiry management.
 *
 * Base path: /inventory/v1/batches
 */
@RestController
@RequestMapping("/inventory/v1/batches")
class BatchController @Autowired constructor(
    private val inventoryBatchService: InventoryBatchService
) {

    // ============================================================================
    // CRUD Endpoints
    // ============================================================================

    /**
     * Create a new batch
     *
     * POST /inventory/v1/batches
     *
     * Creates a batch for batch-tracked inventory items
     *
     * @param request Batch creation request
     * @return Created batch
     */
    @PostMapping("")
    fun createBatch(@Valid @RequestBody request: InventoryBatchRequest): ApiResponse<InventoryBatchResponse> {
        val batch = inventoryBatchService.createBatch(request)
        return ApiResponse.success(batch.asInventoryBatchResponse())
    }

    /**
     * Get batch by UID
     *
     * GET /inventory/v1/batches/{uid}
     *
     * @param uid Batch UID
     * @return Batch details
     */
    @GetMapping("/{uid}")
    fun getBatch(@PathVariable uid: String): ApiResponse<InventoryBatchResponse> {
        val batch = inventoryBatchService.getBatchByUid(uid)
        return ApiResponse.success(batch.asInventoryBatchResponse())
    }

    /**
     * Update batch information
     *
     * PUT /inventory/v1/batches/{uid}
     *
     * @param uid Batch UID
     * @param request Update request
     * @return Updated batch
     */
    @PutMapping("/{uid}")
    fun updateBatch(
        @PathVariable uid: String,
        @Valid @RequestBody request: BatchUpdateRequest
    ): ApiResponse<InventoryBatchResponse> {
        val batch = inventoryBatchService.updateBatch(uid, request)
        return ApiResponse.success(batch.asInventoryBatchResponse())
    }

    /**
     * Delete batch (soft delete)
     *
     * DELETE /inventory/v1/batches/{uid}
     *
     * Marks batch as inactive. Only allowed if no stock remains.
     *
     * @param uid Batch UID
     * @return Success message
     */
    @DeleteMapping("/{uid}")
    fun deleteBatch(@PathVariable uid: String): ApiResponse<String> {
        inventoryBatchService.deleteBatch(uid)
        return ApiResponse.success("Batch deleted successfully")
    }

    // ============================================================================
    // Query Endpoints
    // ============================================================================

    /**
     * Get batches for an inventory item at a specific warehouse
     *
     * GET /inventory/v1/batches/item/{itemId}/warehouse/{warehouseId}
     *
     * @param itemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return List of batches
     */
    @GetMapping("/item/{itemId}/warehouse/{warehouseId}")
    fun getBatchesByItemAndWarehouse(
        @PathVariable itemId: String,
        @PathVariable warehouseId: String
    ): ApiResponse<List<InventoryBatchResponse>> {
        val batches = inventoryBatchService.getBatchesByItemAndWarehouse(itemId, warehouseId)
        return ApiResponse.success(batches.asInventoryBatchResponses())
    }

    /**
     * Get all batches for an inventory item (all warehouses)
     *
     * GET /inventory/v1/batches/item/{itemId}
     *
     * @param itemId Inventory item UID
     * @return List of batches
     */
    @GetMapping("/item/{itemId}")
    fun getBatchesByItem(@PathVariable itemId: String): ApiResponse<List<InventoryBatchResponse>> {
        val batches = inventoryBatchService.getBatchesByItem(itemId)
        return ApiResponse.success(batches.asInventoryBatchResponses())
    }

    // ============================================================================
    // Expiry Management Endpoints
    // ============================================================================

    /**
     * Get batches expiring within specified days
     *
     * GET /inventory/v1/batches/expiring
     *
     * Used for expiry alerts and proactive batch management
     *
     * @param days Days until expiry (default: 30)
     * @return List of expiring batches
     */
    @GetMapping("/expiring")
    fun getExpiringBatches(
        @RequestParam(defaultValue = "30") days: Int
    ): ApiResponse<List<InventoryBatchResponse>> {
        val batches = inventoryBatchService.getExpiringBatches(days)
        return ApiResponse.success(batches.asInventoryBatchResponses())
    }

    /**
     * Get expiry alerts
     *
     * GET /inventory/v1/batches/expiry-alerts
     *
     * Returns summary of batches expiring soon
     *
     * @param days Days threshold (default: from tenant config)
     * @return List of expiring batches with summary info
     */
    @GetMapping("/expiry-alerts")
    fun getExpiryAlerts(
        @RequestParam(required = false) days: Int?
    ): ApiResponse<List<BatchSummaryResponse>> {
        val alertDays = days ?: 30  // Default to 30 days
        val batches = inventoryBatchService.getExpiryAlerts(alertDays)
        return ApiResponse.success(batches.asBatchSummaryResponses())
    }

    /**
     * Mark expired batches
     *
     * POST /inventory/v1/batches/mark-expired
     *
     * Manually trigger expiry check and mark expired batches
     * Usually called by scheduled job, but can be triggered manually
     *
     * @return Count of batches marked as expired
     */
    @PostMapping("/mark-expired")
    fun markExpiredBatches(): ApiResponse<Int> {
        val count = inventoryBatchService.markExpiredBatches()
        return ApiResponse.success(count)
    }
}
