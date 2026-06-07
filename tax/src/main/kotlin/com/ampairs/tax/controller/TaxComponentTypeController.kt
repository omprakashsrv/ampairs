package com.ampairs.tax.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.tax.domain.dto.TaxComponentTypeDto
import com.ampairs.tax.service.MasterTaxComponentService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/tax/v1/component-types")
class TaxComponentTypeController(
    private val masterTaxComponentService: MasterTaxComponentService
) {

    @GetMapping("/{countryCode}")
    fun getComponentTypes(
        @PathVariable countryCode: String
    ): ApiResponse<List<TaxComponentTypeDto>> {
        return ApiResponse.success(masterTaxComponentService.getComponentTypesForCountry(countryCode))
    }
}
