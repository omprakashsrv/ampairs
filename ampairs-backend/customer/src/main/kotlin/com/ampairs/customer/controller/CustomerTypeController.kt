package com.ampairs.customer.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.customer.domain.model.CustomerType
import com.ampairs.customer.domain.service.CustomerTypeService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/**
 * Controller for managing workspace customer types
 */
@RestController
@RequestMapping("/customer/v1/customer-types")
class CustomerTypeController(
    private val customerTypeService: CustomerTypeService
) {

    /**
     * Get all active customer types for current workspace
     */
    @GetMapping("")
    fun getAllCustomerTypes(): ApiResponse<List<CustomerType>> {
        val customerTypes = customerTypeService.getAllActiveCustomerTypes()
        return ApiResponse.success(customerTypes)
    }

    /**
     * Search customer types by keyword within current workspace
     */
    @GetMapping("/search")
    fun searchCustomerTypes(@RequestParam("q") searchTerm: String): ApiResponse<List<CustomerType>> {
        val customerTypes = customerTypeService.searchCustomerTypes(searchTerm)
        return ApiResponse.success(customerTypes)
    }

    /**
     * Get customer types that allow credit within current workspace
     */
    @GetMapping("/with-credit")
    fun getCustomerTypesWithCredit(): ApiResponse<List<CustomerType>> {
        val customerTypes = customerTypeService.getCustomerTypesWithCredit()
        return ApiResponse.success(customerTypes)
    }

    /**
     * Get customer type by code within current workspace
     */
    @GetMapping("/{typeCode}")
    fun getCustomerTypeByCode(@PathVariable typeCode: String): ApiResponse<CustomerType> {
        val customerType = customerTypeService.findByTypeCode(typeCode.uppercase())
            ?: return ApiResponse.error("Customer type not found", "CUSTOMER_TYPE_NOT_FOUND")

        return ApiResponse.success(customerType)
    }

    /**
     * Create new customer type in current workspace
     */
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun createCustomerType(@RequestBody @Valid customerType: CustomerType): ApiResponse<CustomerType> {
        val createdType = customerTypeService.createCustomerType(customerType)
        return ApiResponse.success(createdType)
    }

    /**
     * Update existing customer type
     */
    @PutMapping("/{typeCode}")
    fun updateCustomerType(
        @PathVariable typeCode: String,
        @RequestBody @Valid updates: CustomerType
    ): ApiResponse<CustomerType> {
        val updatedType = customerTypeService.updateCustomerType(typeCode.uppercase(), updates)
            ?: return ApiResponse.error("Customer type not found", "CUSTOMER_TYPE_NOT_FOUND")

        return ApiResponse.success(updatedType)
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