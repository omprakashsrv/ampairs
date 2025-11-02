package com.ampairs.business.model.dto

import com.ampairs.business.model.enums.BusinessType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request DTO for updating business profile information.
 */
data class BusinessProfileUpdateRequest(
    @field:NotBlank(message = "Business name is required")
    @field:Size(max = 255, message = "Business name must not exceed 255 characters")
    val name: String,

    val businessType: BusinessType,

    @field:Size(max = 5000, message = "Description must not exceed 5000 characters")
    val description: String? = null,

    @field:Size(max = 255, message = "Owner name must not exceed 255 characters")
    val ownerName: String? = null,

    @field:Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    val addressLine1: String? = null,

    @field:Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    val addressLine2: String? = null,

    @field:Size(max = 100, message = "City must not exceed 100 characters")
    val city: String? = null,

    @field:Size(max = 100, message = "State must not exceed 100 characters")
    val state: String? = null,

    @field:Size(max = 20, message = "Postal code must not exceed 20 characters")
    val postalCode: String? = null,

    @field:Size(max = 100, message = "Country must not exceed 100 characters")
    val country: String? = null,

    val latitude: Double? = null,
    val longitude: Double? = null,

    @field:Size(max = 20, message = "Phone must not exceed 20 characters")
    val phone: String? = null,

    @field:Email(message = "Invalid email format")
    @field:Size(max = 255, message = "Email must not exceed 255 characters")
    val email: String? = null,

    @field:Size(max = 500, message = "Website must not exceed 500 characters")
    val website: String? = null,

    @field:Size(max = 50, message = "Tax ID must not exceed 50 characters")
    val taxId: String? = null,

    @field:Size(max = 100, message = "Registration number must not exceed 100 characters")
    val registrationNumber: String? = null,

    val customAttributes: Map<String, Any>? = null,

    val active: Boolean = true
)
