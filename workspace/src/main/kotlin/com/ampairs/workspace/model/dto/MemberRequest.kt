package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.enums.WorkspaceRole
import com.ampairs.workspace.security.WorkspacePermission
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * Request DTO for inviting a member to workspace
 */
data class InviteMemberRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
        val email: String,

        val role: WorkspaceRole = WorkspaceRole.MEMBER,

        val message: String? = null,

        val sendEmail: Boolean = true,
)

/**
 * Request DTO for updating member role and permissions
 */
data class UpdateMemberRequest(
        val role: WorkspaceRole? = null,

        val customPermissions: Set<WorkspacePermission>? = null,

        val isActive: Boolean? = null,
)

/**
 * Request DTO for bulk member operations
 */
data class BulkMemberRequest(
        val memberIds: List<String>,

        val action: String, // "activate", "deactivate", "remove", "update_role"

        val role: WorkspaceRole? = null,
)