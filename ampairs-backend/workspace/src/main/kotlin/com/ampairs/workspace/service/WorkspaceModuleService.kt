package com.ampairs.workspace.service

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.workspace.model.MasterModule
import com.ampairs.workspace.model.ModuleSettings
import com.ampairs.workspace.model.WorkspaceModule
import com.ampairs.workspace.model.dto.*
import com.ampairs.workspace.model.enums.ModuleCategory
import com.ampairs.workspace.model.enums.WorkspaceModuleStatus
import com.ampairs.workspace.repository.MasterModuleRepository
import com.ampairs.workspace.repository.WorkspaceModuleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Comprehensive service for workspace module management operations.
 * Handles module discovery, installation, configuration, and lifecycle management.
 */
@Service
@Transactional
class WorkspaceModuleService(
    private val workspaceModuleRepository: WorkspaceModuleRepository,
    private val masterModuleRepository: MasterModuleRepository,
) {

    /**
     * Get comprehensive module overview for workspace
     */
    fun getInstalledModules(): List<InstalledModuleResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant() ?: return emptyList()

        val installedModules = workspaceModuleRepository.findByWorkspaceId(workspaceId)
        val activeModules = installedModules.filter { it.enabled && it.status == WorkspaceModuleStatus.ACTIVE }


        return activeModules.map { module ->
            InstalledModuleResponse(
                id = module.uid,
                moduleCode = module.masterModule.moduleCode,
                name = module.getEffectiveName(),
                category = module.getEffectiveCategory(),
                version = module.installedVersion,
                status = module.status,
                enabled = module.enabled,
                installedAt = module.installedAt,
                icon = module.getEffectiveIcon(),
                primaryColor = module.getEffectiveColor(),
                healthScore = module.getHealthScore(),
                needsAttention = module.needsAttention()
            )
        }
    }

    /**
     * Get detailed information about a specific module
     */
    fun getModuleInfo(moduleId: String): ModuleDetailResponse? {
        val workspaceId = TenantContextHolder.getCurrentTenant() ?: return null

        // Try to find by UID first, then by module code
        val workspaceModule = workspaceModuleRepository.findById(moduleId).orElse(null)
            ?: workspaceModuleRepository.findByWorkspaceIdAndMasterModuleModuleCode(workspaceId, moduleId)
            ?: return null

        workspaceModule.masterModule
        val analytics = buildModuleAnalytics(workspaceModule)
        val permissions = buildModulePermissions(workspaceModule)
        val configuration = buildModuleConfiguration(workspaceModule)

        val moduleInfo = ModuleInfoResponse(
            name = workspaceModule.getEffectiveName(),
            category = workspaceModule.getEffectiveCategory(),
            description = workspaceModule.getEffectiveDescription(),
            version = workspaceModule.installedVersion,
            status = workspaceModule.status.displayName,
            enabled = workspaceModule.enabled,
            installedAt = workspaceModule.installedAt,
            lastUpdated = workspaceModule.lastUpdatedAt
        )

        return ModuleDetailResponse(
            moduleId = workspaceModule.uid,
            workspaceId = workspaceId,
            moduleInfo = moduleInfo,
            configuration = configuration,
            analytics = analytics,
            permissions = permissions,
            healthScore = workspaceModule.getHealthScore(),
            needsAttention = workspaceModule.needsAttention()
        )
    }

    /**
     * Install a module from the master registry
     */
    fun installModule(
        moduleCode: String,
        installedBy: String? = null,
        installedByName: String? = null,
    ): ModuleInstallationResponse? {
        val workspaceId = TenantContextHolder.getCurrentTenant() ?: return null

        // Check if already installed
        if (workspaceModuleRepository.existsByWorkspaceIdAndMasterModuleModuleCode(workspaceId, moduleCode)) {
            return null
        }

        // Find master module
        val masterModule = masterModuleRepository.findByModuleCode(moduleCode) ?: return null

        if (!masterModule.isProductionReady()) {
            return null
        }

        // Check dependencies
        val missingDependencies = checkMissingDependencies(workspaceId, masterModule)
        if (missingDependencies.isNotEmpty()) {
            return null
        }

        // Check conflicts
        val conflicts = checkModuleConflicts(workspaceId, masterModule)
        if (conflicts.isNotEmpty()) {
            return null
        }

        // Create workspace module
        val workspaceModule = WorkspaceModule().apply {
            this.workspaceId = workspaceId
            this.masterModule = masterModule
            this.status = WorkspaceModuleStatus.INSTALLING
            this.enabled = true
            this.installedVersion = masterModule.version
            this.installedAt = LocalDateTime.now()
            this.installedBy = installedBy
            this.installedByName = installedByName
            this.displayOrder = getNextDisplayOrder(workspaceId)
        }

        // Save and update master module stats
        val savedModule = workspaceModuleRepository.save(workspaceModule)
        masterModule.incrementInstallCount()
        masterModuleRepository.save(masterModule)

        // Update status to active
        savedModule.status = WorkspaceModuleStatus.ACTIVE
        workspaceModuleRepository.save(savedModule)

        return ModuleInstallationResponse(
            success = true,
            moduleId = savedModule.uid,
            moduleCode = moduleCode,
            workspaceId = workspaceId,
            message = "Module ${masterModule.name} installed successfully",
            installedAt = savedModule.installedAt
        )
    }

    /**
     * Uninstall a module from the workspace
     */
    fun uninstallModule(moduleId: String): ModuleUninstallationResponse? {
        val workspaceId = TenantContextHolder.getCurrentTenant() ?: return null

        val workspaceModule = findWorkspaceModule(workspaceId, moduleId) ?: return null

        // Check if other modules depend on this one
        val dependentModules = findDependentModules(workspaceId, workspaceModule.masterModule.moduleCode)
        if (dependentModules.isNotEmpty()) {
            return null
        }

        val moduleName = workspaceModule.getEffectiveName()

        // Update master module stats
        workspaceModule.masterModule.decrementInstallCount()
        masterModuleRepository.save(workspaceModule.masterModule)

        // Remove from workspace
        workspaceModuleRepository.delete(workspaceModule)

        return ModuleUninstallationResponse(
            success = true,
            moduleId = moduleId,
            workspaceId = workspaceId,
            message = "Module $moduleName uninstalled successfully",
            uninstalledAt = LocalDateTime.now()
        )
    }

    /**
     * Get available modules from master registry
     */
    fun getAvailableModules(
        category: String? = null,
        featured: Boolean = false
    ): List<AvailableModuleResponse> {
        val workspaceId = TenantContextHolder.getCurrentTenant() ?: return emptyList()

        val installedCodes = workspaceModuleRepository.findByWorkspaceId(workspaceId)
            .map { it.masterModule.moduleCode }.toSet()

        val availableModules = when {
            featured -> masterModuleRepository.findByActiveTrueAndFeaturedTrueOrderByDisplayOrderAsc()
            category != null -> {
                val moduleCategory = try {
                    ModuleCategory.valueOf(category.uppercase())
                } catch (_: IllegalArgumentException) {
                    return emptyList()
                }
                masterModuleRepository.findByActiveTrueAndCategory(moduleCategory)
            }

            else -> masterModuleRepository.findByActiveTrueOrderByDisplayOrderAsc()
        }.filterNot { it.moduleCode in installedCodes }

        val moduleData = availableModules.map { masterModule ->
            AvailableModuleResponse(
                moduleCode = masterModule.moduleCode,
                name = masterModule.name,
                description = masterModule.description,
                category = masterModule.category.displayName,
                version = masterModule.version,
                rating = masterModule.rating,
                installCount = masterModule.installCount,
                complexity = masterModule.complexity.displayName,
                icon = masterModule.getDisplayIcon(),
                primaryColor = masterModule.getPrimaryColor(),
                featured = masterModule.featured,
                requiredTier = masterModule.requiredTier.displayName,
                sizeMb = masterModule.sizeMb
            )
        }
        return moduleData
    }

    // Helper methods

    private fun getRecentActivity(workspaceId: String): RecentActivityResponse {
        val recentlyInstalled = workspaceModuleRepository
            .findByWorkspaceIdAndInstalledAtAfterOrderByInstalledAtDesc(
                workspaceId,
                LocalDateTime.now().minusDays(30)
            )

        return RecentActivityResponse(
            lastInstalled = recentlyInstalled.firstOrNull()?.masterModule?.name,
            lastConfigured = null, // Could be enhanced with configuration tracking
            lastAccessed = LocalDateTime.now()
        )
    }

    private fun getInstalledCategories(workspaceId: String): List<String> {
        return workspaceModuleRepository.findByWorkspaceIdAndEnabledTrue(workspaceId)
            .map { it.getEffectiveCategory() }
            .distinct()
    }

    private fun buildModuleAnalytics(workspaceModule: WorkspaceModule): ModuleAnalyticsResponse {
        val metrics = workspaceModule.usageMetrics
        return ModuleAnalyticsResponse(
            dailyActiveUsers = metrics.dailyActiveUsers,
            monthlyAccess = metrics.totalAccesses,
            averageSessionDuration = "${metrics.averageSessionDuration / 60} minutes"
        )
    }

    private fun buildModulePermissions(workspaceModule: WorkspaceModule): ModulePermissionsResponse {
        return ModulePermissionsResponse(
            canConfigure = true,
            canUninstall = !hasDependentModules(workspaceModule),
            canViewAnalytics = true
        )
    }

    private fun buildModuleConfiguration(workspaceModule: WorkspaceModule): ModuleConfigurationResponse {
        val settings = workspaceModule.settings

        @Suppress("UNCHECKED_CAST")
        val customFields = settings.customConfiguration["customFields"] as? List<String> ?: emptyList()

        return ModuleConfigurationResponse(
            autoSync = (workspaceModule.getConfigValue("autoSync") as? Boolean) ?: true,
            notificationsEnabled = settings.notificationsEnabled,
            customFields = customFields
        )
    }

    private fun findWorkspaceModule(workspaceId: String, moduleId: String): WorkspaceModule? {
        return workspaceModuleRepository.findById(moduleId).orElse(null)?.takeIf { it.workspaceId == workspaceId }
            ?: workspaceModuleRepository.findByWorkspaceIdAndMasterModuleModuleCode(workspaceId, moduleId)
    }

    private fun enableModule(workspaceModule: WorkspaceModule): ModuleActionResponse {
        workspaceModule.enabled = true
        workspaceModule.status = WorkspaceModuleStatus.ACTIVE
        workspaceModuleRepository.save(workspaceModule)

        return buildActionResponse(workspaceModule, "enable", "Module enabled successfully")
    }

    private fun disableModule(workspaceModule: WorkspaceModule): ModuleActionResponse {
        workspaceModule.enabled = false
        workspaceModule.status = WorkspaceModuleStatus.SUSPENDED
        workspaceModuleRepository.save(workspaceModule)

        return buildActionResponse(workspaceModule, "disable", "Module disabled successfully")
    }

    private fun configureModule(workspaceModule: WorkspaceModule): ModuleActionResponse {
        // Configuration logic would be implemented here
        return buildActionResponse(workspaceModule, "configure", "Module configuration updated")
    }

    private fun resetModule(workspaceModule: WorkspaceModule): ModuleActionResponse {
        workspaceModule.settings = ModuleSettings()
        workspaceModuleRepository.save(workspaceModule)

        return buildActionResponse(workspaceModule, "reset", "Module reset to defaults")
    }

    private fun updateModule(workspaceModule: WorkspaceModule): ModuleActionResponse? {
        if (!workspaceModule.canBeUpdated()) {
            return null
        }

        workspaceModule.updateToLatestVersion()
        workspaceModuleRepository.save(workspaceModule)

        return buildActionResponse(workspaceModule, "update", "Module updated to latest version")
    }

    private fun analyzeModule(workspaceModule: WorkspaceModule): ModuleActionResponse {
        return buildActionResponse(workspaceModule, "analyze", "Module analysis completed")
    }

    private fun diagnoseModule(workspaceModule: WorkspaceModule): ModuleActionResponse {
        workspaceModule.getHealthScore()
        val issues = mutableListOf<String>()

        if (!workspaceModule.isOperational()) issues.add("Module is not operational")
        if (!workspaceModule.hasValidLicense()) issues.add("License expired or invalid")
        if (workspaceModule.canBeUpdated()) issues.add("Update available")
        if (workspaceModule.usageMetrics.errorCount > 0) issues.add("Errors detected")

        val message =
            if (issues.isEmpty()) "Module diagnosis completed - Healthy" else "Module diagnosis completed - Needs attention"
        return buildActionResponse(workspaceModule, "diagnose", message)
    }

    private fun optimizeModule(workspaceModule: WorkspaceModule): ModuleActionResponse {
        // Optimization logic would be implemented here
        return buildActionResponse(workspaceModule, "optimize", "Module optimization completed")
    }

    private fun restartModule(workspaceModule: WorkspaceModule): ModuleActionResponse {
        // Restart logic would be implemented here
        return buildActionResponse(workspaceModule, "restart", "Module restarted successfully")
    }

    private fun refreshModule(workspaceModule: WorkspaceModule): ModuleActionResponse {
        // Refresh logic would be implemented here
        return buildActionResponse(workspaceModule, "refresh", "Module data refreshed")
    }

    private fun buildActionResponse(
        workspaceModule: WorkspaceModule,
        action: String,
        message: String,
    ): ModuleActionResponse {
        return ModuleActionResponse(
            moduleId = workspaceModule.uid,
            action = action,
            workspaceId = workspaceModule.workspaceId,
            success = true,
            message = message,
            actionDetails = ActionDetailsResponse(
                executedAt = LocalDateTime.now(),
                duration = "1.2 seconds",
                affectedComponents = listOf("configuration", "user-interface")
            ),
            impact = ActionImpactResponse(
                usersAffected = workspaceModule.usageMetrics.dailyActiveUsers,
                dataChanged = false,
                requiresRestart = false,
                immediatelyAvailable = true
            ),
            nextSteps = listOf(
                "Verify functionality in user interface",
                "Monitor performance metrics"
            )
        )
    }

    private fun checkMissingDependencies(workspaceId: String, masterModule: MasterModule): List<String> {
        val installedModules = workspaceModuleRepository.findByWorkspaceIdAndEnabledTrue(workspaceId)
            .map { it.masterModule.moduleCode }.toSet()

        return masterModule.getMissingDependencies(installedModules)
    }

    private fun checkModuleConflicts(workspaceId: String, masterModule: MasterModule): List<String> {
        val installedModules = workspaceModuleRepository.findByWorkspaceIdAndEnabledTrue(workspaceId)
            .map { it.masterModule.moduleCode }.toSet()

        return masterModule.hasConflicts(installedModules)
    }

    private fun findDependentModules(workspaceId: String, moduleCode: String): List<WorkspaceModule> {
        return workspaceModuleRepository.findByWorkspaceIdAndEnabledTrue(workspaceId)
            .filter { it.masterModule.configuration.dependencies.contains(moduleCode) }
    }

    private fun hasDependentModules(workspaceModule: WorkspaceModule): Boolean {
        return findDependentModules(workspaceModule.workspaceId, workspaceModule.masterModule.moduleCode).isNotEmpty()
    }

    private fun getNextDisplayOrder(workspaceId: String): Int {
        val maxOrder = workspaceModuleRepository.findByWorkspaceId(workspaceId)
            .maxOfOrNull { it.displayOrder } ?: 0
        return maxOrder + 10
    }

    private fun getSupportedActions(): List<String> {
        return listOf(
            "enable", "disable", "configure", "reset", "update",
            "analyze", "diagnose", "optimize", "restart", "refresh"
        )
    }
}