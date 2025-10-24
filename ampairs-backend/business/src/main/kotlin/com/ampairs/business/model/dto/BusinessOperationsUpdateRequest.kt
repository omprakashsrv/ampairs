package com.ampairs.business.model.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Request DTO for updating business operational settings.
 */
data class BusinessOperationsUpdateRequest(
    @field:NotBlank(message = "Timezone is required")
    @field:Size(max = 50, message = "Timezone must not exceed 50 characters")
    val timezone: String,

    @field:NotBlank(message = "Currency is required")
    @field:Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    val currency: String,

    @field:NotBlank(message = "Language is required")
    @field:Size(max = 10, message = "Language must not exceed 10 characters")
    val language: String,

    @field:NotBlank(message = "Date format is required")
    @field:Size(max = 20, message = "Date format must not exceed 20 characters")
    val dateFormat: String,

    @field:NotBlank(message = "Time format is required")
    @field:Size(max = 10, message = "Time format must not exceed 10 characters")
    val timeFormat: String,

    @field:Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Opening hours must be in HH:MM format")
    val openingHours: String? = null,

    @field:Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Closing hours must be in HH:MM format")
    val closingHours: String? = null,

    val operatingDays: List<String> = emptyList()
)
