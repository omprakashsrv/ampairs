package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.WorkspaceTeam
import com.ampairs.workspace.security.WorkspacePermission
import java.time.LocalDateTime

/**
 * Detailed team response with all team information
 */
data class TeamResponse(
        val id: String,

        val workspaceId: String,

        val teamCode: String,

        val name: String,

        val description: String?,

        val department: String?,

        val permissions: Set<WorkspacePermission>,

        val teamLead: TeamMemberSummary?,

        val isActive: Boolean,

        val maxMembers: Int?,

        val memberCount: Int,

        val createdAt: LocalDateTime,

        val updatedAt: LocalDateTime
)

/**
 * Simplified team response for lists
 */
data class TeamListResponse(
        val id: String,

        val teamCode: String,

        val name: String,

        val department: String?,

        val teamLead: TeamMemberSummary?,

        val memberCount: Int,

        val isActive: Boolean
)

/**
 * Summary of team member details
 */
data class TeamMemberSummary(
        val id: String,

        val userId: String,

        val name: String?,

        val email: String?,

        val avatarUrl: String?,

        val jobTitle: String?,

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
