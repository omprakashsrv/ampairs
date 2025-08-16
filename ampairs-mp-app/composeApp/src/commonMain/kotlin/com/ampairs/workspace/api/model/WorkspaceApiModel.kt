package com.ampairs.workspace.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceApiModel(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("slug") val slug: String,
    @SerialName("description") val description: String? = null,
    @SerialName("workspace_type") val workspaceType: String = "BUSINESS",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("subscription_plan") val subscriptionPlan: String = "FREE",
    @SerialName("max_members") val maxMembers: Int = 5,
    @SerialName("storage_limit_gb") val storageLimitGb: Int = 1,
    @SerialName("storage_used_gb") val storageUsedGb: Int = 0,
    @SerialName("timezone") val timezone: String = "UTC",
    @SerialName("language") val language: String = "en",
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("last_activity_at") val lastActivityAt: String? = null,
    @SerialName("trial_expires_at") val trialExpiresAt: String? = null,
    @SerialName("member_count") val memberCount: Int? = null,
    @SerialName("is_trial") val isTrial: Boolean? = null,
    @SerialName("storage_percentage") val storagePercentage: Double? = null,
)

@Serializable
data class WorkspaceListApiModel(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("slug") val slug: String,
    @SerialName("description") val description: String? = null,
    @SerialName("workspace_type") val workspaceType: String = "BUSINESS",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("subscription_plan") val subscriptionPlan: String = "FREE",
    @SerialName("member_count") val memberCount: Int = 1,
    @SerialName("last_activity_at") val lastActivityAt: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class CreateWorkspaceRequest(
    @SerialName("name") val name: String,
    @SerialName("slug") val slug: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("workspace_type") val workspaceType: String = "BUSINESS",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("timezone") val timezone: String = "UTC",
    @SerialName("language") val language: String = "en",
)

@Serializable
data class UpdateWorkspaceRequest(
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("workspace_type") val workspaceType: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("timezone") val timezone: String? = null,
    @SerialName("language") val language: String? = null,
)

// Paginated response models matching Spring Boot Page structure
@Serializable
data class PageSort(
    @SerialName("empty") val empty: Boolean,
    @SerialName("unsorted") val unsorted: Boolean,
    @SerialName("sorted") val sorted: Boolean,
)

@Serializable
data class PageableInfo(
    @SerialName("page_number") val pageNumber: Int,
    @SerialName("page_size") val pageSize: Int,
    @SerialName("sort") val sort: PageSort,
    @SerialName("offset") val offset: Int,
    @SerialName("unpaged") val unpaged: Boolean,
    @SerialName("paged") val paged: Boolean,
)

@Serializable
data class PagedWorkspaceResponse(
    @SerialName("content") val content: List<WorkspaceListApiModel>,
    @SerialName("pageable") val pageable: PageableInfo,
    @SerialName("last") val last: Boolean,
    @SerialName("total_elements") val totalElements: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("first") val first: Boolean,
    @SerialName("size") val size: Int,
    @SerialName("number") val number: Int,
    @SerialName("sort") val sort: PageSort,
    @SerialName("number_of_elements") val numberOfElements: Int,
    @SerialName("empty") val empty: Boolean,
)