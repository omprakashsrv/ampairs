package com.ampairs.customer.domain.dto

import com.ampairs.core.domain.model.Company
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class CustomerUpdateRequest(
    @NotNull @NotEmpty var id: String?,
    @NotNull @NotEmpty var name: String,
    var gstin: String?,
    val countryCode: Int,
    var phone: String?,
    var email: String?,
    var pincode: String?
)

fun CustomerUpdateRequest.toCompany(): Company {
    val company = Company()
    company.id = this.id ?: ""
    company.countryCode = this.countryCode
    company.phone = this.phone ?: ""
    company.email = this.email ?: ""
    company.pincode = this.pincode ?: ""
    company.gstin = this.gstin ?: ""
    return company
}