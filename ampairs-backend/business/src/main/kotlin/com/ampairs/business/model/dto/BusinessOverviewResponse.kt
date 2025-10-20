package com.ampairs.business.model.dto

import java.time.Instant

/**
 * Business overview response for dashboard display.
 * Contains summary information about the business.
 */
data class BusinessOverviewResponse(
    val uid: String,
    val name: String,
    val businessType: String,
    val currency: String,
    val timezone: String,
    val email: String?,
    val phone: String?,
    val address: String,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
