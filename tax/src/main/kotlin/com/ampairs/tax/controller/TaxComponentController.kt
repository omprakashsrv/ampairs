package com.ampairs.tax.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.tax.domain.dto.PageResponse
import com.ampairs.tax.domain.dto.TaxComponentDto
import com.ampairs.tax.service.TaxComponentService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tax/component")
class TaxComponentController(
    private val taxComponentService: TaxComponentService
) {

    @GetMapping
    fun getTaxComponents(
        @RequestParam(required = false) modifiedAfter: Long?,
        @RequestParam(required = false) taxType: String?,
        @RequestParam(required = false) jurisdiction: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "1000") size: Int
    ): ApiResponse<PageResponse<TaxComponentDto>> {
        // Multi-tenancy via @TenantId handles workspace scoping automatically
        val result = taxComponentService.getTaxComponents(
            modifiedAfter = modifiedAfter,
            taxType = taxType,
            jurisdiction = jurisdiction,
            page = page,
            size = size
        )
        return ApiResponse.success(result)
    }
}
