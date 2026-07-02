package com.ampairs.inventory.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.inventory.domain.dto.InventoryTransactionSyncRequest
import com.ampairs.inventory.domain.dto.InventoryTransactionSyncResponse
import com.ampairs.inventory.domain.dto.asInventoryTransactionSyncResponse
import com.ampairs.inventory.service.InventorySyncService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*

/**
 * Inventory Transaction (stock movement) controller — offline-sync `/sync` contract (spec 014).
 * Movements are an append-only ledger: push only creates new movements; the server fills
 * `balance_after` + `transaction_number` and applies the signed stock delta.
 *
 * Base path: /inventory/v1/transactions
 */
@RestController
@RequestMapping("/inventory/v1/transactions")
class InventoryTransactionController(
    private val inventorySyncService: InventorySyncService,
) {

    @GetMapping("/sync")
    fun getTransactionsSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<InventoryTransactionSyncResponse>> {
        val jpaSortBy = when (sortBy) {
            "transactionDate", "createdAt", "updatedAt" -> sortBy
            else -> "updatedAt"
        }
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), jpaSortBy))
        return ApiResponse.success(
            PageResponse.from(inventorySyncService.getTransactionsAfterSync(lastSync, pageable)) {
                it.asInventoryTransactionSyncResponse()
            }
        )
    }

    @PostMapping("/sync")
    fun bulkUpsertTransactions(
        @RequestBody requests: List<@Valid InventoryTransactionSyncRequest>,
    ): ApiResponse<List<InventoryTransactionSyncResponse>> =
        ApiResponse.success(inventorySyncService.bulkUpsertTransactions(requests))
}
