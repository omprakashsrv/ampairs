package com.ampairs.ecom.domain.dto

import com.ampairs.ecom.domain.model.CustomerAddress
import jakarta.validation.constraints.NotBlank

data class CustomerAddressRequest(
    // Client-generated uid (the app owns address ids). When present, create is idempotent on this
    // uid so the local id stays authoritative and matches at checkout.
    val uid: String? = null,

    val label: String? = null,

    @field:NotBlank
    val addressLine1: String,

    val addressLine2: String? = null,

    @field:NotBlank
    val city: String,

    @field:NotBlank
    val state: String,

    @field:NotBlank
    val pinCode: String,

    val country: String = "IN",
    val phone: String? = null,
    val isDefault: Boolean = false,
    // Optional geolocation, when the address was picked on the map.
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class CustomerAddressResponse(
    val uid: String,
    val label: String?,
    val addressLine1: String,
    val addressLine2: String?,
    val city: String,
    val state: String,
    val pinCode: String,
    val country: String,
    val phone: String?,
    val isDefault: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

fun CustomerAddress.asAddressResponse() = CustomerAddressResponse(
    uid = uid,
    label = label,
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    city = city,
    state = state,
    pinCode = pinCode,
    country = country,
    phone = phone,
    isDefault = isDefault,
    latitude = latitude,
    longitude = longitude,
)
