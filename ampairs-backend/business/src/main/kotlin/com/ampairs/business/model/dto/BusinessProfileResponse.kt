package com.ampairs.business.model.dto

import java.time.Instant

/**
 * Business profile response containing company information and registration details.
 */
data class BusinessProfileResponse(
    val uid: String,
    val name: String,
    val businessType: String,
    val description: String?,
    val ownerName: String?,
    val addressLine1: String?,
    val addressLine2: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String?,
    val latitude: Double?,
    val longitude: Double?,
    val phone: String?,
    val email: String?,
    val website: String?,
    val taxId: String?,
    val registrationNumber: String?,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
