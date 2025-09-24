package com.ampairs.customer.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.customer.domain.dto.*
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.service.CustomerService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/customer/v1")
class CustomerController @Autowired constructor(
    private val customerService: CustomerService,
) {

    @PostMapping("")
    fun updateUser(@RequestBody @Valid customerUpdateRequest: CustomerUpdateRequest): ApiResponse<CustomerResponse> {
        val customer = customerUpdateRequest.toCustomer()
        val result = customerService.upsertCustomer(customer).asCustomerResponse()
        return ApiResponse.success(result)
    }

    @PostMapping("/customers")
    fun updateCustomers(@RequestBody @Valid customerUpdateRequest: List<CustomerUpdateRequest>): ApiResponse<List<CustomerResponse>> {
        val customers = customerUpdateRequest.toCustomers()
        val result = customerService.updateCustomers(customers).asCustomersResponse()
        return ApiResponse.success(result)
    }

    @GetMapping("")
    fun getCustomers(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "20") size: Int,
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

    /**
     * Retail-specific Customer Management API endpoints
     */

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    fun createCustomer(@RequestBody request: CustomerCreateRequest): ApiResponse<CustomerResponse> {
        val customer = Customer().apply {
            name = request.name
            customerType = request.customerType ?: "RETAIL"
            phone = request.phone ?: ""
            email = request.email ?: ""
            gstNumber = request.gstNumber
            panNumber = request.panNumber
            creditLimit = request.creditLimit ?: 0.0
            creditDays = request.creditDays ?: 0
            address = request.address?.street ?: ""
            city = request.address?.city ?: ""
            state = request.address?.state ?: ""
            pincode = request.address?.postalCode ?: ""
            country = request.address?.country ?: "India"
            attributes = request.attributes ?: emptyMap()
            status = "ACTIVE"
        }

        val createdCustomer = customerService.createCustomer(customer)
        return ApiResponse.success(createdCustomer.asCustomerResponse())
    }

    @GetMapping("/{customerId}")
    fun getCustomer(@PathVariable customerId: String): ApiResponse<CustomerResponse> {
        val customer = customerService.getCustomers(null).find { it.uid == customerId }
            ?: return ApiResponse.error("Customer not found", "CUSTOMER_NOT_FOUND")

        return ApiResponse.success(customer.asCustomerResponse())
    }

    @PutMapping("/{customerId}")
    fun updateCustomer(
        @PathVariable customerId: String,
        @RequestBody request: CustomerUpdateRequest
    ): ApiResponse<CustomerResponse> {
        val updates = Customer().apply {
            name = request.name
            phone = request.phone ?: ""
            email = request.email ?: ""
            gstNumber = request.gstNumber
            address = request.address ?: ""
            city = request.city
            state = request.state ?: ""
            pincode = request.pincode ?: ""
            status = request.status ?: "ACTIVE"
            attributes = request.attributes ?: emptyMap()
        }

        val updatedCustomer = customerService.updateCustomer(customerId, updates)
            ?: return ApiResponse.error("Customer not found", "CUSTOMER_NOT_FOUND")

        return ApiResponse.success(updatedCustomer.asCustomerResponse())
    }

    @GetMapping("/gst/{gstNumber}")
    fun getCustomerByGst(@PathVariable gstNumber: String): ApiResponse<CustomerResponse> {
        val customer = customerService.getCustomerByGstNumber(gstNumber)
            ?: return ApiResponse.error("Customer not found with GST: $gstNumber", "CUSTOMER_NOT_FOUND")

        return ApiResponse.success(customer.asCustomerResponse())
    }

    @PostMapping("/validate-gst")
    fun validateGstNumber(@RequestBody request: Map<String, String>): ApiResponse<Map<String, Any>> {
        val gstNumber = request["gst_number"] ?: return ApiResponse.error("GST number is required", "VALIDATION_ERROR")

        val isValid = customerService.validateGstNumber(gstNumber)
        val response = mapOf(
            "gst_number" to gstNumber,
            "is_valid" to isValid,
            "message" to if (isValid) "Valid GST number" else "Invalid GST number format"
        )

        return ApiResponse.success(response)
    }

    @PutMapping("/{customerId}/outstanding")
    fun updateOutstanding(
        @PathVariable customerId: String,
        @RequestBody request: Map<String, Any>
    ): ApiResponse<CustomerResponse> {
        val amount = (request["amount"] as? Number)?.toDouble()
            ?: return ApiResponse.error("Amount is required", "VALIDATION_ERROR")
        val isPayment = request["is_payment"] as? Boolean ?: false

        val updatedCustomer = customerService.updateOutstanding(customerId, amount, isPayment)
            ?: return ApiResponse.error("Customer not found", "CUSTOMER_NOT_FOUND")

        return ApiResponse.success(updatedCustomer.asCustomerResponse())
    }
}

/**
 * DTOs for retail customer API
 */
data class CustomerCreateRequest(
    val name: String,
    val customerType: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val gstNumber: String? = null,
    val panNumber: String? = null,
    val creditLimit: Double? = null,
    val creditDays: Int? = null,
    val address: CustomerAddressRequest? = null,
    val attributes: Map<String, Any>? = null
)

data class CustomerAddressRequest(
    val street: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String = "India"
)