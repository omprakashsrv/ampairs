package com.ampairs.workspace.model.dto

import com.ampairs.core.domain.User
import com.ampairs.workspace.model.WorkspaceMember
import com.ampairs.workspace.security.WorkspacePermission
import com.ampairs.workspace.model.enums.WorkspaceRole
import java.time.LocalDateTime

/**
 * Response DTO for workspace member information
 */
data class MemberResponse(
    val id: String,

    val userId: String,

    val workspaceId: String,

    // Flattened user fields (kept for backwards compatibility)
    val email: String? = null,

    val firstName: String? = null,

    val lastName: String? = null,

    val avatarUrl: String? = null,

    // User details from core User interface
    val user: User? = null,

    val role: WorkspaceRole,

    val permissions: Set<WorkspacePermission>,

    val isActive: Boolean,

    val joinedAt: LocalDateTime,

    val lastActivityAt: LocalDateTime?,

    val invitationAcceptedAt: LocalDateTime?,

    val createdAt: LocalDateTime,

    val updatedAt: LocalDateTime,
)

/**
 * Simplified member response for lists
 */
data class MemberListResponse(
    val id: String,

    val userId: String,

    // User details from core User interface
    val user: User? = null,

    val role: WorkspaceRole,

    val isActive: Boolean,

    val joinedAt: LocalDateTime,

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
 * User role response with detailed permission information
 */
data class UserRoleResponse(
    val userId: String,
    val workspaceId: String,
    val currentRole: String,
    val membershipStatus: String,
    val joinedAt: String,
    val lastActivity: String? = null,
    val roleHierarchy: Map<String, Boolean>,
    val permissions: Map<String, Map<String, Boolean>>,
    val moduleAccess: List<String>,
)

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