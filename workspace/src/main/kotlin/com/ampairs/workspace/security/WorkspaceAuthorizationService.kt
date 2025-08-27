package com.ampairs.workspace.security

import com.ampairs.user.model.User
import com.ampairs.workspace.service.WorkspaceMemberService
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

/**
 * Service for workspace-level authorization checks using Spring Security
 * This centralizes all workspace permission logic for use with @PreAuthorize
 */
@Service("workspaceAuthorizationService")
class WorkspaceAuthorizationService(
    private val memberService: WorkspaceMemberService,
) {

    /**
     * Check if the authenticated user has permission to perform an action on a workspace
     * Used with @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, 'WORKSPACE_UPDATE')")
     */
    fun hasWorkspacePermission(authentication: Authentication, workspaceId: String, permission: String): Boolean {
        val user = authentication.principal as? User ?: return false
        return memberService.hasPermission(workspaceId, user.uid, permission)
    }

    /**
     * Check if the authenticated user has permission to perform an action on a workspace (enum version)
     * Used with @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).WORKSPACE_UPDATE)")
     */
    fun hasWorkspacePermission(authentication: Authentication, workspaceId: String, permission: WorkspacePermission): Boolean {
        val user = authentication.principal as? User ?: return false
        return memberService.hasPermission(workspaceId, user.uid, permission.permissionName)
    }

    /**
     * Check if user is workspace member (any role)
     */
    fun isWorkspaceMember(authentication: Authentication, workspaceId: String): Boolean {
        val user = authentication.principal as? User ?: return false
        return memberService.isWorkspaceMember(workspaceId, user.uid)
    }
}