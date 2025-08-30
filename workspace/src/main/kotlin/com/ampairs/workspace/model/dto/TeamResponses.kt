package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.WorkspaceTeam
import com.ampairs.workspace.security.WorkspacePermission
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

/**
 * Detailed team response with all team information
 */
data class TeamResponse(
    @field:JsonProperty("id")
    val id: String,

    @field:JsonProperty("workspace_id")
    val workspaceId: String,

    @field:JsonProperty("team_code")
    val teamCode: String,

    @field:JsonProperty("name")
    val name: String,

    @field:JsonProperty("description")
    val description: String?,

    @field:JsonProperty("department")
    val department: String?,

    @field:JsonProperty("permissions")
    val permissions: Set<WorkspacePermission>,

    @field:JsonProperty("team_lead")
    val teamLead: TeamMemberSummary?,

    @field:JsonProperty("is_active")
    val isActive: Boolean,

    @field:JsonProperty("max_members")
    val maxMembers: Int?,

    @field:JsonProperty("member_count")
    val memberCount: Int,

    @field:JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @field:JsonProperty("updated_at")
    val updatedAt: LocalDateTime
)

/**
 * Simplified team response for lists
 */
data class TeamListResponse(
    @field:JsonProperty("id")
    val id: String,

    @field:JsonProperty("team_code")
    val teamCode: String,

    @field:JsonProperty("name")
    val name: String,

    @field:JsonProperty("department")
    val department: String?,

    @field:JsonProperty("team_lead")
    val teamLead: TeamMemberSummary?,

    @field:JsonProperty("member_count")
    val memberCount: Int,

    @field:JsonProperty("is_active")
    val isActive: Boolean
)

/**
 * Summary of team member details
 */
data class TeamMemberSummary(
    @field:JsonProperty("id")
    val id: String,

    @field:JsonProperty("user_id")
    val userId: String,

    @field:JsonProperty("name")
    val name: String?,

    @field:JsonProperty("email")
    val email: String?,

    @field:JsonProperty("avatar_url")
    val avatarUrl: String?,

    @field:JsonProperty("job_title")
    val jobTitle: String?,

    @field:JsonProperty("is_primary_team")
    val isPrimaryTeam: Boolean
)

/**
 * Convert WorkspaceTeam to TeamResponse
 */
fun WorkspaceTeam.toResponse(
    teamLead: TeamMemberSummary? = null,
    memberCount: Int = 0
): TeamResponse = TeamResponse(
    id = this.uid,
    workspaceId = this.workspaceId,
    teamCode = this.teamCode,
    name = this.name,
    description = this.description,
    department = this.department,
    permissions = this.permissions,
    teamLead = teamLead,
    isActive = this.isActive,
    maxMembers = this.maxMembers,
    memberCount = memberCount,
    createdAt = this.createdAt ?: LocalDateTime.now(),
    updatedAt = this.updatedAt ?: LocalDateTime.now()
)

/**
 * Convert WorkspaceTeam to TeamListResponse
 */
fun WorkspaceTeam.toListResponse(
    teamLead: TeamMemberSummary? = null,
    memberCount: Int = 0
): TeamListResponse = TeamListResponse(
    id = this.uid,
    teamCode = this.teamCode,
    name = this.name,
    department = this.department,
    teamLead = teamLead,
    memberCount = memberCount,
    isActive = this.isActive
)
