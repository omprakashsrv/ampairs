package com.ampairs.customer.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.exception.NotFoundException
import com.ampairs.customer.domain.dto.*
import com.ampairs.customer.domain.service.CustomerService
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/customer/v1")
@Tag(name = "Customer Management", description = "Customer CRUD and management operations")
class CustomerController(
    private val customerService: CustomerService,
) {

    /**
     * Bulk upsert (push) endpoint of the unified sync contract. The body MAY contain
     * soft-deleted rows (status = DELETED) so deletions propagate in-band — there is no
     * separate per-row DELETE call in the sync push path.
     */
    @PostMapping("/customers/sync")
    fun updateCustomers(@RequestBody @Valid customerUpdateRequest: List<CustomerUpdateRequest>): ApiResponse<List<CustomerResponse>> {
        val customers = customerUpdateRequest.toCustomers()
        val result = customerService.updateCustomers(customers).asCustomersResponse()
        return ApiResponse.success(result)
    }

    /**
     * Incremental sync feed (pull): customers updated at/after last_sync, INCLUDING
     * soft-deleted rows (status = DELETED) so clients can detect and propagate deletions.
     */
    @GetMapping("/customers/sync")
    fun getCustomers(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String
    ): ApiResponse<PageResponse<CustomerResponse>> {
        // Use JPA property names for sorting (Spring Data handles JPA queries automatically)
        val jpaPropertyName = when (sortBy) {
            "createdAt" -> "createdAt"
            "updatedAt" -> "updatedAt"
            "name" -> "name"
            "customerType" -> "customerType"
            "phone" -> "phone"
            "email" -> "email"
            else -> "updatedAt" // default fallback
        }

        val sort = Sort.by(Sort.Direction.fromString(sortDir), jpaPropertyName)
        val pageable = PageRequest.of(page, size, sort)

        val customersPage = customerService.getCustomersAfterSync(lastSync, pageable)
        return ApiResponse.success(PageResponse.from(customersPage) { it.asCustomerResponse() })
    }

    @GetMapping("/states")
    fun getStates(@RequestParam("last_updated") lastUpdated: Long?): ApiResponse<List<StateResponse>> {
        val states = customerService.getStates()
        val result = states.asStatesResponse()
        return ApiResponse.success(result)
    }

    @GetMapping("/{customerId}")
    fun getCustomer(@PathVariable customerId: String): ApiResponse<CustomerResponse> {
        val customer = customerService.getCustomerByUid(customerId)
            ?: throw NotFoundException("Customer not found: $customerId")
        return ApiResponse.success(customer.asCustomerResponse())
    }
}
