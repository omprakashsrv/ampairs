package com.ampairs.workspace.repository

import com.ampairs.workspace.model.WorkspaceMember
import com.ampairs.workspace.model.enums.WorkspaceRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
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
     * Find all active members of a workspace with pagination
     */
    fun findByWorkspaceIdAndIsActiveTrue(workspaceId: String, pageable: Pageable): Page<WorkspaceMember>

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
     * Search members by user ID (simplified to avoid cross-module JPA queries)
     * Note: User search functionality moved to service layer to avoid cross-module dependencies
     */
    fun findByWorkspaceIdAndUserIdContainingAndIsActiveTrue(
        workspaceId: String,
        userIdPattern: String,
        pageable: Pageable,
    ): Page<WorkspaceMember>

    /**
     * Update last active timestamp for member
     */
    @Modifying
    @Query("UPDATE com.ampairs.workspace.model.WorkspaceMember wm SET wm.lastActiveAt = :timestamp WHERE wm.uid = :memberId")
    fun updateLastActivity(@Param("memberId") memberId: String, @Param("timestamp") timestamp: LocalDateTime)

    /**
     * Check if user exists in workspace
     */
    fun existsByWorkspaceIdAndUserId(workspaceId: String, userId: String): Boolean

    /**
     * Find all members by workspace ordered by creation date
     */
    fun findByWorkspaceIdOrderByCreatedAtDesc(workspaceId: String): List<WorkspaceMember>

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
        FROM com.ampairs.workspace.model.WorkspaceMember wm 
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
     * Find members by role (simplified to avoid cross-module JPA queries)
     * Note: User search functionality moved to service layer
     */
    fun findByWorkspaceIdAndRoleAndIsActiveTrue(
        workspaceId: String,
        role: WorkspaceRole,
        pageable: Pageable,
    ): Page<WorkspaceMember>

    /**
     * Find all active members (simplified to avoid cross-module JPA queries)
     * Note: User search functionality moved to service layer
     */
    fun findByWorkspaceIdAndIsActiveTrueOrderByJoinedAtDesc(
        workspaceId: String,
        pageable: Pageable,
    ): Page<WorkspaceMember>

    /**
     * Delete all members by workspace ID
     */
    @Modifying
    @Query("DELETE FROM com.ampairs.workspace.model.WorkspaceMember wm WHERE wm.workspaceId = :workspaceId")
    fun deleteByWorkspaceId(@Param("workspaceId") workspaceId: String)

    /**
     * Find members by workspace ID and primary team ID
     */
    fun findByWorkspaceIdAndPrimaryTeamId(workspaceId: String, primaryTeamId: String): List<WorkspaceMember>

    /**
     * Count members by team IDs containing specific team ID
     */
    @Query("SELECT COUNT(*) FROM workspace_members wm WHERE JSON_CONTAINS(wm.team_ids, JSON_QUOTE(:teamId))", nativeQuery = true)
    fun countByTeamIdsContaining(@Param("teamId") teamId: String): Int

    /**
     * Find member by ID with primary team loaded via EntityGraph
     */
    @EntityGraph(attributePaths = ["primaryTeam"])
    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.uid = :memberId")
    fun findByIdWithPrimaryTeam(@Param("memberId") memberId: String): Optional<WorkspaceMember>

    /**
     * Find member by UID (string identifier)
     */
    fun findByUid(uid: String): Optional<WorkspaceMember>

    /**
     * Find multiple members by UIDs (string identifiers)
     */
    fun findByUidIn(uids: Collection<String>): List<WorkspaceMember>

    /**
     * Find active members with primary team loaded via EntityGraph - uses @TenantId
     */
    @EntityGraph(attributePaths = ["primaryTeam"])
    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.isActive = true ORDER BY wm.joinedAt DESC")
    fun findByIsActiveTrueWithPrimaryTeam(pageable: Pageable): Page<WorkspaceMember>

    /**
     * Find user's membership in current tenant - uses @TenantId
     */
    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.userId = :userId AND wm.isActive = true")
    fun findByUserIdAndIsActiveTrue(@Param("userId") userId: String): Optional<WorkspaceMember>

    /**
     * Find user's membership with primary team in current tenant - uses @TenantId
     */
    @EntityGraph(attributePaths = ["primaryTeam"])
    @Query("SELECT wm FROM WorkspaceMember wm WHERE wm.userId = :userId AND wm.isActive = true")
    fun findByUserIdAndIsActiveTrueWithPrimaryTeam(@Param("userId") userId: String): Optional<WorkspaceMember>

    /**
     * Check if user exists with specific role in current tenant - uses @TenantId
     */
    fun existsByUserIdAndRoleAndIsActiveTrue(userId: String, role: WorkspaceRole): Boolean
}