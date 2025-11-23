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

        // Logo (return API URLs, not S3 object keys)
        logoUrl = this.logoUrl?.let { "/api/v1/business/logo" },
        logoThumbnailUrl = this.logoThumbnailUrl?.let { "/api/v1/business/logo/thumbnail" },

        // Tax/Regulatory
        taxId = this.taxId,
        registrationNumber = this.registrationNumber,

        // Custom Attributes
        customAttributes = this.customAttributes,

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
        this.businessType = com.ampairs.business.model.enums.BusinessType.fromString(this@toBusiness.businessType)
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

        // Custom Attributes
        this.customAttributes = this@toBusiness.customAttributes

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

    // Custom Attributes
    request.customAttributes?.let { this.customAttributes = it }

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

// ==================== Specific Response DTOs ====================

/**
 * Convert Business entity to BusinessOverviewResponse DTO.
 * Contains summary information for dashboard display.
 */
fun Business.asBusinessOverviewResponse(): BusinessOverviewResponse {
    return BusinessOverviewResponse(
        uid = this.uid,
        name = this.name,
        businessType = this.businessType.name,
        currency = this.currency,
        timezone = this.timezone,
        email = this.email,
        phone = this.phone,
        address = this.getFullAddress(),
        customAttributes = this.customAttributes,
        active = this.active,
        createdAt = this.createdAt ?: Instant.now(),
        updatedAt = this.updatedAt ?: Instant.now()
    )
}

/**
 * Convert Business entity to BusinessProfileResponse DTO.
 * Contains detailed profile and registration information.
 */
fun Business.asBusinessProfileResponse(): BusinessProfileResponse {
    return BusinessProfileResponse(
        uid = this.uid,
        name = this.name,
        businessType = this.businessType.name,
        description = this.description,
        ownerName = this.ownerName,
        addressLine1 = this.addressLine1,
        addressLine2 = this.addressLine2,
        city = this.city,
        state = this.state,
        postalCode = this.postalCode,
        country = this.country,
        latitude = this.latitude,
        longitude = this.longitude,
        phone = this.phone,
        email = this.email,
        website = this.website,
        taxId = this.taxId,
        registrationNumber = this.registrationNumber,
        customAttributes = this.customAttributes,
        active = this.active,
        createdAt = this.createdAt ?: Instant.now(),
        updatedAt = this.updatedAt ?: Instant.now()
    )
}

/**
 * Convert Business entity to BusinessOperationsResponse DTO.
 * Contains operational configuration settings.
 */
fun Business.asBusinessOperationsResponse(): BusinessOperationsResponse {
    return BusinessOperationsResponse(
        uid = this.uid,
        timezone = this.timezone,
        currency = this.currency,
        language = this.language,
        dateFormat = this.dateFormat,
        timeFormat = this.timeFormat,
        openingHours = this.openingHours,
        closingHours = this.closingHours,
        operatingDays = this.operatingDays,
        updatedAt = this.updatedAt ?: Instant.now()
    )
}

/**
 * Convert Business entity to TaxConfigurationResponse DTO.
 * Contains tax-related configuration.
 */
fun Business.asTaxConfigurationResponse(): TaxConfigurationResponse {
    return TaxConfigurationResponse(
        uid = this.uid,
        taxId = this.taxId,
        registrationNumber = this.registrationNumber,
        taxSettings = this.taxSettings,
        country = this.country,
        state = this.state,
        updatedAt = this.updatedAt ?: Instant.now()
    )
}

// ==================== Apply Specific Updates ====================

/**
 * Apply BusinessProfileUpdateRequest to existing Business entity.
 */
fun Business.applyProfileUpdate(request: BusinessProfileUpdateRequest, updatedBy: String? = null): Business {
    this.name = request.name
    this.businessType = request.businessType
    this.description = request.description
    this.ownerName = request.ownerName
    this.addressLine1 = request.addressLine1
    this.addressLine2 = request.addressLine2
    this.city = request.city
    this.state = request.state
    this.postalCode = request.postalCode
    this.country = request.country
    this.latitude = request.latitude
    this.longitude = request.longitude
    this.phone = request.phone
    this.email = request.email
    this.website = request.website
    this.taxId = request.taxId
    this.registrationNumber = request.registrationNumber
    this.customAttributes = request.customAttributes
    this.active = request.active
    this.updatedBy = updatedBy
    return this
}

/**
 * Apply BusinessOperationsUpdateRequest to existing Business entity.
 */
fun Business.applyOperationsUpdate(request: BusinessOperationsUpdateRequest, updatedBy: String? = null): Business {
    this.timezone = request.timezone
    this.currency = request.currency
    this.language = request.language
    this.dateFormat = request.dateFormat
    this.timeFormat = request.timeFormat
    this.openingHours = request.openingHours
    this.closingHours = request.closingHours
    this.operatingDays = request.operatingDays
    this.updatedBy = updatedBy
    return this
}

/**
 * Apply TaxConfigurationUpdateRequest to existing Business entity.
 */
fun Business.applyTaxConfigUpdate(request: TaxConfigurationUpdateRequest, updatedBy: String? = null): Business {
    request.taxId?.let { this.taxId = it }
    request.registrationNumber?.let { this.registrationNumber = it }
    request.taxSettings?.let { this.taxSettings = it }
    this.updatedBy = updatedBy
    return this
}
