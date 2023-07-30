package com.ampairs.customer.domain.dto

import com.ampairs.customer.domain.model.Customer
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class CustomerUpdateRequest(
    @NotNull var id: String?,
    @NotNull var refId: String?,
    @NotNull @NotEmpty var name: String,
    var gstin: String?,
    val countryCode: Int,
    var phone: String?,
    var landline: String?,
    var email: String?,
    var pincode: String?,
    var address: String?,
    var state: String?,
)

fun CustomerUpdateRequest.toCustomer(): Customer {
    val customer = Customer()
    customer.id = this.id ?: ""
    customer.refId = this.refId ?: ""
    customer.countryCode = this.countryCode
    customer.phone = this.phone ?: ""
    customer.landline = this.landline ?: ""
    customer.email = this.email ?: ""
    customer.pincode = this.pincode ?: ""
    customer.gstin = this.gstin ?: ""
    customer.address = this.address ?: ""
    customer.state = this.state ?: ""
    return customer
}

fun List<CustomerUpdateRequest>.toCustomers(): List<Customer> {
    return map {
        val customer = Customer()
        customer.id = it.id ?: ""
        customer.refId = it.refId ?: ""
        customer.countryCode = it.countryCode
        customer.phone = it.phone ?: ""
        customer.landline = it.landline ?: ""
        customer.email = it.email ?: ""
        customer.pincode = it.pincode ?: ""
        customer.gstin = it.gstin ?: ""
        customer.address = it.address ?: ""
        customer.state = it.state ?: ""
        customer.email = ""
        customer
    }
}