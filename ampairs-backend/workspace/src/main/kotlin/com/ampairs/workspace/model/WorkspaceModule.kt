package com.ampairs.workspace.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.workspace.model.enums.WorkspaceModuleStatus
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.TenantId
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/**
 * Workspace-specific module settings and customizations
 */
data class ModuleSettings(
    var customName: String? = null,
    var customDescription: String? = null,
    var customIcon: String? = null,
    var customColor: String? = null,
    var displayOrder: Int = 0,
    var visibility: String = "VISIBLE", // VISIBLE, HIDDEN, ADMIN_ONLY
    var enabledFeatures: List<String> = emptyList(),
    var disabledFeatures: List<String> = emptyList(),
    var customConfiguration: Map<String, Any> = emptyMap(),
    var notificationsEnabled: Boolean = true,
    var autoUpdate: Boolean = true,
    var quickAccess: Boolean = false // Show in quick access bar
)

/**
 * Module usage analytics and performance metrics
 */
data class ModuleUsageMetrics(
    var totalAccesses: Int = 0,
    var uniqueUsers: Int = 0,
    var lastAccessedAt: LocalDateTime? = null,
    var averageSessionDuration: Long = 0, // in seconds
    var totalUsageDuration: Long = 0, // in seconds
    var errorCount: Int = 0,
    var lastErrorAt: LocalDateTime? = null,
    var performanceScore: Double = 1.0, // 0.0 to 1.0
    var userSatisfactionScore: Double = 0.0, // 0.0 to 5.0
    var featureUsageStats: Map<String, Int> = emptyMap(),
    var monthlyActiveUsers: Int = 0,
    var weeklyActiveUsers: Int = 0,
    var dailyActiveUsers: Int = 0
)

/**
 * User-specific module preferences and permissions
 */
data class UserModulePreferences(
    var userId: String,
    var customName: String? = null,
    var customIcon: String? = null,
    var displayOrder: Int? = null,
    var quickAccess: Boolean = false,
    var notificationsEnabled: Boolean = true,
    var lastAccessedAt: LocalDateTime? = null,
    var accessCount: Int = 0,
    var favorited: Boolean = false,
    var grantedPermissions: List<String> = emptyList()
)

/**
 * Represents a business module that has been installed and configured for a specific workspace.
 * Links the master module registry to workspace-specific configurations and user preferences.
 */
@Entity
@Table(
    name = "workspace_modules",
    indexes = [
        Index(name = "idx_workspace_module_workspace", columnList = "workspace_id"),
        Index(name = "idx_workspace_module_master", columnList = "master_module_id"),
        Index(name = "idx_workspace_module_unique", columnList = "workspace_id,master_module_id", unique = true),
        Index(name = "idx_workspace_module_status", columnList = "status"),
        Index(name = "idx_workspace_module_enabled", columnList = "enabled"),
        Index(name = "idx_workspace_module_installed", columnList = "installed_at"),
        Index(name = "idx_workspace_module_order", columnList = "display_order"),
        Index(name = "idx_workspace_module_category", columnList = "category_override")
    ]
)
class WorkspaceModule : BaseDomain() {

    /**
     * ID of the workspace this module belongs to (tenant ID for multi-tenancy)
     */
    @Column(name = "workspace_id", nullable = false, length = 36)
    @TenantId
    var workspaceId: String = TenantContextHolder.getCurrentTenant() ?: ""

    /**
     * Reference to the master module definition
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "master_module_id", nullable = false)
    var masterModule: MasterModule = MasterModule()

    /**
     * Current status of this module in the workspace
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: WorkspaceModuleStatus = WorkspaceModuleStatus.INSTALLED

    /**
     * Whether this module is currently enabled for use
     */
    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true

    /**
     * Version of the module installed in this workspace
     */
    @Column(name = "installed_version", nullable = false, length = 50)
    var installedVersion: String = ""

    /**
     * When the module was first installed in this workspace
     */
    @Column(name = "installed_at", nullable = false)
    var installedAt: LocalDateTime = LocalDateTime.now()

    /**
     * User who installed the module
     */
    @Column(name = "installed_by", length = 36)
    var installedBy: String? = null

    /**
     * Name of user who installed the module
     */
    @Column(name = "installed_by_name", length = 255)
    var installedByName: String? = null

    /**
     * When the module was last updated
     */
    @Column(name = "last_updated_at")
    var lastUpdatedAt: LocalDateTime? = null

    /**
     * User who last updated the module
     */
    @Column(name = "last_updated_by", length = 36)
    var lastUpdatedBy: String? = null

    /**
     * Override category for workspace-specific organization
     */
    @Column(name = "category_override", length = 100)
    var categoryOverride: String? = null

    /**
     * Display order for module arrangement in workspace
     */
    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    /**
     * Workspace-specific module settings and customizations (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "JSON")
    var settings: ModuleSettings = ModuleSettings()

    /**
     * Usage metrics and analytics (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "usage_metrics", columnDefinition = "JSON")
    var usageMetrics: ModuleUsageMetrics = ModuleUsageMetrics()

    /**
     * User-specific preferences and permissions (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "user_preferences", columnDefinition = "JSON")
    var userPreferences: List<UserModulePreferences> = emptyList()

    /**
     * Module-specific data and configuration (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "module_data", columnDefinition = "JSON")
    var moduleData: Map<String, Any> = emptyMap()

    /**
     * License information if required
     */
    @Column(name = "license_info", length = 1000)
    var licenseInfo: String? = null

    /**
     * License expiration date
     */
    @Column(name = "license_expires_at")
    var licenseExpiresAt: LocalDateTime? = null

    /**
     * Storage space used by this module in MB
     */
    @Column(name = "storage_used_mb", nullable = false)
    var storageUsedMb: Int = 0

    /**
     * Configuration notes or comments
     */
    @Column(name = "configuration_notes", columnDefinition = "TEXT")
    var configurationNotes: String? = null

    override fun obtainSeqIdPrefix(): String {
        return "WMD" // Workspace Module
    }

    /**
     * Check if module is operational (enabled and active status)
     */
    fun isOperational(): Boolean {
        return enabled && status == WorkspaceModuleStatus.ACTIVE
    }

    /**
     * Check if license is valid (if license is required)
     */
    fun hasValidLicense(): Boolean {
        return licenseInfo == null || licenseExpiresAt?.let { it.isAfter(LocalDateTime.now()) } ?: true
    }

    /**
     * Get effective display name (custom name or master module name)
     */
    fun getEffectiveName(): String {
        return settings.customName ?: masterModule.name
    }

    /**
     * Get effective description (custom or master module description)
     */
    fun getEffectiveDescription(): String {
        return settings.customDescription ?: masterModule.description ?: ""
    }

    /**
     * Get effective icon (custom or master module icon)
     */
    fun getEffectiveIcon(): String {
        return settings.customIcon ?: masterModule.getDisplayIcon()
    }

    /**
     * Get effective color (custom or master module color)
     */
    fun getEffectiveColor(): String {
        return settings.customColor ?: masterModule.getPrimaryColor()
    }

    /**
     * Get effective category (override or master module category)
     */
    fun getEffectiveCategory(): String {
        return categoryOverride ?: masterModule.category.name
    }

    /**
     * Check if user has specific preferences for this module
     */
    fun getUserPreferences(userId: String): UserModulePreferences? {
        return userPreferences.find { it.userId == userId }
    }

    /**
     * Update or create user preferences for this module
     */
    fun updateUserPreferences(userId: String, preferences: UserModulePreferences) {
        userPreferences = userPreferences.filter { it.userId != userId } + preferences.copy(userId = userId)
    }

    /**
     * Record module access by user
     */
    fun recordAccess(userId: String) {
        // Update overall metrics
        usageMetrics = usageMetrics.copy(
            totalAccesses = usageMetrics.totalAccesses + 1,
            lastAccessedAt = LocalDateTime.now()
        )

        // Update user-specific preferences
        val currentPrefs = getUserPreferences(userId) ?: UserModulePreferences(userId = userId)
        val updatedPrefs = currentPrefs.copy(
            lastAccessedAt = LocalDateTime.now(),
            accessCount = currentPrefs.accessCount + 1
        )
        updateUserPreferences(userId, updatedPrefs)
    }

    /**
     * Record an error occurrence
     */
    fun recordError() {
        usageMetrics = usageMetrics.copy(
            errorCount = usageMetrics.errorCount + 1,
            lastErrorAt = LocalDateTime.now()
        )
    }

    /**
     * Update user satisfaction score
     */
    fun updateSatisfactionScore(score: Double) {
        require(score in 0.0..5.0) { "Satisfaction score must be between 0.0 and 5.0" }
        usageMetrics = usageMetrics.copy(userSatisfactionScore = score)
    }

    /**
     * Check if module can be updated to a newer version
     */
    fun canBeUpdated(): Boolean {
        return installedVersion != masterModule.version && masterModule.isProductionReady()
    }

    /**
     * Update to latest version
     */
    fun updateToLatestVersion(updatedBy: String? = null) {
        installedVersion = masterModule.version
        lastUpdatedAt = LocalDateTime.now()
        lastUpdatedBy = updatedBy
    }

    /**
     * Check if module is visible to users based on role
     */
    fun isVisibleToUser(userRole: String): Boolean {
        return when (settings.visibility) {
            "VISIBLE" -> true
            "HIDDEN" -> false
            "ADMIN_ONLY" -> userRole in listOf("ADMIN", "OWNER")
            else -> true
        }
    }

    /**
     * Get module configuration value
     */
    fun getConfigValue(key: String): Any? {
        return settings.customConfiguration[key]
    }

    /**
     * Set module configuration value
     */
    fun setConfigValue(key: String, value: Any) {
        settings = settings.copy(
            customConfiguration = settings.customConfiguration.toMutableMap().apply { put(key, value) }
        )
    }

    /**
     * Check if specific feature is enabled
     */
    fun isFeatureEnabled(feature: String): Boolean {
        return when {
            settings.disabledFeatures.contains(feature) -> false
            settings.enabledFeatures.isNotEmpty() -> settings.enabledFeatures.contains(feature)
            else -> true // Default enabled if no specific configuration
        }
    }

    /**
     * Enable a specific feature
     */
    fun enableFeature(feature: String) {
        settings = settings.copy(
            enabledFeatures = (settings.enabledFeatures + feature).distinct(),
            disabledFeatures = settings.disabledFeatures - feature
        )
    }

    /**
     * Disable a specific feature
     */
    fun disableFeature(feature: String) {
        settings = settings.copy(
            disabledFeatures = (settings.disabledFeatures + feature).distinct(),
            enabledFeatures = settings.enabledFeatures - feature
        )
    }

    /**
     * Check if module needs attention (errors, updates, license expiry)
     */
    fun needsAttention(): Boolean {
        return usageMetrics.errorCount > 0 ||
                canBeUpdated() ||
                !hasValidLicense() ||
                status != WorkspaceModuleStatus.ACTIVE
    }

    /**
     * Get health score (0.0 to 1.0)
     */
    fun getHealthScore(): Double {
        var score = 1.0

        // Deduct for errors
        if (usageMetrics.totalAccesses > 0) {
            val errorRate = usageMetrics.errorCount.toDouble() / usageMetrics.totalAccesses
            score -= errorRate * 0.3
        }

        // Deduct for outdated version
        if (canBeUpdated()) {
            score -= 0.1
        }

        // Deduct for invalid license
        if (!hasValidLicense()) {
            score -= 0.2
        }

        // Deduct for inactive status
        if (!isOperational()) {
            score -= 0.4
        }

        return maxOf(0.0, score)
    }

    /**
     * Get user engagement level (0.0 to 1.0)
     */
    fun getUserEngagementLevel(): Double {
        val activeUsers = usageMetrics.dailyActiveUsers
        val totalUsers = userPreferences.size

        return if (totalUsers > 0) {
            activeUsers.toDouble() / totalUsers
        } else 0.0
    }

    /**
     * Check if module is popular (high usage and satisfaction)
     */
    fun isPopular(): Boolean {
        return usageMetrics.totalAccesses > 100 &&
                usageMetrics.userSatisfactionScore > 4.0 &&
                getUserEngagementLevel() > 0.5
    }

    /**
     * Get module display priority (for sorting)
     */
    fun getDisplayPriority(): Int {
        var priority = settings.displayOrder

        // Boost priority for quick access modules
        if (settings.quickAccess) priority -= 1000

        // Boost priority for frequently used modules
        if (usageMetrics.totalAccesses > 50) priority -= 100

        return priority
    }
}