package com.ampairs.customer.domain.dto

import com.ampairs.core.domain.model.Address
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.CustomerType
import java.time.LocalDateTime

data class CustomerResponse(
    var uid: String,
    var name: String,
    var countryCode: Int,
    var phone: String,
    var landline: String,
    var email: String?,
    var customerType: CustomerType,
    var gstNumber: String?,
    var panNumber: String?,
    var creditLimit: Double,
    var creditDays: Int,
    var outstandingAmount: Double,
    var address: String?,
    var pincode: String?,
    var state: String?,
    var street: String = "",
    var street2: String = "",
    var city: String = "",
    var status: String = "",
    var country: String = "",
    var billingAddress: Address = Address(),
    var shippingAddress: Address = Address(),
    var attributes: Map<String, Any>?,
    val latitude: Double?,
    val longitude: Double?,
    var lastUpdated: Long?,
    var createdAt: LocalDateTime?,
    var updatedAt: LocalDateTime?,
)


fun List<Customer>.asCustomersResponse(): List<CustomerResponse> {
    return map {
        it.asCustomerResponse()
    }
}

fun Customer.asCustomerResponse(): CustomerResponse {
    return CustomerResponse(
        uid = this.uid,
        name = this.name,
        countryCode = this.countryCode,
        phone = this.phone,
        landline = this.landline,
        email = this.email,
        customerType = this.customerType,
        gstNumber = this.gstNumber,
        panNumber = this.panNumber,
        creditLimit = this.creditLimit,
        creditDays = this.creditDays,
        outstandingAmount = this.outstandingAmount,
        address = this.address,
        pincode = this.pincode,
        state = this.state,
        street = this.street,
        street2 = this.street2,
        city = this.city,
        country = this.country,
        status = this.status,
        billingAddress = this.billingAddress,
        shippingAddress = this.shippingAddress,
        attributes = this.attributes,
        // Spring Data Point: x=longitude, y=latitude - fix coordinate mapping
        latitude = this.location?.y,
        longitude = this.location?.x,
        lastUpdated = this.lastUpdated,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )
}

