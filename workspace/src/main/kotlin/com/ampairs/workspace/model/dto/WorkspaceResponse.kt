package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.Workspace
import com.ampairs.workspace.model.enums.SubscriptionPlan
import com.ampairs.workspace.model.enums.WorkspaceType
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * Response DTO for workspace information
 */
data class WorkspaceResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("slug")
    val slug: String,

    @JsonProperty("description")
    val description: String?,

    @JsonProperty("workspace_type")
    val workspaceType: WorkspaceType,

    @JsonProperty("avatar_url")
    val avatarUrl: String?,

    @JsonProperty("is_active")
    val isActive: Boolean,

    @JsonProperty("subscription_plan")
    val subscriptionPlan: SubscriptionPlan,

    @JsonProperty("max_members")
    val maxMembers: Int,

    @JsonProperty("storage_limit_gb")
    val storageLimitGb: Int,

    @JsonProperty("storage_used_gb")
    val storageUsedGb: Int,

    @JsonProperty("timezone")
    val timezone: String,

    @JsonProperty("language")
    val language: String,

    @JsonProperty("created_by")
    val createdBy: String,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime,

    @JsonProperty("last_activity_at")
    val lastActivityAt: LocalDateTime?,

    @JsonProperty("trial_expires_at")
    val trialExpiresAt: LocalDateTime?,

    @JsonProperty("member_count")
    val memberCount: Int? = null,

    @JsonProperty("is_trial")
    val isTrial: Boolean? = null,

    @JsonProperty("storage_percentage")
    val storagePercentage: Double? = null,
)

/**
 * Simplified workspace response for lists
 */
data class WorkspaceListResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("slug")
    val slug: String,

    @JsonProperty("description")
    val description: String?,

    @JsonProperty("workspace_type")
    val workspaceType: WorkspaceType,

    @JsonProperty("avatar_url")
    val avatarUrl: String?,

    @JsonProperty("subscription_plan")
    val subscriptionPlan: SubscriptionPlan,

    @JsonProperty("member_count")
    val memberCount: Int,

    @JsonProperty("last_activity_at")
    val lastActivityAt: LocalDateTime?,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,
)

/**
 * Extension function to convert Workspace entity to WorkspaceResponse
 */
fun Workspace.toResponse(memberCount: Int? = null): WorkspaceResponse {
    return WorkspaceResponse(
        id = this.uid, // Use uid instead of id
        name = this.name,
        slug = this.slug,
        description = this.description,
        workspaceType = this.workspaceType,
        avatarUrl = this.avatarUrl,
        isActive = this.active,
        subscriptionPlan = this.subscriptionPlan,
        maxMembers = this.maxMembers,
        storageLimitGb = this.storageLimitGb,
        storageUsedGb = this.storageUsedGb,
        timezone = this.timezone,
        language = this.language,
        createdBy = this.ownerId, // Use ownerId instead of createdBy
        createdAt = this.createdAt ?: LocalDateTime.now(),
        updatedAt = this.updatedAt ?: LocalDateTime.now(),
        lastActivityAt = this.lastActivityAt,
        trialExpiresAt = this.trialExpiresAt,
        memberCount = memberCount,
        isTrial = this.isInTrial(),
        storagePercentage = if (storageLimitGb > 0) (storageUsedGb.toDouble() / storageLimitGb * 100) else 0.0
    )
}

/**
 * Extension function to convert Workspace entity to WorkspaceListResponse
 */
fun Workspace.toListResponse(memberCount: Int): WorkspaceListResponse {
    return WorkspaceListResponse(
        id = this.uid, // Use uid instead of id
        name = this.name,
        slug = this.slug,
        description = this.description,
        workspaceType = this.workspaceType,
        avatarUrl = this.avatarUrl,
        subscriptionPlan = this.subscriptionPlan,
        memberCount = memberCount,
        lastActivityAt = this.lastActivityAt,
        createdAt = this.createdAt ?: LocalDateTime.now(),
    )
}