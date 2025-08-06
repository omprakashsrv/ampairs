package com.ampairs.customer.domain.dto

import com.ampairs.core.domain.model.Address
import com.ampairs.customer.domain.model.Customer

data class CustomerResponse(
    var id: String,
    var name: String,
    var companyId: String,
    var countryCode: Int,
    var phone: String,
    var landline: String,
    var email: String?,
    var gstin: String?,
    var address: String?,
    var pincode: String?,
    var state: String?,
    var street: String = "",
    var street2: String = "",
    var city: String = "",
    var country: String = "",
    var billingAddress: Address = Address(),
    var shippingAddress: Address = Address(),
    val latitude: Double?,
    val longitude: Double?,
    val active: Boolean,
    val softDeleted: Boolean,
    var billingSameAsRegistered: Boolean,
    var shippingSameAsBilling: Boolean,
    var lastUpdated: Long?,
    var createdAt: String?,
    var updatedAt: String?,
)


fun List<Customer>.asCustomersResponse(): List<CustomerResponse> {
    return map {
        it.asCustomerResponse()
    }
}

fun Customer.asCustomerResponse(): CustomerResponse {
    return CustomerResponse(
        id = this.uid,
        name = this.name,
        companyId = this.companyId,
        countryCode = this.countryCode,
        phone = this.phone,
        landline = this.landline,
        email = this.email,
        gstin = this.gstin,
        address = this.address,
        pincode = this.pincode,
        state = this.state,
        latitude = this.location?.x,
        longitude = this.location?.y,
        lastUpdated = this.lastUpdated,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        active = this.active,
        softDeleted = this.softDeleted,
        street = this.street,
        street2 = this.street2,
        city = this.city,
        country = this.country,
        billingAddress = this.billingAddress,
        shippingAddress = this.shippingAddress,
        shippingSameAsBilling = this.shippingSameAsBilling,
        billingSameAsRegistered = this.billingSameAsRegistered
    )
}

