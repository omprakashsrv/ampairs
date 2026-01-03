package com.ampairs.inventory.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.inventory.domain.dto.WarehouseRequest
import com.ampairs.inventory.domain.dto.WarehouseResponse
import com.ampairs.inventory.domain.dto.asWarehouseResponse
import com.ampairs.inventory.domain.dto.asWarehouseResponses
import com.ampairs.inventory.domain.dto.toWarehouse
import com.ampairs.inventory.service.WarehouseService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

/**
 * Warehouse Controller
 *
 * REST API endpoints for warehouse management.
 * Provides CRUD operations and warehouse-specific actions.
 *
 * Base path: /inventory/v1/warehouses
 */
@RestController
@RequestMapping("/inventory/v1/warehouses")
class WarehouseController @Autowired constructor(
    private val warehouseService: WarehouseService
) {

    /**
     * Create a new warehouse
     *
     * POST /inventory/v1/warehouses
     *
     * @param request Warehouse creation request
     * @return Created warehouse
     */
    @PostMapping("")
    fun createWarehouse(
        @Valid @RequestBody request: WarehouseRequest
    ): ApiResponse<WarehouseResponse> {
        val warehouse = request.toWarehouse()
        val created = warehouseService.createWarehouse(warehouse)
        return ApiResponse.success(created.asWarehouseResponse())
    }

    /**
     * Get all warehouses
     *
     * GET /inventory/v1/warehouses
     *
     * @param activeOnly Filter by active status (default: false)
     * @return List of warehouses
     */
    @GetMapping("")
    fun getAllWarehouses(
        @RequestParam(name = "active_only", defaultValue = "false") activeOnly: Boolean
    ): ApiResponse<List<WarehouseResponse>> {
        val warehouses = if (activeOnly) {
            warehouseService.getActiveWarehouses()
        } else {
            warehouseService.getAllWarehouses()
        }
        return ApiResponse.success(warehouses.asWarehouseResponses())
    }

    /**
     * Get warehouse by UID
     *
     * GET /inventory/v1/warehouses/{uid}
     *
     * @param uid Warehouse unique identifier
     * @return Warehouse details
     */
    @GetMapping("/{uid}")
    fun getWarehouse(
        @PathVariable uid: String
    ): ApiResponse<WarehouseResponse> {
        val warehouse = warehouseService.getWarehouseByUid(uid)
            ?: return ApiResponse.error("WAREHOUSE_NOT_FOUND", "Warehouse not found: $uid")

        return ApiResponse.success(warehouse.asWarehouseResponse())
    }

    /**
     * Update warehouse
     *
     * PUT /inventory/v1/warehouses/{uid}
     *
     * @param uid Warehouse unique identifier
     * @param request Warehouse update request
     * @return Updated warehouse
     */
    @PutMapping("/{uid}")
    fun updateWarehouse(
        @PathVariable uid: String,
        @Valid @RequestBody request: WarehouseRequest
    ): ApiResponse<WarehouseResponse> {
        val updates = request.toWarehouse()
        val updated = warehouseService.updateWarehouse(uid, updates)
        return ApiResponse.success(updated.asWarehouseResponse())
    }

    /**
     * Delete warehouse (soft delete)
     *
     * DELETE /inventory/v1/warehouses/{uid}
     *
     * @param uid Warehouse unique identifier
     * @return Deletion confirmation
     */
    @DeleteMapping("/{uid}")
    fun deleteWarehouse(
        @PathVariable uid: String
    ): ApiResponse<Map<String, Any>> {
        warehouseService.deleteWarehouse(uid)
        return ApiResponse.success(
            mapOf(
                "deleted" to true,
                "uid" to uid
            )
        )
    }

    /**
     * Set warehouse as default
     *
     * POST /inventory/v1/warehouses/{uid}/set-default
     *
     * @param uid Warehouse unique identifier
     * @return Updated warehouse
     */
    @PostMapping("/{uid}/set-default")
    fun setDefaultWarehouse(
        @PathVariable uid: String
    ): ApiResponse<WarehouseResponse> {
        val warehouse = warehouseService.setDefaultWarehouse(uid)
        return ApiResponse.success(warehouse.asWarehouseResponse())
    }

    /**
     * Get default warehouse
     *
     * GET /inventory/v1/warehouses/default
     *
     * @return Default warehouse if exists
     */
    @GetMapping("/default")
    fun getDefaultWarehouse(): ApiResponse<WarehouseResponse?> {
        val warehouse = warehouseService.getDefaultWarehouse()
        return if (warehouse != null) {
            ApiResponse.success(warehouse.asWarehouseResponse())
        } else {
            ApiResponse.success(null)
        }
    }

    /**
     * Get warehouse count
     *
     * GET /inventory/v1/warehouses/count
     *
     * @return Count of warehouses
     */
    @GetMapping("/count")
    fun countWarehouses(): ApiResponse<Map<String, Long>> {
        val count = warehouseService.countWarehouses()
        return ApiResponse.success(mapOf("count" to count))
    }
}
