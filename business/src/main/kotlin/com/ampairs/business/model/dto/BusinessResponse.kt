package com.ampairs.business.model.dto

import java.time.Instant

/**
 * Response DTO for Business API endpoints.
 *
 * **Timezone Handling**:
 * - createdAt and updatedAt use `Instant` (UTC timestamps)
 * - Serializes as ISO-8601 with Z: "2025-10-10T14:30:00Z"
 * - Frontend automatically converts to browser timezone
 *
 * **JSON Naming**:
 * - Global snake_case config handles naming automatically
 * - camelCase fields → snake_case JSON (e.g., businessType → business_type)
 * - NO @JsonProperty annotations needed
 */
data class BusinessResponse(
    val uid: String,
    val name: String,
    val businessType: String,
    val description: String?,
    val ownerName: String?,

    // Address
    val addressLine1: String?,
    val addressLine2: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String?,

    // Location
    val latitude: Double?,
    val longitude: Double?,

    // Contact
    val phone: String?,
    val email: String?,
    val website: String?,

    // Logo
    val logoUrl: String?,
    val logoThumbnailUrl: String?,

    // Tax/Regulatory
    val taxId: String?,
    val registrationNumber: String?,

    // Custom Attributes
    val customAttributes: Map<String, Any>?,

    // Operational Config
    val timezone: String,
    val currency: String,
    val language: String,
    val dateFormat: String,
    val timeFormat: String,

    // Business Hours
    val openingHours: String?,
    val closingHours: String?,
    val operatingDays: List<String>,

    // Status
    val active: Boolean,

    // Timestamps (Instant serializes as "2025-10-10T14:30:00Z")
    val createdAt: Instant,
    val updatedAt: Instant
)
