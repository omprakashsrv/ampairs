package com.ampairs.product.controller

import com.ampairs.product.domain.dto.tax.*
import com.ampairs.product.domain.dto.tax.TaxCodeResponse as TaxDtoCodeResponse
import com.ampairs.product.service.TaxService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/product/v1/tax")
class TaxController(val taxService: TaxService) {

    @PostMapping("/tax_infos")
    fun updateTaxInfos(@RequestBody taxInfos: List<TaxInfoRequest>): List<TaxInfoResponse> {
        return taxService.updateTaxInfos(taxInfos.asDatabaseModel()).asResponse()
    }

    @GetMapping("/tax_infos")
    fun getTaxInfos(): List<TaxInfoResponse> {
        return taxService.getTaxInfos().asResponse()
    }

    @PostMapping("/tax_codes")
    fun updateTaxCodes(@RequestBody taxCodes: List<TaxCodeRequest>): List<TaxDtoCodeResponse> {
        return taxService.updateTaxCodes(taxCodes.asDatabaseModel()).asResponse()
    }

    @GetMapping("/tax_codes")
    fun getTaxCodes(): List<TaxDtoCodeResponse> {
        return taxService.getTaxCodes().asResponse()
    }


}