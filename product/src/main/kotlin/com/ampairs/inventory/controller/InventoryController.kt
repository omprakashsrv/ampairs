package com.ampairs.inventory.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.inventory.domain.dto.*
import com.ampairs.inventory.service.InventoryItemService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*

/**
 * Inventory Controller
 *
 * REST API endpoints for inventory item management.
 * Base path: /inventory/v1/items
 */
@RestController
@RequestMapping("/inventory/v1/items")
class InventoryController(
    private val inventoryItemService: InventoryItemService
) {

    @GetMapping("")
    fun getAllInventoryItems(
        @RequestParam(name = "active_only", defaultValue = "false") activeOnly: Boolean,
        @RequestParam(name = "warehouse_id", required = false) warehouseId: String?,
        @RequestParam(name = "search", required = false) search: String?,
        @RequestParam(name = "page", defaultValue = "0") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
        @RequestParam(name = "sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam(name = "sort_dir", defaultValue = "DESC") sortDir: String
    ): ApiResponse<Map<String, Any>> {
        val sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy)
        val pageable = PageRequest.of(page, size, sort)

        val itemsPage = when {
            search != null -> inventoryItemService.searchInventoryItems(search, pageable)
            warehouseId != null -> {
                val items = inventoryItemService.getInventoryByWarehouse(warehouseId, activeOnly)
                org.springframework.data.domain.PageImpl(items, pageable, items.size.toLong())
            }
            else -> inventoryItemService.getAllInventoryItems(pageable, activeOnly)
        }

        return ApiResponse.success(mapOf(
            "items" to itemsPage.content.asInventoryItemResponses(),
            "total_elements" to itemsPage.totalElements,
            "total_pages" to itemsPage.totalPages,
            "current_page" to itemsPage.number,
            "page_size" to itemsPage.size
        ))
    }
}
