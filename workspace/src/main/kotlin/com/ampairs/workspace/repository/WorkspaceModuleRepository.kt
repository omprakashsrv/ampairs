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
     * Cross-tenant: every enabled install of a given module code across all workspaces. Used by
     * scheduled batch jobs (e.g. cb_maintenance PM generation / escalation) to iterate the
     * workspaces where a module is turned on. `workspaceId` is `@TenantId`-filtered, so this must
     * be a native query to bypass Hibernate's tenant filter — the job runs with no ambient tenant,
     * and a derived/JPQL query here would silently return zero rows for every workspace.
     */
    @Query(
        value = """
            SELECT wm.* FROM workspace_modules wm
            JOIN master_modules mm ON mm.id = wm.master_module_id
            WHERE mm.module_code = :moduleCode AND wm.enabled = true
        """,
        nativeQuery = true
    )
    fun findByMasterModuleModuleCodeAndEnabledTrue(@Param("moduleCode") moduleCode: String): List<WorkspaceModule>

    /**
     * Sync checkpoint: max `updatedAt` across a workspace's installed modules (null when none).
     * Only ever called with the ambient tenant already set to this same `workspaceId`, so the
     * explicit filter here is redundant with (not a substitute for) `@TenantId` auto-filtering.
     */
    @Query("SELECT MAX(m.updatedAt) FROM WorkspaceModule m WHERE m.workspaceId = :workspaceId")
    fun findMaxUpdatedAtByWorkspaceId(@Param("workspaceId") workspaceId: String): Instant?

}