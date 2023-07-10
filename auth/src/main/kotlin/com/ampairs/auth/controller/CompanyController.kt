package com.ampairs.auth.controller

import com.ampairs.core.domain.dto.UserResponse
import com.ampairs.core.domain.dto.UserUpdateRequest
import com.ampairs.core.user.model.dto.CompanyResponse
import com.ampairs.core.user.model.dto.asCompanyResponse
import com.ampairs.core.user.service.CompanyService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/company/v1")
class CompanyController @Autowired constructor(
    private val companyService: CompanyService
) {

    @PostMapping("/register")
    fun registerCompany(@RequestBody @Valid userUpdateRequest: UserUpdateRequest): UserResponse? {
        return null
    }

    @PostMapping("/update")
    fun updateCompany(@RequestBody @Valid userUpdateRequest: UserUpdateRequest): UserResponse? {
        return null
    }

    @GetMapping("")
    fun getCompany(): CompanyResponse {
        return companyService.getUserCompany().asCompanyResponse()
    }

}