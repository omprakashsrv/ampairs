package com.ampairs.customer.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.customer.domain.dto.CustomerTypeCreateRequest
import com.ampairs.customer.domain.dto.CustomerTypeResponse
import com.ampairs.customer.domain.dto.CustomerTypeUpdateRequest
import com.ampairs.customer.domain.dto.asCustomerTypeResponse
import com.ampairs.customer.domain.dto.asCustomerTypeResponses
import com.ampairs.customer.domain.dto.toCustomerType
import com.ampairs.customer.domain.service.CustomerTypeService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
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
     * Get all active customer types for current workspace with pagination
     */
    @GetMapping("")
    fun getAllCustomerTypes(
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "20") size: Int,
        @RequestParam("sort_by", defaultValue = "displayOrder") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String
    ): ApiResponse<PageResponse<CustomerTypeResponse>> {
        // Use JPA property names for sorting
        val jpaPropertyName = when (sortBy) {
            "createdAt" -> "createdAt"
            "updatedAt" -> "updatedAt"
            "name" -> "name"
            "typeCode" -> "typeCode"
            "displayOrder" -> "displayOrder"
            "active" -> "active"
            else -> "displayOrder" // default fallback
        }

        val sort = Sort.by(Sort.Direction.fromString(sortDir), jpaPropertyName)
        val pageable = PageRequest.of(page, size, sort)

        val customerTypesPage = customerTypeService.getAllActiveCustomerTypes(pageable)
        return ApiResponse.success(PageResponse.from(customerTypesPage) { it.asCustomerTypeResponse() })
    }

    /**
     * Search customer types by keyword within current workspace
     */
    @GetMapping("/search")
    fun searchCustomerTypes(@RequestParam("q") searchTerm: String): ApiResponse<List<CustomerTypeResponse>> {
        val customerTypes = customerTypeService.searchCustomerTypes(searchTerm)
        return ApiResponse.success(customerTypes.asCustomerTypeResponses())
    }

    /**
     * Get customer types that allow credit within current workspace
     */
    @GetMapping("/with-credit")
    fun getCustomerTypesWithCredit(): ApiResponse<List<CustomerTypeResponse>> {
        val customerTypes = customerTypeService.getCustomerTypesWithCredit()
        return ApiResponse.success(customerTypes.asCustomerTypeResponses())
    }

    /**
     * Get customer type by code within current workspace
     */
    @GetMapping("/{typeCode}")
    fun getCustomerTypeByCode(@PathVariable typeCode: String): ApiResponse<CustomerTypeResponse> {
        val customerType = customerTypeService.findByTypeCode(typeCode.uppercase())
            ?: return ApiResponse.error("Customer type not found", "CUSTOMER_TYPE_NOT_FOUND")

        return ApiResponse.success(customerType.asCustomerTypeResponse())
    }

    /**
     * Create new customer type in current workspace
     */
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun createCustomerType(@RequestBody @Valid request: CustomerTypeCreateRequest): ApiResponse<CustomerTypeResponse> {
        val customerType = request.toCustomerType()
        val createdType = customerTypeService.createCustomerType(customerType)
        return ApiResponse.success(createdType.asCustomerTypeResponse())
    }

    /**
     * Update existing customer type
     */
    @PutMapping("/{typeCode}")
    fun updateCustomerType(
        @PathVariable typeCode: String,
        @RequestBody @Valid request: CustomerTypeUpdateRequest
    ): ApiResponse<CustomerTypeResponse> {
        val existingType = customerTypeService.findByTypeCode(typeCode.uppercase())
            ?: return ApiResponse.error("Customer type not found", "CUSTOMER_TYPE_NOT_FOUND")

        // Apply updates to existing entity
        request.name?.let { existingType.name = it }
        request.description?.let { existingType.description = it }
        request.displayOrder?.let { existingType.displayOrder = it }
        request.active?.let { existingType.active = it }
        request.defaultCreditLimit?.let { existingType.defaultCreditLimit = it }
        request.defaultCreditDays?.let { existingType.defaultCreditDays = it }
        request.metadata?.let { existingType.metadata = it }

        val updatedType = customerTypeService.updateCustomerType(typeCode.uppercase(), existingType)
            ?: return ApiResponse.error("Failed to update customer type", "UPDATE_FAILED")

        return ApiResponse.success(updatedType.asCustomerTypeResponse())
    }

    /**
     * Get customer type statistics for current workspace
     */
    @GetMapping("/statistics")
    fun getCustomerTypeStatistics(): ApiResponse<Map<String, Any>> {
        val stats = customerTypeService.getCustomerTypeStatistics()
        return ApiResponse.success(stats)
    }
}