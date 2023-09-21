package com.ampairs.customer.domain.dto

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
    val latitude: Double?,
    val longitude: Double?,
    val active: Boolean,
    val softDeleted: Boolean,
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
        id = this.id,
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
        softDeleted = this.softDeleted
    )
}

