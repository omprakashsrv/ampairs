package com.ampairs.customer.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Customer(
    val id: String = "",
    val name: String = "",
    val email: String? = null,
    val phone: String? = null,
    @SerialName("country_code")
    val countryCode: Int = 91,
    val gstin: String? = null,
    val address: String? = null,
    val street: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val country: String = "India",
    @SerialName("billing_address")
    val billingAddress: CustomerAddress? = null,
    @SerialName("shipping_address")
    val shippingAddress: CustomerAddress? = null,
    val active: Boolean = true,
    @SerialName("workspace_id")
    val workspaceId: String = "",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class CustomerAddress(
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val country: String = "India"
)

data class CustomerListItem(
    val id: String,
    val name: String,
    val phone: String?,
    val email: String?,
    val city: String?
)

fun Customer.toListItem(): CustomerListItem = CustomerListItem(
    id = id,
    name = name,
    phone = phone,
    email = email,
    city = city
)