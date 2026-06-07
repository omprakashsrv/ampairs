package com.ampairs.tax.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.tax.domain.dto.MasterTaxCodeDto
import com.ampairs.tax.service.MasterTaxCodeService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/tax/v1/master-codes")
class MasterTaxCodeController(
    private val masterTaxCodeService: MasterTaxCodeService
) {

    @GetMapping("/search")
    fun search(
        @RequestParam q: String,
        @RequestParam(defaultValue = "IN") countryCode: String,
        @RequestParam(required = false) codeType: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<PageResponse<MasterTaxCodeDto>> {
        val result = masterTaxCodeService.search(
            query = q,
            countryCode = countryCode,
            codeType = codeType,
            category = category,
            page = page,
            size = size
        )
        return ApiResponse.success(result)
    }

    @GetMapping("/popular")
    fun getPopular(
        @RequestParam(defaultValue = "IN") countryCode: String,
        @RequestParam(required = false) industry: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<PageResponse<MasterTaxCodeDto>> {
        val result = masterTaxCodeService.getPopular(
            countryCode = countryCode,
            industry = industry,
            page = page,
            size = size
        )
        return ApiResponse.success(result)
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: String): ApiResponse<MasterTaxCodeDto> {
        return ApiResponse.success(masterTaxCodeService.getById(id))
    }
}
