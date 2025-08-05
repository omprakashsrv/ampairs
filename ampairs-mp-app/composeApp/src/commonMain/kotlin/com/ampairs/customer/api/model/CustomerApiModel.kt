package com.ampairs.customer.api.model

import com.ampairs.order.domain.Address
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class CustomerApiModel(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("ref_id") val refId: String? = "",
    @SerialName("company_id") val companyId: String? = "",
    @SerialName("gstin") val gstin: String?,
    @SerialName("email") val email: String?,
    @SerialName("address") val address: String?,
    @SerialName("street") val street: String?,
    @SerialName("street2") val street2: String?,
    @SerialName("city") val city: String?,
    @SerialName("country") val country: String?,
    @SerialName("billing_same_as_registered") val billingSameAsRegistered: Boolean? = true,
    @SerialName("shipping_same_as_billing") val shippingSameAsBilling: Boolean? = true,
    @SerialName("billing_address") val billingAddress: Address? = null,
    @SerialName("shipping_address") val shippingAddress: Address? = null,
    @SerialName("pincode") val pincode: String?,
    @SerialName("state") val state: String?,
    @SerialName("country_code") val countryCode: Int,
    @SerialName("phone") val phone: String?,
    @SerialName("landline") val landline: String?,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("latitude") val latitude: Double?,
    @SerialName("longitude") val longitude: Double?,
    @SerialName("active") val active: Boolean?,
    @SerialName("soft_deleted") val softDeleted: Boolean?,
    @SerialName("last_updated") var lastUpdated: Long = 0,
)