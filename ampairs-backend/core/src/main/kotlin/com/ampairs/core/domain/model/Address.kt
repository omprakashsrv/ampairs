package com.ampairs.core.domain.model

data class Address(
    var street: String = "",
    var street2: String = "",
    var address: String = "",
    var city: String = "",
    var state: String = "",
    var country: String = "",
    var attention: String = "",
    var pincode: String = "",
    var phone: String = "",
)