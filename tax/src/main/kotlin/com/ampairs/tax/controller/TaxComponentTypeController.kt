package com.ampairs.tax.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.tax.domain.dto.TaxComponentTypeDto
import com.ampairs.tax.service.TaxComponentService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tax/component-type")
class TaxComponentTypeController(
    private val taxComponentService: TaxComponentService
) {

    @GetMapping("/{countryCode}")
    fun getComponentTypes(
        @PathVariable countryCode: String
    ): ApiResponse<List<TaxComponentTypeDto>> {
        // Get distinct component types for the country
        // For now, return hardcoded list for India (can be made dynamic later)
        val types = getComponentTypesForCountry(countryCode.uppercase())
        return ApiResponse.success(types)
    }

    private fun getComponentTypesForCountry(countryCode: String): List<TaxComponentTypeDto> {
        return when (countryCode) {
            "IN" -> listOf(
                TaxComponentTypeDto(
                    id = "CGST",
                    name = "CGST",
                    displayName = "Central GST",
                    countryCode = "IN",
                    taxType = "GST",
                    isCompound = false,
                    calculationMethod = "PERCENTAGE",
                    description = "Central Goods and Services Tax"
                ),
                TaxComponentTypeDto(
                    id = "SGST",
                    name = "SGST",
                    displayName = "State GST",
                    countryCode = "IN",
                    taxType = "GST",
                    isCompound = false,
                    calculationMethod = "PERCENTAGE",
                    description = "State Goods and Services Tax"
                ),
                TaxComponentTypeDto(
                    id = "IGST",
                    name = "IGST",
                    displayName = "Integrated GST",
                    countryCode = "IN",
                    taxType = "GST",
                    isCompound = false,
                    calculationMethod = "PERCENTAGE",
                    description = "Integrated Goods and Services Tax"
                ),
                TaxComponentTypeDto(
                    id = "UTGST",
                    name = "UTGST",
                    displayName = "Union Territory GST",
                    countryCode = "IN",
                    taxType = "GST",
                    isCompound = false,
                    calculationMethod = "PERCENTAGE",
                    description = "Union Territory Goods and Services Tax"
                ),
                TaxComponentTypeDto(
                    id = "CESS",
                    name = "CESS",
                    displayName = "Cess",
                    countryCode = "IN",
                    taxType = "CESS",
                    isCompound = false,
                    calculationMethod = "PERCENTAGE",
                    description = "Additional cess on specific goods"
                )
            )
            "US" -> listOf(
                TaxComponentTypeDto(
                    id = "SALES_TAX",
                    name = "SALES_TAX",
                    displayName = "Sales Tax",
                    countryCode = "US",
                    taxType = "SALES_TAX",
                    isCompound = false,
                    calculationMethod = "PERCENTAGE",
                    description = "State and Local Sales Tax"
                )
            )
            "GB" -> listOf(
                TaxComponentTypeDto(
                    id = "VAT",
                    name = "VAT",
                    displayName = "Value Added Tax",
                    countryCode = "GB",
                    taxType = "VAT",
                    isCompound = false,
                    calculationMethod = "PERCENTAGE",
                    description = "UK Value Added Tax"
                )
            )
            else -> emptyList()
        }
    }
}
