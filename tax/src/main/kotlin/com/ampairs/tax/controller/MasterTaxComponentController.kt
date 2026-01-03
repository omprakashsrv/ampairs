package com.ampairs.tax.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.tax.domain.dto.MasterTaxComponentDto
import com.ampairs.tax.service.MasterTaxComponentService
import org.springframework.web.bind.annotation.*
import kotlin.math.min

@RestController
@RequestMapping("/tax/v1/master-component")
class MasterTaxComponentController(
    private val masterTaxComponentService: MasterTaxComponentService
) {

    @GetMapping
    fun getAllComponents(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ApiResponse<PageResponse<MasterTaxComponentDto>> {
        // Validate page size (max 100 as per guide)
        val validSize = min(size, 100)

        val result = masterTaxComponentService.getAllComponents(
            page = page,
            size = validSize
        )

        return ApiResponse.success(result)
    }

    @GetMapping("/search")
    fun searchComponents(
        @RequestParam(required = false) componentTypeId: String?,
        @RequestParam(required = false) jurisdiction: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ApiResponse<PageResponse<MasterTaxComponentDto>> {
        // Validate page size (max 100 as per guide)
        val validSize = min(size, 100)

        val result = masterTaxComponentService.searchComponents(
            componentTypeId = componentTypeId,
            jurisdiction = jurisdiction,
            page = page,
            size = validSize
        )

        return ApiResponse.success(result)
    }

    @GetMapping("/{id}")
    fun getComponentById(
        @PathVariable id: String
    ): ApiResponse<MasterTaxComponentDto> {
        val result = masterTaxComponentService.getComponentById(id)
        return ApiResponse.success(result)
    }

    @GetMapping("/by-type/{componentTypeId}")
    fun getComponentsByType(
        @PathVariable componentTypeId: String
    ): ApiResponse<List<MasterTaxComponentDto>> {
        val result = masterTaxComponentService.findComponentsByType(componentTypeId)
        return ApiResponse.success(result)
    }
}
