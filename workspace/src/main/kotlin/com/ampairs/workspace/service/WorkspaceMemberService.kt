package com.ampairs.workspace.service

import com.ampairs.core.domain.User
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
    private val userDetailProvider: UserDetailProvider
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
        // Populate user detail if available
        val userDetail =
            if (userDetailProvider.isUserServiceAvailable()) userDetailProvider.getUserDetail(updatedMember.userId) else null
        return updatedMember.toResponse(userDetail)
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
        // Batch-load user details when provider is available to populate nested user DTOs
        if (userDetailProvider.isUserServiceAvailable()) {
            val userIds = members.content.map { it.userId }
            val userDetails = userDetailProvider.getUserDetails(userIds)
            return members.map { member ->
                val ud = userDetails[member.userId]
                member.toListResponse(ud)
            }
        }

        return members.map { it.toListResponse() }
    }

    /**
     * Get workspace members with optimized loading using EntityGraph
     * This method uses @EntityGraph to efficiently load member details with their primary teams
     * and avoids N+1 queries for both user information and team relationships.
     *
     * Additionally, if a UserDetailProvider is available, it will batch-load user details
     * from the external user service for enhanced user information.
     */
    fun getWorkspaceMembersOptimized(workspaceId: String, pageable: Pageable): Page<MemberListResponse> {
        logger.debug("Fetching workspace members with optimized loading (EntityGraph) for workspace: $workspaceId")

        // Use EntityGraph to load members with primary team in a single query
        val members = memberRepository.findByWorkspaceIdAndIsActiveTrueWithPrimaryTeam(workspaceId, pageable)

        // Extract user IDs for batch loading
        val userIds = members.content.map { it.userId }

        // Batch load user details if provider is available
        val userDetails: Map<String, User> = if (userDetailProvider.isUserServiceAvailable()) {
            userDetailProvider.getUserDetails(userIds)
        } else {
            emptyMap()
        }

        return members.map { member ->
            val userDetail = userDetails[member.userId]
            // The toListResponse method will now use the EntityGraph-loaded primaryTeam automatically
            member.toListResponse(userDetail)
        }
    }

    /**
     * Extract first name from full name
     */
    private fun extractFirstName(fullName: String?): String? {
        return fullName?.split(" ")?.firstOrNull()
    }

    /**
     * Extract last name from full name
     */
    private fun extractLastName(fullName: String?): String? {
        val nameParts = fullName?.split(" ")
        return if (nameParts != null && nameParts.size > 1) {
            nameParts.drop(1).joinToString(" ")
        } else null
    }

    /**
     * Get member by ID with user details and team information using EntityGraph
     */
    fun getMemberById(memberId: String): MemberResponse {
        val member = memberRepository.findByIdWithPrimaryTeam(memberId)
            .orElseThrow { NotFoundException("Member not found: $memberId") }
        
        val userDetail = if (userDetailProvider.isUserServiceAvailable()) {
            userDetailProvider.getUserDetail(member.userId)
        } else null
        
        // The toResponse method will use the EntityGraph-loaded primaryTeam automatically
        return member.toResponse(userDetail)
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
     * Get user's role in workspace
     */
    fun getUserRole(workspaceId: String, userId: String): WorkspaceRole? {
        val member = memberRepository.findByWorkspaceIdAndUserIdAndIsActiveTrue(workspaceId, userId)
        return member.orElse(null)?.role
    }

    /**
     * Get workspace member by workspace ID and user ID with team information using EntityGraph
     */
    fun getWorkspaceMember(workspaceId: String, userId: String): WorkspaceMember? {
        return memberRepository.findByWorkspaceIdAndUserIdAndIsActiveTrueWithPrimaryTeam(workspaceId, userId)
            .orElse(null)
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
                "MEMBER_INVITE" -> member.role in listOf(
                    WorkspaceRole.OWNER,
                    WorkspaceRole.ADMIN,
                    WorkspaceRole.MANAGER
                )

                "MEMBER_MANAGE" -> member.role in listOf(
                    WorkspaceRole.OWNER,
                    WorkspaceRole.ADMIN,
                    WorkspaceRole.MANAGER
                )

                "MEMBER_DELETE", "MEMBER_REMOVE" -> member.role in listOf(
                    WorkspaceRole.OWNER,
                    WorkspaceRole.ADMIN,
                    WorkspaceRole.MANAGER
                )

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
    fun getMemberStatistics(workspaceId: String): Map<String, Any> {
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

        return mapOf(
            "total_members" to totalMembers,
            "active_members" to activeMembers,
            "recent_joins" to recentJoins,
            "by_role" to membersByRole,
            "by_status" to mapOf(
                "ACTIVE" to activeMembers,
                "INACTIVE" to (totalMembers - activeMembers),
                "PENDING" to 0L, // Will be populated by invitation service
                "SUSPENDED" to 0L
            )
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
        val members = memberRepository.findByUidIn(request.memberIds)

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

    /**
     * Advanced member search with multiple filters
     */
    fun searchWorkspaceMembers(
        workspaceId: String,
        searchQuery: String?,
        role: String?,
        status: String?,
        department: String?,
        pageable: Pageable
    ): Page<MemberListResponse> {
        // Convert string role to enum if provided
        val roleEnum = role?.takeIf { it != "ALL" }?.let { 
            try { WorkspaceRole.valueOf(it) } catch (e: Exception) { null }
        }
        
        // For now, use basic search and filtering
        val members = when {
            roleEnum != null -> memberRepository.findByWorkspaceIdAndRoleAndIsActiveTrue(workspaceId, roleEnum, pageable)
            else -> memberRepository.findByWorkspaceIdAndIsActiveTrue(workspaceId, pageable)
        }

        // Batch-load user details when provider is available to populate nested user DTOs
        if (userDetailProvider.isUserServiceAvailable()) {
            val userIds = members.content.map { it.userId }
            val userDetails = userDetailProvider.getUserDetails(userIds)
            return members.map { member ->
                val ud = userDetails[member.userId]
                member.toListResponse(ud)
            }
        }
        
        return members.map { it.toListResponse() }
    }

    /**
     * Get workspace departments
     */
    fun getWorkspaceDepartments(workspaceId: String): List<String> {
        // TODO: Implement when department field is added to member entity
        // For now return empty list
        return emptyList()
    }

    /**
     * Bulk update members
     */
    fun bulkUpdateMembers(workspaceId: String, request: Map<String, Any>): Map<String, Any> {
        val memberIds = request["member_ids"] as? List<String> ?: emptyList()
        val role = request["role"] as? String
        val status = request["status"] as? String
        val notifyMembers = request["notify_members"] as? Boolean ?: false
        
        val members = memberRepository.findByUidIn(memberIds)
        var updatedCount = 0
        val failedUpdates = mutableListOf<Map<String, String>>()
        
        members.forEach { member ->
            try {
                if (member.workspaceId == workspaceId) {
                    role?.let { 
                        try {
                            member.role = WorkspaceRole.valueOf(it)
                        } catch (e: Exception) {
                            failedUpdates.add(mapOf(
                                "member_id" to member.uid,
                                "error" to "Invalid role: $it"
                            ))
                            return@forEach
                        }
                    }
                    
                    status?.let { 
                        member.isActive = when(it) {
                            "ACTIVE" -> true
                            "INACTIVE" -> false
                            else -> member.isActive
                        }
                    }
                    
                    memberRepository.save(member)
                    updatedCount++
                } else {
                    failedUpdates.add(mapOf(
                        "member_id" to member.uid,
                        "error" to "Member not in this workspace"
                    ))
                }
            } catch (e: Exception) {
                failedUpdates.add(mapOf(
                    "member_id" to member.uid,
                    "error" to (e.message ?: "Unknown error")
                ))
            }
        }
        
        return mapOf(
            "updated_count" to updatedCount,
            "failed_updates" to failedUpdates
        )
    }

    /**
     * Bulk remove members
     */
    fun bulkRemoveMembers(workspaceId: String, request: Map<String, Any>): Map<String, Any> {
        val memberIds = request["member_ids"] as? List<String> ?: emptyList()
        val reason = request["reason"] as? String
        
        val members = memberRepository.findByUidIn(memberIds)
        var removedCount = 0
        val failedRemovals = mutableListOf<Map<String, String>>()
        
        // Check for owners that would be removed
        val ownerCount = memberRepository.countByWorkspaceIdAndRoleAndIsActiveTrue(workspaceId, WorkspaceRole.OWNER)
        val ownersToRemove = members.count { it.role == WorkspaceRole.OWNER && it.workspaceId == workspaceId }
        
        if (ownerCount.toInt() - ownersToRemove <= 0) {
            return mapOf(
                "removed_count" to 0,
                "failed_removals" to listOf(mapOf(
                    "error" to "Cannot remove all owners from workspace"
                ))
            )
        }
        
        members.forEach { member ->
            try {
                if (member.workspaceId == workspaceId) {
                    memberRepository.delete(member)
                    removedCount++
                } else {
                    failedRemovals.add(mapOf(
                        "member_id" to member.uid,
                        "error" to "Member not in this workspace"
                    ))
                }
            } catch (e: Exception) {
                failedRemovals.add(mapOf(
                    "member_id" to member.uid,
                    "error" to (e.message ?: "Unknown error")
                ))
            }
        }
        
        return mapOf(
            "removed_count" to removedCount,
            "failed_removals" to failedRemovals
        )
    }

    /**
     * Export members data
     */
    fun exportMembers(
        workspaceId: String, 
        format: String, 
        role: String?, 
        status: String?, 
        department: String?, 
        searchQuery: String?
    ): ByteArray {
        // Get filtered members
        val roleEnum = role?.takeIf { it != "ALL" }?.let { 
            try { WorkspaceRole.valueOf(it) } catch (e: Exception) { null }
        }
        
        val members = when {
            roleEnum != null -> memberRepository.findByWorkspaceIdAndRole(workspaceId, roleEnum)
            else -> memberRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
        }
        
        // Generate CSV content
        val csvContent = StringBuilder()
        csvContent.append("Name,Email,Role,Status,Joined Date,Last Activity\n")
        
        members.forEach { member ->
            csvContent.append("\"${member.userId}\",")  // TODO: Get actual user name when user service is available
            csvContent.append("\"${member.userId}@example.com\",") // TODO: Get actual email
            csvContent.append("\"${member.role.name}\",")
            csvContent.append("\"${if (member.isActive) "ACTIVE" else "INACTIVE"}\",")
            csvContent.append("\"${member.joinedAt}\",")
            csvContent.append("\"${member.lastActiveAt ?: ""}\"\n")
        }
        
        return csvContent.toString().toByteArray(Charsets.UTF_8)
    }

    /**
     * Update member status
     */
    fun updateMemberStatus(workspaceId: String, memberId: String, status: String, reason: String?): MemberResponse {
        val member = findMemberById(memberId)
        
        if (member.workspaceId != workspaceId) {
            throw BusinessException("MEMBER_NOT_IN_WORKSPACE", "Member does not belong to this workspace")
        }
        
        val wasActive = member.isActive
        when (status.uppercase()) {
            "ACTIVE" -> member.isActive = true
            "INACTIVE" -> member.isActive = false
            "SUSPENDED" -> member.isActive = false
            else -> throw BusinessException("INVALID_STATUS", "Invalid status: $status")
        }
        
        val updatedMember = memberRepository.save(member)
        val userDetail = if (userDetailProvider.isUserServiceAvailable()) userDetailProvider.getUserDetail(updatedMember.userId) else null
        return updatedMember.toResponse(userDetail)
    }

    fun findMemberById(memberId: String): WorkspaceMember {
        return memberRepository.findByUid(memberId)
            .orElseThrow { NotFoundException("Member not found: $memberId") }
    }
}
