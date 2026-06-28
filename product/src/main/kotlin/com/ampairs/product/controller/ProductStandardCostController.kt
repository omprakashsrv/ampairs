package com.ampairs.product.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.product.domain.dto.ProductStandardCostRequest
import com.ampairs.product.domain.dto.ProductStandardCostResponse
import com.ampairs.product.service.ProductStandardCostService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Effective-dated standard purchase cost — `/sync` resource (cost-side mirror of price-list items).
 * The mobile app pulls these to auto-fill purchase voucher line rates as of the voucher date.
 */
@RestController
@RequestMapping("/product/v1/standard-costs")
@Validated
class ProductStandardCostController(
    private val service: ProductStandardCostService,
) {

    @GetMapping("/sync")
    fun sync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String,
    ): ApiResponse<PageResponse<ProductStandardCostResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), jpaSort(sortBy)))
        return ApiResponse.success(PageResponse.from(service.getAfterSync(lastSync, pageable)))
    }

    @PostMapping("/sync")
    fun push(
        @RequestBody requests: List<@Valid ProductStandardCostRequest>,
    ): ApiResponse<List<ProductStandardCostResponse>> =
        ApiResponse.success(service.bulkUpsert(requests))

    private fun jpaSort(sortBy: String): String = when (sortBy) {
        "productId" -> "productId"
        "effectiveFrom" -> "effectiveFrom"
        "createdAt" -> "createdAt"
        else -> "updatedAt"
    }
}
