package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.enums.WorkspaceRole
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * Request DTO for inviting a member to workspace
 */
data class InviteMemberRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    @JsonProperty("email")
    val email: String,

    @JsonProperty("role")
    val role: WorkspaceRole = WorkspaceRole.MEMBER,

    @JsonProperty("message")
    val message: String? = null,

    @JsonProperty("send_email")
    val sendEmail: Boolean = true,
)

/**
 * Request DTO for updating member role and permissions
 */
data class UpdateMemberRequest(
    @JsonProperty("role")
    val role: WorkspaceRole? = null,

    @JsonProperty("custom_permissions")
    val customPermissions: List<String>? = null,

    @JsonProperty("is_active")
    val isActive: Boolean? = null,
)

/**
 * Request DTO for bulk member operations
 */
data class BulkMemberRequest(
    @JsonProperty("member_ids")
    val memberIds: List<String>,

    @JsonProperty("action")
    val action: String, // "activate", "deactivate", "remove", "update_role"

    @JsonProperty("role")
    val role: WorkspaceRole? = null,
)