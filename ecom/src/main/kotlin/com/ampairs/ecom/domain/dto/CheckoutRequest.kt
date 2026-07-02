package com.ampairs.ecom.domain.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank

data class DeliveryAddressDto(
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
    // Optional geolocation of the drop point, when the buyer picked it on the map.
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class CheckoutRequest(
    /**
     * The CRM account the buyer is ordering for (from the "ordering for" picker). Null → the server
     * resolves the buyer's default/only account, or creates one on first order.
     */
    val customerId: String? = null,

    val deliveryAddressId: String? = null,

    @field:Valid
    val deliveryAddress: DeliveryAddressDto? = null,

    val saveAddress: Boolean = false,
    val notes: String? = null,
) {
    @AssertTrue(message = "Either deliveryAddressId or deliveryAddress must be provided")
    fun isDeliveryAddressProvided(): Boolean = deliveryAddressId != null || deliveryAddress != null
}
