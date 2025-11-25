package com.ampairs.workspace.repository

import com.ampairs.workspace.model.Workspace
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
    fun findByUid(uid: String): Optional<Workspace>

    /**
     * Check if a slug is available
     */
    fun existsBySlug(slug: String): Boolean

    /**
     * Search workspaces by name or description
     */
    @Query(
        """
        SELECT w FROM Workspace w 
        WHERE w.active = true 
        AND (LOWER(w.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) 
             OR LOWER(w.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    """
    )
    fun searchWorkspaces(@Param("searchTerm") searchTerm: String, pageable: Pageable): Page<Workspace>

    /**
     * Find workspaces that a user has access to with pagination
     */
    /**
     * Find workspaces by user ID - bypasses @TenantId filtering for cross-tenant workspace listing
     * Uses native query to avoid automatic tenant filtering since users can belong to multiple workspaces
     */
    @Query(
        value = """
        SELECT DISTINCT w.* FROM workspaces w
        INNER JOIN workspace_members wm ON w.uid = wm.workspace_id
        WHERE wm.user_id = :userId
        AND wm.is_active = true
        AND w.active = true
        """,
        countQuery = """
        SELECT COUNT(DISTINCT w.uid) FROM workspaces w
        INNER JOIN workspace_members wm ON w.uid = wm.workspace_id
        WHERE wm.user_id = :userId
        AND wm.is_active = true
        AND w.active = true
        """,
        nativeQuery = true
    )
    fun findWorkspacesByUserId(@Param("userId") userId: String, pageable: Pageable): Page<Workspace>

    /**
     * Update last activity timestamp for a workspace
     */
    @Modifying
    @Query("UPDATE Workspace w SET w.lastActivityAt = :timestamp WHERE w.id = :workspaceId")
    fun updateLastActivity(@Param("workspaceId") workspaceId: String, @Param("timestamp") timestamp: LocalDateTime)

    /**
     * Count active workspaces created by a user
     * Used for multi-workspace discount calculation
     */
    @Query(
        value = """
        SELECT COUNT(*) FROM workspaces w
        WHERE w.created_by = :userId
        AND w.active = true
        """,
        nativeQuery = true
    )
    fun countByCreatedByAndActiveTrue(@Param("userId") userId: String): Int

}