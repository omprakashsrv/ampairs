package com.ampairs.workspace.model.dto

import com.ampairs.workspace.security.WorkspacePermission
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.*

/**
 * Request to create a new team
 */
data class CreateTeamRequest(
    @field:NotBlank(message = "Team name is required")
    @field:Size(min = 2, max = 100, message = "Team name must be between 2 and 100 characters")
    @field:JsonProperty("name")
    val name: String,

    @field:Size(max = 500, message = "Description cannot exceed 500 characters")
    @field:JsonProperty("description")
    val description: String? = null,

    @field:Size(max = 50, message = "Team code cannot exceed 50 characters")
    @field:Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "Team code can only contain letters, numbers, hyphens and underscores")
    @field:JsonProperty("team_code")
    val teamCode: String? = null,

    @field:Size(max = 100, message = "Department name cannot exceed 100 characters")
    @field:JsonProperty("department")
    val department: String? = null,

    @field:JsonProperty("permissions")
    val permissions: Set<WorkspacePermission> = setOf(),

    @field:JsonProperty("team_lead_id")
    val teamLeadId: String? = null,

    @field:Min(1, message = "Maximum members must be at least 1")
    @field:Max(1000, message = "Maximum members cannot exceed 1000")
    @field:JsonProperty("max_members")
    val maxMembers: Int? = null
)

/**
 * Request to update an existing team
 */
data class UpdateTeamRequest(
    @field:Size(min = 2, max = 100, message = "Team name must be between 2 and 100 characters")
    @field:JsonProperty("name")
    val name: String? = null,

    @field:Size(max = 500, message = "Description cannot exceed 500 characters")
    @field:JsonProperty("description")
    val description: String? = null,

    @field:Size(max = 100, message = "Department name cannot exceed 100 characters")
    @field:JsonProperty("department")
    val department: String? = null,

    @field:JsonProperty("permissions")
    val permissions: Set<WorkspacePermission>? = null,

    @field:JsonProperty("team_lead_id")
    val teamLeadId: String? = null,

    @field:Min(1, message = "Maximum members must be at least 1")
    @field:Max(1000, message = "Maximum members cannot exceed 1000")
    @field:JsonProperty("max_members")
    val maxMembers: Int? = null,

    @field:JsonProperty("is_active")
    val isActive: Boolean? = null
)

/**
 * Request to add members to a team
 */
data class AddTeamMembersRequest(
    @field:NotEmpty(message = "Member IDs are required")
    @field:Size(max = 100, message = "Cannot add more than 100 members at once")
    @field:JsonProperty("member_ids")
    val memberIds: List<String>,

    @field:JsonProperty("set_as_primary")
    val setAsPrimary: Boolean = false
)

/**
 * Request to remove members from a team
 */
data class RemoveTeamMembersRequest(
    @field:NotEmpty(message = "Member IDs are required")
    @field:Size(max = 100, message = "Cannot remove more than 100 members at once")
    @field:JsonProperty("member_ids")
    val memberIds: List<String>
)

/**
 * Request to update team member settings
 */
data class UpdateTeamMemberRequest(
    @field:JsonProperty("set_as_primary")
    val setAsPrimary: Boolean? = null,

    @field:JsonProperty("custom_permissions")
    val customPermissions: Set<WorkspacePermission>? = null
)
