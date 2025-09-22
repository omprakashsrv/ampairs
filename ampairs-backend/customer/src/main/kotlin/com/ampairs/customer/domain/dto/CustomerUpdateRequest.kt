package com.ampairs.customer.domain.dto

import com.ampairs.core.domain.model.Address
import com.ampairs.core.validation.*
import com.ampairs.customer.domain.model.Customer
import jakarta.validation.constraints.*

data class CustomerUpdateRequest(
    @field:SafeString(maxLength = 50, message = "ID contains invalid characters")
    @field:Size(max = 50, message = "ID cannot exceed 50 characters")
    var uid: String?,

    @field:SafeString(maxLength = 50, message = "Reference ID contains invalid characters")
    @field:Size(max = 50, message = "Reference ID cannot exceed 50 characters")
    var refId: String?,

    @field:NotNull(message = "Name is required")
    @field:NotBlank(message = "Name cannot be blank")
    @field:SafeString(maxLength = 100, message = "Name contains invalid characters")
    @field:Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    var name: String,

    @field:ValidGstin
    var gstin: String?,

//    @field:ValidCountryCode
    val countryCode: Int,

    @field:ValidPhone
    var phone: String?,

    @field:SafeString(maxLength = 15, message = "Landline contains invalid characters")
    @field:Pattern(regexp = "^[0-9\\-\\+\\(\\)\\s]*$", message = "Invalid landline format")
    var landline: String?,

    @field:ValidEmail
    var email: String?,

    @field:ValidPincode
    var pincode: String?,

    var status: String?,

    var attributes: Map<String, Any>?,

    @field:SafeString(maxLength = 500, message = "Address contains invalid characters")
    var address: String?,

    @field:SafeString(maxLength = 100, message = "State contains invalid characters")
    @field:Size(max = 100, message = "State cannot exceed 100 characters")
    var state: String?,

    @field:SafeString(maxLength = 200, message = "Street contains invalid characters")
    @field:Size(max = 200, message = "Street cannot exceed 200 characters")
    var street: String = "",

    @field:SafeString(maxLength = 200, message = "Street2 contains invalid characters")
    @field:Size(max = 200, message = "Street2 cannot exceed 200 characters")
    var street2: String = "",

    @field:SafeString(maxLength = 100, message = "City contains invalid characters")
    @field:Size(max = 100, message = "City cannot exceed 100 characters")
    var city: String = "",

    @field:SafeString(maxLength = 100, message = "Country contains invalid characters")
    @field:Size(max = 100, message = "Country cannot exceed 100 characters")
    var country: String = "",

    var billingAddress: Address? = Address(),
    var shippingAddress: Address? = Address(),

    @field:DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @field:DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    val latitude: Double?,

    @field:DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @field:DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    val longitude: Double?,

    val active: Boolean,
    val softDeleted: Boolean,
)

fun CustomerUpdateRequest.toCustomer(): Customer {
    val customer = Customer()
    customer.uid = this.uid ?: ""
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
    customer.status = this.status ?: "ACTIVE"
    customer.attributes = this.attributes
    return customer
}

fun List<CustomerUpdateRequest>.toCustomers(): List<Customer> {
    return map {
        it.toCustomer()
    }
}