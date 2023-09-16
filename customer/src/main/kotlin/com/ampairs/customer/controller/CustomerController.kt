package com.ampairs.customer.controller

import com.ampairs.customer.domain.dto.*
import com.ampairs.customer.domain.service.CustomerService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/customer/v1")
class CustomerController @Autowired constructor(
    private val customerService: CustomerService,
) {

    @PostMapping("")
    fun updateUser(@RequestBody @Valid customerUpdateRequest: CustomerUpdateRequest): CustomerResponse {
        val customer = customerUpdateRequest.toCustomer()
        return customerService.updateCustomer(customer).asCustomerResponse()
    }

    @PostMapping("/customers")
    fun updateCustomers(@RequestBody @Valid customerUpdateRequest: List<CustomerUpdateRequest>): List<CustomerResponse> {
        val customers = customerUpdateRequest.toCustomers()
        return customerService.updateCustomers(customers).asCustomersResponse()
    }

    @GetMapping("")
    fun getCustomers(@RequestParam("last_updated") lastUpdated: Long?): List<CustomerResponse> {
        val customers = customerService.getCustomers(lastUpdated)
        return customers.asCustomersResponse()
    }

}