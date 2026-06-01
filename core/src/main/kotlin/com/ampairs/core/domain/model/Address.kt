package com.ampairs.core.domain.model

data class Address(
    var street: String? = null,
    var street2: String? = null,
    var address: String? = null,
    var city: String? = null,
    var state: String? = null,
    var country: String? = null,
    var attention: String? = null,
    var pincode: String? = null,
    var phone: String? = null,
)