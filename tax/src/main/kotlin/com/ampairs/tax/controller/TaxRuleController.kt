package com.ampairs.tax.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.tax.domain.dto.PageResponse
import com.ampairs.tax.domain.dto.TaxRuleDto
import com.ampairs.tax.service.TaxRuleService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tax/rule")
class TaxRuleController(
    private val taxRuleService: TaxRuleService
) {

    @GetMapping
    fun getTaxRules(
        @RequestParam(required = false) modifiedAfter: Long?,
        @RequestParam(required = false) taxCode: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "1000") size: Int
    ): ApiResponse<PageResponse<TaxRuleDto>> {
        // Multi-tenancy via @TenantId handles workspace scoping automatically
        val result = taxRuleService.getTaxRules(
            modifiedAfter = modifiedAfter,
            taxCode = taxCode,
            page = page,
            size = size
        )
        return ApiResponse.success(result)
    }

    @GetMapping("/tax-code/{taxCodeId}")
    fun getTaxRulesByTaxCode(
        @PathVariable taxCodeId: String
    ): ApiResponse<List<TaxRuleDto>> {
        // Multi-tenancy via @TenantId handles workspace scoping automatically
        val rules = taxRuleService.findByTaxCodeId(taxCodeId)
        return ApiResponse.success(rules)
    }
}
