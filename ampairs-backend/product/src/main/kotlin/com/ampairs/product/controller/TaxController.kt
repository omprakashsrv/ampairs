package com.ampairs.product.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.product.domain.dto.tax.*
import com.ampairs.product.domain.dto.tax.TaxCodeResponse as TaxDtoCodeResponse
import com.ampairs.product.service.TaxService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/product/v1/tax")
class TaxController(val taxService: TaxService) {

    @PostMapping("/tax_infos")
    fun updateTaxInfos(@RequestBody taxInfos: List<TaxInfoRequest>): ApiResponse<List<TaxInfoResponse>> {
        val result = taxService.updateTaxInfos(taxInfos.asDatabaseModel()).asResponse()
        return ApiResponse.success(result)
    }

    @GetMapping("/tax_infos")
    fun getTaxInfos(): ApiResponse<List<TaxInfoResponse>> {
        val result = taxService.getTaxInfos().asResponse()
        return ApiResponse.success(result)
    }

    @PostMapping("/tax_codes")
    fun updateTaxCodes(@RequestBody taxCodes: List<TaxCodeRequest>): ApiResponse<List<TaxDtoCodeResponse>> {
        val result = taxService.updateTaxCodes(taxCodes.asDatabaseModel()).asResponse()
        return ApiResponse.success(result)
    }

    @GetMapping("/tax_codes")
    fun getTaxCodes(): ApiResponse<List<TaxDtoCodeResponse>> {
        val result = taxService.getTaxCodes().asResponse()
        return ApiResponse.success(result)
    }


}