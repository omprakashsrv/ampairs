package com.ampairs.customer.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.customer.domain.model.MasterCustomerType
import com.ampairs.customer.domain.service.MasterCustomerTypeService
import org.springframework.web.bind.annotation.*

/**
 * Controller for managing master customer types catalog
 */
@RestController
@RequestMapping("/customer/v1/master-customer-types")
class MasterCustomerTypeController(
    private val masterCustomerTypeService: MasterCustomerTypeService
) {

    /**
     * Get all active master customer types
     */
    @GetMapping("")
    fun getAllCustomerTypes(): ApiResponse<List<MasterCustomerType>> {
        val customerTypes = masterCustomerTypeService.getAllActiveCustomerTypes()
        return ApiResponse.success(customerTypes)
    }

    /**
     * Search customer types by keyword
     */
    @GetMapping("/search")
    fun searchCustomerTypes(@RequestParam("q") searchTerm: String): ApiResponse<List<MasterCustomerType>> {
        val customerTypes = masterCustomerTypeService.searchCustomerTypes(searchTerm)
        return ApiResponse.success(customerTypes)
    }

    /**
     * Get customer types that allow credit
     */
    @GetMapping("/with-credit")
    fun getCustomerTypesWithCredit(): ApiResponse<List<MasterCustomerType>> {
        val customerTypes = masterCustomerTypeService.getCustomerTypesWithCredit()
        return ApiResponse.success(customerTypes)
    }

    /**
     * Get master customer type by code
     */
    @GetMapping("/{typeCode}")
    fun getCustomerTypeByCode(@PathVariable typeCode: String): ApiResponse<MasterCustomerType> {
        val customerType = masterCustomerTypeService.findByTypeCode(typeCode.uppercase())
            ?: return ApiResponse.error("Master customer type not found", "MASTER_CUSTOMER_TYPE_NOT_FOUND")

        return ApiResponse.success(customerType)
    }

    /**
     * Get master customer type statistics
     */
    @GetMapping("/statistics")
    fun getMasterCustomerTypeStatistics(): ApiResponse<Map<String, Any>> {
        val stats = masterCustomerTypeService.getMasterCustomerTypeStatistics()
        return ApiResponse.success(stats)
    }
}