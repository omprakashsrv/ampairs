package com.ampairs.company.repository

import com.ampairs.company.model.UserCompany
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserCompanyRepository : CrudRepository<UserCompany, String> {

    fun findAllByUserId(userId: String): List<UserCompany>
    fun findByUserIdAndCompanyId(userId: String, companyId: String): UserCompany?
}