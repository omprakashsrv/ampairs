package com.ampairs.customer.domain.dto

import com.ampairs.core.domain.model.Address
import com.ampairs.customer.domain.model.Customer
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

data class CustomerResponse(
    var uid: String,
    var name: String,
    var countryCode: Int,
    var phone: String,
    var landline: String,
    var email: String?,
    var customerType: String,
    var customerGroup: String,
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
    var createdAt: Instant?,
    var updatedAt: Instant?,
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
        customerGroup = this.customerGroup,
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
        // JTS Point: x=longitude, y=latitude (standard GIS coordinate mapping)
        latitude = this.location?.y,
        longitude = this.location?.x,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )
}

/**
 * Request DTO for GST number validation
 */
data class GstValidationRequest(
    @field:NotBlank(message = "GST number is required")
    val gstNumber: String
)

/**
 * Response DTO for GST number validation
 */
data class GstValidationResponse(
    val gstNumber: String,
    val isValid: Boolean,
    val message: String
)

/**
 * Request DTO for updating customer outstanding amount
 */
data class UpdateOutstandingRequest(
    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.0", message = "Amount must be non-negative")
    val amount: Double,

    @field:NotNull(message = "Payment flag is required")
    val isPayment: Boolean
)

