package com.ampairs.workspace.db

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.model.PageResult
import com.ampairs.common.flower_core.Resource
import com.ampairs.workspace.api.WorkspaceMemberApi
import com.ampairs.workspace.api.model.MemberApiModel // This import might be unused now if MemberListResponse is used instead from the API layer
import com.ampairs.workspace.api.model.UpdateMemberRequest
import com.ampairs.workspace.api.model.UserRoleResponse
import com.ampairs.workspace.domain.WorkspaceMember
import com.ampairs.workspace.db.dao.WorkspaceMemberDao
import com.ampairs.workspace.domain.asDatabaseModel
import com.ampairs.workspace.domain.asDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

// Assuming MemberListResponse and User (as provided by you) are accessible in this scope,
// potentially via imports like:
// import com.ampairs.workspace.api.model.MemberListResponse
// import com.ampairs.core.model.User // Or wherever User is defined
// import com.ampairs.workspace.api.model.WorkspaceRole // Or wherever WorkspaceRole is defined
// import kotlinx.datetime.LocalDateTime // If MemberListResponse uses kotlinx.datetime.LocalDateTime

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
                val pagedResponse = response.data!! // Assuming response.data is now PagedResult<MemberListResponse>

                val members = pagedResponse.content.map { memberListItem -> // memberListItem is now of type MemberListResponse
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
                throw Exception("Failed to fetch workspace members: ${e.message}")
            }
        }
    }

    /**
     * Get detailed member information
     */
    suspend fun getMemberDetails(workspaceId: String, memberId: String): Resource<WorkspaceMember> {
        return try {
            val response = memberApi.getMemberDetails(workspaceId, memberId)

            if (response.error == null && response.data != null) {
                val memberData = response.data!! // This might also need updates if its response structure changed

                val member = WorkspaceMember(
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
                Resource.success(member)
            } else {
                Resource.error(response.error?.message ?: "Failed to fetch member details", "API_ERROR", null)
            }
        } catch (e: Exception) {
            Resource.error(e.message ?: "Failed to fetch member details", "EXCEPTION", null)
        }
    }

    /**
     * Update member role and permissions
     */
    suspend fun updateMember(
        workspaceId: String,
        memberId: String,
        request: UpdateMemberRequest,
    ): Resource<WorkspaceMember> {
        return try {
            val response = memberApi.updateMember(workspaceId, memberId, request)

            if (response.error == null && response.data != null) {
                val memberData = response.data!! // This might also need updates if its response structure changed

                val member = WorkspaceMember(
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
                Resource.success(member)
            } else {
                Resource.error(response.error?.message ?: "Failed to update member", "API_ERROR", null)
            }
        } catch (e: Exception) {
            Resource.error(e.message ?: "Failed to update member", "EXCEPTION", null)
        }
    }

    /**
     * Remove member from workspace
     */
    suspend fun removeMember(workspaceId: String, memberId: String): Resource<String> {
        return try {
            val response = memberApi.removeMember(workspaceId, memberId)

            if (response.error == null && response.data != null) {
                // Remove from local database
                val currentUserId = getCurrentUserId()
                if (currentUserId != null) {
                    memberDao.deleteWorkspaceMemberForUser(currentUserId, workspaceId, memberId)
                }
                Resource.success(response.data!!)
            } else {
                Resource.error(response.error?.message ?: "Failed to remove member", "API_ERROR", null)
            }
        } catch (e: Exception) {
            Resource.error(e.message ?: "Failed to remove member", "EXCEPTION", null)
        }
    }

    /**
     * Get current user's role and permissions in workspace
     */
    suspend fun getMyRole(workspaceId: String): Resource<UserRoleResponse> {
        return try {
            val response = memberApi.getMyRole(workspaceId)

            if (response.error == null && response.data != null) {
                Resource.success(response.data!!)
            } else {
                Resource.error(response.error?.message ?: "Failed to get user role", "API_ERROR", null)
            }
        } catch (e: Exception) {
            Resource.error(e.message ?: "Failed to get user role", "EXCEPTION", null)
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
        return memberDao.getWorkspaceMemberForUser(currentUserId, workspaceId, memberId)?.asDomainModel()
    }

    /**
     * Search workspace members locally for current user
     */
    suspend fun searchWorkspaceMembersLocally(workspaceId: String, query: String): Flow<List<WorkspaceMember>> {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")

        return memberDao.searchWorkspaceMembersForUser(currentUserId, workspaceId, query).map { entities ->
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
