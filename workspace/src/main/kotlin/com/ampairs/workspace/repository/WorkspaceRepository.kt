package com.ampairs.workspace.repository

import com.ampairs.workspace.model.Workspace
import com.ampairs.workspace.model.enums.SubscriptionPlan
import com.ampairs.workspace.model.enums.WorkspaceType
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
 * Repository for workspace operations with advanced querying capabilities
 */
@Repository
interface WorkspaceRepository : JpaRepository<Workspace, String> {

    /**
     * Find workspace by slug (URL-friendly identifier)
     */
    fun findBySlug(slug: String): Optional<Workspace>

    /**
     * Check if a slug is available
     */
    fun existsBySlug(slug: String): Boolean

    /**
     * Find workspaces by owner (created_by)
     */
    fun findByCreatedBy(ownerId: String, pageable: Pageable): Page<Workspace>

    /**
     * Find active workspaces by type
     */
    fun findByWorkspaceTypeAndIsActiveTrue(workspaceType: WorkspaceType, pageable: Pageable): Page<Workspace>

    /**
     * Find workspaces by subscription plan
     */
    fun findBySubscriptionPlan(subscriptionPlan: SubscriptionPlan, pageable: Pageable): Page<Workspace>

    /**
     * Find workspaces that haven't been active since a specific date
     */
    fun findByLastActivityAtBefore(date: LocalDateTime): List<Workspace>

    /**
     * Find workspaces with expired trials
     */
    @Query(
        """
        SELECT w FROM workspaces w 
        WHERE w.trialExpiresAt IS NOT NULL 
        AND w.trialExpiresAt < :currentDate
        AND w.subscriptionPlan = :freePlan
    """
    )
    fun findExpiredTrials(
        @Param("currentDate") currentDate: LocalDateTime,
        @Param("freePlan") freePlan: SubscriptionPlan = SubscriptionPlan.FREE,
    ): List<Workspace>

    /**
     * Find workspaces approaching storage limit
     */
    @Query(
        """
        SELECT w FROM workspaces w 
        WHERE w.isActive = true 
        AND (w.storageUsedGb * 100.0 / w.storageLimitGb) >= :percentage
    """
    )
    fun findWorkspacesApproachingStorageLimit(@Param("percentage") percentage: Double): List<Workspace>

    /**
     * Count workspaces by owner
     */
    fun countByCreatedBy(ownerId: String): Long

    /**
     * Count active workspaces by type
     */
    fun countByWorkspaceTypeAndIsActiveTrue(workspaceType: WorkspaceType): Long

    /**
     * Search workspaces by name or description
     */
    @Query(
        """
        SELECT w FROM workspaces w 
        WHERE w.isActive = true 
        AND (LOWER(w.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) 
             OR LOWER(w.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    """
    )
    fun searchWorkspaces(@Param("searchTerm") searchTerm: String, pageable: Pageable): Page<Workspace>

    /**
     * Find workspaces that a user has access to (via WorkspaceMember relationship)
     */
    @Query(
        """
        SELECT DISTINCT w FROM workspaces w 
        INNER JOIN workspace_members wm ON w.id = wm.workspaceId 
        WHERE wm.userId = :userId 
        AND wm.isActive = true 
        AND w.isActive = true
        ORDER BY wm.lastActiveAt DESC, w.lastActivityAt DESC
    """
    )
    fun findWorkspacesByUserId(@Param("userId") userId: String): List<Workspace>

    /**
     * Find workspaces that a user has access to with pagination
     */
    @Query(
        """
        SELECT DISTINCT w FROM workspaces w 
        INNER JOIN workspace_members wm ON w.id = wm.workspaceId 
        WHERE wm.userId = :userId 
        AND wm.isActive = true 
        AND w.isActive = true
    """
    )
    fun findWorkspacesByUserId(@Param("userId") userId: String, pageable: Pageable): Page<Workspace>

    /**
     * Update last activity timestamp for a workspace
     */
    @Modifying
    @Query("UPDATE workspaces w SET w.lastActivityAt = :timestamp WHERE w.id = :workspaceId")
    fun updateLastActivity(@Param("workspaceId") workspaceId: String, @Param("timestamp") timestamp: LocalDateTime)

    /**
     * Update storage usage for a workspace
     */
    @Modifying
    @Query("UPDATE workspaces w SET w.storageUsedGb = :storageUsedGb WHERE w.id = :workspaceId")
    fun updateStorageUsage(@Param("workspaceId") workspaceId: String, @Param("storageUsedGb") storageUsedGb: Int)

    /**
     * Get workspace statistics
     */
    @Query(
        """
        SELECT 
            COUNT(w) as totalWorkspaces,
            COUNT(CASE WHEN w.isActive = true THEN 1 END) as activeWorkspaces,
            COUNT(CASE WHEN w.subscriptionPlan = :freePlan THEN 1 END) as freeWorkspaces,
            COUNT(CASE WHEN w.subscriptionPlan != :freePlan THEN 1 END) as paidWorkspaces
        FROM workspaces w
    """
    )
    fun getWorkspaceStatistics(@Param("freePlan") freePlan: SubscriptionPlan = SubscriptionPlan.FREE): Map<String, Long>

    /**
     * Deactivate inactive workspaces
     */
    @Modifying
    @Query(
        """
        UPDATE workspaces w 
        SET w.isActive = false, w.updatedAt = :currentDate 
        WHERE w.lastActivityAt < :inactiveDate
        AND w.isActive = true
    """
    )
    fun deactivateInactiveWorkspaces(
        @Param("inactiveDate") inactiveDate: LocalDateTime,
        @Param("currentDate") currentDate: LocalDateTime,
    ): Int
}