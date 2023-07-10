package com.ampairs.customer.domain.dto

import com.ampairs.core.user.model.Customer
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

fun CustomerUpdateRequest.toCustomer(): Customer {
    val customer = Customer()
    customer.id = this.id ?: ""
    customer.countryCode = this.countryCode
    customer.phone = this.phone ?: ""
    customer.email = this.email ?: ""
    customer.pincode = this.pincode ?: ""
    customer.gstin = this.gstin ?: ""
    return customer
}