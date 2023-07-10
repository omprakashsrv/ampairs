package com.ampairs.customer.controller

import com.ampairs.core.user.model.SessionUser
import com.ampairs.core.user.service.CompanyService
import com.ampairs.customer.domain.dto.CustomerResponse
import com.ampairs.customer.domain.dto.CustomerUpdateRequest
import com.ampairs.customer.domain.dto.asCompanyResponse
import com.ampairs.customer.domain.dto.toCustomer
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

    @PostMapping("/update")
    fun updateUser(@RequestBody @Valid customerUpdateRequest: CustomerUpdateRequest): CustomerResponse {
        val company = customerUpdateRequest.toCustomer()
        val sessionUser: SessionUser = SecurityContextHolder.getContext().authentication.principal as SessionUser
        return customerService.updateCustomer(sessionUser.company.id, company).asCompanyResponse()
    }

    @GetMapping("")
    fun getCustomers(@RequestParam("last_updated") lastUpdated: Long?): List<CustomerResponse> {
        val sessionUser: SessionUser = SecurityContextHolder.getContext().authentication.principal as SessionUser
        val customers = customerService.getCustomers(sessionUser.company.id, lastUpdated)
        return customers.asCompanyResponse()
    }

}