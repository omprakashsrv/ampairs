package com.ampairs.order.domain.model

data class Address(
    var street: String = "",
    var street2: String = "",
    var address: String = "",
    var city: String = "",
    var state: String = "",
    var zip: String = "",
    var county: String = "",
    var attention: String = "",
    var phone: String = "",
)