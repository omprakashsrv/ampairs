package com.ampairs.customer.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.customer.domain.model.CustomerGroup
import com.ampairs.customer.domain.service.CustomerGroupService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/**
 * Controller for managing workspace customer groups
 */
@RestController
@RequestMapping("/customer/v1/customer-groups")
class CustomerGroupController(
    private val customerGroupService: CustomerGroupService
) {

    /**
     * Get all active customer groups for current workspace
     */
    @GetMapping("")
    fun getAllCustomerGroups(): ApiResponse<List<CustomerGroup>> {
        val customerGroups = customerGroupService.getAllActiveCustomerGroups()
        return ApiResponse.success(customerGroups)
    }

    /**
     * Get all customer groups ordered by priority within current workspace
     */
    @GetMapping("/by-priority")
    fun getCustomerGroupsByPriority(): ApiResponse<List<CustomerGroup>> {
        val customerGroups = customerGroupService.getCustomerGroupsByPriority()
        return ApiResponse.success(customerGroups)
    }

    /**
     * Search customer groups by keyword within current workspace
     */
    @GetMapping("/search")
    fun searchCustomerGroups(@RequestParam("q") searchTerm: String): ApiResponse<List<CustomerGroup>> {
        val customerGroups = customerGroupService.searchCustomerGroups(searchTerm)
        return ApiResponse.success(customerGroups)
    }

    /**
     * Get customer groups that have discount benefits within current workspace
     */
    @GetMapping("/with-discount")
    fun getCustomerGroupsWithDiscount(): ApiResponse<List<CustomerGroup>> {
        val customerGroups = customerGroupService.getCustomerGroupsWithDiscount()
        return ApiResponse.success(customerGroups)
    }

    /**
     * Get customer group by code within current workspace
     */
    @GetMapping("/{groupCode}")
    fun getCustomerGroupByCode(@PathVariable groupCode: String): ApiResponse<CustomerGroup> {
        val customerGroup = customerGroupService.findByGroupCode(groupCode.uppercase())
            ?: return ApiResponse.error("Customer group not found", "CUSTOMER_GROUP_NOT_FOUND")

        return ApiResponse.success(customerGroup)
    }

    /**
     * Create new customer group in current workspace
     */
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun createCustomerGroup(@RequestBody @Valid customerGroup: CustomerGroup): ApiResponse<CustomerGroup> {
        val createdGroup = customerGroupService.createCustomerGroup(customerGroup)
        return ApiResponse.success(createdGroup)
    }

    /**
     * Update existing customer group
     */
    @PutMapping("/{groupCode}")
    fun updateCustomerGroup(
        @PathVariable groupCode: String,
        @RequestBody @Valid updates: CustomerGroup
    ): ApiResponse<CustomerGroup> {
        val updatedGroup = customerGroupService.updateCustomerGroup(groupCode.uppercase(), updates)
            ?: return ApiResponse.error("Customer group not found", "CUSTOMER_GROUP_NOT_FOUND")

        return ApiResponse.success(updatedGroup)
    }

    /**
     * Get customer group statistics for current workspace
     */
    @GetMapping("/statistics")
    fun getCustomerGroupStatistics(): ApiResponse<Map<String, Any>> {
        val stats = customerGroupService.getCustomerGroupStatistics()
        return ApiResponse.success(stats)
    }
}