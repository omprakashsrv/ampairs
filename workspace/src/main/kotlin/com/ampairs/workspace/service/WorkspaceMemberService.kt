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
        // Check if member already exists
        val existingMember = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
        if (existingMember.isPresent) {
            val member = existingMember.get()
            logger.warn("Member already exists for workspace: $workspaceId, user: $userId. Updating to owner role.")

            // Update existing member to owner role if not already
            if (member.role != WorkspaceRole.OWNER) {
                member.role = WorkspaceRole.OWNER
                member.isActive = true
                member.joinedAt = member.joinedAt ?: LocalDateTime.now()
                return memberRepository.save(member)
            }

            return member
        }

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
        activityService.logMemberAdded(workspaceId, userId, "Unknown User", role.name, "SYSTEM", "System")

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
                activityService.logMemberRoleChanged(
                    workspaceId,
                    member.userId,
                    "Unknown User",
                    oldRole.name,
                    it.name,
                    updatedBy,
                    "Unknown User"
                )
            }
        }

        request.customPermissions?.let { permissions ->
            member.permissions = permissions
        }

        request.isActive?.let {
            val wasActive = member.isActive
            member.isActive = it

            // Log activation/deactivation
            if (wasActive != it) {
                if (it) {
                    activityService.logMemberActivated(
                        workspaceId,
                        member.userId,
                        "Unknown User",
                        updatedBy,
                        "Unknown User"
                    )
                } else {
                    activityService.logMemberDeactivated(
                        workspaceId,
                        member.userId,
                        "Unknown User",
                        updatedBy,
                        "Unknown User"
                    )
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
        activityService.logMemberRemoved(workspaceId, member.userId, "Unknown User", removedBy, "Unknown User")

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
        return try {
            val member = memberRepository.findByWorkspaceIdAndUserIdAndIsActiveTrue(workspaceId, userId)
                .orElse(null) ?: return false

            // Standardized permission mapping with clear, consistent naming
            when (permission) {
                // Workspace management permissions (consolidated)
                "WORKSPACE_MANAGE", "WORKSPACE_UPDATE", "WORKSPACE_SETTINGS" -> 
                    member.role in listOf(WorkspaceRole.OWNER, WorkspaceRole.ADMIN)
                "WORKSPACE_DELETE", "WORKSPACE_ARCHIVE" -> 
                    member.role in listOf(WorkspaceRole.OWNER, WorkspaceRole.ADMIN)
                
                // Member management permissions
                "MEMBER_VIEW" -> true // All active members can view other members
                "MEMBER_INVITE" -> member.role in listOf(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MANAGER)
                "MEMBER_MANAGE" -> member.role in listOf(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MANAGER)
                "MEMBER_DELETE", "MEMBER_REMOVE" -> member.role in listOf(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MANAGER)
                
                else -> false
            }
        } catch (e: Exception) {
            logger.warn("Error checking permissions for user $userId in workspace $workspaceId: ${e.message}")
            // Fallback: try alternative query or return conservative result
            try {
                // Alternative approach using existence check
                memberRepository.existsByWorkspaceIdAndUserIdAndIsActiveTrue(workspaceId, userId)
                // If exists but couldn't get role, be conservative and deny advanced permissions
                when (permission) {
                    "VIEW_MEMBERS" -> true // Basic permission
                    else -> false // Conservative approach for advanced permissions
                }
            } catch (ex: Exception) {
                logger.warn("Fallback permission check also failed for user $userId in workspace $workspaceId: ${ex.message}")
                false // Conservative fallback
            }
        }
    }

    /**
     * Get active member count
     */
    fun getActiveMemberCount(workspaceId: String): Int {
        return try {
            memberRepository.countByWorkspaceIdAndIsActiveTrue(workspaceId).toInt()
        } catch (e: Exception) {
            logger.warn("Error getting active member count for workspace $workspaceId: ${e.message}")
            // Return a reasonable default - at least 1 (assuming workspace has an owner)
            1
        }
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
        // Simplified search without cross-module queries
        val members = if (role != null) {
            memberRepository.findByWorkspaceIdAndRoleAndIsActiveTrue(workspaceId, role, pageable)
        } else {
            memberRepository.findByWorkspaceIdAndIsActiveTrueOrderByJoinedAtDesc(workspaceId, pageable)
        }
        
        // TODO: If advanced user search is needed, implement it at the service layer
        // by fetching user data separately and filtering results

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
                activityService.logBulkMemberActivation(workspaceId, request.memberIds.size, operatedBy, "Unknown User")
            }

            "deactivate" -> {
                members.forEach { it.isActive = false }
                memberRepository.saveAll(members)
                activityService.logBulkMemberDeactivation(
                    workspaceId,
                    request.memberIds.size,
                    operatedBy,
                    "Unknown User"
                )
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
                activityService.logBulkMemberRemoval(workspaceId, request.memberIds.size, operatedBy, "Unknown User")
            }

            "update_role" -> {
                request.role?.let { newRole ->
                    members.forEach { it.role = newRole }
                    memberRepository.saveAll(members)
                    activityService.logBulkRoleUpdate(
                        workspaceId,
                        request.memberIds.size,
                        newRole.name,
                        operatedBy,
                        "Unknown User"
                    )
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