package com.ampairs.business.model.dto

import com.ampairs.business.model.Business
import java.time.Instant

/**
 * Extension functions for mapping between Business entity and DTOs.
 *
 * **Naming Convention**:
 * - asBusinessResponse(): Entity → Response DTO
 * - toBusiness(): Request DTO → Entity
 * - applyUpdate(): Apply partial update to entity
 *
 * **Timezone Handling**:
 * - Uses Instant for timestamps (UTC, ISO-8601 with Z suffix)
 * - createdAt/updatedAt inherit from OwnableBaseDomain
 * - No timezone conversion needed (UTC throughout)
 */

// ==================== Entity → Response DTO ====================

/**
 * Convert Business entity to BusinessResponse DTO.
 * Used by controller to serialize API responses.
 */
fun Business.asBusinessResponse(): BusinessResponse {
    return BusinessResponse(
        uid = this.uid,
        name = this.name,
        businessType = this.businessType.name,
        description = this.description,
        ownerName = this.ownerName,

        // Address
        addressLine1 = this.addressLine1,
        addressLine2 = this.addressLine2,
        city = this.city,
        state = this.state,
        postalCode = this.postalCode,
        country = this.country,

        // Location
        latitude = this.latitude,
        longitude = this.longitude,

        // Contact
        phone = this.phone,
        email = this.email,
        website = this.website,

        // Tax/Regulatory
        taxId = this.taxId,
        registrationNumber = this.registrationNumber,

        // Operational Config
        timezone = this.timezone,
        currency = this.currency,
        language = this.language,
        dateFormat = this.dateFormat,
        timeFormat = this.timeFormat,

        // Business Hours
        openingHours = this.openingHours,
        closingHours = this.closingHours,
        operatingDays = this.operatingDays,

        // Status
        active = this.active,

        // Timestamps (Instant → ISO-8601 with Z)
        createdAt = this.createdAt ?: Instant.now(),
        updatedAt = this.updatedAt ?: Instant.now()
    )
}

/**
 * Convert list of Business entities to list of BusinessResponse DTOs.
 */
fun List<Business>.asBusinessResponses(): List<BusinessResponse> {
    return this.map { it.asBusinessResponse() }
}

// ==================== Request DTO → Entity ====================

/**
 * Convert BusinessCreateRequest to Business entity.
 * Used by service layer when creating new business profiles.
 *
 * @param ownerId The workspace/owner this business belongs to (from tenant context)
 * @param createdBy User ID who is creating the business
 */
fun BusinessCreateRequest.toBusiness(ownerId: String, createdBy: String? = null): Business {
    return Business().apply {
        this.ownerId = ownerId
        this.name = this@toBusiness.name
        this.businessType = this@toBusiness.businessType
        this.description = this@toBusiness.description
        this.ownerName = this@toBusiness.ownerName

        // Address
        this.addressLine1 = this@toBusiness.addressLine1
        this.addressLine2 = this@toBusiness.addressLine2
        this.city = this@toBusiness.city
        this.state = this@toBusiness.state
        this.postalCode = this@toBusiness.postalCode
        this.country = this@toBusiness.country

        // Location
        this.latitude = this@toBusiness.latitude
        this.longitude = this@toBusiness.longitude

        // Contact
        this.phone = this@toBusiness.phone
        this.email = this@toBusiness.email
        this.website = this@toBusiness.website

        // Tax/Regulatory
        this.taxId = this@toBusiness.taxId
        this.registrationNumber = this@toBusiness.registrationNumber

        // Operational Config
        this.timezone = this@toBusiness.timezone
        this.currency = this@toBusiness.currency
        this.language = this@toBusiness.language
        this.dateFormat = this@toBusiness.dateFormat
        this.timeFormat = this@toBusiness.timeFormat

        // Business Hours
        this.openingHours = this@toBusiness.openingHours
        this.closingHours = this@toBusiness.closingHours
        this.operatingDays = this@toBusiness.operatingDays

        // Audit
        this.createdBy = createdBy
        this.active = true

        // Timestamps are auto-set by @PrePersist in OwnableBaseDomain
    }
}

// ==================== Apply Partial Update ====================

/**
 * Apply BusinessUpdateRequest to existing Business entity.
 * Only updates fields that are non-null in the request (partial update).
 *
 * @param request The update request with fields to change
 * @param updatedBy User ID who is updating the business
 * @return The updated Business entity
 */
fun Business.applyUpdate(request: BusinessUpdateRequest, updatedBy: String? = null): Business {
    // Profile
    request.name?.let { this.name = it }
    request.businessType?.let { this.businessType = it }
    request.description?.let { this.description = it }
    request.ownerName?.let { this.ownerName = it }

    // Address
    request.addressLine1?.let { this.addressLine1 = it }
    request.addressLine2?.let { this.addressLine2 = it }
    request.city?.let { this.city = it }
    request.state?.let { this.state = it }
    request.postalCode?.let { this.postalCode = it }
    request.country?.let { this.country = it }

    // Location
    request.latitude?.let { this.latitude = it }
    request.longitude?.let { this.longitude = it }

    // Contact
    request.phone?.let { this.phone = it }
    request.email?.let { this.email = it }
    request.website?.let { this.website = it }

    // Tax/Regulatory
    request.taxId?.let { this.taxId = it }
    request.registrationNumber?.let { this.registrationNumber = it }

    // Operational Config
    request.timezone?.let { this.timezone = it }
    request.currency?.let { this.currency = it }
    request.language?.let { this.language = it }
    request.dateFormat?.let { this.dateFormat = it }
    request.timeFormat?.let { this.timeFormat = it }

    // Business Hours
    request.openingHours?.let { this.openingHours = it }
    request.closingHours?.let { this.closingHours = it }
    request.operatingDays?.let { this.operatingDays = it }

    // Status
    request.active?.let { this.active = it }

    // Audit
    this.updatedBy = updatedBy

    // updatedAt is auto-set by @PreUpdate in OwnableBaseDomain

    return this
}
