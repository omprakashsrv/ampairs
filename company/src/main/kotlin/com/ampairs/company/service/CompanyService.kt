package com.ampairs.company.service

import com.ampairs.company.model.Company
import com.ampairs.company.repository.CompanyRepository
import com.ampairs.company.repository.UserCompanyRepository
import com.ampairs.user.model.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CompanyService @Autowired constructor(
    val companyRepository: CompanyRepository,
    val userCompanyRepository: UserCompanyRepository,
) {
    fun getUserCompanies(userId: String): List<Company> {
        val userCompanies = userCompanyRepository.findAllByUserId(userId)
        val companies: MutableList<Company> = mutableListOf()
        for (userCompany in userCompanies) {
            companies.add(userCompany.company)
        }
        return companies
    }

    fun getUserCompany(userId: String, companyId: String): Company? {
        val userCompany = userCompanyRepository.findByUserIdAndCompanyId(userId, companyId)
        return userCompany?.company
    }

    fun getUserCompany(): List<Company> {
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val user = auth.principal as User
        val findAllByUserId = userCompanyRepository.findAllByUserId(user.id)
        return findAllByUserId.map { it.company }
    }

}