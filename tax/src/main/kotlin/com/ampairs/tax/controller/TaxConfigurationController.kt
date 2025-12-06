package com.ampairs.tax.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.tax.domain.dto.TaxConfigurationDto
import com.ampairs.tax.domain.dto.UpdateTaxConfigurationRequest
import com.ampairs.tax.service.TaxConfigurationServiceV2
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tax/configuration")
class TaxConfigurationController(
    private val taxConfigurationService: TaxConfigurationServiceV2
) {

    @GetMapping
    fun getConfiguration(): ApiResponse<TaxConfigurationDto> {
        // Multi-tenancy via @TenantId handles workspace scoping automatically
        val config = taxConfigurationService.getConfiguration()
        return ApiResponse.success(config)
    }

    @PutMapping
    fun updateConfiguration(
        @RequestBody request: UpdateTaxConfigurationRequest
    ): ApiResponse<TaxConfigurationDto> {
        // Multi-tenancy via @TenantId handles workspace scoping automatically
        val config = taxConfigurationService.updateConfiguration(request)
        return ApiResponse.success(config)
    }
}
