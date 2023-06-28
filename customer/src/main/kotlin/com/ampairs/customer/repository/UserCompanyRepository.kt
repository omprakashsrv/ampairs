package com.ampairs.customer.repository

import com.ampairs.core.domain.model.UserCompany
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserCompanyRepository : CrudRepository<UserCompany, String> {

    fun findAllByUserId(userId: String): List<UserCompany>
    fun findByUserIdAndCompanyId(userId: String, companyId: String): UserCompany?
}