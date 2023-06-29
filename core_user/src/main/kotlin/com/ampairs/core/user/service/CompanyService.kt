package com.ampairs.core.user.service

import com.ampairs.core.domain.model.Company
import com.ampairs.core.user.repository.CompanyRepository
import com.ampairs.core.user.repository.UserCompanyRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CompanyService @Autowired constructor(
    val companyRepository: CompanyRepository,
    val userCompanyRepository: UserCompanyRepository
) {
    fun getUserCompanies(userId: String): List<Company> {
        val userCompanies = userCompanyRepository.findAllByUserId(userId)
        val companies: MutableList<Company> = mutableListOf()
        for (userCompany in userCompanies) {
            companies.add(userCompany.company)
        }
        return companies;
    }

    fun getUserCompany(userId: String, companyId: String): Company? {
        val userCompany = userCompanyRepository.findByUserIdAndCompanyId(userId, companyId)
        return userCompany?.company;
    }

    fun getUserCompany(): Company {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        return auth.principal as Company
    }

}