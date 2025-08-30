package com.ampairs.workspace.model.dto

import com.ampairs.core.domain.User
import com.ampairs.workspace.model.WorkspaceMember
import com.ampairs.workspace.security.WorkspacePermission
import com.ampairs.workspace.model.enums.WorkspaceRole
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * Response DTO for workspace member information
 */
data class MemberResponse(
    @field:JsonProperty("id")
    val id: String,

    @field:JsonProperty("user_id")
    val userId: String,

    @field:JsonProperty("workspace_id")
    val workspaceId: String,

    // Flattened user fields (kept for backwards compatibility)
    @field:JsonProperty("email")
    val email: String? = null,

    @field:JsonProperty("first_name")
    val firstName: String? = null,

    @field:JsonProperty("last_name")
    val lastName: String? = null,

    @field:JsonProperty("avatar_url")
    val avatarUrl: String? = null,

    // User details from core User interface
    @field:JsonProperty("user")
    val user: User? = null,

    @field:JsonProperty("role")
    val role: WorkspaceRole,

    @field:JsonProperty("permissions")
    val permissions: Set<WorkspacePermission>,

    @field:JsonProperty("is_active")
    val isActive: Boolean,

    @field:JsonProperty("joined_at")
    val joinedAt: LocalDateTime,

    @field:JsonProperty("last_activity_at")
    val lastActivityAt: LocalDateTime?,

    @field:JsonProperty("invitation_accepted_at")
    val invitationAcceptedAt: LocalDateTime?,

    @field:JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @field:JsonProperty("updated_at")
    val updatedAt: LocalDateTime,
)

/**
 * Simplified member response for lists
 */
data class MemberListResponse(
    @field:JsonProperty("id")
    val id: String,

    @field:JsonProperty("user_id")
    val userId: String,

    // User details from core User interface
    @field:JsonProperty("user")
    val user: User? = null,

    @field:JsonProperty("role")
    val role: WorkspaceRole,

    @field:JsonProperty("is_active")
    val isActive: Boolean,

    @field:JsonProperty("joined_at")
    val joinedAt: LocalDateTime,

    @field:JsonProperty("last_activity_at")
    val lastActivityAt: LocalDateTime?,
)

/**
 * Extension function to convert WorkspaceMember entity to MemberResponse
 */
fun WorkspaceMember.toResponse(userInfo: User? = null): MemberResponse {
    return MemberResponse(
        id = this.uid,
        userId = this.userId,
        workspaceId = this.workspaceId,
        email = userInfo?.email,
        firstName = userInfo?.firstName,
        lastName = userInfo?.lastName,
        avatarUrl = userInfo?.profilePictureUrl,
        user = userInfo,
        role = this.role,
        permissions = this.permissions,
        isActive = this.isActive,
        joinedAt = this.joinedAt ?: LocalDateTime.now(),
        lastActivityAt = this.lastActiveAt,
        invitationAcceptedAt = this.invitationAcceptedAt,
        createdAt = this.createdAt ?: LocalDateTime.now(),
        updatedAt = this.updatedAt ?: LocalDateTime.now(),
    )
}

/**
 * Extension function to convert WorkspaceMember entity to MemberListResponse
 */
fun WorkspaceMember.toListResponse(userInfo: User? = null): MemberListResponse {
    return MemberListResponse(
        id = this.uid,
        userId = this.userId,
        user = userInfo,
        role = this.role,
        isActive = this.isActive,
        joinedAt = this.joinedAt ?: LocalDateTime.now(),
        lastActivityAt = this.lastActiveAt
    )
}