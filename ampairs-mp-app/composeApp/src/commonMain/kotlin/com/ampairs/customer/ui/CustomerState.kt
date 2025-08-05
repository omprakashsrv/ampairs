package com.ampairs.customer.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ampairs.customer.domain.Customer

class CustomerState(val customer: Customer) {
    var name by mutableStateOf(this.customer.name)
    var gstin by mutableStateOf(this.customer.gstin)
    var email by mutableStateOf(this.customer.email)
    var address by mutableStateOf(this.customer.address)
    var street by mutableStateOf(this.customer.street)
    var street2 by mutableStateOf(this.customer.street2)
    var city by mutableStateOf(this.customer.city)
    var country by mutableStateOf(this.customer.country)
    var billingAddress by mutableStateOf(this.customer.billingAddress)
    var shippingAddress by mutableStateOf(this.customer.shippingAddress)
    var billingSameAsRegistered by mutableStateOf(this.customer.billingSameAsRegistered ?: true)
    var shippingSameAsBilling by mutableStateOf(this.customer.shippingSameAsBilling ?: true)
    var pincode by mutableStateOf(this.customer.pincode)
    var state by mutableStateOf(this.customer.state)
    var countryCode by mutableStateOf(this.customer.countryCode)
    var phone by mutableStateOf(this.customer.phone)
    var landline by mutableStateOf(this.customer.landline)
}

fun CustomerState.toDomainModel(): Customer {
    this.customer.name = this.name
    this.customer.email = this.email
    this.customer.gstin = this.gstin
    this.customer.address = this.address
    this.customer.pincode = this.pincode
    this.customer.state = this.state
    this.customer.countryCode = this.countryCode
    this.customer.phone = this.phone
    this.customer.landline = this.landline
    this.customer.street = this.street
    this.customer.street2 = this.street2
    this.customer.city = this.city
    this.customer.country = this.country
    this.customer.billingAddress = this.billingAddress
    this.customer.shippingAddress = this.shippingAddress
    this.customer.shippingSameAsBilling = this.shippingSameAsBilling
    this.customer.billingSameAsRegistered = this.billingSameAsRegistered
    return this.customer
}