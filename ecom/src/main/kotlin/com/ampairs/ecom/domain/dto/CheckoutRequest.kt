package com.ampairs.ecom.domain.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue

data class DeliveryAddressDto(
    val addressLine1: String,
    val addressLine2: String? = null,
    val city: String,
    val state: String,
    val pinCode: String,
    val country: String = "IN",
    val phone: String? = null,
)

data class CheckoutRequest(
    val deliveryAddressId: String? = null,

    @field:Valid
    val deliveryAddress: DeliveryAddressDto? = null,

    val saveAddress: Boolean = false,
    val notes: String? = null,
) {
    @AssertTrue(message = "Either deliveryAddressId or deliveryAddress must be provided")
    fun isDeliveryAddressProvided(): Boolean = deliveryAddressId != null || deliveryAddress != null
}
