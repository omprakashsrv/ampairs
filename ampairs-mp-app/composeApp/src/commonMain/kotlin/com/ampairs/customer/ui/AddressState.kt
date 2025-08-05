package com.ampairs.customer.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ampairs.order.domain.Address

class AddressState(val existingAddress: Address) {
    var street: String by mutableStateOf(existingAddress.street)
    var street2: String by mutableStateOf(existingAddress.street2)
    var address: String by mutableStateOf(existingAddress.address)
    var city: String by mutableStateOf(existingAddress.city)
    var state: String by mutableStateOf(existingAddress.state)
    var zip: String by mutableStateOf(existingAddress.zip)
    var country: String by mutableStateOf(existingAddress.country)
    var attention: String by mutableStateOf(existingAddress.attention)
    var phone: String by mutableStateOf(existingAddress.phone)

    fun getAddress(): Address {
        existingAddress.street = street
        existingAddress.street2 = street2
        existingAddress.address = address
        existingAddress.city = city
        existingAddress.state = state
        existingAddress.zip = zip
        existingAddress.country = country
        existingAddress.attention = attention
        existingAddress.phone = phone
        return existingAddress
    }
}