package com.ampairs.pricing.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.pricing.domain.dto.PriceListItemRequest
import com.ampairs.pricing.domain.dto.PriceListItemResponse
import com.ampairs.pricing.domain.dto.PriceListRequest
import com.ampairs.pricing.domain.dto.PriceListResponse
import com.ampairs.pricing.domain.dto.PriceResolutionRequest
import com.ampairs.pricing.domain.dto.PriceResolutionResponse
import com.ampairs.pricing.exception.PriceListNotFoundException
import com.ampairs.pricing.service.PriceListService
import com.ampairs.pricing.service.PricingResolutionService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/pricing/v1/price-lists")
@Validated
class PriceListController(
    private val priceListService: PriceListService,
    private val pricingResolutionService: PricingResolutionService,
) {

    // ── price lists /sync ───────────────────────────────────────────────────────────────
    @GetMapping("/sync")
    fun getListsSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<PriceListResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), jpaSort(sortBy)))
        return ApiResponse.success(PageResponse.from(priceListService.getListsAfterSync(lastSync, pageable)))
    }

    @PostMapping("/sync")
    fun bulkUpsertLists(
        @RequestBody requests: List<@Valid PriceListRequest>,
    ): ApiResponse<List<PriceListResponse>> =
        ApiResponse.success(priceListService.bulkUpsertLists(requests))

    // ── price-list items /sync ──────────────────────────────────────────────────────────
    @GetMapping("/items/sync")
    fun getItemsSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<PriceListItemResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), jpaSort(sortBy)))
        return ApiResponse.success(PageResponse.from(priceListService.getItemsAfterSync(lastSync, pageable)))
    }

    @PostMapping("/items/sync")
    fun bulkUpsertItems(
        @RequestBody requests: List<@Valid PriceListItemRequest>,
    ): ApiResponse<List<PriceListItemResponse>> =
        ApiResponse.success(priceListService.bulkUpsertItems(requests))

    // ── reads ───────────────────────────────────────────────────────────────────────────
    @GetMapping("/{uid}")
    fun getList(@PathVariable uid: String): ApiResponse<PriceListResponse> {
        val list = priceListService.findByUid(uid)
            ?: throw PriceListNotFoundException("Price list not found for uid: $uid")
        return ApiResponse.success(list)
    }

    @GetMapping("/{uid}/items")
    fun getListItems(@PathVariable uid: String): ApiResponse<List<PriceListItemResponse>> =
        ApiResponse.success(priceListService.itemsForList(uid))

    // ── resolution ───────────────────────────────────────────────────────────────────────
    @PostMapping("/resolve")
    fun resolve(@Valid @RequestBody request: PriceResolutionRequest): ApiResponse<PriceResolutionResponse> =
        ApiResponse.success(pricingResolutionService.resolve(request))

    private fun jpaSort(sortBy: String): String = when (sortBy) {
        "name" -> "name"
        "priority" -> "priority"
        "status" -> "status"
        "productId" -> "productId"
        "createdAt" -> "createdAt"
        "updatedAt" -> "updatedAt"
        else -> "updatedAt"
    }
}
