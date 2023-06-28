package com.ampairs.customer.domain.dto

import com.ampairs.core.domain.model.Company

data class CustomerResponse(
    var id: String,
    var countryCode: Int,
    var phone: String,
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


fun List<Company>.asCustomerResponse(): List<CustomerResponse> {
    return map {
        CustomerResponse(
            id = it.id,
            countryCode = it.countryCode,
            phone = it.phone,
            email = it.email,
            gstin = it.gstin,
            address = it.address,
            pincode = it.pincode,
            state = it.state,
            latitude = it.location?.x,
            longitude = it.location?.y,
            lastUpdated = it.lastUpdated,
            createdAt = it.createdAt.toString(),
            updatedAt = it.updatedAt.toString()
        )
    }
}

fun Company.asCustomerResponse(): CustomerResponse {
    return CustomerResponse(
        id = this.id,
        countryCode = this.countryCode,
        phone = this.phone,
        email = this.email,
        gstin = this.gstin,
        address = this.address,
        pincode = this.pincode,
        state = this.state,
        latitude = this.location?.x,
        longitude = this.location?.y,
        lastUpdated = this.lastUpdated,
        createdAt = this.createdAt.toString(),
        updatedAt = this.updatedAt.toString()
    )
}

