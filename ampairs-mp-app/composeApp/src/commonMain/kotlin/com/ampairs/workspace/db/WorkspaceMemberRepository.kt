package com.ampairs.workspace.db

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.model.PageResult
import com.ampairs.workspace.api.WorkspaceMemberApi
import com.ampairs.workspace.api.model.MemberApiModel
import com.ampairs.workspace.api.model.UpdateMemberRequest
import com.ampairs.workspace.api.model.UserRoleResponse
import com.ampairs.workspace.domain.WorkspaceMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository for workspace member management
 *
 * Handles member-related operations including viewing, updating, and removing
 * members with proper data transformation and error handling.
 */
class WorkspaceMemberRepository(
    private val memberApi: WorkspaceMemberApi,
    private val tokenRepository: TokenRepository,
) {

    /**
     * Get workspace members with pagination
     */
    suspend fun getWorkspaceMembers(
        workspaceId: String,
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "joinedAt",
        sortDir: String = "desc",
    ): PageResult<WorkspaceMember> {
        val response = memberApi.getWorkspaceMembers(workspaceId, page, size, sortBy, sortDir)

        return if (response.error == null && response.data != null) {
            val pagedResponse = response.data!!

            val members = pagedResponse.content.map { memberListItem ->
                WorkspaceMember(
                    id = memberListItem.id,
                    userId = memberListItem.userId,
                    workspaceId = workspaceId,
                    email = memberListItem.email,
                    name = memberListItem.name,
                    role = memberListItem.role,
                    status = memberListItem.status,
                    joinedAt = memberListItem.joinedAt,
                    lastActivity = memberListItem.lastActivity,
                    permissions = memberListItem.permissions,
                    avatarUrl = memberListItem.avatarUrl,
                    phone = memberListItem.phone,
                    department = memberListItem.department,
                    isOnline = memberListItem.isOnline
                )
            }

            PageResult(
                content = members,
                totalElements = pagedResponse.totalElements,
                totalPages = pagedResponse.totalPages,
                currentPage = pagedResponse.page,
                pageSize = pagedResponse.size,
                isFirst = pagedResponse.isFirst,
                isLast = pagedResponse.isLast,
                isEmpty = members.isEmpty()
            )
        } else {
            throw Exception(response.error?.message ?: "Failed to fetch workspace members")
        }
    }

    /**
     * Get detailed member information
     */
    suspend fun getMemberDetails(workspaceId: String, memberId: String): WorkspaceMember {
        val response = memberApi.getMemberDetails(workspaceId, memberId)

        return if (response.error == null && response.data != null) {
            val memberData = response.data!!

            WorkspaceMember(
                id = memberData.id,
                userId = memberData.userId,
                workspaceId = memberData.workspaceId,
                email = memberData.email,
                name = memberData.name,
                role = memberData.role,
                status = memberData.status,
                joinedAt = memberData.joinedAt,
                lastActivity = memberData.lastActivity,
                permissions = memberData.permissions,
                avatarUrl = memberData.avatarUrl,
                phone = memberData.phone,
                department = memberData.department,
                isOnline = memberData.isOnline
            )
        } else {
            throw Exception(response.error?.message ?: "Failed to fetch member details")
        }
    }

    /**
     * Update member role and permissions
     */
    suspend fun updateMember(
        workspaceId: String,
        memberId: String,
        request: UpdateMemberRequest,
    ): WorkspaceMember {
        val response = memberApi.updateMember(workspaceId, memberId, request)

        return if (response.error == null && response.data != null) {
            val memberData = response.data!!

            WorkspaceMember(
                id = memberData.id,
                userId = memberData.userId,
                workspaceId = memberData.workspaceId,
                email = memberData.email,
                name = memberData.name,
                role = memberData.role,
                status = memberData.status,
                joinedAt = memberData.joinedAt,
                lastActivity = memberData.lastActivity,
                permissions = memberData.permissions,
                avatarUrl = memberData.avatarUrl,
                phone = memberData.phone,
                department = memberData.department,
                isOnline = memberData.isOnline
            )
        } else {
            throw Exception(response.error?.message ?: "Failed to update member")
        }
    }

    /**
     * Remove member from workspace
     */
    suspend fun removeMember(workspaceId: String, memberId: String): String {
        val response = memberApi.removeMember(workspaceId, memberId)

        return if (response.error == null && response.data != null) {
            response.data!!
        } else {
            throw Exception(response.error?.message ?: "Failed to remove member")
        }
    }

    /**
     * Get current user's role and permissions in workspace
     */
    suspend fun getMyRole(workspaceId: String): UserRole {
        val response = memberApi.getMyRole(workspaceId)

        return if (response.error == null && response.data != null) {
            val roleData = response.data!!

            UserRole(
                userId = roleData.userId,
                workspaceId = roleData.workspaceId,
                currentRole = roleData.currentRole,
                membershipStatus = roleData.membershipStatus,
                joinedAt = roleData.joinedAt,
                lastActivity = roleData.lastActivity,
                roleHierarchy = roleData.roleHierarchy,
                permissions = roleData.permissions,
                moduleAccess = roleData.moduleAccess,
            )
        } else {
            throw Exception(response.error?.message ?: "Failed to get user role")
        }
    }
}

/**
 * Domain model for user role and permissions
 */
data class UserRole(
    val userId: String,
    val workspaceId: String,
    val currentRole: String,
    val membershipStatus: String,
    val joinedAt: String,
    val lastActivity: String? = null,
    val roleHierarchy: Map<String, Boolean>,
    val permissions: Map<String, Map<String, Boolean>>,
    val moduleAccess: List<String>,
    val restrictions: Map<String, Any>? = null,
)