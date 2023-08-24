package com.ampairs.customer.domain.dto

import com.ampairs.customer.domain.model.Customer

data class CustomerResponse(
    var id: String,
    var name: String,
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
    var lastUpdated: Long?,
    var createdAt: String?,
    var updatedAt: String?,
)


fun List<Customer>.asCompanyResponse(): List<CustomerResponse> {
    return map {
        CustomerResponse(
            id = it.id,
            name = it.name,
            countryCode = it.countryCode,
            phone = it.phone,
            landline = it.landline,
            email = it.email,
            gstin = it.gstin,
            address = it.address,
            pincode = it.pincode,
            state = it.state,
            latitude = it.location?.x,
            longitude = it.location?.y,
            lastUpdated = it.lastUpdated,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt
        )
    }
}

fun List<Customer>.asCustomerResponse(): List<CustomerUpdateRequest> {
    return map {
        CustomerUpdateRequest(
            id = it.id,
            refId = it.refId,
            name = it.name,
            countryCode = it.countryCode,
            phone = it.phone,
            landline = it.landline,
            email = it.email,
            gstin = it.gstin,
            address = it.address,
            pincode = it.pincode,
            state = it.state,
        )
    }
}

fun Customer.asCompanyResponse(): CustomerResponse {
    return CustomerResponse(
        id = this.id,
        name = this.name,
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
        updatedAt = this.updatedAt
    )
}

