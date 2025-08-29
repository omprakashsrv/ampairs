package com.ampairs.workspace.repository

import com.ampairs.workspace.model.WorkspaceModule
import com.ampairs.workspace.model.enums.ModuleCategory
import com.ampairs.workspace.model.enums.WorkspaceModuleStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
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
    @Query("""
        SELECT wm FROM WorkspaceModule wm 
        JOIN wm.masterModule mm 
        WHERE wm.workspaceId = :workspaceId 
        AND mm.moduleCode = :moduleCode
    """)
    fun findByWorkspaceIdAndModuleCode(
        @Param("workspaceId") workspaceId: String,
        @Param("moduleCode") moduleCode: String
    ): WorkspaceModule?

    /**
     * Check if module is installed in workspace
     */
    @Query("""
        SELECT COUNT(wm) > 0 FROM WorkspaceModule wm 
        JOIN wm.masterModule mm 
        WHERE wm.workspaceId = :workspaceId 
        AND mm.moduleCode = :moduleCode
    """)
    fun isModuleInstalledInWorkspace(
        @Param("workspaceId") workspaceId: String,
        @Param("moduleCode") moduleCode: String
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
     */
    @Query("""
        SELECT wm FROM WorkspaceModule wm 
        JOIN wm.masterModule mm 
        WHERE wm.workspaceId = :workspaceId 
        AND wm.installedVersion != mm.version 
        AND wm.enabled = true
    """)
    fun findModulesNeedingUpdates(@Param("workspaceId") workspaceId: String): List<WorkspaceModule>

    /**
     * Get workspace module usage statistics
     */
    @Query("""
        SELECT new map(
            COUNT(*) as totalModules,
            COUNT(CASE WHEN wm.enabled = true THEN 1 END) as enabledModules,
            COUNT(CASE WHEN wm.status = 'ACTIVE' THEN 1 END) as activeModules,
            SUM(wm.storageUsedMb) as totalStorageUsedMb,
            AVG(wm.usageMetrics.userSatisfactionScore) as averageSatisfaction,
            SUM(wm.usageMetrics.totalAccesses) as totalAccesses
        )
        FROM WorkspaceModule wm 
        WHERE wm.workspaceId = :workspaceId
    """)
    fun getWorkspaceModuleStatistics(@Param("workspaceId") workspaceId: String): Map<String, Any>

    /**
     * Find most used modules in workspace
     */
    @Query("""
        SELECT wm FROM WorkspaceModule wm 
        WHERE wm.workspaceId = :workspaceId 
        AND wm.enabled = true 
        ORDER BY wm.usageMetrics.totalAccesses DESC, wm.usageMetrics.lastAccessedAt DESC
    """)
    fun findMostUsedModules(@Param("workspaceId") workspaceId: String, pageable: Pageable): Page<WorkspaceModule>

    /**
     * Find modules with errors or performance issues
     */
    @Query("""
        SELECT wm FROM WorkspaceModule wm 
        WHERE wm.workspaceId = :workspaceId 
        AND wm.usageMetrics.errorCount > 0
        ORDER BY wm.usageMetrics.errorCount DESC
    """)
    fun findModulesWithIssues(@Param("workspaceId") workspaceId: String): List<WorkspaceModule>

    /**
     * Find modules by category through master module relationship
     */
    @Query("""
        SELECT wm FROM WorkspaceModule wm 
        JOIN wm.masterModule mm 
        WHERE wm.workspaceId = :workspaceId 
        AND (wm.categoryOverride = :category OR mm.category = :category)
        AND wm.enabled = true
        ORDER BY wm.displayOrder ASC
    """)
    fun findByWorkspaceIdAndCategory(
        @Param("workspaceId") workspaceId: String,
        @Param("category") category: ModuleCategory
    ): List<WorkspaceModule>

    /**
     * Search modules in workspace by name or description
     */
    @Query("""
        SELECT wm FROM WorkspaceModule wm 
        JOIN wm.masterModule mm 
        WHERE wm.workspaceId = :workspaceId 
        AND wm.enabled = true
        AND (LOWER(mm.name) LIKE LOWER(CONCAT('%', :query, '%')) 
             OR LOWER(mm.description) LIKE LOWER(CONCAT('%', :query, '%'))
             OR LOWER(wm.settings.customName) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY wm.displayOrder ASC
    """)
    fun searchModulesInWorkspace(
        @Param("workspaceId") workspaceId: String,
        @Param("query") query: String
    ): List<WorkspaceModule>

    /**
     * Find modules with expiring licenses
     */
    @Query("""
        SELECT wm FROM WorkspaceModule wm 
        WHERE wm.workspaceId = :workspaceId 
        AND wm.licenseExpiresAt IS NOT NULL 
        AND wm.licenseExpiresAt BETWEEN :now AND :expiryThreshold
        ORDER BY wm.licenseExpiresAt ASC
    """)
    fun findModulesWithExpiringLicenses(
        @Param("workspaceId") workspaceId: String,
        @Param("now") now: LocalDateTime,
        @Param("expiryThreshold") expiryThreshold: LocalDateTime
    ): List<WorkspaceModule>

    /**
     * Count enabled modules by category in workspace
     */
    @Query("""
        SELECT 
            CASE WHEN wm.categoryOverride IS NOT NULL THEN wm.categoryOverride ELSE mm.category END as category,
            COUNT(wm) as count 
        FROM WorkspaceModule wm 
        JOIN wm.masterModule mm
        WHERE wm.workspaceId = :workspaceId 
        AND wm.enabled = true 
        GROUP BY CASE WHEN wm.categoryOverride IS NOT NULL THEN wm.categoryOverride ELSE mm.category END
    """)
    fun countModulesByCategory(@Param("workspaceId") workspaceId: String): List<Map<String, Any>>

    /**
     * Find modules visible to user role
     */
    @Query("""
        SELECT wm FROM WorkspaceModule wm 
        JOIN wm.masterModule mm
        WHERE wm.workspaceId = :workspaceId 
        AND wm.enabled = true
        AND (wm.settings.visibility = 'VISIBLE' 
             OR (wm.settings.visibility = 'ADMIN_ONLY' AND :userRole IN ('ADMIN', 'OWNER')))
        ORDER BY wm.displayOrder ASC
    """)
    fun findVisibleModulesForUser(
        @Param("workspaceId") workspaceId: String,
        @Param("userRole") userRole: String
    ): List<WorkspaceModule>

    /**
     * Find quick access modules for user
     */
    @Query("""
        SELECT wm FROM WorkspaceModule wm 
        WHERE wm.workspaceId = :workspaceId 
        AND wm.enabled = true
        AND (wm.settings.quickAccess = true 
             OR JSON_CONTAINS(wm.userPreferences, JSON_OBJECT('userId', :userId, 'quickAccess', true)))
        ORDER BY wm.displayOrder ASC
    """)
    fun findQuickAccessModulesForUser(
        @Param("workspaceId") workspaceId: String,
        @Param("userId") userId: String
    ): List<WorkspaceModule>

    /**
     * Find user's favorite modules
     */
    @Query("""
        SELECT wm FROM WorkspaceModule wm 
        WHERE wm.workspaceId = :workspaceId 
        AND wm.enabled = true
        AND JSON_CONTAINS(wm.userPreferences, JSON_OBJECT('userId', :userId, 'favorited', true))
        ORDER BY wm.displayOrder ASC
    """)
    fun findUserFavoriteModules(
        @Param("workspaceId") workspaceId: String,
        @Param("userId") userId: String
    ): List<WorkspaceModule>

    /**
     * Find modules by multiple workspace IDs (for bulk operations)
     */
    fun findByWorkspaceIdIn(workspaceIds: List<String>): List<WorkspaceModule>

    /**
     * Get total storage usage across all modules in workspace
     */
    @Query("""
        SELECT COALESCE(SUM(wm.storageUsedMb), 0) 
        FROM WorkspaceModule wm 
        WHERE wm.workspaceId = :workspaceId
    """)
    fun getTotalStorageUsage(@Param("workspaceId") workspaceId: String): Int

    /**
     * Find unused modules (not accessed in specified period)
     */
    @Query("""
        SELECT wm FROM WorkspaceModule wm 
        WHERE wm.workspaceId = :workspaceId 
        AND wm.enabled = true
        AND (wm.usageMetrics.lastAccessedAt IS NULL 
             OR wm.usageMetrics.lastAccessedAt < :lastUsedBefore)
        ORDER BY wm.usageMetrics.lastAccessedAt ASC NULLS FIRST
    """)
    fun findUnusedModules(
        @Param("workspaceId") workspaceId: String,
        @Param("lastUsedBefore") lastUsedBefore: LocalDateTime
    ): List<WorkspaceModule>

    /**
     * Find popular modules (high usage and satisfaction)
     */
    @Query("""
        SELECT wm FROM WorkspaceModule wm 
        WHERE wm.workspaceId = :workspaceId 
        AND wm.enabled = true
        AND wm.usageMetrics.totalAccesses > :minAccesses
        AND wm.usageMetrics.userSatisfactionScore > :minSatisfaction
        ORDER BY wm.usageMetrics.totalAccesses DESC, wm.usageMetrics.userSatisfactionScore DESC
    """)
    fun findPopularModules(
        @Param("workspaceId") workspaceId: String,
        @Param("minAccesses") minAccesses: Int,
        @Param("minSatisfaction") minSatisfaction: Double
    ): List<WorkspaceModule>

    /**
     * Update storage usage for module
     */
    @Modifying
    @Query("""
        UPDATE WorkspaceModule wm 
        SET wm.storageUsedMb = :storageUsedMb 
        WHERE wm.uid = :workspaceModuleId
    """)
    fun updateStorageUsage(
        @Param("workspaceModuleId") workspaceModuleId: String,
        @Param("storageUsedMb") storageUsedMb: Int
    )

    /**
     * Update module display order
     */
    @Modifying
    @Query("""
        UPDATE WorkspaceModule wm 
        SET wm.displayOrder = :displayOrder 
        WHERE wm.uid = :workspaceModuleId
    """)
    fun updateDisplayOrder(
        @Param("workspaceModuleId") workspaceModuleId: String,
        @Param("displayOrder") displayOrder: Int
    )

    /**
     * Get user activity summary for modules
     */
    @Query("""
        SELECT new map(
            'totalActiveUsers' as key,
            COUNT(DISTINCT JSON_EXTRACT(up.value, '$.userId')) as value
        )
        FROM WorkspaceModule wm 
        JOIN JSON_TABLE(wm.userPreferences, '$[*]' COLUMNS (value JSON PATH '$')) up
        WHERE wm.workspaceId = :workspaceId 
        AND wm.enabled = true
        AND JSON_EXTRACT(up.value, '$.lastAccessedAt') > :activeThreshold
    """)
    fun getUserActivitySummary(
        @Param("workspaceId") workspaceId: String,
        @Param("activeThreshold") activeThreshold: LocalDateTime
    ): Map<String, Any>

    /**
     * Find modules that need attention (errors, updates, expiry)
     */
    @Query("""
        SELECT wm FROM WorkspaceModule wm 
        JOIN wm.masterModule mm
        WHERE wm.workspaceId = :workspaceId 
        AND wm.enabled = true
        AND (wm.usageMetrics.errorCount > 0 
             OR wm.installedVersion != mm.version 
             OR (wm.licenseExpiresAt IS NOT NULL AND wm.licenseExpiresAt < :soonThreshold)
             OR wm.status != 'ACTIVE')
        ORDER BY 
            CASE WHEN wm.usageMetrics.errorCount > 0 THEN 1
                 WHEN wm.status != 'ACTIVE' THEN 2
                 WHEN wm.licenseExpiresAt IS NOT NULL AND wm.licenseExpiresAt < :soonThreshold THEN 3
                 WHEN wm.installedVersion != mm.version THEN 4
                 ELSE 5 END
    """)
    fun findModulesNeedingAttention(
        @Param("workspaceId") workspaceId: String,
        @Param("soonThreshold") soonThreshold: LocalDateTime
    ): List<WorkspaceModule>

    /**
     * Get module category distribution
     */
    @Query("""
        SELECT 
            CASE WHEN wm.categoryOverride IS NOT NULL THEN wm.categoryOverride ELSE mm.category END as category,
            COUNT(*) as totalCount,
            COUNT(CASE WHEN wm.enabled = true THEN 1 END) as enabledCount,
            AVG(wm.usageMetrics.totalAccesses) as avgUsage
        FROM WorkspaceModule wm 
        JOIN wm.masterModule mm
        WHERE wm.workspaceId = :workspaceId
        GROUP BY CASE WHEN wm.categoryOverride IS NOT NULL THEN wm.categoryOverride ELSE mm.category END
        ORDER BY COUNT(*) DESC
    """)
    fun getModuleCategoryDistribution(@Param("workspaceId") workspaceId: String): List<Map<String, Any>>
}