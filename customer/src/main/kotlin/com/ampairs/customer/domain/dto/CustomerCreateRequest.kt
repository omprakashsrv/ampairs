package com.ampairs.customer.domain.dto

import jakarta.validation.constraints.NotBlank

data class CustomerCreateRequest(
    @field:NotBlank(message = "Customer name is required")
    val name: String,
    val customerType: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val gstNumber: String? = null,
    val panNumber: String? = null,
    val creditLimit: Double? = null,
    val creditDays: Int? = null,
    val address: CustomerAddressRequest? = null,
    val attributes: Map<String, Any>? = null
)

data class CustomerAddressRequest(
    val street: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String = "India"
)
