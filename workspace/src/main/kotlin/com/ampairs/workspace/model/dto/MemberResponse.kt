package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.WorkspaceMember
import com.ampairs.workspace.model.enums.WorkspaceRole
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * Response DTO for workspace member information
 */
data class MemberResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("user_id")
    val userId: String,

    @JsonProperty("workspace_id")
    val workspaceId: String,

    @JsonProperty("email")
    val email: String? = null,

    @JsonProperty("first_name")
    val firstName: String? = null,

    @JsonProperty("last_name")
    val lastName: String? = null,

    @JsonProperty("avatar_url")
    val avatarUrl: String? = null,

    @JsonProperty("role")
    val role: WorkspaceRole,

    @JsonProperty("custom_permissions")
    val customPermissions: List<String>,

    @JsonProperty("effective_permissions")
    val effectivePermissions: List<String>,

    @JsonProperty("is_active")
    val isActive: Boolean,

    @JsonProperty("joined_at")
    val joinedAt: LocalDateTime,

    @JsonProperty("last_activity_at")
    val lastActivityAt: LocalDateTime?,

    @JsonProperty("invitation_accepted_at")
    val invitationAcceptedAt: LocalDateTime?,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime,
)

/**
 * Simplified member response for lists
 */
data class MemberListResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("user_id")
    val userId: String,

    @JsonProperty("email")
    val email: String? = null,

    @JsonProperty("first_name")
    val firstName: String? = null,

    @JsonProperty("last_name")
    val lastName: String? = null,

    @JsonProperty("avatar_url")
    val avatarUrl: String? = null,

    @JsonProperty("role")
    val role: WorkspaceRole,

    @JsonProperty("is_active")
    val isActive: Boolean,

    @JsonProperty("joined_at")
    val joinedAt: LocalDateTime,

    @JsonProperty("last_activity_at")
    val lastActivityAt: LocalDateTime?,
)

/**
 * Response DTO for member statistics
 */
data class MemberStatsResponse(
    @JsonProperty("total_members")
    val totalMembers: Long,

    @JsonProperty("active_members")
    val activeMembers: Long,

    @JsonProperty("pending_invitations")
    val pendingInvitations: Long,

    @JsonProperty("members_by_role")
    val membersByRole: Map<String, Long>,

    @JsonProperty("recent_joins")
    val recentJoins: Long,
)

/**
 * Extension function to convert WorkspaceMember entity to MemberResponse
 */
fun WorkspaceMember.toResponse(): MemberResponse {
    return MemberResponse(
        id = this.uid, // Use uid instead of id
        userId = this.userId,
        workspaceId = this.workspaceId,
        email = null, // Will be populated from User entity if needed
        firstName = null, // Will be populated from User entity if needed
        lastName = null, // Will be populated from User entity if needed
        avatarUrl = null, // Will be populated from User entity if needed
        role = this.role,
        customPermissions = this.getCustomPermissionsList(),
        effectivePermissions = this.getEffectivePermissions().map { it.name },
        isActive = this.isActive,
        joinedAt = this.joinedAt ?: LocalDateTime.now(),
        lastActivityAt = this.lastActivityAt,
        invitationAcceptedAt = this.invitationAcceptedAt,
        createdAt = LocalDateTime.parse(this.createdAt ?: "2023-01-01T00:00:00"),
        updatedAt = LocalDateTime.parse(this.updatedAt ?: "2023-01-01T00:00:00")
    )
}

/**
 * Extension function to convert WorkspaceMember entity to MemberListResponse
 */
fun WorkspaceMember.toListResponse(): MemberListResponse {
    return MemberListResponse(
        id = this.uid, // Use uid instead of id
        userId = this.userId,
        email = null, // Will be populated from User entity if needed
        firstName = null, // Will be populated from User entity if needed
        lastName = null, // Will be populated from User entity if needed
        avatarUrl = null, // Will be populated from User entity if needed
        role = this.role,
        isActive = this.isActive,
        joinedAt = this.joinedAt ?: LocalDateTime.now(),
        lastActivityAt = this.lastActivityAt
    )
}