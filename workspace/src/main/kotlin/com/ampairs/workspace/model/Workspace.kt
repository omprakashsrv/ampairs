package com.ampairs.workspace.model

import com.ampairs.core.config.Constants
import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.workspace.model.enums.SubscriptionPlan
import com.ampairs.workspace.model.enums.WorkspaceStatus
import com.ampairs.workspace.model.enums.WorkspaceType
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Core workspace entity representing a business organization within the Ampairs system.
 * Each workspace is a multi-tenant boundary containing users, customers, products, orders, and invoices.
 */
@Entity
@Table(
    name = "workspaces",
    indexes = [
        Index(name = "idx_workspace_slug", columnList = "slug", unique = true),
        Index(name = "idx_workspace_tenant", columnList = "tenant_id"),
        Index(name = "idx_workspace_owner", columnList = "created_by"),
        Index(name = "idx_workspace_status", columnList = "status"),
        Index(name = "idx_workspace_type", columnList = "workspace_type"),
        Index(name = "idx_workspace_active", columnList = "last_activity_at")
    ]
)
class Workspace : BaseDomain() {

    /**
     * Business name of the workspace/company
     */
    @Column(name = "name", nullable = false, length = 100)
    var name: String = ""

    /**
     * URL-friendly identifier for the workspace (e.g., "acme-corp")
     */
    @Column(name = "slug", nullable = false, unique = true, length = 50)
    var slug: String = ""

    /**
     * Business description and purpose
     */
    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null

    /**
     * Type of business/workspace
     */
    @Column(name = "workspace_type", nullable = false)
    @Enumerated(EnumType.STRING)
    var workspaceType: WorkspaceType = WorkspaceType.BUSINESS

    /**
     * Company logo/avatar URL
     */
    @Column(name = "avatar_url", length = 500)
    var avatarUrl: String? = null

    /**
     * Business address line 1
     */
    @Column(name = "address_line1", length = 255)
    var addressLine1: String? = null

    /**
     * Business address line 2
     */
    @Column(name = "address_line2", length = 255)
    var addressLine2: String? = null

    /**
     * Business city
     */
    @Column(name = "city", length = 100)
    var city: String? = null

    /**
     * Business state/province
     */
    @Column(name = "state", length = 100)
    var state: String? = null

    /**
     * Business postal code
     */
    @Column(name = "postal_code", length = 20)
    var postalCode: String? = null

    /**
     * Business country
     */
    @Column(name = "country", length = 100)
    var country: String? = null

    /**
     * Primary business phone number
     */
    @Column(name = "phone", length = 20)
    var phone: String? = null

    /**
     * Primary business email
     */
    @Column(name = "email", length = 255)
    var email: String? = null

    /**
     * Business website URL
     */
    @Column(name = "website", length = 255)
    var website: String? = null

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
     * Current subscription plan
     */
    @Column(name = "subscription_plan", nullable = false)
    @Enumerated(EnumType.STRING)
    var subscriptionPlan: SubscriptionPlan = SubscriptionPlan.FREE

    /**
     * Maximum number of users allowed
     */
    @Column(name = "max_users", nullable = false)
    var maxUsers: Int = 3

    /**
     * Storage limit in GB
     */
    @Column(name = "storage_limit_gb", nullable = false)
    var storageLimitGb: Int = 1

    /**
     * Current storage usage in GB
     */
    @Column(name = "storage_used_gb", nullable = false)
    var storageUsedGb: Int = 0

    /**
     * Last activity timestamp
     */
    @Column(name = "last_activity_at")
    var lastActivityAt: LocalDateTime? = null

    /**
     * Workspace status
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: WorkspaceStatus = WorkspaceStatus.ACTIVE

    /**
     * Business timezone
     */
    @Column(name = "timezone", length = 50)
    var timezone: String = "UTC"

    /**
     * Primary language
     */
    @Column(name = "language", length = 10)
    var language: String = "en"

    /**
     * Currency for business transactions
     */
    @Column(name = "currency", length = 3)
    var currency: String = "USD"

    /**
     * Date format preference
     */
    @Column(name = "date_format", length = 20)
    var dateFormat: String = "DD/MM/YYYY"

    /**
     * Time format preference
     */
    @Column(name = "time_format", length = 10)
    var timeFormat: String = "24H"

    /**
     * Subscription last updated timestamp
     */
    @Column(name = "subscription_updated_at")
    var subscriptionUpdatedAt: LocalDateTime? = null

    /**
     * Trial expiration date
     */
    @Column(name = "trial_expires_at")
    var trialExpiresAt: LocalDateTime? = null

    /**
     * Business hours start time (24H format)
     */
    @Column(name = "business_hours_start", length = 5)
    var businessHoursStart: String? = null

    /**
     * Business hours end time (24H format)
     */
    @Column(name = "business_hours_end", length = 5)
    var businessHoursEnd: String? = null

    /**
     * Working days (JSON array of day names)
     */
    @Column(name = "working_days", columnDefinition = "TEXT")
    var workingDays: String = "[\"Monday\",\"Tuesday\",\"Wednesday\",\"Thursday\",\"Friday\"]"

    /**
     * Custom workspace settings (JSON)
     */
    @Column(name = "settings", columnDefinition = "TEXT")
    var settings: String = "{}"

    /**
     * Feature flags enabled for this workspace (JSON)
     */
    @Column(name = "features", columnDefinition = "TEXT")
    var features: String = "{}"

    /**
     * Whether this workspace is active
     */
    @Column(name = "active", nullable = false)
    var active: Boolean = true

    /**
     * ID of the user who created this workspace
     */
    @Column(name = "created_by", length = 36)
    var createdBy: String? = null

    // JPA Relationships
    
    /**
     * Workspace members (using JoinColumn instead of mappedBy to avoid entity conflicts)
     */
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", referencedColumnName = "uid")
    @JsonIgnore
    var members: MutableSet<WorkspaceMember> = mutableSetOf()

    /**
     * Workspace invitations (using JoinColumn instead of mappedBy to avoid entity conflicts)
     */
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", referencedColumnName = "uid")
    @JsonIgnore
    var invitations: MutableSet<WorkspaceInvitation> = mutableSetOf()

    /**
     * Workspace settings (using JoinColumn instead of mappedBy to avoid entity conflicts)
     */
    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", referencedColumnName = "uid")
    @JsonIgnore
    var workspaceSettings: WorkspaceSettings? = null

    /**
     * Workspace activities (using JoinColumn instead of mappedBy to avoid entity conflicts)
     */
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", referencedColumnName = "uid")
    @JsonIgnore
    var activities: MutableSet<WorkspaceActivity> = mutableSetOf()

    override fun obtainSeqIdPrefix(): String {
        return Constants.WORKSPACE_PREFIX
    }

    /**
     * Check if workspace can add more users
     */
    fun canAddUsers(additionalUsers: Int = 1): Boolean {
        val currentUserCount = members.count { it.isActive }
        return (currentUserCount + additionalUsers) <= maxUsers
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
            "multi_location" -> subscriptionPlan != SubscriptionPlan.FREE
            "custom_reports" -> subscriptionPlan.hasAdvancedFeatures
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

    /**
     * Get active member count
     */
    fun getActiveMemberCount(): Int {
        return members.count { it.isActive }
    }

    /**
     * Get full business address
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
     * Check if workspace is active and operational
     */
    fun isOperational(): Boolean {
        return status == WorkspaceStatus.ACTIVE && !isTrialExpired()
    }

    /**
     * Check if trial has expired
     */
    private fun isTrialExpired(): Boolean {
        return trialExpiresAt?.let { it.isBefore(LocalDateTime.now()) } ?: false
    }

    /**
     * Get workspace display name (name or slug)
     */
    fun getDisplayName(): String {
        return if (name.isNotBlank()) name else slug
    }
}