package com.ampairs.company.service

import com.ampairs.company.model.Company
import com.ampairs.company.model.UserCompany
import com.ampairs.company.model.enums.Role
import com.ampairs.company.repository.CompanyRepository
import com.ampairs.company.repository.UserCompanyRepository
import com.ampairs.user.model.User
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CompanyService @Autowired constructor(
    val companyRepository: CompanyRepository,
    val userCompanyRepository: UserCompanyRepository,
) {
    fun getCompanies(userId: String): List<Company> {
        val userCompanies = getUserCompanies(userId)
        val companies: MutableList<Company> = mutableListOf()
        for (userCompany in userCompanies) {
            companies.add(userCompany.company)
        }
        return companies
    }

    fun getUserCompanies(userId: String): List<UserCompany> {
        return userCompanyRepository.findAllByUserId(userId)
    }

    @Transactional
    fun updateCompany(company: Company, user: User): Company {
        val newCompany = company.id.isEmpty()
        if (!newCompany) {
            val existingCompany =
                companyRepository.findById(company.id).orElseThrow {
                    Exception("No company found with given id")
                }
            company.seqId = existingCompany.seqId
        }
        val updatedCompany = companyRepository.save(company)
        if (newCompany) {
            val userCompany = UserCompany()
            userCompany.companyId = updatedCompany.id
            userCompany.userId = user.id
            userCompany.role = Role.OWNER
            userCompanyRepository.save(userCompany)
        }
        return updatedCompany
    }

}