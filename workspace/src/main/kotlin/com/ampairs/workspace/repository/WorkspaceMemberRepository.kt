package com.ampairs.workspace.repository

import com.ampairs.workspace.model.WorkspaceMember
import com.ampairs.workspace.model.enums.WorkspaceRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

/**
 * Repository for workspace member operations
 */
@Repository
interface WorkspaceMemberRepository : JpaRepository<WorkspaceMember, String> {

    /**
     * Find member by workspace and user
     */
    fun findByWorkspaceIdAndUserId(workspaceId: String, userId: String): Optional<WorkspaceMember>

    /**
     * Find active member by workspace and user
     */
    fun findByWorkspaceIdAndUserIdAndIsActiveTrue(workspaceId: String, userId: String): Optional<WorkspaceMember>

    /**
     * Check if user is member of workspace
     */
    fun existsByWorkspaceIdAndUserIdAndIsActiveTrue(workspaceId: String, userId: String): Boolean

    /**
     * Find all active members of a workspace
     */
    fun findByWorkspaceIdAndIsActiveTrueOrderByJoinedAtDesc(workspaceId: String): List<WorkspaceMember>

    /**
     * Find all active members of a workspace with pagination
     */
    fun findByWorkspaceIdAndIsActiveTrue(workspaceId: String, pageable: Pageable): Page<WorkspaceMember>

    /**
     * Find all workspaces a user is member of
     */
    fun findByUserIdAndIsActiveTrueOrderByLastActiveAtDesc(userId: String): List<WorkspaceMember>

    /**
     * Find members by role in workspace
     */
    fun findByWorkspaceIdAndRoleAndIsActiveTrue(workspaceId: String, role: WorkspaceRole): List<WorkspaceMember>

    /**
     * Find workspace owners
     */
    fun findByWorkspaceIdAndRole(workspaceId: String, role: WorkspaceRole): List<WorkspaceMember>

    /**
     * Count active members in workspace
     */
    fun countByWorkspaceIdAndIsActiveTrue(workspaceId: String): Long

    /**
     * Count members by role in workspace
     */
    fun countByWorkspaceIdAndRoleAndIsActiveTrue(workspaceId: String, role: WorkspaceRole): Long

    /**
     * Find inactive members (for cleanup)
     */
    fun findByLastActiveAtBeforeAndIsActiveTrue(lastActiveDate: LocalDateTime): List<WorkspaceMember>

    /**
     * Find recently joined members
     */
    fun findByJoinedAtAfterAndIsActiveTrue(joinedAfter: LocalDateTime): List<WorkspaceMember>

    /**
     * Search members by partial user information (this would need to join with user table)
     */
    @Query(
        """
        SELECT wm FROM workspace_members wm
        WHERE wm.workspaceId = :workspaceId 
        AND wm.isActive = true
        AND wm.userId IN (
            SELECT u.id FROM users u 
            WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        )
    """
    )
    fun searchMembersByUserInfo(
        @Param("workspaceId") workspaceId: String,
        @Param("searchTerm") searchTerm: String,
        pageable: Pageable,
    ): Page<WorkspaceMember>

    /**
     * Update last active timestamp for member
     */
    @Modifying
    @Query("UPDATE workspace_members wm SET wm.lastActiveAt = :timestamp WHERE wm.id = :memberId")
    fun updateLastActivity(@Param("memberId") memberId: String, @Param("timestamp") timestamp: LocalDateTime)

    /**
     * Update member role
     */
    @Modifying
    @Query("UPDATE workspace_members wm SET wm.role = :role WHERE wm.id = :memberId")
    fun updateMemberRole(@Param("memberId") memberId: String, @Param("role") role: WorkspaceRole)

    /**
     * Deactivate member
     */
    @Modifying
    @Query(
        """
        UPDATE workspace_members wm 
        SET wm.isActive = false, 
            wm.deactivatedAt = :deactivatedAt,
            wm.deactivatedBy = :deactivatedBy,
            wm.deactivationReason = :reason
        WHERE wm.id = :memberId
    """
    )
    fun deactivateMember(
        @Param("memberId") memberId: String,
        @Param("deactivatedAt") deactivatedAt: LocalDateTime,
        @Param("deactivatedBy") deactivatedBy: String,
        @Param("reason") reason: String?,
    )

    /**
     * Get member statistics for a workspace
     */
    @Query(
        """
        SELECT 
            COUNT(wm) as totalMembers,
            COUNT(CASE WHEN wm.isActive = true THEN 1 END) as activeMembers,
            COUNT(CASE WHEN wm.role = :ownerRole AND wm.isActive = true THEN 1 END) as owners,
            COUNT(CASE WHEN wm.role = :adminRole AND wm.isActive = true THEN 1 END) as admins,
            COUNT(CASE WHEN wm.role = :memberRole AND wm.isActive = true THEN 1 END) as members
        FROM workspace_members wm 
        WHERE wm.workspaceId = :workspaceId
    """
    )
    fun getMemberStatistics(
        @Param("workspaceId") workspaceId: String,
        @Param("ownerRole") ownerRole: WorkspaceRole = WorkspaceRole.OWNER,
        @Param("adminRole") adminRole: WorkspaceRole = WorkspaceRole.ADMIN,
        @Param("memberRole") memberRole: WorkspaceRole = WorkspaceRole.MEMBER,
    ): Map<String, Long>

    /**
     * Find members with specific permission level or higher
     */
    @Query(
        """
        SELECT wm FROM workspace_members wm
        WHERE wm.workspaceId = :workspaceId 
        AND wm.isActive = true
        AND wm.role IN :roles
    """
    )
    fun findMembersWithRoles(
        @Param("workspaceId") workspaceId: String,
        @Param("roles") roles: List<WorkspaceRole>,
    ): List<WorkspaceMember>

    /**
     * Delete inactive members (hard delete for cleanup)
     */
    @Modifying
    @Query("DELETE FROM workspace_members wm WHERE wm.isActive = false AND wm.deactivatedAt < :cleanupDate")
    fun deleteInactiveMembers(@Param("cleanupDate") cleanupDate: LocalDateTime): Int

    /**
     * Find members who haven't been active for a long time
     */
    @Query(
        """
        SELECT wm FROM workspace_members wm
        WHERE wm.isActive = true 
        AND (wm.lastActiveAt IS NULL OR wm.lastActiveAt < :inactiveDate)
        AND wm.joinedAt < :joinedBefore
    """
    )
    fun findLongInactiveMembers(
        @Param("inactiveDate") inactiveDate: LocalDateTime,
        @Param("joinedBefore") joinedBefore: LocalDateTime,
    ): List<WorkspaceMember>

    // Additional methods needed by services

    /**
     * Check if user exists in workspace
     */
    fun existsByWorkspaceIdAndUserId(workspaceId: String, userId: String): Boolean

    /**
     * Count members by role in workspace
     */
    fun countByWorkspaceIdAndRole(workspaceId: String, role: WorkspaceRole): Long

    /**
     * Find all members by workspace ordered by creation date
     */
    fun findByWorkspaceIdOrderByCreatedAtDesc(workspaceId: String): List<WorkspaceMember>

    /**
     * Check if user has specific role and is active in workspace
     */
    fun existsByWorkspaceIdAndUserIdAndRoleAndIsActiveTrue(
        workspaceId: String,
        userId: String,
        role: WorkspaceRole,
    ): Boolean

    /**
     * Count total members in workspace (active and inactive)
     */
    fun countByWorkspaceId(workspaceId: String): Long

    /**
     * Get member counts grouped by role
     */
    @Query(
        """
        SELECT wm.role as role, COUNT(wm) as count 
        FROM workspace_members wm 
        WHERE wm.workspaceId = :workspaceId AND wm.isActive = true
        GROUP BY wm.role
    """
    )
    fun getMemberCountsByRole(@Param("workspaceId") workspaceId: String): List<Map<String, Any>>

    /**
     * Count members who joined after a specific date
     */
    fun countByWorkspaceIdAndJoinedAtAfter(workspaceId: String, joinedAfter: LocalDateTime): Long

    /**
     * Search members by role with search term
     */
    @Query(
        """
        SELECT wm FROM workspace_members wm
        WHERE wm.workspaceId = :workspaceId 
        AND wm.isActive = true
        AND wm.role = :role
        AND wm.userId IN (
            SELECT u.id FROM users u 
            WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        )
    """
    )
    fun searchMembersByRole(
        @Param("workspaceId") workspaceId: String,
        @Param("role") role: WorkspaceRole,
        @Param("searchTerm") searchTerm: String,
        pageable: Pageable,
    ): Page<WorkspaceMember>

    /**
     * Search all members with search term
     */
    @Query(
        """
        SELECT wm FROM workspace_members wm
        WHERE wm.workspaceId = :workspaceId 
        AND wm.isActive = true
        AND wm.userId IN (
            SELECT u.id FROM users u 
            WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        )
    """
    )
    fun searchMembers(
        @Param("workspaceId") workspaceId: String,
        @Param("searchTerm") searchTerm: String,
        pageable: Pageable,
    ): Page<WorkspaceMember>

    /**
     * Delete all members by workspace ID
     */
    @Modifying
    @Query("DELETE FROM workspace_members wm WHERE wm.workspaceId = :workspaceId")
    fun deleteByWorkspaceId(@Param("workspaceId") workspaceId: String)
}