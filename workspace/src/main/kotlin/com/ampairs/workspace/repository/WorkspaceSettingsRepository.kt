package com.ampairs.workspace.repository

import com.ampairs.workspace.model.WorkspaceSettings
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

/**
 * Repository for workspace settings operations
 */
@Repository
interface WorkspaceSettingsRepository : JpaRepository<WorkspaceSettings, String> {

    /**
     * Find settings by workspace ID
     */
    fun findByWorkspaceId(workspaceId: String): Optional<WorkspaceSettings>

    /**
     * Check if settings exist for workspace
     */
    fun existsByWorkspaceId(workspaceId: String): Boolean

    /**
     * Delete settings by workspace ID
     */
    fun deleteByWorkspaceId(workspaceId: String)

}