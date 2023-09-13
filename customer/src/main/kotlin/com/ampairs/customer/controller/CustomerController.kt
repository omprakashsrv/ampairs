package com.ampairs.customer.controller

import com.ampairs.core.user.model.SessionUser
import com.ampairs.core.user.service.CompanyService
import com.ampairs.customer.domain.dto.*
import com.ampairs.customer.domain.service.CustomerService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/customer/v1")
class CustomerController @Autowired constructor(
    private val customerService: CustomerService,
    private val companyService: CompanyService
) {

    @PostMapping("")
    fun updateUser(@RequestBody @Valid customerUpdateRequest: CustomerUpdateRequest): CustomerResponse {
        val company = customerUpdateRequest.toCustomer()
        val sessionUser: SessionUser = SecurityContextHolder.getContext().authentication.principal as SessionUser
        return customerService.updateCustomer(sessionUser.company.id, company).asCustomerResponse()
    }

    @PostMapping("/customers")
    fun updateCustomers(@RequestBody @Valid customerUpdateRequest: List<CustomerUpdateRequest>): List<CustomerResponse> {
        val customers = customerUpdateRequest.toCustomers()
        val sessionUser: SessionUser = SecurityContextHolder.getContext().authentication.principal as SessionUser
        return customerService.updateCustomers(sessionUser.company.id, customers).asCustomersResponse()
    }

    @GetMapping("")
    fun getCustomers(@RequestParam("last_updated") lastUpdated: Long?): List<CustomerResponse> {
        val customers = customerService.getCustomers(lastUpdated)
        return customers.asCustomersResponse()
    }

}