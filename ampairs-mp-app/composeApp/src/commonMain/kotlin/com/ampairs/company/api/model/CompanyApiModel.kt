package com.ampairs.company.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CompanyApiModel(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("gstin") val gstin: String?,
    @SerialName("email") val email: String?,
    @SerialName("address") val address: String?,
    @SerialName("pincode") val pincode: String?,
    @SerialName("state") val state: String?,
    @SerialName("country_code") val countryCode: Int,
    @SerialName("phone") val phone: String?,
    @SerialName("landline") val landline: String?,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("latitude") val latitude: Double?,
    @SerialName("longitude") val longitude: Double?,
    @SerialName("last_updated") val lastUpdated: Long,
)