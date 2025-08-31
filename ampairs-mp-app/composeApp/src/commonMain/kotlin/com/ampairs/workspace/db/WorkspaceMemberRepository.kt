package com.ampairs.workspace.db

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.model.PageResult
import com.ampairs.workspace.api.WorkspaceMemberApi
import com.ampairs.workspace.api.model.UpdateMemberRequest
import com.ampairs.workspace.api.model.UserRoleResponse
import com.ampairs.workspace.api.model.WorkspaceRole
import com.ampairs.workspace.domain.WorkspaceMember
import com.ampairs.workspace.db.dao.WorkspaceMemberDao
import com.ampairs.workspace.domain.asDatabaseModel
import com.ampairs.workspace.domain.asDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

/**
 * Repository for workspace member management
 *
 * Handles member-related operations including viewing, updating, and removing
 * members with proper data transformation and error handling.
 */
class WorkspaceMemberRepository(
    private val memberApi: WorkspaceMemberApi,
    private val memberDao: WorkspaceMemberDao,
    private val tokenRepository: TokenRepository,
) {

    private suspend fun getCurrentUserId(): String? {
        return tokenRepository.getCurrentUserId()
    }

    /**
     * Get workspace members with pagination and improved error handling
     */
    suspend fun getWorkspaceMembers(
        workspaceId: String,
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "joinedAt",
        sortDir: String = "desc",
    ): PageResult<WorkspaceMember> {
        return try {
            val response = memberApi.getWorkspaceMembers(workspaceId, page, size, sortBy, sortDir)

            if (response.error == null && response.data != null) {
                val pagedResponse =
                    response.data!! // Assuming response.data is now PagedResult<MemberListResponse>

                val members =
                    pagedResponse.content.map { memberListItem -> // memberListItem is now of type MemberListResponse
                        WorkspaceMember(
                            id = memberListItem.id,
                            userId = memberListItem.userId,
                            workspaceId = workspaceId,
                            // Assuming WorkspaceMember.email is String, provide default if user or email is null
                            email = memberListItem.user?.email,
                            // Assuming WorkspaceMember.name is String, use getDisplayName or fallback
                            name = memberListItem.user?.getDisplayName() ?: memberListItem.userId,
                            // Assuming WorkspaceMember.role is String and WorkspaceRole is an enum or has a 'name' property
                            // If WorkspaceRole.toString() is desired and appropriate, use memberListItem.role.toString()
                            role = memberListItem.role,
                            status = if (memberListItem.isActive) "ACTIVE" else "INACTIVE",
                            joinedAt = memberListItem.joinedAt,
                            lastActivity = memberListItem.lastActivityAt,
                            // Data for permissions is not in MemberListResponse, defaulting to emptyMap()
                            permissions = emptyMap(),
                            avatarUrl = memberListItem.user?.profilePictureUrl,
                            phone = memberListItem.user?.phone,
                        )
                    }

                // Save to local database with current user association
                val currentUserId = getCurrentUserId() ?: "unknown_user"
                val currentTime = System.currentTimeMillis()

                members.forEach { member ->
                    val memberEntity = member.asDatabaseModel().copy(
                        user_id = currentUserId,
                        sync_state = "SYNCED",
                        last_synced_at = currentTime,
                        server_updated_at = currentTime,
                        local_updated_at = currentTime
                    )
                    memberDao.insertWorkspaceMember(memberEntity)
                }

                PageResult(
                    content = members,
                    totalElements = pagedResponse.totalElements.toInt(),
                    totalPages = pagedResponse.totalPages,
                    currentPage = pagedResponse.pageNumber,
                    pageSize = pagedResponse.pageSize,
                    isFirst = pagedResponse.first,
                    isLast = pagedResponse.last,
                    isEmpty = members.isEmpty()
                )
            } else {
                // Network failed, try to return local data
                val localMembers = getLocalWorkspaceMembers(workspaceId).first()

                val startIndex = page * size
                val endIndex = minOf(startIndex + size, localMembers.size)
                val pageContent = if (startIndex < localMembers.size) {
                    localMembers.subList(startIndex, endIndex)
                } else {
                    emptyList()
                }

                if (localMembers.isNotEmpty()) {
                    PageResult(
                        content = pageContent,
                        totalElements = localMembers.size,
                        totalPages = (localMembers.size + size - 1) / size,
                        currentPage = page,
                        pageSize = size,
                        isFirst = page == 0,
                        isLast = endIndex >= localMembers.size,
                        isEmpty = localMembers.isEmpty()
                    )
                } else {
                    throw Exception(response.error?.message ?: "Failed to fetch workspace members")
                }
            }
        } catch (e: Exception) {
            // If network fails, try to return cached data
            try {
                val localMembers = getLocalWorkspaceMembers(workspaceId).first()
                if (localMembers.isNotEmpty()) {
                    val startIndex = page * size
                    val endIndex = minOf(startIndex + size, localMembers.size)
                    val pageContent = if (startIndex < localMembers.size) {
                        localMembers.subList(startIndex, endIndex)
                    } else {
                        emptyList()
                    }

                    PageResult(
                        content = pageContent,
                        totalElements = localMembers.size,
                        totalPages = (localMembers.size + size - 1) / size,
                        currentPage = page,
                        pageSize = size,
                        isFirst = page == 0,
                        isLast = endIndex >= localMembers.size,
                        isEmpty = localMembers.isEmpty()
                    )
                } else {
                    throw Exception("Failed to fetch workspace members: ${e.message}")
                }
            } catch (cacheError: Exception) {
                throw Exception("Failed to fetch workspace members: ${cacheError.message}")
            }
        }
    }

    /**
     * Get detailed member information
     */
    suspend fun getMemberDetails(workspaceId: String, memberId: String): WorkspaceMember {
        val response = memberApi.getMemberDetails(workspaceId, memberId)
        if (response.error == null && response.data != null) {
            val memberData = response.data!!
            return WorkspaceMember(
                id = memberData.id,
                userId = memberData.userId,
                workspaceId = memberData.workspaceId,
                email = memberData.email,
                name = memberData.name,
                role = memberData.role,
                status = memberData.status,
                joinedAt = memberData.joinedAt,
                lastActivity = memberData.lastActivityAt,
                permissions = memberData.permissions.associateWith { true },
                avatarUrl = memberData.avatarUrl,
                phone = memberData.user?.phone,
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
        if (response.error == null && response.data != null) {
            val memberData = response.data!!
            return WorkspaceMember(
                id = memberData.id,
                userId = memberData.userId,
                workspaceId = memberData.workspaceId,
                email = memberData.email,
                name = memberData.name,
                role = memberData.role,
                status = memberData.status,
                joinedAt = memberData.joinedAt,
                lastActivity = memberData.lastActivityAt,
                permissions = memberData.permissions.associateWith { true },
                avatarUrl = memberData.avatarUrl,
                phone = memberData.user?.phone,
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
        if (response.error == null && response.data != null) {
            // Remove from local database
            val currentUserId = getCurrentUserId()
            if (currentUserId != null) {
                memberDao.deleteWorkspaceMemberForUser(currentUserId, workspaceId, memberId)
            }
            return response.data!!
        } else {
            throw Exception(response.error?.message ?: "Failed to remove member")
        }
    }

    /**
     * Get current user's role and permissions in workspace
     */
    suspend fun getMyRole(workspaceId: String): UserRoleResponse {
        val response = memberApi.getMyRole(workspaceId)
        if (response.error == null && response.data != null) {
            return response.data!!
        } else {
            throw Exception(response.error?.message ?: "Failed to get user role")
        }
    }

    /**
     * Get workspace members from local database for current user
     */
    suspend fun getLocalWorkspaceMembers(workspaceId: String): Flow<List<WorkspaceMember>> {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")

        return memberDao.getWorkspaceMembersForUser(currentUserId, workspaceId).map { entities ->
            entities.map { it.asDomainModel() }
        }
    }

    /**
     * Get workspace member from local database
     */
    suspend fun getLocalWorkspaceMember(workspaceId: String, memberId: String): WorkspaceMember? {
        val currentUserId = getCurrentUserId() ?: return null
        return memberDao.getWorkspaceMemberForUser(currentUserId, workspaceId, memberId)
            ?.asDomainModel()
    }

    /**
     * Search workspace members locally for current user
     */
    suspend fun searchWorkspaceMembersLocally(
        workspaceId: String,
        query: String
    ): Flow<List<WorkspaceMember>> {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")

        return memberDao.searchWorkspaceMembersForUser(currentUserId, workspaceId, query)
            .map { entities ->
                entities.map { it.asDomainModel() }
            }
    }

    /**
     * Clear local workspace members for current user
     */
    suspend fun clearLocalWorkspaceMembers(workspaceId: String) {
        val currentUserId = getCurrentUserId()
        if (currentUserId != null) {
            memberDao.deleteAllWorkspaceMembersForUser(currentUserId, workspaceId)
        }
    }

    /**
     * Get available roles for the workspace
     */
    suspend fun getAvailableRoles(workspaceId: String): List<WorkspaceRole> {
        val response = memberApi.getAvailableRoles(workspaceId)
        if (response.error == null && response.data != null) {
            return response.data!!
        } else {
            throw Exception(response.error?.message ?: "Failed to fetch available roles")
        }
    }

    /**
     * Get available permissions for the workspace
     */
    suspend fun getAvailablePermissions(workspaceId: String): Map<String, Map<String, Boolean>> {
        val response = memberApi.getAvailablePermissions(workspaceId)
        if (response.error == null && response.data != null) {
            // Convert API response list to UI expected map format
            val permissionsList = response.data!!
            val groupedPermissions = permissionsList.groupBy { permission ->
                // Extract module from permission name (e.g., "MEMBER_VIEW" -> "MEMBER")
                val parts = permission.permissionName.split("_")
                if (parts.size > 1) parts[0].lowercase() else "general"
            }
            
            return groupedPermissions.mapValues { (_, permissions) ->
                permissions.associate { permission ->
                    // Extract action from permission name (e.g., "MEMBER_VIEW" -> "view")
                    val action = permission.permissionName.split("_").drop(1).joinToString("_").lowercase()
                    action.ifEmpty { "general" } to true
                }
            }
        } else {
            throw Exception(response.error?.message ?: "Failed to fetch available permissions")
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
