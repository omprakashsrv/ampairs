package com.ampairs.workspace.domain

import com.ampairs.workspace.api.model.WorkspaceApiModel
import com.ampairs.workspace.api.model.WorkspaceListApiModel
import com.ampairs.workspace.db.entity.WorkspaceEntity

data class Workspace(
    val id: String,
    val name: String,
    val slug: String,
    val description: String? = null,
    val workspaceType: String = "BUSINESS",
    val avatarUrl: String? = null,
    val isActive: Boolean = true,
    val subscriptionPlan: String = "FREE",
    val maxMembers: Int = 5,
    val storageLimitGb: Int = 1,
    val storageUsedGb: Int = 0,
    val timezone: String = "UTC",
    val language: String = "en",
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String,
    val lastActivityAt: String? = null,
    val trialExpiresAt: String? = null,
    val memberCount: Int? = null,
    val isTrial: Boolean? = null,
    val storagePercentage: Double? = null,
)

data class WorkspaceList(
    val id: String,
    val name: String,
    val slug: String,
    val description: String? = null,
    val workspaceType: String = "BUSINESS",
    val avatarUrl: String? = null,
    val subscriptionPlan: String = "FREE",
    val memberCount: Int = 1,
    val lastActivityAt: String? = null,
    val createdAt: String,
)


// Extension functions for converting between different model types

fun WorkspaceApiModel.asDomainModel(): Workspace {
    return Workspace(
        id = this.id,
        name = this.name,
        slug = this.slug,
        description = this.description,
        workspaceType = this.workspaceType,
        avatarUrl = this.avatarUrl,
        isActive = this.isActive,
        subscriptionPlan = this.subscriptionPlan,
        maxMembers = this.maxMembers,
        storageLimitGb = this.storageLimitGb,
        storageUsedGb = this.storageUsedGb,
        timezone = this.timezone,
        language = this.language,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastActivityAt = this.lastActivityAt,
        trialExpiresAt = this.trialExpiresAt,
        memberCount = this.memberCount,
        isTrial = this.isTrial,
        storagePercentage = this.storagePercentage,
    )
}

fun WorkspaceListApiModel.asDomainModel(): WorkspaceList {
    return WorkspaceList(
        id = this.id,
        name = this.name,
        slug = this.slug,
        description = this.description,
        workspaceType = this.workspaceType,
        avatarUrl = this.avatarUrl,
        subscriptionPlan = this.subscriptionPlan,
        memberCount = this.memberCount,
        lastActivityAt = this.lastActivityAt,
        createdAt = this.createdAt,
    )
}

fun Workspace.asDatabaseModel(): WorkspaceEntity {
    return WorkspaceEntity(
        seq_id = 0,
        id = this.id,
        name = this.name,
        slug = this.slug,
        description = this.description ?: "",
        workspaceType = this.workspaceType,
        avatarUrl = this.avatarUrl ?: "",
        isActive = this.isActive,
        subscriptionPlan = this.subscriptionPlan,
        maxMembers = this.maxMembers,
        storageLimitGb = this.storageLimitGb,
        storageUsedGb = this.storageUsedGb,
        timezone = this.timezone,
        language = this.language,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastActivityAt = this.lastActivityAt ?: "",
        trialExpiresAt = this.trialExpiresAt ?: "",
        memberCount = this.memberCount ?: 0,
        isTrial = this.isTrial ?: false,
        storagePercentage = this.storagePercentage ?: 0.0,
    )
}

fun WorkspaceEntity.asDomainModel(): Workspace {
    return Workspace(
        id = this.id,
        name = this.name,
        slug = this.slug,
        description = if (this.description.isEmpty()) null else this.description,
        workspaceType = this.workspaceType,
        avatarUrl = if (this.avatarUrl.isEmpty()) null else this.avatarUrl,
        isActive = this.isActive,
        subscriptionPlan = this.subscriptionPlan,
        maxMembers = this.maxMembers,
        storageLimitGb = this.storageLimitGb,
        storageUsedGb = this.storageUsedGb,
        timezone = this.timezone,
        language = this.language,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastActivityAt = if (this.lastActivityAt.isEmpty()) null else this.lastActivityAt,
        trialExpiresAt = if (this.trialExpiresAt.isEmpty()) null else this.trialExpiresAt,
        memberCount = this.memberCount,
        isTrial = this.isTrial,
        storagePercentage = this.storagePercentage,
    )
}