package com.ampairs.tax.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.tax.domain.dto.WorkspaceTaxComponentDto
import com.ampairs.tax.service.TaxComponentService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tax/v1/components")
class TaxComponentController(
    private val taxComponentService: TaxComponentService
) {

    @GetMapping
    fun getComponents(
        @RequestParam(required = false) modifiedAfter: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "1000") size: Int
    ): ApiResponse<PageResponse<WorkspaceTaxComponentDto>> {
        // Multi-tenancy via @TenantId handles workspace scoping automatically
        val result = taxComponentService.getWorkspaceComponents(
            modifiedAfter = modifiedAfter,
            page = page,
            size = size
        )
        return ApiResponse.success(result)
    }
}
