package com.ampairs.workspace.repository

import com.ampairs.workspace.model.WorkspaceSettings
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
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

    /**
     * Find settings modified after a specific date
     */
    fun findByLastModifiedAtAfter(modifiedAfter: LocalDateTime): List<WorkspaceSettings>

    /**
     * Find settings modified by a specific user
     */
    fun findByLastModifiedBy(modifiedBy: String): List<WorkspaceSettings>

    /**
     * Update last modified timestamp
     */
    @Modifying
    @Query(
        """
        UPDATE workspace_settings ws 
        SET ws.lastModifiedAt = :modifiedAt, ws.lastModifiedBy = :modifiedBy
        WHERE ws.workspaceId = :workspaceId
    """
    )
    fun updateLastModified(
        @Param("workspaceId") workspaceId: String,
        @Param("modifiedAt") modifiedAt: LocalDateTime,
        @Param("modifiedBy") modifiedBy: String?,
    )

    /**
     * Update branding settings
     */
    @Modifying
    @Query(
        """
        UPDATE workspace_settings ws 
        SET ws.branding = :branding,
            ws.lastModifiedAt = :modifiedAt,
            ws.lastModifiedBy = :modifiedBy
        WHERE ws.workspaceId = :workspaceId
    """
    )
    fun updateBranding(
        @Param("workspaceId") workspaceId: String,
        @Param("branding") branding: String,
        @Param("modifiedAt") modifiedAt: LocalDateTime,
        @Param("modifiedBy") modifiedBy: String?,
    )

    /**
     * Update notification settings
     */
    @Modifying
    @Query(
        """
        UPDATE workspace_settings ws 
        SET ws.notifications = :notifications,
            ws.lastModifiedAt = :modifiedAt,
            ws.lastModifiedBy = :modifiedBy
        WHERE ws.workspaceId = :workspaceId
    """
    )
    fun updateNotifications(
        @Param("workspaceId") workspaceId: String,
        @Param("notifications") notifications: String,
        @Param("modifiedAt") modifiedAt: LocalDateTime,
        @Param("modifiedBy") modifiedBy: String?,
    )

    /**
     * Update integration settings
     */
    @Modifying
    @Query(
        """
        UPDATE workspace_settings ws 
        SET ws.integrations = :integrations,
            ws.lastModifiedAt = :modifiedAt,
            ws.lastModifiedBy = :modifiedBy
        WHERE ws.workspaceId = :workspaceId
    """
    )
    fun updateIntegrations(
        @Param("workspaceId") workspaceId: String,
        @Param("integrations") integrations: String,
        @Param("modifiedAt") modifiedAt: LocalDateTime,
        @Param("modifiedBy") modifiedBy: String?,
    )

    /**
     * Update security settings
     */
    @Modifying
    @Query(
        """
        UPDATE workspace_settings ws 
        SET ws.security = :security,
            ws.lastModifiedAt = :modifiedAt,
            ws.lastModifiedBy = :modifiedBy
        WHERE ws.workspaceId = :workspaceId
    """
    )
    fun updateSecurity(
        @Param("workspaceId") workspaceId: String,
        @Param("security") security: String,
        @Param("modifiedAt") modifiedAt: LocalDateTime,
        @Param("modifiedBy") modifiedBy: String?,
    )

    /**
     * Update feature settings
     */
    @Modifying
    @Query(
        """
        UPDATE workspace_settings ws 
        SET ws.features = :features,
            ws.lastModifiedAt = :modifiedAt,
            ws.lastModifiedBy = :modifiedBy
        WHERE ws.workspaceId = :workspaceId
    """
    )
    fun updateFeatures(
        @Param("workspaceId") workspaceId: String,
        @Param("features") features: String,
        @Param("modifiedAt") modifiedAt: LocalDateTime,
        @Param("modifiedBy") modifiedBy: String?,
    )

    /**
     * Update preference settings
     */
    @Modifying
    @Query(
        """
        UPDATE workspace_settings ws 
        SET ws.preferences = :preferences,
            ws.lastModifiedAt = :modifiedAt,
            ws.lastModifiedBy = :modifiedBy
        WHERE ws.workspaceId = :workspaceId
    """
    )
    fun updatePreferences(
        @Param("workspaceId") workspaceId: String,
        @Param("preferences") preferences: String,
        @Param("modifiedAt") modifiedAt: LocalDateTime,
        @Param("modifiedBy") modifiedBy: String?,
    )

    /**
     * Get count of workspaces with custom branding
     */
    @Query("SELECT COUNT(ws) FROM workspace_settings ws WHERE ws.branding != '{}'")
    fun countWorkspacesWithCustomBranding(): Long

    /**
     * Get count of workspaces with integrations configured
     */
    @Query("SELECT COUNT(ws) FROM workspace_settings ws WHERE ws.integrations != '{}'")
    fun countWorkspacesWithIntegrations(): Long

    /**
     * Find workspaces with specific feature enabled
     */
    @Query("SELECT ws FROM workspace_settings ws WHERE ws.features LIKE CONCAT('%', :feature, '%')")
    fun findWorkspacesWithFeature(@Param("feature") feature: String): List<WorkspaceSettings>

    /**
     * Backup settings before major update
     */
    @Query(
        """
        SELECT ws.workspaceId, ws.branding, ws.notifications, ws.integrations, 
               ws.security, ws.features, ws.preferences
        FROM workspace_settings ws 
        WHERE ws.workspaceId IN :workspaceIds
    """
    )
    fun backupSettings(@Param("workspaceIds") workspaceIds: List<String>): List<Map<String, Any>>
}