package com.ampairs.core.domain.dto

import com.ampairs.core.domain.model.User

data class UserResponse(
    var id: String,
    var firstName: String,
    var lastName: String?,
    var userName: String?,
    var countryCode: Int,
    var phone: String,
    var companies: List<CompanyResponse>
)

fun User.toUserResponse(): UserResponse {
    return UserResponse(
        id = this.id,
        firstName = this.firstName,
        lastName = this.lastName,
        countryCode = this.countryCode,
        phone = this.phone,
        userName = this.userName,
        companies = this.userCompanies.stream()
            .map { userCompany -> userCompany.company }.toList().toCompanyResponse()
    )
}