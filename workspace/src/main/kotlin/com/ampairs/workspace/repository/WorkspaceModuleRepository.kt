package com.ampairs.workspace.repository

import com.ampairs.workspace.model.WorkspaceModule
import com.ampairs.workspace.model.enums.ModuleCategory
import com.ampairs.workspace.model.enums.WorkspaceModuleStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Repository for managing workspace module configurations.
 * Handles module installations and configurations specific to each workspace.
 */
@Repository
interface WorkspaceModuleRepository : JpaRepository<WorkspaceModule, String>, JpaSpecificationExecutor<WorkspaceModule> {

    /**
     * Find all modules for a specific workspace
     */
    fun findByWorkspaceId(workspaceId: String): List<WorkspaceModule>

    /**
     * Find enabled modules for a workspace ordered by display order
     */
    fun findByWorkspaceIdAndEnabledTrueOrderByDisplayOrderAsc(workspaceId: String): List<WorkspaceModule>

    /**
     * Find modules by workspace and status
     */
    fun findByWorkspaceIdAndStatus(workspaceId: String, status: WorkspaceModuleStatus): List<WorkspaceModule>

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
     * Find modules installed by specific user
     */
    fun findByInstalledBy(installedBy: String): List<WorkspaceModule>

    /**
     * Find recently installed modules in workspace
     */
    fun findByWorkspaceIdAndInstalledAtAfterOrderByInstalledAtDesc(
        workspaceId: String,
        installedAfter: LocalDateTime
    ): List<WorkspaceModule>

    /**
     * Find modules that need updates (outdated versions)
     * Note: Version comparison moved to service layer
     */
    fun findByWorkspaceIdAndEnabledTrue(workspaceId: String): List<WorkspaceModule>

    /**
     * Get workspace module usage statistics
     * Note: Statistics aggregation moved to service layer
     */
    fun countByWorkspaceId(workspaceId: String): Long

    /**
     * Find most used modules in workspace
     * Note: Usage-based sorting moved to service layer due to JSON field limitations
     */
    @Query("SELECT wm FROM WorkspaceModule wm WHERE wm.workspaceId = :workspaceId AND wm.enabled = true ORDER BY wm.displayOrder ASC")
    fun findMostUsedModules(
        @Param("workspaceId") workspaceId: String,
        pageable: Pageable,
    ): Page<WorkspaceModule>

    /**
     * Find modules with errors or performance issues
     * Note: Error checking moved to service layer due to JSON field limitations
     */
    fun findByWorkspaceIdAndEnabled(
        workspaceId: String,
        enabled: Boolean
    ): List<WorkspaceModule>

    /**
     * Find modules by category through master module relationship
     * Note: Category override logic moved to service layer
     */
    fun findByWorkspaceIdAndEnabledTrueAndMasterModuleCategoryOrderByDisplayOrderAsc(
        workspaceId: String,
        category: ModuleCategory
    ): List<WorkspaceModule>

    /**
     * Search modules in workspace by name or description
     * Note: Search logic moved to service layer for complex text matching
     */
    fun findByWorkspaceIdAndEnabledTrueAndMasterModuleNameContainingIgnoreCaseOrderByDisplayOrderAsc(
        workspaceId: String,
        name: String
    ): List<WorkspaceModule>

    /**
     * Find modules with expiring licenses
     */
    fun findByWorkspaceIdAndLicenseExpiresAtBetweenOrderByLicenseExpiresAtAsc(
        workspaceId: String,
        now: LocalDateTime,
        expiryThreshold: LocalDateTime
    ): List<WorkspaceModule>

    /**
     * Count enabled modules by category in workspace
     * Note: Category override logic and grouping moved to service layer
     */
    fun countByWorkspaceIdAndEnabledTrueAndMasterModuleCategory(
        workspaceId: String,
        category: ModuleCategory
    ): Long

    /**
     * Find modules visible to user role
     * Note: Role-based visibility logic moved to service layer
     */
    @Query("SELECT wm FROM WorkspaceModule wm WHERE wm.workspaceId = :workspaceId AND wm.enabled = true ORDER BY wm.displayOrder ASC")
    fun findVisibleModulesForUser(
        @Param("workspaceId") workspaceId: String,
        @Param("userRole") userRole: String,
    ): List<WorkspaceModule>

    /**
     * Find quick access modules for user
     * Note: Quick access logic moved to service layer
     */
    @Query("SELECT wm FROM WorkspaceModule wm WHERE wm.workspaceId = :workspaceId AND wm.enabled = true ORDER BY wm.displayOrder ASC")
    fun findQuickAccessModulesForUser(
        @Param("workspaceId") workspaceId: String,
        @Param("userId") userId: String,
    ): List<WorkspaceModule>

    /**
     * Find user's favorite modules
     * Note: Favorite logic moved to service layer
     */
    @Query("SELECT wm FROM WorkspaceModule wm WHERE wm.workspaceId = :workspaceId AND wm.enabled = true ORDER BY wm.displayOrder ASC")
    fun findUserFavoriteModules(
        @Param("workspaceId") workspaceId: String,
        @Param("userId") userId: String,
    ): List<WorkspaceModule>

    /**
     * Find modules by multiple workspace IDs (for bulk operations)
     */
    fun findByWorkspaceIdIn(workspaceIds: List<String>): List<WorkspaceModule>

    /**
     * Get total storage usage across all modules in workspace
     * Note: Sum aggregation moved to service layer
     */
    fun findAllByWorkspaceId(workspaceId: String): List<WorkspaceModule>

    /**
     * Find unused modules (not accessed in specified period)
     * Note: Usage date filtering moved to service layer
     */
    @Query("SELECT wm FROM WorkspaceModule wm WHERE wm.workspaceId = :workspaceId AND wm.enabled = true ORDER BY wm.displayOrder ASC")
    fun findUnusedModules(
        @Param("workspaceId") workspaceId: String,
        @Param("lastUsedBefore") lastUsedBefore: LocalDateTime,
    ): List<WorkspaceModule>

    /**
     * Find popular modules (high usage and satisfaction)
     * Note: Popularity filtering moved to service layer
     */
    @Query("SELECT wm FROM WorkspaceModule wm WHERE wm.workspaceId = :workspaceId AND wm.enabled = true ORDER BY wm.displayOrder ASC")
    fun findPopularModules(
        @Param("workspaceId") workspaceId: String,
        @Param("minAccesses") minAccesses: Int,
        @Param("minSatisfaction") minSatisfaction: Double,
    ): List<WorkspaceModule>

    /**
     * Update storage usage for module
     * Note: Use save() method in service layer instead
     */
    // Removed - use repository.save() in service layer

    /**
     * Update module display order
     * Note: Use save() method in service layer instead
     */
    // Removed - use repository.save() in service layer

    /**
     * Get user activity summary for modules
     * Note: Activity tracking moved to service layer due to JSON field limitations
     */
    fun countByWorkspaceIdAndEnabledTrue(workspaceId: String): Long

    /**
     * Find modules that need attention (errors, updates, expiry)
     * Note: Complex attention logic moved to service layer
     */
    fun findByWorkspaceIdAndEnabledTrueAndStatusNot(
        workspaceId: String,
        status: WorkspaceModuleStatus
    ): List<WorkspaceModule>

    /**
     * Get module category distribution
     * Note: Category distribution aggregation moved to service layer
     */
    fun findByWorkspaceIdOrderByMasterModuleCategory(workspaceId: String): List<WorkspaceModule>
}