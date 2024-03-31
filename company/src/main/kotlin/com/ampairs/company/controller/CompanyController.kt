package com.ampairs.company.controller

import com.ampairs.company.model.dto.*
import com.ampairs.company.service.CompanyService
import com.ampairs.user.model.User
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/company/v1")
class CompanyController @Autowired constructor(
    private val companyService: CompanyService,
) {

    @PostMapping("")
    fun registerCompany(@RequestBody @Valid companyRequest: CompanyRequest): CompanyRequest {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User
        return companyService.updateCompany(companyRequest.toCompany(), user).toCompanyRequest()
    }

    @GetMapping("")
    fun getCompanies(): List<CompanyResponse> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User
        return companyService.getCompanies(user.id).toCompanyResponse()
    }

}