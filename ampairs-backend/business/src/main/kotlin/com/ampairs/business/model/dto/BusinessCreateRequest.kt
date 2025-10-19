package com.ampairs.business.model.dto

import com.ampairs.business.model.enums.BusinessType
import jakarta.validation.constraints.*

/**
 * Request DTO for creating a new business profile.
 *
 * **Validation**:
 * - Jakarta validation annotations for field-level validation
 * - Service layer handles business rule validation (e.g., hours consistency)
 *
 * **JSON Naming**:
 * - Global snake_case config handles naming automatically
 * - NO @JsonProperty annotations needed
 */
data class BusinessCreateRequest(
    // Profile
    @field:NotBlank(message = "Business name is required")
    @field:Size(min = 2, max = 255, message = "Business name must be 2-255 characters")
    val name: String,

    @field:NotBlank(message = "Business type is required")
    val businessType: String,

    @field:Size(max = 1000, message = "Description cannot exceed 1000 characters")
    val description: String? = null,

    @field:Size(max = 255, message = "Owner name cannot exceed 255 characters")
    val ownerName: String? = null,

    // Address
    @field:Size(max = 255, message = "Address line 1 cannot exceed 255 characters")
    val addressLine1: String? = null,

    @field:Size(max = 255, message = "Address line 2 cannot exceed 255 characters")
    val addressLine2: String? = null,

    @field:Size(max = 100, message = "City cannot exceed 100 characters")
    val city: String? = null,

    @field:Size(max = 100, message = "State cannot exceed 100 characters")
    val state: String? = null,

    @field:Size(max = 20, message = "Postal code cannot exceed 20 characters")
    val postalCode: String? = null,

    @field:Size(max = 100, message = "Country cannot exceed 100 characters")
    val country: String? = null,

    // Location
    @field:DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @field:DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    val latitude: Double? = null,

    @field:DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @field:DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    val longitude: Double? = null,

    // Contact
    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Invalid phone number format (use E.164 format)"
    )
    @field:Size(max = 20, message = "Phone cannot exceed 20 characters")
    val phone: String? = null,

    @field:Email(message = "Invalid email format")
    @field:Size(max = 255, message = "Email cannot exceed 255 characters")
    val email: String? = null,

    @field:Size(max = 500, message = "Website URL cannot exceed 500 characters")
    val website: String? = null,

    // Tax/Regulatory
    @field:Size(max = 50, message = "Tax ID cannot exceed 50 characters")
    val taxId: String? = null,

    @field:Size(max = 100, message = "Registration number cannot exceed 100 characters")
    val registrationNumber: String? = null,

    // Operational Config
    @field:Size(max = 50, message = "Timezone cannot exceed 50 characters")
    val timezone: String = "UTC",

    @field:Size(min = 3, max = 3, message = "Currency must be 3-letter ISO 4217 code")
    @field:Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3 uppercase letters")
    val currency: String = "INR",

    @field:Size(max = 10, message = "Language cannot exceed 10 characters")
    val language: String = "en",

    @field:Size(max = 20, message = "Date format cannot exceed 20 characters")
    val dateFormat: String = "DD-MM-YYYY",

    @field:Pattern(regexp = "^(12H|24H)$", message = "Time format must be either 12H or 24H")
    val timeFormat: String = "12H",

    // Business Hours
    @field:Pattern(
        regexp = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$",
        message = "Opening hours must be in HH:MM format (00:00-23:59)"
    )
    val openingHours: String? = null,

    @field:Pattern(
        regexp = "^([0-1][0-9]|2[0-3]):[0-5][0-9]$",
        message = "Closing hours must be in HH:MM format (00:00-23:59)"
    )
    val closingHours: String? = null,

    val operatingDays: List<String> = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
)
