package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.Workspace
import com.ampairs.workspace.model.enums.SubscriptionPlan
import com.ampairs.workspace.model.enums.WorkspaceType
import java.time.LocalDateTime

/**
 * Response DTO for workspace information
 */
data class WorkspaceResponse(
        val id: String,

        val name: String,

        val slug: String,

        val description: String?,

        val workspaceType: WorkspaceType,

        val avatarUrl: String?,

        val isActive: Boolean,

        val subscriptionPlan: SubscriptionPlan,

        val maxMembers: Int,

        val storageLimitGb: Int,

        val storageUsedGb: Int,

        val timezone: String,

        val language: String,

        val createdBy: String,

        val createdAt: LocalDateTime,

        val updatedAt: LocalDateTime,

        val lastActivityAt: LocalDateTime?,

        val trialExpiresAt: LocalDateTime?,

        val memberCount: Int? = null,

        val isTrial: Boolean? = null,

        val storagePercentage: Double? = null,
)

/**
 * Simplified workspace response for lists
 */
data class WorkspaceListResponse(
        val id: String,

        val name: String,

        val slug: String,

        val description: String?,

        val workspaceType: WorkspaceType,

        val avatarUrl: String?,

        val subscriptionPlan: SubscriptionPlan,

        val memberCount: Int,

        val lastActivityAt: LocalDateTime?,

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
        maxMembers = this.maxUsers,
        storageLimitGb = this.storageLimitGb,
        storageUsedGb = this.storageUsedGb,
        timezone = this.timezone,
        language = this.language,
        createdBy = this.createdBy ?: "", // Use createdBy field
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