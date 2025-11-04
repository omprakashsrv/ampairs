package com.ampairs.workspace.repository

import com.ampairs.workspace.model.WorkspaceModule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

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

}