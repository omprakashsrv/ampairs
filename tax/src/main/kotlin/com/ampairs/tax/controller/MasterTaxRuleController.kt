package com.ampairs.tax.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.tax.domain.dto.MasterTaxRuleDto
import com.ampairs.tax.service.MasterTaxRuleService
import org.springframework.web.bind.annotation.*
import kotlin.math.min

@RestController
@RequestMapping("/tax/v1/master-rule")
class MasterTaxRuleController(
    private val masterTaxRuleService: MasterTaxRuleService
) {

    @GetMapping
    fun getAllRules(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ApiResponse<PageResponse<MasterTaxRuleDto>> {
        // Validate page size (max 100 as per guide)
        val validSize = min(size, 100)

        val result = masterTaxRuleService.getAllRules(
            page = page,
            size = validSize
        )

        return ApiResponse.success(result)
    }

    @GetMapping("/search")
    fun searchRules(
        @RequestParam countryCode: String,
        @RequestParam(required = false) taxCodeType: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ApiResponse<PageResponse<MasterTaxRuleDto>> {
        // Validate page size (max 100 as per guide)
        val validSize = min(size, 100)

        val result = masterTaxRuleService.searchRules(
            countryCode = countryCode.uppercase(),
            taxCodeType = taxCodeType,
            page = page,
            size = validSize
        )

        return ApiResponse.success(result)
    }

    @GetMapping("/{id}")
    fun getRuleById(
        @PathVariable id: String
    ): ApiResponse<MasterTaxRuleDto> {
        val result = masterTaxRuleService.getRuleById(id)
        return ApiResponse.success(result)
    }

    @GetMapping("/by-master-code/{masterTaxCodeId}")
    fun getRulesByMasterTaxCode(
        @PathVariable masterTaxCodeId: String
    ): ApiResponse<List<MasterTaxRuleDto>> {
        val result = masterTaxRuleService.findRulesByMasterTaxCode(masterTaxCodeId)
        return ApiResponse.success(result)
    }
}
