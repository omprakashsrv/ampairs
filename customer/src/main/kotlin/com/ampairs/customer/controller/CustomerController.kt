package com.ampairs.customer.controller

import com.ampairs.core.domain.dto.UserResponse
import com.ampairs.core.domain.dto.UserUpdateRequest
import com.ampairs.core.domain.model.User
import com.ampairs.customer.domain.service.CustomerService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/customer/v1")
class CustomerController @Autowired constructor(
    private val customerService: CustomerService
) {

    @PostMapping("/update")
    fun updateUser(@RequestBody @Valid userUpdateRequest: UserUpdateRequest): UserResponse {
        val user: User = customerService.updateCustomer(userUpdateRequest);
        return UserResponse(user)
    }

    @GetMapping("")
    fun getCustomers(): UserResponse {
        val sessionUser = customerService.getCustomers()
        return UserResponse(sessionUser)
    }

}