package com.ampairs.customer.domain.service

import com.ampairs.customer.domain.model.Company
import com.ampairs.customer.repository.CompanyRepository
import com.ampairs.customer.repository.UserCompanyRepository
import org.springframework.beans.factory.annotation.Autowired
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

}