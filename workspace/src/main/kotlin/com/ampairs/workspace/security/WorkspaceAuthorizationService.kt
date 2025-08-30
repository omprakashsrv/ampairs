package com.ampairs.workspace.security

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.core.service.UserService
import com.ampairs.workspace.model.enums.WorkspaceRole
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
    private val userService: UserService,
) {

    /**
     * Check if the authenticated user has permission to perform an action on a workspace
     * Used with @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, 'WORKSPACE_UPDATE')")
     */
    fun hasWorkspacePermission(authentication: Authentication, workspaceId: String, permission: String): Boolean {
        val userId = AuthenticationHelper.getCurrentUserId(authentication) ?: return false
        return memberService.hasPermission(workspaceId, userId, permission)
    }

    /**
     * Check if the authenticated user has permission to perform an action on a workspace (enum version)
     * Used with @PreAuthorize("@workspaceAuthorizationService.hasWorkspacePermission(authentication, #workspaceId, T(com.ampairs.workspace.security.WorkspacePermission).WORKSPACE_UPDATE)")
     */
    fun hasWorkspacePermission(
        authentication: Authentication,
        workspaceId: String,
        permission: WorkspacePermission
    ): Boolean {
        val userId = AuthenticationHelper.getCurrentUserId(authentication) ?: return false
        return memberService.hasPermission(workspaceId, userId, permission.permissionName)
    }

    /**
     * Check if user is workspace member (any role)
     */
    fun isWorkspaceMember(authentication: Authentication, workspaceId: String): Boolean {
        val userId = AuthenticationHelper.getCurrentUserId(authentication) ?: return false
        return memberService.isWorkspaceMember(workspaceId, userId)
    }

    /**
     * Check if user has permission in the current tenant workspace
     * Used when tenant context is already set by SessionUserFilter
     */
    fun hasCurrentTenantPermission(authentication: Authentication, permission: String): Boolean {
        val workspaceId = TenantContextHolder.getCurrentTenant() ?: return false
        val userId = AuthenticationHelper.getCurrentUserId(authentication) ?: return false
        return memberService.hasPermission(workspaceId, userId, permission)
    }

    /**
     * Check if user has permission in the current tenant workspace (enum version)
     */
    fun hasCurrentTenantPermission(authentication: Authentication, permission: WorkspacePermission): Boolean {
        return hasCurrentTenantPermission(authentication, permission.permissionName)
    }

    /**
     * Check if user is member of the current tenant workspace
     */
    fun isCurrentTenantMember(authentication: Authentication): Boolean {
        val workspaceId = TenantContextHolder.getCurrentTenant() ?: return false
        val userId = AuthenticationHelper.getCurrentUserId(authentication) ?: return false
        return memberService.isWorkspaceMember(workspaceId, userId)
    }

    /**
     * Check if user has specific role or higher in workspace
     */
    fun hasWorkspaceRole(authentication: Authentication, workspaceId: String, requiredRole: WorkspaceRole): Boolean {
        val userId = AuthenticationHelper.getCurrentUserId(authentication) ?: return false
        val userRole = memberService.getUserRole(workspaceId, userId) ?: return false
        return userRole.hasPermissionLevel(requiredRole)
    }

    /**
     * Check if user has specific role or higher in current tenant workspace
     */
    fun hasCurrentTenantRole(authentication: Authentication, requiredRole: WorkspaceRole): Boolean {
        val workspaceId = TenantContextHolder.getCurrentTenant() ?: return false
        return hasWorkspaceRole(authentication, workspaceId, requiredRole)
    }

    /**
     * Check if user is workspace admin (ADMIN role or higher) in current tenant
     */
    fun isCurrentTenantAdmin(authentication: Authentication): Boolean {
        return hasCurrentTenantRole(authentication, WorkspaceRole.ADMIN)
    }

    /**
     * Check if user is workspace manager (MANAGER role or higher) in current tenant
     */
    fun isCurrentTenantManager(authentication: Authentication): Boolean {
        return hasCurrentTenantRole(authentication, WorkspaceRole.MANAGER)
    }
}