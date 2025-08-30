package com.ampairs.workspace.service

import com.ampairs.core.exception.BusinessException
import com.ampairs.core.exception.NotFoundException
import com.ampairs.workspace.model.WorkspaceTeam
import com.ampairs.workspace.model.WorkspaceMember
import com.ampairs.workspace.model.dto.*
import com.ampairs.workspace.repository.WorkspaceTeamRepository
import com.ampairs.workspace.repository.WorkspaceMemberRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class WorkspaceTeamService(
    private val teamRepository: WorkspaceTeamRepository,
    private val memberRepository: WorkspaceMemberRepository,
    private val memberService: WorkspaceMemberService,
    private val userDetailProvider: UserDetailProvider
) {
    private val logger = LoggerFactory.getLogger(WorkspaceTeamService::class.java)

    /**
     * Create a new team in the workspace
     */
    fun createTeam(workspaceId: String, request: CreateTeamRequest): TeamResponse {
        // Generate team code if not provided
        val teamCode = request.teamCode ?: generateTeamCode(request.name)

        // Validate team code uniqueness
        if (teamRepository.existsByWorkspaceIdAndTeamCode(workspaceId, teamCode)) {
            throw BusinessException("TEAM_CODE_EXISTS", "Team with code $teamCode already exists")
        }

        // Validate team lead if provided
        request.teamLeadId?.let { leadId ->
            if (!memberService.isWorkspaceMember(workspaceId, leadId)) {
                throw BusinessException("INVALID_TEAM_LEAD", "Team lead must be a workspace member")
            }
        }

        val team = WorkspaceTeam().apply {
            this.workspaceId = workspaceId
            this.teamCode = teamCode
            this.name = request.name
            this.description = request.description
            this.department = request.department
            this.permissions = request.permissions
            this.teamLeadId = request.teamLeadId
            this.maxMembers = request.maxMembers
            this.isActive = true
        }

        val savedTeam = teamRepository.save(team)
        return buildTeamResponse(savedTeam)
    }

    /**
     * Update existing team
     */
    fun updateTeam(workspaceId: String, teamId: String, request: UpdateTeamRequest): TeamResponse {
        val team = findTeamById(teamId).also {
            if (it.workspaceId != workspaceId) {
                throw BusinessException("TEAM_NOT_IN_WORKSPACE", "Team does not belong to this workspace")
            }
        }

        request.name?.let { team.name = it }
        request.description?.let { team.description = it }
        request.department?.let { team.department = it }
        request.permissions?.let { team.permissions = it }
        request.maxMembers?.let { team.maxMembers = it }
        request.isActive?.let { team.isActive = it }

        // Update team lead if changed
        request.teamLeadId?.let { leadId ->
            if (leadId != team.teamLeadId) {
                if (!memberService.isWorkspaceMember(workspaceId, leadId)) {
                    throw BusinessException("INVALID_TEAM_LEAD", "Team lead must be a workspace member")
                }
                team.teamLeadId = leadId
            }
        }

        val updatedTeam = teamRepository.save(team)
        return buildTeamResponse(updatedTeam)
    }

    /**
     * Get team by ID with full details
     */
    fun getTeamById(workspaceId: String, teamId: String): TeamResponse {
        val team = findTeamById(teamId).also {
            if (it.workspaceId != workspaceId) {
                throw BusinessException("TEAM_NOT_IN_WORKSPACE", "Team does not belong to this workspace")
            }
        }
        return buildTeamResponse(team)
    }

    /**
     * Search teams with filters
     */
    fun searchTeams(
        workspaceId: String,
        searchText: String?,
        department: String?,
        isActive: Boolean?,
        pageable: Pageable
    ): Page<TeamListResponse> {
        return teamRepository.searchTeams(workspaceId, searchText, department, isActive, pageable)
            .map { buildTeamListResponse(it) }
    }

    /**
     * Add members to team
     */
    fun addMembers(workspaceId: String, teamId: String, request: AddTeamMembersRequest): TeamResponse {
        val team = findTeamById(teamId).also {
            if (it.workspaceId != workspaceId) {
                throw BusinessException("TEAM_NOT_IN_WORKSPACE", "Team does not belong to this workspace")
            }
        }

        // Check team capacity
        val currentMemberCount = countTeamMembers(teamId)
        val maxMembers = team.maxMembers
        if (maxMembers != null && currentMemberCount + request.memberIds.size > maxMembers) {
            throw BusinessException("TEAM_CAPACITY_EXCEEDED", "Team has reached maximum member capacity")
        }

        // Add members to team
        request.memberIds.forEach { memberId ->
            val member = memberService.findMemberById(memberId)
            if (member.workspaceId != workspaceId) {
                throw BusinessException("MEMBER_NOT_IN_WORKSPACE", "Member does not belong to this workspace")
            }
            member.addToTeam(teamId, request.setAsPrimary)
            memberRepository.save(member)
        }

        return buildTeamResponse(team)
    }

    /**
     * Remove members from team
     */
    fun removeMembers(workspaceId: String, teamId: String, request: RemoveTeamMembersRequest): TeamResponse {
        val team = findTeamById(teamId).also {
            if (it.workspaceId != workspaceId) {
                throw BusinessException("TEAM_NOT_IN_WORKSPACE", "Team does not belong to this workspace")
            }
        }

        // Remove members from team
        request.memberIds.forEach { memberId ->
            val member = memberService.findMemberById(memberId)
            if (member.workspaceId != workspaceId) {
                throw BusinessException("MEMBER_NOT_IN_WORKSPACE", "Member does not belong to this workspace")
            }
            member.removeFromTeam(teamId)
            memberRepository.save(member)
        }

        return buildTeamResponse(team)
    }

    /**
     * Update team member settings
     */
    fun updateTeamMember(
        workspaceId: String,
        teamId: String,
        memberId: String,
        request: UpdateTeamMemberRequest
    ): TeamMemberSummary {
        val team = findTeamById(teamId).also {
            if (it.workspaceId != workspaceId) {
                throw BusinessException("TEAM_NOT_IN_WORKSPACE", "Team does not belong to this workspace")
            }
        }

        val member = memberService.findMemberById(memberId).also {
            if (it.workspaceId != workspaceId) {
                throw BusinessException("MEMBER_NOT_IN_WORKSPACE", "Member does not belong to this workspace")
            }
            if (!it.isInTeam(teamId)) {
                throw BusinessException("NOT_TEAM_MEMBER", "User is not a member of this team")
            }
        }

        request.setAsPrimary?.let { isPrimary ->
            if (isPrimary) {
                member.primaryTeamId = teamId
            } else if (member.primaryTeamId == teamId) {
                member.primaryTeamId = member.teamIds.firstOrNull { it != teamId }
            }
        }

        val savedMember = memberRepository.save(member)
        return buildTeamMemberSummary(savedMember, team)
    }

    /**
     * Delete team (soft delete by deactivating)
     */
    fun deleteTeam(workspaceId: String, teamId: String) {
        val team = findTeamById(teamId).also {
            if (it.workspaceId != workspaceId) {
                throw BusinessException("TEAM_NOT_IN_WORKSPACE", "Team does not belong to this workspace")
            }
        }

        team.isActive = false
        teamRepository.save(team)

        // Update members who had this as their primary team
        memberRepository.findByWorkspaceIdAndPrimaryTeamId(workspaceId, teamId).forEach { member ->
            member.primaryTeamId = member.teamIds.firstOrNull { it != teamId }
            memberRepository.save(member)
        }
    }

    // Private helper methods

    private fun findTeamById(teamId: String): WorkspaceTeam {
        return teamRepository.findById(teamId)
            .orElseThrow { NotFoundException("Team not found: $teamId") }
    }

    private fun generateTeamCode(teamName: String): String {
        return teamName.uppercase()
            .replace(Regex("[^A-Z0-9]"), "")
            .take(10)
    }

    private fun countTeamMembers(teamId: String): Int {
        return memberRepository.countByTeamIdsContaining(teamId)
    }

    private fun buildTeamResponse(team: WorkspaceTeam): TeamResponse {
        val teamLead = team.teamLeadId?.let { leadId ->
            memberRepository.findById(leadId)
                .map { buildTeamMemberSummary(it, team) }
                .orElse(null)
        }

        val memberCount = countTeamMembers(team.uid)
        return team.toResponse(teamLead, memberCount)
    }

    private fun buildTeamListResponse(team: WorkspaceTeam): TeamListResponse {
        val teamLead = team.teamLeadId?.let { leadId ->
            memberRepository.findById(leadId)
                .map { buildTeamMemberSummary(it, team) }
                .orElse(null)
        }

        val memberCount = countTeamMembers(team.uid)
        return team.toListResponse(teamLead, memberCount)
    }

    private fun buildTeamMemberSummary(member: WorkspaceMember, team: WorkspaceTeam): TeamMemberSummary {
        val userDetail = if (userDetailProvider.isUserServiceAvailable()) {
            userDetailProvider.getUserDetail(member.userId)
        } else null

        return TeamMemberSummary(
            id = member.uid,
            userId = member.userId,
            name = userDetail?.firstName?.let { fn ->
                userDetail.lastName?.let { ln -> "$fn $ln" } ?: fn
            } ?: member.memberName,
            email = userDetail?.email ?: member.memberEmail,
            avatarUrl = userDetail?.profilePictureUrl,
            jobTitle = member.jobTitle,
            isPrimaryTeam = member.primaryTeamId == team.uid
        )
    }
}
