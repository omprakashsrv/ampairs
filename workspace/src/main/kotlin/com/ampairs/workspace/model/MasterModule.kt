package com.ampairs.workspace.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.workspace.model.enums.*
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/**
 * Module configuration metadata
 */
data class ModuleConfiguration(
    var requiredPermissions: List<String> = emptyList(),
    var optionalPermissions: List<String> = emptyList(),
    var defaultEnabled: Boolean = true,
    var dependencies: List<String> = emptyList(),
    var conflictsWith: List<String> = emptyList(),
    var customSettings: Map<String, Any> = emptyMap()
)

/**
 * Module UI and presentation metadata
 */
data class ModuleUIMetadata(
    var icon: String = "apps",
    var primaryColor: String = "#6750A4",
    var secondaryColor: String? = null,
    var backgroundColor: String? = null,
    var screenshots: List<String> = emptyList(),
    var bannerImage: String? = null,
    var demoUrl: String? = null,
    var videoUrl: String? = null,
    var tags: List<String> = emptyList(),
    var keywords: List<String> = emptyList()
)

/**
 * Business relevance scoring for different business types
 */
data class BusinessRelevance(
    var businessType: BusinessType,
    var relevanceScore: Int, // 1-10, higher = more relevant
    var isEssential: Boolean = false,
    var recommendationReason: String? = null
)

/**
 * Master registry of business modules available in the Ampairs ecosystem.
 * This serves as the central catalog from which workspaces can select modules.
 */
@Entity
@Table(
    name = "master_modules",
    indexes = [
        Index(name = "idx_master_module_code", columnList = "module_code", unique = true),
        Index(name = "idx_master_module_category", columnList = "category"),
        Index(name = "idx_master_module_status", columnList = "status"),
        Index(name = "idx_master_module_tier", columnList = "required_tier"),
        Index(name = "idx_master_module_complexity", columnList = "complexity"),
        Index(name = "idx_master_module_active", columnList = "active"),
        Index(name = "idx_master_module_featured", columnList = "featured")
    ]
)
class MasterModule : BaseDomain() {

    /**
     * Unique identifier code for the module (e.g., "customer-management")
     */
    @Column(name = "module_code", nullable = false, unique = true, length = 100)
    var moduleCode: String = ""

    /**
     * Human-readable name of the module
     */
    @Column(name = "name", nullable = false, length = 200)
    var name: String = ""

    /**
     * Detailed description of the module functionality
     */
    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null

    /**
     * Short tagline or summary
     */
    @Column(name = "tagline", length = 500)
    var tagline: String? = null

    /**
     * Module category for organization
     */
    @Column(name = "category", nullable = false)
    @Enumerated(EnumType.STRING)
    var category: ModuleCategory = ModuleCategory.ADMINISTRATION

    /**
     * Current status of the module
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: ModuleStatus = ModuleStatus.ACTIVE

    /**
     * Minimum subscription tier required
     */
    @Column(name = "required_tier", nullable = false)
    @Enumerated(EnumType.STRING)
    var requiredTier: SubscriptionTier = SubscriptionTier.FREE

    /**
     * Minimum user role required to access this module
     */
    @Column(name = "required_role", nullable = false)
    @Enumerated(EnumType.STRING)
    var requiredRole: UserRole = UserRole.EMPLOYEE

    /**
     * Module complexity level
     */
    @Column(name = "complexity", nullable = false)
    @Enumerated(EnumType.STRING)
    var complexity: ModuleComplexity = ModuleComplexity.STANDARD

    /**
     * Current version of the module
     */
    @Column(name = "version", nullable = false, length = 50)
    var version: String = "1.0.0"

    /**
     * Business relevance for different business types (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "business_relevance", columnDefinition = "JSON")
    var businessRelevance: List<BusinessRelevance> = emptyList()

    /**
     * Module configuration and metadata (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", columnDefinition = "JSON")
    var configuration: ModuleConfiguration = ModuleConfiguration()

    /**
     * UI presentation metadata (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ui_metadata", columnDefinition = "JSON")
    var uiMetadata: ModuleUIMetadata = ModuleUIMetadata()

    /**
     * Module developer/provider
     */
    @Column(name = "provider", length = 255)
    var provider: String = "Ampairs"

    /**
     * Support contact email
     */
    @Column(name = "support_email", length = 255)
    var supportEmail: String? = null

    /**
     * Documentation URL
     */
    @Column(name = "documentation_url", length = 500)
    var documentationUrl: String? = null

    /**
     * Module homepage URL
     */
    @Column(name = "homepage_url", length = 500)
    var homepageUrl: String? = null

    /**
     * Installation/setup guide URL
     */
    @Column(name = "setup_guide_url", length = 500)
    var setupGuideUrl: String? = null

    /**
     * Estimated size requirement in MB
     */
    @Column(name = "size_mb", nullable = false)
    var sizeMb: Int = 0

    /**
     * Number of installations across all workspaces
     */
    @Column(name = "install_count", nullable = false)
    var installCount: Int = 0

    /**
     * Average user rating (1.0 to 5.0)
     */
    @Column(name = "rating")
    var rating: Double = 0.0

    /**
     * Number of ratings
     */
    @Column(name = "rating_count", nullable = false)
    var ratingCount: Int = 0

    /**
     * Whether this module is featured in recommendations
     */
    @Column(name = "featured", nullable = false)
    var featured: Boolean = false

    /**
     * Display order for sorting
     */
    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    /**
     * Whether this module is active and available
     */
    @Column(name = "active", nullable = false)
    var active: Boolean = true

    /**
     * Release notes or changelog
     */
    @Column(name = "release_notes", columnDefinition = "TEXT")
    var releaseNotes: String? = null

    /**
     * Last update timestamp
     */
    @Column(name = "last_updated_at")
    var lastUpdatedAt: LocalDateTime? = null

    // JPA Relationships

    /**
     * Workspace modules using this master module
     */
    @OneToMany(mappedBy = "masterModule", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    var workspaceModules: MutableSet<WorkspaceModule> = mutableSetOf()

    override fun obtainSeqIdPrefix(): String {
        return "MMD" // Master Module
    }

    /**
     * Check if module is available for a subscription tier
     */
    fun isAvailableForTier(tier: SubscriptionTier): Boolean {
        return tier.supportsModule(requiredTier)
    }

    /**
     * Check if user has required role to access module
     */
    fun hasRequiredRole(userRole: UserRole): Boolean {
        return userRole.hasAccessLevel(requiredRole)
    }

    /**
     * Get relevance score for a specific business type
     */
    fun getRelevanceScore(businessType: BusinessType): Int {
        return businessRelevance.find { it.businessType == businessType }?.relevanceScore ?: 0
    }

    /**
     * Check if module is essential for a business type
     */
    fun isEssentialFor(businessType: BusinessType): Boolean {
        return businessRelevance.find { it.businessType == businessType }?.isEssential ?: false
    }

    /**
     * Get recommendation reason for a business type
     */
    fun getRecommendationReason(businessType: BusinessType): String? {
        return businessRelevance.find { it.businessType == businessType }?.recommendationReason
    }

    /**
     * Check if module is ready for production use
     */
    fun isProductionReady(): Boolean {
        return status in listOf(ModuleStatus.ACTIVE) && active
    }

    /**
     * Check if module has dependencies
     */
    fun hasDependencies(): Boolean {
        return configuration.dependencies.isNotEmpty()
    }

    /**
     * Get missing dependencies for a workspace
     */
    fun getMissingDependencies(installedModules: Set<String>): List<String> {
        return configuration.dependencies.filter { dependency ->
            !installedModules.contains(dependency)
        }
    }

    /**
     * Check if module conflicts with installed modules
     */
    fun hasConflicts(installedModules: Set<String>): List<String> {
        return configuration.conflictsWith.filter { conflictModule ->
            installedModules.contains(conflictModule)
        }
    }

    /**
     * Update rating
     */
    fun updateRating(newRating: Double) {
        require(newRating in 1.0..5.0) { "Rating must be between 1.0 and 5.0" }
        val totalRating = (rating * ratingCount) + newRating
        ratingCount++
        rating = totalRating / ratingCount
    }

    /**
     * Increment install count
     */
    fun incrementInstallCount() {
        installCount++
    }

    /**
     * Decrement install count
     */
    fun decrementInstallCount() {
        if (installCount > 0) {
            installCount--
        }
    }

    /**
     * Get display icon with fallback
     */
    fun getDisplayIcon(): String {
        return uiMetadata.icon.ifBlank { "apps" }
    }

    /**
     * Get primary color with fallback
     */
    fun getPrimaryColor(): String {
        return uiMetadata.primaryColor.ifBlank { "#6750A4" }
    }

    /**
     * Get summary for listings
     */
    fun getSummary(): String {
        return tagline ?: description?.take(200) ?: "No description available"
    }

    /**
     * Check if module requires specific permissions
     */
    fun requiresPermissions(): Boolean {
        return configuration.requiredPermissions.isNotEmpty()
    }

    /**
     * Get all tags and keywords for search
     */
    fun getSearchTerms(): List<String> {
        return (uiMetadata.tags + uiMetadata.keywords + listOf(name, moduleCode)).distinct()
    }
}