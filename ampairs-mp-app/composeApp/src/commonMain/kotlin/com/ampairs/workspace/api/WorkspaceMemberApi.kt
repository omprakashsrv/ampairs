package com.ampairs.workspace.api

import com.ampairs.network.model.Response
import com.ampairs.workspace.api.model.MemberApiModel
import com.ampairs.workspace.api.model.PagedMemberResponse
import com.ampairs.workspace.api.model.UpdateMemberRequest
import com.ampairs.workspace.api.model.UserRoleResponse

/**
 * Workspace Member Management API
 *
 * Handles all member-related operations within workspaces including
 * viewing, updating, and removing members with role-based access control.
 */
interface WorkspaceMemberApi {

    /**
     * Get workspace members with pagination and sorting
     *
     * @param workspaceId Target workspace identifier
     * @param page Page number (0-based) for pagination
     * @param size Page size (1-100, default: 20)
     * @param sortBy Sort field (joinedAt, name, email, role, lastActivity)
     * @param sortDir Sort direction (asc, desc)
     * @return Paginated list of workspace members
     */
    suspend fun getWorkspaceMembers(
        workspaceId: String,
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "joinedAt",
        sortDir: String = "desc",
    ): Response<PagedMemberResponse>

    /**
     * Get detailed information about a specific workspace member
     *
     * @param workspaceId Target workspace identifier
     * @param memberId Unique member identifier
     * @return Comprehensive member details including profile and permissions
     */
    suspend fun getMemberDetails(
        workspaceId: String,
        memberId: String,
    ): Response<MemberApiModel>

    /**
     * Update member role and permissions
     *
     * @param workspaceId Target workspace identifier
     * @param memberId Member identifier to update
     * @param request Update member request with new role and permissions
     * @return Updated member information
     */
    suspend fun updateMember(
        workspaceId: String,
        memberId: String,
        request: UpdateMemberRequest,
    ): Response<MemberApiModel>

    /**
     * Remove member from workspace
     *
     * Permanently removes a member from the workspace, revoking all access rights
     * and permissions. This operation is irreversible.
     *
     * @param workspaceId Target workspace identifier
     * @param memberId Member identifier to remove
     * @return Success message with removal details
     */
    suspend fun removeMember(
        workspaceId: String,
        memberId: String,
    ): Response<String>

    /**
     * Get current user's role and permissions in workspace
     *
     * Returns comprehensive information about the authenticated user's role,
     * permissions, and access rights within the specified workspace context.
     *
     * @param workspaceId Target workspace identifier
     * @return User's role, permissions, and access information
     */
    suspend fun getMyRole(workspaceId: String): Response<UserRoleResponse>
}