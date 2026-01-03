package com.ampairs.inventory.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.inventory.domain.dto.*
import com.ampairs.inventory.service.InventoryItemService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
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
class InventoryController @Autowired constructor(
    private val inventoryItemService: InventoryItemService
) {

    @PostMapping("")
    fun createInventoryItem(@Valid @RequestBody request: InventoryItemRequest): ApiResponse<InventoryItemResponse> {
        val item = request.toInventoryItem()
        val created = inventoryItemService.createInventoryItem(item)
        return ApiResponse.success(created.asInventoryItemResponse())
    }

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

    @GetMapping("/{uid}")
    fun getInventoryItem(@PathVariable uid: String): ApiResponse<InventoryItemResponse> {
        val item = inventoryItemService.getInventoryItemByUid(uid)
            ?: return ApiResponse.error("INVENTORY_ITEM_NOT_FOUND", "Inventory item not found: $uid")
        return ApiResponse.success(item.asInventoryItemResponse())
    }

    @PutMapping("/{uid}")
    fun updateInventoryItem(
        @PathVariable uid: String,
        @Valid @RequestBody request: InventoryItemRequest
    ): ApiResponse<InventoryItemResponse> {
        val updates = request.toInventoryItem()
        val updated = inventoryItemService.updateInventoryItem(uid, updates)
        return ApiResponse.success(updated.asInventoryItemResponse())
    }

    @DeleteMapping("/{uid}")
    fun deleteInventoryItem(@PathVariable uid: String): ApiResponse<Map<String, Any>> {
        inventoryItemService.deleteInventoryItem(uid)
        return ApiResponse.success(mapOf("deleted" to true, "uid" to uid))
    }

    @GetMapping("/low-stock")
    fun getLowStockItems(
        @RequestParam(name = "warehouse_id", required = false) warehouseId: String?
    ): ApiResponse<List<InventoryItemResponse>> {
        val items = if (warehouseId != null) {
            inventoryItemService.getLowStockItemsByWarehouse(warehouseId)
        } else {
            inventoryItemService.getLowStockItems()
        }
        return ApiResponse.success(items.asInventoryItemResponses())
    }

    @GetMapping("/out-of-stock")
    fun getOutOfStockItems(): ApiResponse<List<InventoryItemResponse>> {
        val items = inventoryItemService.getOutOfStockItems()
        return ApiResponse.success(items.asInventoryItemResponses())
    }

    @GetMapping("/low-stock/count")
    fun getLowStockCount(): ApiResponse<Long> {
        val count = inventoryItemService.countLowStockItems()
        return ApiResponse.success(count)
    }

    @GetMapping("/by-product/{productId}")
    fun getInventoryByProduct(@PathVariable productId: String): ApiResponse<List<InventoryItemResponse>> {
        val items = inventoryItemService.getInventoryByProduct(productId)
        return ApiResponse.success(items.asInventoryItemResponses())
    }

    @GetMapping("/stock-summary")
    fun getStockSummary(
        @RequestParam(name = "warehouse_id", required = false) warehouseId: String?
    ): ApiResponse<List<StockSummaryResponse>> {
        val items = if (warehouseId != null) {
            inventoryItemService.getInventoryByWarehouse(warehouseId, activeOnly = true)
        } else {
            inventoryItemService.getAllInventoryItems(PageRequest.of(0, 1000), activeOnly = true).content
        }
        return ApiResponse.success(items.asStockSummaries())
    }
}