package com.ampairs.purchase.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.exception.NotFoundException
import com.ampairs.purchase.domain.dto.PurchaseResponse
import com.ampairs.purchase.domain.dto.PurchaseUpdateRequest
import com.ampairs.purchase.domain.dto.toResponse
import com.ampairs.purchase.service.PurchaseService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/purchase/v1/purchases")
class PurchaseController(
    private val purchaseService: PurchaseService,
) {

    /**
     * Offline-sync bulk upsert (push). Client UID-keyed; preserves taxInfos/totals/priceMode/
     * overallDiscountMode as supplied (no server recompute). Assigns purchaseNumber when blank.
     * A RECEIVED purchase increases inventory; a CANCELLED one reverses it.
     */
    @PostMapping("/sync")
    fun syncPurchases(@RequestBody @Valid requests: List<PurchaseUpdateRequest>): ApiResponse<List<PurchaseResponse>> {
        return ApiResponse.success(purchaseService.bulkUpsertPurchases(requests))
    }

    /**
     * Incremental sync feed (pull): purchases updated at/after last_sync (ISO-8601), INCLUDING
     * cancelled rows, ordered by updatedAt ASC, paginated.
     */
    @GetMapping("/sync")
    fun getPurchasesSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String
    ): ApiResponse<PageResponse<PurchaseResponse>> {
        val direction = Sort.Direction.fromString(sortDir)
        val pageable = PageRequest.of(page, size, Sort.by(direction, sortBy))
        val purchasesPage = purchaseService.getPurchasesAfterSync(lastSync, pageable)
        return ApiResponse.success(PageResponse.from(purchasesPage) { it.toResponse(it.purchaseItems) })
    }

    @GetMapping("/{purchaseId}")
    fun getPurchase(@PathVariable purchaseId: String): ApiResponse<PurchaseResponse> {
        val purchase = purchaseService.getPurchaseByUid(purchaseId)
            ?: throw NotFoundException("Purchase not found: $purchaseId")
        return ApiResponse.success(purchase.toResponse(purchase.purchaseItems))
    }
}
