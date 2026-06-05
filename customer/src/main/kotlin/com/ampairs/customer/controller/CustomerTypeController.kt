package com.ampairs.customer.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.customer.domain.dto.CustomerTypeResponse
import com.ampairs.customer.domain.dto.CustomerTypeUpdateRequest
import com.ampairs.customer.domain.dto.asCustomerTypeResponse
import com.ampairs.customer.domain.dto.asCustomerTypeResponses
import com.ampairs.customer.domain.dto.toCustomerTypes
import com.ampairs.customer.domain.service.CustomerTypeService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*

/**
 * Controller for managing workspace customer types
 */
@RestController
@RequestMapping("/customer/v1/types")
class CustomerTypeController(
    private val customerTypeService: CustomerTypeService
) {

    /**
     * Incremental sync feed: customer types updated at/after last_sync, INCLUDING
     * inactive (soft-deleted) rows, ordered by updatedAt ASC, paginated. Lets mobile
     * clients do batched incremental pulls and detect deletions.
     */
    @GetMapping("/sync")
    fun getCustomerTypesSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int
    ): ApiResponse<PageResponse<CustomerTypeResponse>> {
        val sort = Sort.by(Sort.Direction.ASC, "updatedAt")
        val pageable = PageRequest.of(page, size, sort)
        val typesPage = customerTypeService.getCustomerTypesAfterSync(lastSync, pageable)
        return ApiResponse.success(PageResponse.from(typesPage) { it.asCustomerTypeResponse() })
    }

    @PostMapping("")
    fun bulkUpsertCustomerTypes(
        @RequestBody @Valid request: List<CustomerTypeUpdateRequest>
    ): ApiResponse<List<CustomerTypeResponse>> {
        val types = request.toCustomerTypes()
        val result = customerTypeService.bulkUpsertCustomerTypes(types)
        return ApiResponse.success(result.asCustomerTypeResponses())
    }
}
