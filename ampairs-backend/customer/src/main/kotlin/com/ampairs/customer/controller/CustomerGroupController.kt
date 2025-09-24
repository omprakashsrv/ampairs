package com.ampairs.customer.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.customer.domain.dto.CustomerGroupCreateRequest
import com.ampairs.customer.domain.dto.CustomerGroupResponse
import com.ampairs.customer.domain.dto.CustomerGroupUpdateRequest
import com.ampairs.customer.domain.dto.asCustomerGroupResponse
import com.ampairs.customer.domain.dto.asCustomerGroupResponses
import com.ampairs.customer.domain.dto.toCustomerGroup
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
    fun getAllCustomerGroups(): ApiResponse<List<CustomerGroupResponse>> {
        val customerGroups = customerGroupService.getAllActiveCustomerGroups()
        return ApiResponse.success(customerGroups.asCustomerGroupResponses())
    }

    /**
     * Get all customer groups ordered by priority within current workspace
     */
    @GetMapping("/by-priority")
    fun getCustomerGroupsByPriority(): ApiResponse<List<CustomerGroupResponse>> {
        val customerGroups = customerGroupService.getCustomerGroupsByPriority()
        return ApiResponse.success(customerGroups.asCustomerGroupResponses())
    }

    /**
     * Search customer groups by keyword within current workspace
     */
    @GetMapping("/search")
    fun searchCustomerGroups(@RequestParam("q") searchTerm: String): ApiResponse<List<CustomerGroupResponse>> {
        val customerGroups = customerGroupService.searchCustomerGroups(searchTerm)
        return ApiResponse.success(customerGroups.asCustomerGroupResponses())
    }

    /**
     * Get customer groups that have discount benefits within current workspace
     */
    @GetMapping("/with-discount")
    fun getCustomerGroupsWithDiscount(): ApiResponse<List<CustomerGroupResponse>> {
        val customerGroups = customerGroupService.getCustomerGroupsWithDiscount()
        return ApiResponse.success(customerGroups.asCustomerGroupResponses())
    }

    /**
     * Get customer group by code within current workspace
     */
    @GetMapping("/{groupCode}")
    fun getCustomerGroupByCode(@PathVariable groupCode: String): ApiResponse<CustomerGroupResponse> {
        val customerGroup = customerGroupService.findByGroupCode(groupCode.uppercase())
            ?: return ApiResponse.error("Customer group not found", "CUSTOMER_GROUP_NOT_FOUND")

        return ApiResponse.success(customerGroup.asCustomerGroupResponse())
    }

    /**
     * Create new customer group in current workspace
     */
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun createCustomerGroup(@RequestBody @Valid request: CustomerGroupCreateRequest): ApiResponse<CustomerGroupResponse> {
        val customerGroup = request.toCustomerGroup()
        val createdGroup = customerGroupService.createCustomerGroup(customerGroup)
        return ApiResponse.success(createdGroup.asCustomerGroupResponse())
    }

    /**
     * Update existing customer group
     */
    @PutMapping("/{groupCode}")
    fun updateCustomerGroup(
        @PathVariable groupCode: String,
        @RequestBody @Valid request: CustomerGroupUpdateRequest
    ): ApiResponse<CustomerGroupResponse> {
        val existingGroup = customerGroupService.findByGroupCode(groupCode.uppercase())
            ?: return ApiResponse.error("Customer group not found", "CUSTOMER_GROUP_NOT_FOUND")

        // Apply updates to existing entity
        request.name?.let { existingGroup.name = it }
        request.description?.let { existingGroup.description = it }
        request.displayOrder?.let { existingGroup.displayOrder = it }
        request.active?.let { existingGroup.active = it }
        request.defaultDiscountPercentage?.let { existingGroup.defaultDiscountPercentage = it }
        request.priorityLevel?.let { existingGroup.priorityLevel = it }
        request.metadata?.let { existingGroup.metadata = it }

        val updatedGroup = customerGroupService.updateCustomerGroup(groupCode.uppercase(), existingGroup)
            ?: return ApiResponse.error("Failed to update customer group", "UPDATE_FAILED")

        return ApiResponse.success(updatedGroup.asCustomerGroupResponse())
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