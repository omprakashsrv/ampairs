package com.ampairs.workspace.model.dto

import com.ampairs.core.domain.User
import com.ampairs.workspace.model.WorkspaceMember
import com.ampairs.workspace.security.WorkspacePermission
import com.ampairs.workspace.model.enums.WorkspaceRole
import java.time.LocalDateTime

/**
 * Summary information for teams associated with a member
 */
data class TeamSummary(
    val id: String,
    val name: String,
    val teamCode: String,
    val department: String?,
    val isPrimaryTeam: Boolean = false
)

/**
 * Response DTO for workspace member information
 */
data class MemberResponse(
    val id: String,

    val userId: String,

    val workspaceId: String,

    // Flattened user fields (kept for backwards compatibility)
    val email: String? = null,

    val firstName: String? = null,

    val lastName: String? = null,

    val avatarUrl: String? = null,

    // User details from core User interface
    val user: User? = null,

    val role: WorkspaceRole,

    val permissions: Set<WorkspacePermission>,

    val isActive: Boolean,

    val joinedAt: LocalDateTime,

    val lastActivityAt: LocalDateTime?,

    val invitationAcceptedAt: LocalDateTime?,

    val createdAt: LocalDateTime,

    val updatedAt: LocalDateTime,

    // Team information
    val primaryTeam: TeamSummary? = null,

    val teams: List<TeamSummary> = emptyList(),

    val jobTitle: String? = null,
)

/**
 * Simplified member response for lists
 */
data class MemberListResponse(
    val id: String,

    val userId: String,

    // User details from core User interface
    val user: User? = null,

    val role: WorkspaceRole,

    val isActive: Boolean,

    val joinedAt: LocalDateTime,

    val lastActivityAt: LocalDateTime?,

    // Team information
    val primaryTeam: TeamSummary? = null,

    val teams: List<TeamSummary> = emptyList(),

    val jobTitle: String? = null,
)

/**
 * Extension function to convert WorkspaceMember entity to MemberResponse
 * Now leverages EntityGraph-loaded teams for efficiency
 */
fun WorkspaceMember.toResponse(
    userInfo: User? = null,
    allTeams: List<TeamSummary> = emptyList() // Optional override for additional teams
): MemberResponse {
    // Primary team from EntityGraph
    val primaryTeamSummary = this.primaryTeam?.let { team ->
        TeamSummary(
            id = team.uid,
            name = team.name,
            teamCode = team.teamCode,
            department = team.department,
            isPrimaryTeam = true
        )
    }

    // For additional teams, we'll need a service method to populate based on teamIds JSON field
    // For now, use the provided allTeams parameter or construct from primaryTeam only
    val teamsList = if (allTeams.isNotEmpty()) {
        allTeams
    } else {
        primaryTeamSummary?.let { listOf(it) } ?: emptyList()
    }

    return MemberResponse(
        id = this.uid,
        userId = this.userId,
        workspaceId = this.workspaceId,
        email = userInfo?.email,
        firstName = userInfo?.firstName,
        lastName = userInfo?.lastName,
        avatarUrl = userInfo?.profilePictureUrl,
        user = userInfo,
        role = this.role,
        permissions = this.permissions,
        isActive = this.isActive,
        joinedAt = this.joinedAt ?: LocalDateTime.now(),
        lastActivityAt = this.lastActiveAt,
        invitationAcceptedAt = this.invitationAcceptedAt,
        createdAt = this.createdAt ?: LocalDateTime.now(),
        updatedAt = this.updatedAt ?: LocalDateTime.now(),
        primaryTeam = primaryTeamSummary,
        teams = teamsList,
        jobTitle = this.jobTitle
    )
}

/**
 * User role response with detailed permission information
 */
data class UserRoleResponse(
    val userId: String,
    val workspaceId: String,
    val currentRole: String,
    val membershipStatus: String,
    val joinedAt: String,
    val lastActivity: String? = null,
    val roleHierarchy: Map<String, Boolean>,
    val permissions: Map<String, Map<String, Boolean>>,
    val moduleAccess: List<String>,
)

/**
 * Extension function to convert WorkspaceMember entity to MemberListResponse
 * Now leverages EntityGraph-loaded teams for efficiency
 */
fun WorkspaceMember.toListResponse(
    userInfo: User? = null,
    allTeams: List<TeamSummary> = emptyList() // Optional override for additional teams
): MemberListResponse {
    // Primary team from EntityGraph
    val primaryTeamSummary = this.primaryTeam?.let { team ->
        TeamSummary(
            id = team.uid,
            name = team.name,
            teamCode = team.teamCode,
            department = team.department,
            isPrimaryTeam = true
        )
    }

    // For additional teams, we'll need a service method to populate based on teamIds JSON field
    // For now, use the provided allTeams parameter or construct from primaryTeam only
    val teamsList = if (allTeams.isNotEmpty()) {
        allTeams
    } else {
        primaryTeamSummary?.let { listOf(it) } ?: emptyList()
    }

    return MemberListResponse(
        id = this.uid,
        userId = this.userId,
        user = userInfo,
        role = this.role,
        isActive = this.isActive,
        joinedAt = this.joinedAt ?: LocalDateTime.now(),
        lastActivityAt = this.lastActiveAt,
        primaryTeam = primaryTeamSummary,
        teams = teamsList,
        jobTitle = this.jobTitle
    )
}