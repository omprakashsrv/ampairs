package com.ampairs.workspace.model

import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.workspace.model.enums.SubscriptionPlan
import com.ampairs.workspace.model.enums.WorkspaceStatus
import com.ampairs.workspace.model.enums.WorkspaceType
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Modern SaaS workspace entity representing an isolated collaborative environment
 * where defined groups of users can access shared resources, data, and tools.
 */
@Entity(name = "workspaces")
@Table(
    name = "workspaces",
    indexes = [
        Index(name = "idx_workspace_slug", columnList = "slug", unique = true),
        Index(name = "idx_workspace_owner", columnList = "created_by"),
        Index(name = "idx_workspace_type", columnList = "workspace_type"),
        Index(name = "idx_workspace_active", columnList = "is_active")
    ]
)
class Workspace : OwnableBaseDomain() {

    /**
     * Human-readable name of the workspace
     */
    @Column(name = "name", nullable = false, length = 100)
    var name: String = ""

    /**
     * URL-friendly identifier for the workspace (e.g., "acme-corp")
     */
    @Column(name = "slug", nullable = false, unique = true, length = 50)
    var slug: String = ""

    /**
     * Optional description of the workspace purpose
     */
    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null

    /**
     * Type of workspace defining its intended use case
     */
    @Column(name = "workspace_type", nullable = false)
    @Enumerated(EnumType.STRING)
    var workspaceType: WorkspaceType = WorkspaceType.TEAM

    /**
     * URL to the workspace avatar/logo image
     */
    @Column(name = "avatar_url", length = 500)
    var avatarUrl: String? = null

    /**
     * JSON settings for workspace customization
     * Contains branding, preferences, and feature flags
     */
    @Column(name = "settings", columnDefinition = "TEXT")
    var settings: String = "{}"


    /**
     * Current subscription plan for this workspace
     */
    @Column(name = "subscription_plan", nullable = false)
    @Enumerated(EnumType.STRING)
    var subscriptionPlan: SubscriptionPlan = SubscriptionPlan.FREE

    /**
     * Maximum number of members allowed (based on subscription)
     */
    @Column(name = "max_members", nullable = false)
    var maxMembers: Int = 3

    /**
     * Storage limit in GB (based on subscription)
     */
    @Column(name = "storage_limit_gb", nullable = false)
    var storageLimitGb: Int = 1

    /**
     * Current storage usage in GB
     */
    @Column(name = "storage_used_gb", nullable = false)
    var storageUsedGb: Int = 0

    /**
     * When the workspace was last accessed by any member
     */
    @Column(name = "last_activity_at")
    var lastActivityAt: LocalDateTime? = null

    /**
     * Current status of the workspace
     */
    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    var status: WorkspaceStatus = WorkspaceStatus.ACTIVE

    /**
     * User ID who last updated this workspace
     */
    @Column(name = "updated_by", length = 36)
    var updatedBy: String? = null

    /**
     * Timezone setting for the workspace
     */
    @Column(name = "timezone", length = 50)
    var timezone: String = "UTC"

    /**
     * Primary language/locale for the workspace
     */
    @Column(name = "language", length = 10)
    var language: String = "en"

    /**
     * When the subscription was last updated
     */
    @Column(name = "subscription_updated_at")
    var subscriptionUpdatedAt: LocalDateTime? = null

    /**
     * Trial expiration date (if applicable)
     */
    @Column(name = "trial_expires_at")
    var trialExpiresAt: LocalDateTime? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.WORKSPACE_PREFIX
    }

    /**
     * Check if workspace is within member limits
     */
    fun canAddMembers(currentMemberCount: Int, additionalMembers: Int = 1): Boolean {
        return (currentMemberCount + additionalMembers) <= maxMembers
    }

    /**
     * Check if workspace is within storage limits
     */
    fun canUseStorage(additionalStorageGb: Int): Boolean {
        return (storageUsedGb + additionalStorageGb) <= storageLimitGb
    }

    /**
     * Check if workspace has a specific feature based on subscription plan
     */
    fun hasFeature(feature: String): Boolean {
        return when (feature) {
            "advanced_analytics" -> subscriptionPlan.hasAdvancedFeatures
            "api_access" -> subscriptionPlan.hasApiAccess
            "priority_support" -> subscriptionPlan.hasPrioritySupport
            "custom_branding" -> subscriptionPlan != SubscriptionPlan.FREE
            "unlimited_storage" -> subscriptionPlan == SubscriptionPlan.ENTERPRISE
            else -> false
        }
    }

    /**
     * Update last activity timestamp
     */
    fun recordActivity() {
        lastActivityAt = LocalDateTime.now()
    }

    /**
     * Check if workspace is in trial period
     */
    fun isInTrial(): Boolean {
        return trialExpiresAt?.let { it.isAfter(LocalDateTime.now()) } ?: false
    }
}