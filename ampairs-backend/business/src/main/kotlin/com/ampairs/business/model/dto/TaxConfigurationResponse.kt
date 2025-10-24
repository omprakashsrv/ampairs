package com.ampairs.business.model.dto

import java.time.Instant

/**
 * Tax configuration response containing tax-related settings.
 */
data class TaxConfigurationResponse(
    val uid: String,
    val taxId: String?,
    val registrationNumber: String?,
    val taxSettings: Map<String, Any>?,
    val country: String?,
    val state: String?,
    val updatedAt: Instant
)
