package com.ampairs.workspace.service

import com.ampairs.core.exception.BusinessException
import com.ampairs.core.exception.NotFoundException
import com.ampairs.workspace.model.WorkspaceMember
import com.ampairs.workspace.model.dto.*
import com.ampairs.workspace.model.enums.WorkspaceRole
import com.ampairs.workspace.repository.WorkspaceMemberRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Service for workspace member management operations
 */
@Service
@Transactional
class WorkspaceMemberService(
    private val memberRepository: WorkspaceMemberRepository,
    private val activityService: WorkspaceActivityService,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceMemberService::class.java)
    }

    /**
     * Add member as owner (used during workspace creation)
     */
    fun addMemberAsOwner(workspaceId: String, userId: String): WorkspaceMember {
        val member = WorkspaceMember().apply {
            this.workspaceId = workspaceId
            this.userId = userId
            this.role = WorkspaceRole.OWNER
            this.isActive = true
            this.joinedAt = LocalDateTime.now()
        }

        val savedMember = memberRepository.save(member)
        logger.info("Added owner to workspace: $workspaceId, user: $userId")

        return savedMember
    }

    /**
     * Add member to workspace
     */
    fun addMember(workspaceId: String, userId: String, role: WorkspaceRole = WorkspaceRole.MEMBER): WorkspaceMember {
        // Check if user is already a member
        if (memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw BusinessException("MEMBER_ALREADY_EXISTS", "User is already a member of this workspace")
        }

        val member = WorkspaceMember().apply {
            this.workspaceId = workspaceId
            this.userId = userId
            this.role = role
            this.isActive = true
            this.joinedAt = LocalDateTime.now()
        }

        val savedMember = memberRepository.save(member)

        // Log activity
        activityService.logMemberAdded(workspaceId, userId, role.name)

        logger.info("Added member to workspace: $workspaceId, user: $userId, role: $role")
        return savedMember
    }

    /**
     * Update member role and permissions
     */
    fun updateMember(
        workspaceId: String,
        memberId: String,
        request: UpdateMemberRequest,
        updatedBy: String,
    ): MemberResponse {
        val member = findMemberById(memberId)

        // Validate workspace
        if (member.workspaceId != workspaceId) {
            throw BusinessException("MEMBER_NOT_IN_WORKSPACE", "Member does not belong to this workspace")
        }

        // Update fields
        request.role?.let {
            val oldRole = member.role
            member.role = it

            // Log role change
            if (oldRole != it) {
                activityService.logMemberRoleChanged(workspaceId, member.userId, oldRole.name, it.name, updatedBy)
            }
        }

        request.customPermissions?.let { permissions ->
            member.customPermissions = permissions.joinToString(",")
        }

        request.isActive?.let {
            val wasActive = member.isActive
            member.isActive = it

            // Log activation/deactivation
            if (wasActive != it) {
                if (it) {
                    activityService.logMemberActivated(workspaceId, member.userId, updatedBy)
                } else {
                    activityService.logMemberDeactivated(workspaceId, member.userId, updatedBy)
                }
            }
        }

        val updatedMember = memberRepository.save(member)
        return updatedMember.toResponse()
    }

    /**
     * Remove member from workspace
     */
    fun removeMember(workspaceId: String, memberId: String, removedBy: String): String {
        val member = findMemberById(memberId)

        // Validate workspace
        if (member.workspaceId != workspaceId) {
            throw BusinessException("MEMBER_NOT_IN_WORKSPACE", "Member does not belong to this workspace")
        }

        // Cannot remove the last owner
        if (member.role == WorkspaceRole.OWNER) {
            val ownerCount = memberRepository.countByWorkspaceIdAndRoleAndIsActiveTrue(workspaceId, WorkspaceRole.OWNER)
            if (ownerCount <= 1) {
                throw BusinessException("CANNOT_REMOVE_LAST_OWNER", "Cannot remove the last owner from workspace")
            }
        }

        memberRepository.delete(member)

        // Log activity
        activityService.logMemberRemoved(workspaceId, member.userId, removedBy)

        logger.info("Removed member from workspace: $workspaceId, user: ${member.userId}")
        return "Member removed successfully"
    }

    /**
     * Get workspace members
     */
    fun getWorkspaceMembers(workspaceId: String, pageable: Pageable): Page<MemberListResponse> {
        val members = memberRepository.findByWorkspaceIdAndIsActiveTrue(workspaceId, pageable)
        return members.map { it.toListResponse() }
    }

    /**
     * Get member by ID
     */
    fun getMemberById(memberId: String): MemberResponse {
        val member = findMemberById(memberId)
        return member.toResponse()
    }

    /**
     * Check if user is workspace member
     */
    fun isWorkspaceMember(workspaceId: String, userId: String): Boolean {
        return memberRepository.existsByWorkspaceIdAndUserIdAndIsActiveTrue(workspaceId, userId)
    }

    /**
     * Check if user is workspace owner
     */
    fun isWorkspaceOwner(workspaceId: String, userId: String): Boolean {
        return memberRepository.existsByWorkspaceIdAndUserIdAndRoleAndIsActiveTrue(
            workspaceId, userId, WorkspaceRole.OWNER
        )
    }

    /**
     * Check if user has specific permission
     */
    fun hasPermission(workspaceId: String, userId: String, permission: String): Boolean {
        val member = memberRepository.findByWorkspaceIdAndUserIdAndIsActiveTrue(workspaceId, userId)
            .orElse(null) ?: return false

        // For now, return basic role-based permissions
        return when (permission) {
            "MANAGE_WORKSPACE" -> member.role in listOf(WorkspaceRole.OWNER, WorkspaceRole.ADMIN)
            "MANAGE_MEMBERS" -> member.role in listOf(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MANAGER)
            "INVITE_MEMBERS" -> member.role in listOf(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MANAGER)
            "VIEW_MEMBERS" -> true // All active members can view other members
            else -> false
        }
    }

    /**
     * Get active member count
     */
    fun getActiveMemberCount(workspaceId: String): Int {
        return memberRepository.countByWorkspaceIdAndIsActiveTrue(workspaceId).toInt()
    }

    /**
     * Get member statistics
     */
    fun getMemberStatistics(workspaceId: String): MemberStatsResponse {
        val totalMembers = memberRepository.countByWorkspaceId(workspaceId)
        val activeMembers = memberRepository.countByWorkspaceIdAndIsActiveTrue(workspaceId)
        val membersByRoleList = memberRepository.getMemberCountsByRole(workspaceId)
        val membersByRole = membersByRoleList.associate {
            (it["role"] as WorkspaceRole).name to (it["count"] as Long)
        }
        val recentJoins = memberRepository.countByWorkspaceIdAndJoinedAtAfter(
            workspaceId,
            LocalDateTime.now().minusDays(7)
        )

        return MemberStatsResponse(
            totalMembers = totalMembers,
            activeMembers = activeMembers,
            pendingInvitations = 0, // Will be populated by invitation service
            membersByRole = membersByRole,
            recentJoins = recentJoins
        )
    }

    /**
     * Search members
     */
    fun searchMembers(
        workspaceId: String,
        query: String,
        role: WorkspaceRole?,
        pageable: Pageable,
    ): Page<MemberListResponse> {
        val members = if (role != null) {
            memberRepository.searchMembersByRole(workspaceId, role, query, pageable)
        } else {
            memberRepository.searchMembers(workspaceId, query, pageable)
        }

        return members.map { it.toListResponse() }
    }

    /**
     * Get members by role
     */
    fun getMembersByRole(workspaceId: String, role: WorkspaceRole): List<MemberListResponse> {
        val members = memberRepository.findByWorkspaceIdAndRole(workspaceId, role)
        return members.map { it.toListResponse() }
    }

    /**
     * Update member last activity
     */
    fun updateMemberActivity(workspaceId: String, userId: String) {
        val member = memberRepository.findByWorkspaceIdAndUserIdAndIsActiveTrue(workspaceId, userId)
            .orElse(null) ?: return
        memberRepository.updateLastActivity(member.uid, LocalDateTime.now())
    }

    /**
     * Remove all members from workspace (used during workspace deletion)
     */
    fun removeAllMembers(workspaceId: String) {
        val deletedCount = memberRepository.deleteByWorkspaceId(workspaceId)
        logger.info("Removed $deletedCount members from workspace: $workspaceId")
    }

    /**
     * Bulk member operations
     */
    fun bulkMemberOperation(workspaceId: String, request: BulkMemberRequest, operatedBy: String): String {
        val members = memberRepository.findAllById(request.memberIds)

        // Validate all members belong to workspace
        members.forEach { member ->
            if (member.workspaceId != workspaceId) {
                throw BusinessException("INVALID_MEMBER", "One or more members don't belong to this workspace")
            }
        }

        when (request.action) {
            "activate" -> {
                members.forEach { it.isActive = true }
                memberRepository.saveAll(members)
                activityService.logBulkMemberActivation(workspaceId, request.memberIds.size, operatedBy)
            }

            "deactivate" -> {
                members.forEach { it.isActive = false }
                memberRepository.saveAll(members)
                activityService.logBulkMemberDeactivation(workspaceId, request.memberIds.size, operatedBy)
            }

            "remove" -> {
                // Check for owners
                val ownerCount =
                    memberRepository.countByWorkspaceIdAndRoleAndIsActiveTrue(workspaceId, WorkspaceRole.OWNER)
                val ownersToRemove = members.count { it.role == WorkspaceRole.OWNER }

                if (ownerCount.toInt() - ownersToRemove <= 0) {
                    throw BusinessException("CANNOT_REMOVE_ALL_OWNERS", "Cannot remove all owners from workspace")
                }

                memberRepository.deleteAll(members)
                activityService.logBulkMemberRemoval(workspaceId, request.memberIds.size, operatedBy)
            }

            "update_role" -> {
                request.role?.let { newRole ->
                    members.forEach { it.role = newRole }
                    memberRepository.saveAll(members)
                    activityService.logBulkRoleUpdate(workspaceId, request.memberIds.size, newRole.name, operatedBy)
                } ?: throw BusinessException("ROLE_REQUIRED", "Role is required for update_role action")
            }

            else -> throw BusinessException("INVALID_ACTION", "Invalid bulk action: ${request.action}")
        }

        return "${request.action} operation completed for ${members.size} members"
    }

    // Private helper methods

    private fun findMemberById(memberId: String): WorkspaceMember {
        return memberRepository.findById(memberId)
            .orElseThrow { NotFoundException("Member not found: $memberId") }
    }
}