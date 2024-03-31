package com.ampairs.company.controller

import com.ampairs.company.model.dto.CompanyResponse
import com.ampairs.company.model.dto.toCompanyResponse
import com.ampairs.company.service.CompanyService
import com.ampairs.user.model.dto.UserResponse
import com.ampairs.user.model.dto.UserUpdateRequest
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/company/v1")
class CompanyController @Autowired constructor(
    private val companyService: CompanyService,
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
        return companyService.getUserCompany().toCompanyResponse()
    }

}