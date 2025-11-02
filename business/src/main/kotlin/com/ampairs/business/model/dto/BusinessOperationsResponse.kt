package com.ampairs.business.model.dto

import java.time.Instant

/**
 * Business operations response containing operational settings.
 */
data class BusinessOperationsResponse(
    val uid: String,
    val timezone: String,
    val currency: String,
    val language: String,
    val dateFormat: String,
    val timeFormat: String,
    val openingHours: String?,
    val closingHours: String?,
    val operatingDays: List<String>,
    val updatedAt: Instant
)
