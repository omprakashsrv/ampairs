package com.ampairs.business.model.dto

import jakarta.validation.constraints.Size

/**
 * Request DTO for updating tax configuration.
 */
data class TaxConfigurationUpdateRequest(
    @field:Size(max = 50, message = "Tax ID must not exceed 50 characters")
    val taxId: String? = null,

    @field:Size(max = 100, message = "Registration number must not exceed 100 characters")
    val registrationNumber: String? = null,

    val taxSettings: Map<String, Any>? = null
)
