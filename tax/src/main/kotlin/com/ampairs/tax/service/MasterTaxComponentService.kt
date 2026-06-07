package com.ampairs.tax.service

import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.exception.NotFoundException
import com.ampairs.tax.domain.dto.MasterTaxComponentDto
import com.ampairs.tax.domain.dto.TaxComponentTypeDto
import com.ampairs.tax.domain.dto.asMasterComponentDtos
import com.ampairs.tax.domain.dto.asDto
import com.ampairs.tax.domain.model.MasterTaxComponent
import com.ampairs.tax.repository.MasterTaxComponentRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MasterTaxComponentService(
    private val masterTaxComponentRepository: MasterTaxComponentRepository
) {

    fun getAllComponents(page: Int, size: Int): PageResponse<MasterTaxComponentDto> {
        val pageable: Pageable = PageRequest.of(page, size)
        val result = masterTaxComponentRepository.findByIsActiveTrue(pageable)
        return PageResponse.from(result) { it.asDto() }
    }

    fun searchComponents(
        componentTypeId: String?,
        jurisdiction: String?,
        page: Int,
        size: Int
    ): PageResponse<MasterTaxComponentDto> {
        val pageable: Pageable = PageRequest.of(page, size)
        val result = masterTaxComponentRepository.searchComponents(
            componentTypeId = componentTypeId,
            jurisdiction = jurisdiction,
            pageable = pageable
        )
        return PageResponse.from(result) { it.asDto() }
    }

    fun getComponentById(id: String): MasterTaxComponentDto {
        val component = masterTaxComponentRepository.findByUid(id)
            ?: throw NotFoundException("Master tax component not found: $id")
        return component.asDto()
    }

    fun findComponentsByType(componentTypeId: String): List<MasterTaxComponentDto> {
        val pageable: Pageable = PageRequest.of(0, 100)
        val result = masterTaxComponentRepository.searchComponents(
            componentTypeId = componentTypeId,
            jurisdiction = null,
            pageable = pageable
        )
        return result.content.asMasterComponentDtos()
    }

    fun getComponentTypesForCountry(countryCode: String): List<TaxComponentTypeDto> = when (countryCode.uppercase()) {
        "IN" -> listOf(
            TaxComponentTypeDto("CGST", "CGST", "Central GST", "IN", "GST", false, "PERCENTAGE", "Central Goods and Services Tax"),
            TaxComponentTypeDto("SGST", "SGST", "State GST", "IN", "GST", false, "PERCENTAGE", "State Goods and Services Tax"),
            TaxComponentTypeDto("IGST", "IGST", "Integrated GST", "IN", "GST", false, "PERCENTAGE", "Integrated Goods and Services Tax"),
            TaxComponentTypeDto("UTGST", "UTGST", "Union Territory GST", "IN", "GST", false, "PERCENTAGE", "Union Territory Goods and Services Tax"),
            TaxComponentTypeDto("CESS", "CESS", "Cess", "IN", "CESS", false, "PERCENTAGE", "Additional cess on specific goods")
        )
        "US" -> listOf(
            TaxComponentTypeDto("SALES_TAX", "SALES_TAX", "Sales Tax", "US", "SALES_TAX", false, "PERCENTAGE", "State and Local Sales Tax")
        )
        "GB" -> listOf(
            TaxComponentTypeDto("VAT", "VAT", "Value Added Tax", "GB", "VAT", false, "PERCENTAGE", "UK Value Added Tax")
        )
        else -> emptyList()
    }
}
