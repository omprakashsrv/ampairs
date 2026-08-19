package com.ampairs.workspace.repository

import com.ampairs.workspace.model.WorkspaceModule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Repository for managing workspace module configurations.
 * Handles module installations and configurations specific to each workspace.
 */
@Repository
interface WorkspaceModuleRepository : JpaRepository<WorkspaceModule, String>,
    JpaSpecificationExecutor<WorkspaceModule> {

    /**
     * Find all modules for a specific workspace
     */
    fun findByWorkspaceId(workspaceId: String): List<WorkspaceModule>

    /**
     * Find module by workspace and master module code
     */
    fun findByWorkspaceIdAndMasterModuleModuleCode(
        workspaceId: String,
        moduleCode: String
    ): WorkspaceModule?

    /**
     * Check if module is installed in workspace
     */
    fun existsByWorkspaceIdAndMasterModuleModuleCode(
        workspaceId: String,
        moduleCode: String
    ): Boolean

    /**
     * Find modules that need updates (outdated versions)
     * Note: Version comparison moved to service layer
     */
    fun findByWorkspaceIdAndEnabledTrue(workspaceId: String): List<WorkspaceModule>

    /**
     * Sync checkpoint: max `updatedAt` across a workspace's installed modules (null when none).
     * WorkspaceModule carries an explicit `workspaceId` column (not `@TenantId`), so the workspace
     * is filtered explicitly here rather than relying on ambient tenant filtering.
     */
    @Query("SELECT MAX(m.updatedAt) FROM WorkspaceModule m WHERE m.workspaceId = :workspaceId")
    fun findMaxUpdatedAtByWorkspaceId(@Param("workspaceId") workspaceId: String): Instant?

}