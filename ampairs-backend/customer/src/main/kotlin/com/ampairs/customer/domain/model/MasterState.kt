package com.ampairs.customer.domain.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.customer.config.Constants
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

/**
 * Master registry of states and countries available in the Ampairs system.
 * This serves as the central catalog from which workspaces can import states.
 */
@Entity
@Table(
    name = "master_states",
    indexes = [
        Index(name = "idx_master_state_code", columnList = "state_code", unique = true),
        Index(name = "idx_master_state_country", columnList = "country_code"),
        Index(name = "idx_master_state_name", columnList = "name"),
        Index(name = "idx_master_state_active", columnList = "active"),
        Index(name = "idx_master_state_featured", columnList = "featured")
    ]
)
class MasterState : BaseDomain() {

    /**
     * Unique identifier code for the state (e.g., "IN-MH" for Maharashtra, India)
     */
    @Column(name = "state_code", nullable = false, unique = true, length = 10)
    var stateCode: String = ""

    /**
     * Full name of the state/province
     */
    @Column(name = "name", nullable = false, length = 100)
    var name: String = ""

    /**
     * Short name or abbreviation (e.g., "MH" for Maharashtra)
     */
    @Column(name = "short_name", nullable = false, length = 10)
    var shortName: String = ""

    /**
     * Country code (ISO 3166-1 alpha-2, e.g., "IN" for India)
     */
    @Column(name = "country_code", nullable = false, length = 2)
    var countryCode: String = ""

    /**
     * Full country name
     */
    @Column(name = "country_name", nullable = false, length = 100)
    var countryName: String = ""

    /**
     * Region or zone within the country (e.g., "Western India", "Northern Region")
     */
    @Column(name = "region", length = 100)
    var region: String? = null

    /**
     * Time zone identifier (e.g., "Asia/Kolkata")
     */
    @Column(name = "timezone", length = 50)
    var timezone: String? = null

    /**
     * Local language name of the state
     */
    @Column(name = "local_name", length = 100)
    var localName: String? = null

    /**
     * Capital city of the state
     */
    @Column(name = "capital", length = 100)
    var capital: String? = null

    /**
     * Population of the state (approximate)
     */
    @Column(name = "population")
    var population: Long? = null

    /**
     * Area in square kilometers
     */
    @Column(name = "area_sq_km")
    var areaSqKm: Double? = null

    /**
     * Tax/GST state code for Indian states (e.g., "27" for Maharashtra)
     */
    @Column(name = "gst_code", length = 2)
    var gstCode: String? = null

    /**
     * Postal code patterns or ranges for the state
     */
    @Column(name = "postal_code_pattern", length = 50)
    var postalCodePattern: String? = null

    /**
     * Whether this state is commonly used/featured
     */
    @Column(name = "featured", nullable = false)
    var featured: Boolean = false

    /**
     * Display order for sorting states
     */
    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    /**
     * Whether this state is active and available for selection
     */
    @Column(name = "active", nullable = false)
    var active: Boolean = true

    /**
     * Additional metadata in JSON format
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    var metadata: String? = null

    // JPA Relationships

    /**
     * Workspace states using this master state
     */
    @OneToMany(mappedBy = "masterState", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    var workspaceStates: MutableSet<State> = mutableSetOf()

    override fun obtainSeqIdPrefix(): String {
        return Constants.MASTER_STATE_PREFIX
    }

    /**
     * Check if this is an Indian state (for GST compliance)
     */
    fun isIndianState(): Boolean {
        return countryCode == "IN"
    }

    /**
     * Get formatted display name with country
     */
    fun getDisplayName(): String {
        return "$name, $countryName"
    }

    /**
     * Get formatted state code for GST (Indian states only)
     */
    fun getGstStateCode(): String? {
        return if (isIndianState()) gstCode else null
    }

    /**
     * Check if postal code matches pattern
     */
    fun isValidPostalCode(postalCode: String): Boolean {
        return postalCodePattern?.let { pattern ->
            postalCode.matches(Regex(pattern))
        } ?: true
    }

    /**
     * Get timezone or default
     */
    fun getTimezoneOrDefault(): String {
        return timezone ?: when (countryCode) {
            "IN" -> "Asia/Kolkata"
            "US" -> "America/New_York"
            "GB" -> "Europe/London"
            else -> "UTC"
        }
    }

    /**
     * Check if state is a union territory (Indian specific)
     */
    fun isUnionTerritory(): Boolean {
        return isIndianState() && listOf(
            "IN-AN", "IN-CH", "IN-DN", "IN-DL", "IN-JK", "IN-LA", "IN-LD", "IN-PY"
        ).contains(stateCode)
    }

    /**
     * Get population density (people per sq km)
     */
    fun getPopulationDensity(): Double? {
        return if (population != null && areaSqKm != null && areaSqKm!! > 0) {
            population!! / areaSqKm!!
        } else null
    }
}