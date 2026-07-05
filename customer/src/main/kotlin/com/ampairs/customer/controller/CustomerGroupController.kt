package com.ampairs.customer.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.customer.domain.dto.CustomerGroupResponse
import com.ampairs.customer.domain.dto.CustomerGroupUpdateRequest
import com.ampairs.customer.domain.dto.asCustomerGroupResponse
import com.ampairs.customer.domain.dto.asCustomerGroupResponses
import com.ampairs.customer.domain.dto.toCustomerGroups
import com.ampairs.customer.domain.service.CustomerGroupService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*

/**
 * Controller for managing workspace customer groups
 */
@RestController
@RequestMapping("/customer/v1/groups")
class CustomerGroupController(
    private val customerGroupService: CustomerGroupService
) {

    /**
     * Incremental sync feed: customer groups updated at/after last_sync, INCLUDING
     * inactive (soft-deleted) rows, ordered by updatedAt ASC, paginated. Lets mobile
     * clients do batched incremental pulls and detect deletions.
     */
    @GetMapping("/sync")
    fun getCustomerGroupsSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String
    ): ApiResponse<PageResponse<CustomerGroupResponse>> {
        val sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy)
        val pageable = PageRequest.of(page, size, sort)
        val groupsPage = customerGroupService.getCustomerGroupsAfterSync(lastSync, pageable)
        return ApiResponse.success(PageResponse.from(groupsPage) { it.asCustomerGroupResponse() })
    }

    /**
     * Bulk upsert (push) endpoint of the unified sync contract. The body MAY contain
     * soft-deleted rows (active = false) so deletions propagate in-band.
     */
    @PostMapping("/sync")
    fun bulkUpsertCustomerGroups(
        @RequestBody request: List<@Valid CustomerGroupUpdateRequest>
    ): ApiResponse<List<CustomerGroupResponse>> {
        val groups = request.toCustomerGroups()
        val result = customerGroupService.bulkUpsertCustomerGroups(groups)
        return ApiResponse.success(result.asCustomerGroupResponses())
    }
}
