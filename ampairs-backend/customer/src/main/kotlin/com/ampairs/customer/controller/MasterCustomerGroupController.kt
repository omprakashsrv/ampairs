package com.ampairs.customer.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.customer.domain.model.MasterCustomerGroup
import com.ampairs.customer.domain.service.MasterCustomerGroupService
import org.springframework.web.bind.annotation.*

/**
 * Controller for managing master customer groups catalog
 */
@RestController
@RequestMapping("/customer/v1/master-customer-groups")
class MasterCustomerGroupController(
    private val masterCustomerGroupService: MasterCustomerGroupService
) {

    /**
     * Get all active master customer groups
     */
    @GetMapping("")
    fun getAllCustomerGroups(): ApiResponse<List<MasterCustomerGroup>> {
        val customerGroups = masterCustomerGroupService.getAllActiveCustomerGroups()
        return ApiResponse.success(customerGroups)
    }

    /**
     * Get all customer groups ordered by priority
     */
    @GetMapping("/by-priority")
    fun getCustomerGroupsByPriority(): ApiResponse<List<MasterCustomerGroup>> {
        val customerGroups = masterCustomerGroupService.getCustomerGroupsByPriority()
        return ApiResponse.success(customerGroups)
    }

    /**
     * Search customer groups by keyword
     */
    @GetMapping("/search")
    fun searchCustomerGroups(@RequestParam("q") searchTerm: String): ApiResponse<List<MasterCustomerGroup>> {
        val customerGroups = masterCustomerGroupService.searchCustomerGroups(searchTerm)
        return ApiResponse.success(customerGroups)
    }

    /**
     * Get customer groups that have discount benefits
     */
    @GetMapping("/with-discount")
    fun getCustomerGroupsWithDiscount(): ApiResponse<List<MasterCustomerGroup>> {
        val customerGroups = masterCustomerGroupService.getCustomerGroupsWithDiscount()
        return ApiResponse.success(customerGroups)
    }

    /**
     * Get master customer group by code
     */
    @GetMapping("/{groupCode}")
    fun getCustomerGroupByCode(@PathVariable groupCode: String): ApiResponse<MasterCustomerGroup> {
        val customerGroup = masterCustomerGroupService.findByGroupCode(groupCode.uppercase())
            ?: return ApiResponse.error("Master customer group not found", "MASTER_CUSTOMER_GROUP_NOT_FOUND")

        return ApiResponse.success(customerGroup)
    }

    /**
     * Get master customer group statistics
     */
    @GetMapping("/statistics")
    fun getMasterCustomerGroupStatistics(): ApiResponse<Map<String, Any>> {
        val stats = masterCustomerGroupService.getMasterCustomerGroupStatistics()
        return ApiResponse.success(stats)
    }
}