package com.ampairs.workspace.repository

import com.ampairs.workspace.model.WorkspaceActivity
import com.ampairs.workspace.model.enums.WorkspaceActivityType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Repository for workspace activity operations
 * Provides methods to query and manage workspace activity logs
 */
@Repository
interface WorkspaceActivityRepository : JpaRepository<WorkspaceActivity, String> {

    /**
     * Find activities by workspace (tenant) ID with pagination
     */
    fun findByWorkspaceIdOrderByCreatedAtDesc(workspaceId: String, pageable: Pageable): Page<WorkspaceActivity>

    /**
     * Find activities by workspace ID and activity type
     */
    fun findByWorkspaceIdAndActivityTypeOrderByCreatedAtDesc(
        workspaceId: String,
        activityType: WorkspaceActivityType,
        pageable: Pageable,
    ): Page<WorkspaceActivity>

    /**
     * Find activities by workspace ID and actor
     */
    fun findByWorkspaceIdAndActorIdOrderByCreatedAtDesc(
        workspaceId: String,
        actorId: String,
        pageable: Pageable,
    ): Page<WorkspaceActivity>

    /**
     * Find activities by workspace ID within date range
     */
    @Query("SELECT wa FROM com.ampairs.workspace.model.WorkspaceActivity wa WHERE wa.workspaceId = :workspaceId AND wa.createdAt BETWEEN :startDate AND :endDate ORDER BY wa.createdAt DESC")
    fun findByWorkspaceIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        @Param("workspaceId") workspaceId: String,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        pageable: Pageable,
    ): Page<WorkspaceActivity>

    /**
     * Find activities by workspace ID and severity level
     */
    fun findByWorkspaceIdAndSeverityOrderByCreatedAtDesc(
        workspaceId: String,
        severity: String,
        pageable: Pageable,
    ): Page<WorkspaceActivity>

    /**
     * Count activities by workspace and activity type
     */
    fun countByWorkspaceIdAndActivityType(workspaceId: String, activityType: WorkspaceActivityType): Long

    /**
     * Count activities by workspace and actor
     */
    fun countByWorkspaceIdAndActorId(workspaceId: String, actorId: String): Long

    /**
     * Count activities by workspace within date range
     */
    @Query("SELECT COUNT(wa) FROM com.ampairs.workspace.model.WorkspaceActivity wa WHERE wa.workspaceId = :workspaceId AND wa.createdAt BETWEEN :startDate AND :endDate")
    fun countByWorkspaceIdAndCreatedAtBetween(
        @Param("workspaceId") workspaceId: String,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
    ): Long

    /**
     * Get activity statistics by type for a workspace
     */
    @Query(
        """
        SELECT wa.activityType, COUNT(wa)
        FROM com.ampairs.workspace.model.WorkspaceActivity wa
        WHERE wa.workspaceId = :workspaceId
        AND wa.createdAt >= :sinceDate
        GROUP BY wa.activityType
        ORDER BY COUNT(wa) DESC
    """
    )
    fun getActivityStatsByType(
        @Param("workspaceId") workspaceId: String,
        @Param("sinceDate") sinceDate: LocalDateTime,
    ): List<Array<Any>>

    /**
     * Get most active users for a workspace
     */
    @Query(
        """
        SELECT wa.actorId, wa.actorName, COUNT(wa)
        FROM com.ampairs.workspace.model.WorkspaceActivity wa
        WHERE wa.workspaceId = :workspaceId
        AND wa.createdAt >= :sinceDate
        GROUP BY wa.actorId, wa.actorName
        ORDER BY COUNT(wa) DESC
    """
    )
    fun getMostActiveUsers(
        @Param("workspaceId") workspaceId: String,
        @Param("sinceDate") sinceDate: LocalDateTime,
        pageable: Pageable,
    ): List<Array<Any>>

    /**
     * Get recent activities for dashboard
     */
    @Query(
        """
        SELECT wa
        FROM com.ampairs.workspace.model.WorkspaceActivity wa
        WHERE wa.workspaceId = :workspaceId
        AND wa.createdAt >= :sinceDate
        ORDER BY wa.createdAt DESC
    """
    )
    fun getRecentActivities(
        @Param("workspaceId") workspaceId: String,
        @Param("sinceDate") sinceDate: LocalDateTime,
        pageable: Pageable,
    ): List<WorkspaceActivity>

    /**
     * Search activities by description or target entity name
     */
    @Query(
        """
        SELECT wa
        FROM com.ampairs.workspace.model.WorkspaceActivity wa
        WHERE wa.workspaceId = :workspaceId
        AND (
            LOWER(wa.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(wa.targetEntityName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(wa.actorName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        )
        ORDER BY wa.createdAt DESC
    """
    )
    fun searchActivities(
        @Param("workspaceId") workspaceId: String,
        @Param("searchTerm") searchTerm: String,
        pageable: Pageable,
    ): Page<WorkspaceActivity>

    /**
     * Find activities by target entity
     */
    fun findByWorkspaceIdAndTargetEntityTypeAndTargetEntityIdOrderByCreatedAtDesc(
        workspaceId: String,
        targetEntityType: String,
        targetEntityId: String,
        pageable: Pageable,
    ): Page<WorkspaceActivity>

    /**
     * Get activity timeline for a specific entity
     */
    @Query(
        """
        SELECT wa
        FROM com.ampairs.workspace.model.WorkspaceActivity wa
        WHERE wa.workspaceId = :workspaceId
        AND wa.targetEntityType = :entityType
        AND wa.targetEntityId = :entityId
        ORDER BY wa.createdAt ASC
    """
    )
    fun getEntityTimeline(
        @Param("workspaceId") workspaceId: String,
        @Param("entityType") entityType: String,
        @Param("entityId") entityId: String,
    ): List<WorkspaceActivity>

    /**
     * Delete activities older than specified date for a workspace
     */
    @Modifying
    @Query("DELETE FROM com.ampairs.workspace.model.WorkspaceActivity wa WHERE wa.workspaceId = :workspaceId AND wa.createdAt < :cutoffDate")
    fun deleteByWorkspaceIdAndCreatedAtBefore(
        @Param("workspaceId") workspaceId: String,
        @Param("cutoffDate") cutoffDate: LocalDateTime,
    ): Long

    /**
     * Delete all activities for a workspace (used during workspace deletion)
     */
    @Modifying
    @Query("DELETE FROM com.ampairs.workspace.model.WorkspaceActivity wa WHERE wa.workspaceId = :workspaceId")
    fun deleteByWorkspaceId(@Param("workspaceId") workspaceId: String): Long

    /**
     * Check if there are any activities for a workspace
     */
    fun existsByWorkspaceId(workspaceId: String): Boolean

    /**
     * Get activity count by severity for monitoring
     */
    @Query(
        """
        SELECT wa.severity, COUNT(wa)
        FROM com.ampairs.workspace.model.WorkspaceActivity wa
        WHERE wa.workspaceId = :workspaceId
        AND wa.createdAt >= :sinceDate
        GROUP BY wa.severity
    """
    )
    fun getActivityCountBySeverity(
        @Param("workspaceId") workspaceId: String,
        @Param("sinceDate") sinceDate: LocalDateTime,
    ): List<Array<Any>>

    /**
     * Get daily activity count for the last N days
     */
    @Query(
        """
        SELECT DATE(wa.createdAt), COUNT(wa)
        FROM com.ampairs.workspace.model.WorkspaceActivity wa
        WHERE wa.workspaceId = :workspaceId
        AND wa.createdAt >= :sinceDate
        GROUP BY DATE(wa.createdAt)
        ORDER BY DATE(wa.createdAt) DESC
    """
    )
    fun getDailyActivityCount(
        @Param("workspaceId") workspaceId: String,
        @Param("sinceDate") sinceDate: LocalDateTime,
    ): List<Array<Any>>
}