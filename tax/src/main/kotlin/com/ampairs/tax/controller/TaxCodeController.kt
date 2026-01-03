package com.ampairs.tax.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.tax.domain.dto.*
import com.ampairs.tax.service.TaxCodeService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/tax/v1/code")
class TaxCodeController(
    private val taxCodeService: TaxCodeService
) {

    @PostMapping("/subscribe")
    fun subscribeToTaxCode(
        @Valid @RequestBody request: SubscribeTaxCodeRequest
    ): ApiResponse<TaxCodeDto> {
        // Multi-tenancy via @TenantId handles workspace scoping automatically
        val taxCode = taxCodeService.subscribe(request)
        return ApiResponse.success(taxCode)
    }

    @GetMapping
    fun getTaxCodes(
        @RequestParam(required = false) modifiedAfter: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "1000") size: Int
    ): ApiResponse<PageResponse<TaxCodeDto>> {
        // Multi-tenancy via @TenantId handles workspace scoping automatically
        val result = taxCodeService.getTaxCodes(
            modifiedAfter = modifiedAfter,
            page = page,
            size = size
        )
        return ApiResponse.success(result)
    }

    @GetMapping("/favorites")
    fun getFavorites(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int
    ): ApiResponse<PageResponse<TaxCodeDto>> {
        // Multi-tenancy via @TenantId handles workspace scoping automatically
        val result = taxCodeService.getFavorites(page, size)
        return ApiResponse.success(result)
    }

    @DeleteMapping("/{taxCodeId}")
    fun unsubscribeFromTaxCode(
        @PathVariable taxCodeId: String
    ): ApiResponse<Unit> {
        // Multi-tenancy via @TenantId handles workspace scoping automatically
        taxCodeService.unsubscribe(taxCodeId)
        return ApiResponse.success(Unit)
    }

    @PatchMapping("/{taxCodeId}")
    fun updateTaxCodeConfiguration(
        @PathVariable taxCodeId: String,
        @Valid @RequestBody request: UpdateTaxCodeRequest
    ): ApiResponse<TaxCodeDto> {
        // Multi-tenancy via @TenantId handles workspace scoping automatically
        val taxCode = taxCodeService.updateConfiguration(taxCodeId, request)
        return ApiResponse.success(taxCode)
    }

    @PostMapping("/{taxCodeId}/usage")
    fun incrementUsageCount(
        @PathVariable taxCodeId: String
    ): ApiResponse<Unit> {
        // Multi-tenancy via @TenantId handles workspace scoping automatically
        taxCodeService.incrementUsage(taxCodeId)
        return ApiResponse.success(Unit)
    }

    @PostMapping("/bulk-subscribe")
    fun bulkSubscribeTaxCodes(
        @Valid @RequestBody request: BulkSubscribeTaxCodesRequest
    ): ApiResponse<BulkSubscribeResultDto> {
        // Multi-tenancy via @TenantId handles workspace scoping automatically
        val result = taxCodeService.bulkSubscribe(request)
        return ApiResponse.success(result)
    }

    @GetMapping("/{taxCodeId}")
    fun getTaxCodeById(
        @PathVariable taxCodeId: String
    ): ApiResponse<TaxCodeDto> {
        // Multi-tenancy via @TenantId handles workspace scoping automatically
        val taxCode = taxCodeService.getById(taxCodeId)
        return ApiResponse.success(taxCode)
    }

    @PostMapping("/{taxCodeId}/favorite")
    fun toggleFavorite(
        @PathVariable taxCodeId: String,
        @Valid @RequestBody request: Map<String, Boolean>
    ): ApiResponse<TaxCodeDto> {
        // Multi-tenancy via @TenantId handles workspace scoping automatically
        val isFavorite = request["isFavorite"] ?: false
        val taxCode = taxCodeService.setFavorite(taxCodeId, isFavorite)
        return ApiResponse.success(taxCode)
    }
}
