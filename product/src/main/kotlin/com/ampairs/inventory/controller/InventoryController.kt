package com.ampairs.inventory.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.inventory.domain.dto.*
import com.ampairs.inventory.service.InventoryItemService
import com.ampairs.inventory.service.InventorySyncService
import jakarta.validation.Valid
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
    private val inventoryItemService: InventoryItemService,
    private val inventorySyncService: InventorySyncService,
) {

    /**
     * Offline-sync pull (spec 014). Includes soft-deleted (is_active=false) rows so deletes propagate.
     */
    @GetMapping("/sync")
    fun getItemsSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<InventoryItemSyncResponse>> {
        val jpaSortBy = when (sortBy) {
            "name", "sku", "currentStock", "createdAt", "updatedAt" -> sortBy
            else -> "updatedAt"
        }
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), jpaSortBy))
        return ApiResponse.success(
            PageResponse.from(inventorySyncService.getItemsAfterSync(lastSync, pageable)) {
                it.asInventoryItemSyncResponse()
            }
        )
    }

    /** Offline-sync push (spec 014). UID-keyed bulk upsert; honors is_active soft-delete. */
    @PostMapping("/sync")
    fun bulkUpsertItems(
        @RequestBody requests: List<@Valid InventoryItemSyncRequest>,
    ): ApiResponse<List<InventoryItemSyncResponse>> =
        ApiResponse.success(inventorySyncService.bulkUpsertItems(requests))

    /** Single item lookup (item detail header). */
    @GetMapping("/{uid}")
    fun getItem(@PathVariable uid: String): ApiResponse<InventoryItemResponse> {
        val item = inventoryItemService.getInventoryItemByUid(uid)
        return ApiResponse.success(item.asInventoryItemResponse())
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
}
