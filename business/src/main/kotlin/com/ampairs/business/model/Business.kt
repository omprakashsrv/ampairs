package com.ampairs.business.model

import com.ampairs.business.model.enums.BusinessType
import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * Business entity representing a business profile and configuration.
 *
 * **Separation of Concerns**:
 * - Workspace entity: Tenant management, subscriptions, members
 * - Business entity: Business profile, operations, configuration
 *
 * **Multi-Tenancy**:
 * - Inherits ownerId from OwnableBaseDomain (with @TenantId for automatic filtering)
 * - ownerId represents the workspace this business belongs to
 * - One-to-one relationship: One Business per Workspace
 *
 * **Timezone Handling**:
 * - All timestamps use `Instant` (UTC, no ambiguity)
 * - Inherits createdAt/updatedAt from OwnableBaseDomain
 * - Serializes as ISO-8601 with Z: "2025-10-10T14:30:00Z"
 */
@Entity
@Table(
    name = "businesses",
    indexes = [
        Index(name = "idx_business_type", columnList = "business_type"),
        Index(name = "idx_business_active", columnList = "active"),
        Index(name = "idx_business_country", columnList = "country"),
        Index(name = "idx_business_created_at", columnList = "created_at")
    ]
)
class Business : OwnableBaseDomain() {

    // ==================== Profile Information ====================

    /**
     * Business/company name
     */
    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    /**
     * Type of business operation
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, length = 50)
    var businessType: BusinessType = BusinessType.RETAIL

    /**
     * Business description and purpose
     */
    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null

    /**
     * Business owner's name
     */
    @Column(name = "owner_name", length = 255)
    var ownerName: String? = null

    // ==================== Address Information ====================

    @Column(name = "address_line1", length = 255)
    var addressLine1: String? = null

    @Column(name = "address_line2", length = 255)
    var addressLine2: String? = null

    @Column(name = "city", length = 100)
    var city: String? = null

    @Column(name = "state", length = 100)
    var state: String? = null

    @Column(name = "postal_code", length = 20)
    var postalCode: String? = null

    @Column(name = "country", length = 100)
    var country: String? = null

    // ==================== Location (GPS Coordinates) ====================

    /**
     * GPS latitude for location-based features
     * Range: -90 to +90
     */
    @Column(name = "latitude")
    var latitude: Double? = null

    /**
     * GPS longitude for location-based features
     * Range: -180 to +180
     */
    @Column(name = "longitude")
    var longitude: Double? = null

    // ==================== Contact Information ====================

    /**
     * Primary phone number (E.164 format recommended)
     */
    @Column(name = "phone", length = 20)
    var phone: String? = null

    /**
     * Primary email address
     */
    @Column(name = "email", length = 255)
    var email: String? = null

    /**
     * Business website URL
     */
    @Column(name = "website", length = 500)
    var website: String? = null

    // ==================== Tax & Regulatory ====================

    /**
     * Tax identification number (GST, VAT, etc.)
     */
    @Column(name = "tax_id", length = 50)
    var taxId: String? = null

    /**
     * Business registration number
     */
    @Column(name = "registration_number", length = 100)
    var registrationNumber: String? = null

    /**
     * Tax settings by region (JSON)
     * Example: {"default_tax_code": "GST-18", "interstate_tax": "IGST"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tax_settings")
    var taxSettings: Map<String, Any>? = null

    /**
     * Custom attributes for flexible data storage (JSON)
     * Allows storing arbitrary key-value pairs without schema changes
     * Example: {"loyalty_program": "enabled", "custom_field_1": "value"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_attributes")
    var customAttributes: Map<String, Any>? = null

    // ==================== Operational Configuration ====================

    /**
     * Business timezone (IANA format: Asia/Kolkata, America/New_York)
     */
    @Column(name = "timezone", nullable = false, length = 50)
    var timezone: String = "UTC"

    /**
     * Currency code (ISO 4217: INR, USD, EUR)
     */
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "INR"

    /**
     * Language code (ISO 639-1: en, hi, es)
     */
    @Column(name = "language", nullable = false, length = 10)
    var language: String = "en"

    /**
     * Date format preference (DD-MM-YYYY, MM/DD/YYYY, YYYY-MM-DD)
     */
    @Column(name = "date_format", nullable = false, length = 20)
    var dateFormat: String = "DD-MM-YYYY"

    /**
     * Time format preference (12H, 24H)
     */
    @Column(name = "time_format", nullable = false, length = 10)
    var timeFormat: String = "12H"

    // ==================== Business Hours ====================

    /**
     * Opening time (HH:MM format, 24-hour)
     */
    @Column(name = "opening_hours", length = 5)
    var openingHours: String? = null

    /**
     * Closing time (HH:MM format, 24-hour)
     */
    @Column(name = "closing_hours", length = 5)
    var closingHours: String? = null

    /**
     * Operating days (JSON array)
     * Example: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "operating_days", nullable = false)
    var operatingDays: List<String> = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

    // ==================== Status ====================

    /**
     * Whether business is active
     */
    @Column(name = "active", nullable = false)
    var active: Boolean = true

    /**
     * User ID who created this business
     */
    @Column(name = "created_by", length = 36)
    var createdBy: String? = null

    /**
     * User ID who last updated this business
     */
    @Column(name = "updated_by", length = 36)
    var updatedBy: String? = null

    // ==================== Methods ====================

    /**
     * Sequential ID prefix for business entities
     */
    override fun obtainSeqIdPrefix(): String {
        return Constants.BUSINESS_PREFIX
    }

    /**
     * Get full address as a single string
     */
    fun getFullAddress(): String {
        val parts = listOfNotNull(
            addressLine1,
            addressLine2,
            city,
            state,
            postalCode,
            country
        ).filter { it.isNotBlank() }
        return parts.joinToString(", ")
    }

    /**
     * Validate business hours consistency
     * @throws IllegalStateException if closing hours are before opening hours
     */
    fun validateBusinessHours() {
        if (openingHours != null && closingHours != null) {
            val opening = openingHours!!.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
            val closing = closingHours!!.split(":").let { it[0].toInt() * 60 + it[1].toInt() }

            if (closing <= opening) {
                throw IllegalStateException("Closing hours ($closingHours) must be after opening hours ($openingHours)")
            }
        }
    }

    /**
     * Check if business operates on a specific day
     */
    fun operatesOn(dayName: String): Boolean {
        return operatingDays.any { it.equals(dayName, ignoreCase = true) }
    }

    /**
     * Get business type description
     */
    fun getBusinessTypeDescription(): String {
        return businessType.description
    }

    override fun toString(): String {
        return "Business(uid='$uid', name='$name', type=${businessType.name}, ownerId='$ownerId')"
    }
}
