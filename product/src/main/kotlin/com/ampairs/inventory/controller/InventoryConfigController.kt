package com.ampairs.inventory.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.inventory.domain.dto.InventoryConfigRequest
import com.ampairs.inventory.domain.dto.InventoryConfigResponse
import com.ampairs.inventory.domain.dto.asInventoryConfigResponse
import com.ampairs.inventory.domain.dto.toInventoryConfig
import com.ampairs.inventory.service.InventoryConfigService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

/**
 * Inventory Configuration Controller
 *
 * REST API endpoints for tenant-level inventory configuration.
 * Base path: /inventory/v1/config
 */
@RestController
@RequestMapping("/inventory/v1/config")
class InventoryConfigController @Autowired constructor(
    private val inventoryConfigService: InventoryConfigService
) {

    /**
     * Get current configuration
     *
     * GET /inventory/v1/config
     *
     * Creates default configuration if none exists
     *
     * @return Current inventory configuration
     */
    @GetMapping("")
    fun getConfig(): ApiResponse<InventoryConfigResponse> {
        val config = inventoryConfigService.getOrCreateConfig()
        return ApiResponse.success(config.asInventoryConfigResponse())
    }

    /**
     * Update configuration
     *
     * PUT /inventory/v1/config
     *
     * @param request Configuration update request
     * @return Updated configuration
     */
    @PutMapping("")
    fun updateConfig(
        @Valid @RequestBody request: InventoryConfigRequest
    ): ApiResponse<InventoryConfigResponse> {
        val config = inventoryConfigService.getOrCreateConfig()
        val updates = request.toInventoryConfig()
        val updated = inventoryConfigService.updateConfig(config.uid, updates)
        return ApiResponse.success(updated.asInventoryConfigResponse())
    }
}
