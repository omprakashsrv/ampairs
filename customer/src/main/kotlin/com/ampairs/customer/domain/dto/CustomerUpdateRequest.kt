package com.ampairs.customer.domain.dto

import com.ampairs.core.domain.model.Address
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
    var street: String = "",
    var street2: String = "",
    var city: String = "",
    var country: String = "",
    var billingAddress: Address? = Address(),
    var shippingAddress: Address? = Address(),
    val latitude: Double?,
    val longitude: Double?,
    val active: Boolean,
    val softDeleted: Boolean,
    var billingSameAsRegistered: Boolean,
    var shippingSameAsBilling: Boolean,
)

fun CustomerUpdateRequest.toCustomer(): Customer {
    val customer = Customer()
    customer.seqId = this.id ?: ""
    customer.refId = this.refId ?: ""
    customer.name = this.name
    customer.countryCode = this.countryCode
    customer.phone = this.phone ?: ""
    customer.landline = this.landline ?: ""
    customer.email = this.email ?: ""
    customer.pincode = this.pincode ?: ""
    customer.gstin = this.gstin ?: ""
    customer.address = this.address ?: ""
    customer.state = this.state ?: ""
    customer.street = this.street
    customer.street2 = this.street2
    customer.city = this.city
    customer.billingAddress = this.billingAddress ?: Address()
    customer.shippingAddress = this.shippingAddress ?: Address()
    customer.active = this.active
    customer.softDeleted = this.softDeleted
    customer.billingSameAsRegistered = this.billingSameAsRegistered
    customer.shippingSameAsBilling = this.shippingSameAsBilling
    return customer
}

fun List<CustomerUpdateRequest>.toCustomers(): List<Customer> {
    return map {
        it.toCustomer()
    }
}