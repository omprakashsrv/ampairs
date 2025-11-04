package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.enums.WorkspaceRole
import com.ampairs.workspace.security.WorkspacePermission

/**
 * Response DTO for workspace role information
 */
data class WorkspaceRoleResponse(
    val name: String,
    val displayName: String,
    val level: Int,
    val description: String,
    val manageableRoles: List<String>
)

/**
 * Response DTO for workspace permission information
 */
data class WorkspacePermissionResponse(
    val name: String,
    val permissionName: String,
    val description: String
)

/**
 * Extension function to convert WorkspaceRole to WorkspaceRoleResponse
 */
fun WorkspaceRole.toResponse(): WorkspaceRoleResponse {
    return WorkspaceRoleResponse(
        name = this.name,
        displayName = this.displayName,
        level = this.level,
        description = this.description,
        manageableRoles = this.getManageableRoles().map { it.name }
    )
}

/**
 * Extension function to convert WorkspacePermission to WorkspacePermissionResponse
 */
fun WorkspacePermission.toResponse(): WorkspacePermissionResponse {
    val description = when (this) {
        WorkspacePermission.WORKSPACE_MANAGE -> "Manage workspace settings, details, and configuration"
        WorkspacePermission.WORKSPACE_DELETE -> "Delete or archive the entire workspace"
        WorkspacePermission.MEMBER_VIEW -> "View workspace members and their basic information"
        WorkspacePermission.MEMBER_INVITE -> "Send invitations to new workspace members"
        WorkspacePermission.MEMBER_MANAGE -> "Manage member roles, permissions, and settings"
        WorkspacePermission.MEMBER_DELETE -> "Remove members from the workspace"
    }
    
    return WorkspacePermissionResponse(
        name = this.name,
        permissionName = this.permissionName,
        description = description
    )
}