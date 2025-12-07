package com.ampairs.tax.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.tax.domain.dto.MasterTaxCodeDto
import com.ampairs.tax.domain.dto.PageResponse
import com.ampairs.tax.service.MasterTaxCodeService
import org.springframework.web.bind.annotation.*
import kotlin.math.min

@RestController
@RequestMapping("/api/v1/tax/master-code")
class MasterTaxCodeController(
    private val masterTaxCodeService: MasterTaxCodeService
) {

    @GetMapping("/search")
    fun searchMasterTaxCodes(
        @RequestParam query: String,
        @RequestParam countryCode: String,
        @RequestParam(required = false) codeType: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ApiResponse<PageResponse<MasterTaxCodeDto>> {

        // Validate page size (max 100 as per guide)
        val validSize = min(size, 100)

        val result = masterTaxCodeService.searchCodes(
            query = query,
            countryCode = countryCode.uppercase(),
            codeType = codeType,
            category = category,
            page = page,
            size = validSize
        )

        return ApiResponse.success(result)
    }

    @GetMapping("/popular")
    fun getPopularTaxCodes(
        @RequestParam countryCode: String,
        @RequestParam(required = false) industry: String?,
        @RequestParam(defaultValue = "20") limit: Int
    ): ApiResponse<List<MasterTaxCodeDto>> {

        val result = masterTaxCodeService.getPopularCodes(
            countryCode = countryCode.uppercase(),
            industry = industry,
            limit = limit
        )

        return ApiResponse.success(result)
    }
}
