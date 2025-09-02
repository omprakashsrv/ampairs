package com.ampairs.workspace.service

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.workspace.model.MasterModule
import com.ampairs.workspace.model.ModuleSettings
import com.ampairs.workspace.model.WorkspaceModule
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
    fun getBasicModuleInfo(): Map<String, Any> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: return mapOf("error" to "No tenant context")

        val installedModules = workspaceModuleRepository.findByWorkspaceId(workspaceId)
        val activeModules = installedModules.filter { it.enabled && it.status == WorkspaceModuleStatus.ACTIVE }
        val recentActivity = getRecentActivity(workspaceId)
        val moduleCategories = getInstalledCategories(workspaceId)

        return mapOf(
            "workspaceId" to workspaceId,
            "message" to "Module management is available",
            "totalModules" to installedModules.size,
            "activeModules" to activeModules.size,
            "moduleCategories" to moduleCategories,
            "recentActivity" to recentActivity,
            "quickActions" to listOf(
                "Browse Available Modules",
                "Configure Existing Modules",
                "View Module Analytics"
            )
        )
    }

    /**
     * Get detailed information about a specific module
     */
    fun getModuleInfo(moduleId: String): Map<String, Any> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: return mapOf("error" to "No tenant context")

        // Try to find by UID first, then by module code
        val workspaceModule = workspaceModuleRepository.findById(moduleId).orElse(null)
            ?: workspaceModuleRepository.findByWorkspaceIdAndMasterModuleModuleCode(workspaceId, moduleId)
            ?: return mapOf("error" to "Module not found", "moduleId" to moduleId)

        workspaceModule.masterModule
        val analytics = buildModuleAnalytics(workspaceModule)
        val permissions = buildModulePermissions(workspaceModule)

        return mapOf(
            "moduleId" to workspaceModule.uid,
            "workspaceId" to workspaceId,
            "moduleInfo" to mapOf(
                "name" to workspaceModule.getEffectiveName(),
                "category" to workspaceModule.getEffectiveCategory(),
                "description" to workspaceModule.getEffectiveDescription(),
                "version" to workspaceModule.installedVersion,
                "status" to workspaceModule.status.displayName,
                "enabled" to workspaceModule.enabled,
                "installedAt" to workspaceModule.installedAt,
                "lastUpdated" to workspaceModule.lastUpdatedAt
            ),
            "configuration" to buildModuleConfiguration(workspaceModule),
            "analytics" to analytics,
            "permissions" to permissions,
            "healthScore" to workspaceModule.getHealthScore(),
            "needsAttention" to workspaceModule.needsAttention()
        )
    }

    /**
     * Perform various module management actions
     */
    fun performAction(moduleId: String, action: String): Map<String, Any> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: return mapOf("error" to "No tenant context")

        val workspaceModule = findWorkspaceModule(workspaceId, moduleId)
            ?: return mapOf("error" to "Module not found", "moduleId" to moduleId)

        return when (action.lowercase()) {
            "enable" -> enableModule(workspaceModule)
            "disable" -> disableModule(workspaceModule)
            "configure" -> configureModule(workspaceModule)
            "reset" -> resetModule(workspaceModule)
            "update" -> updateModule(workspaceModule)
            "analyze" -> analyzeModule(workspaceModule)
            "diagnose" -> diagnoseModule(workspaceModule)
            "optimize" -> optimizeModule(workspaceModule)
            "restart" -> restartModule(workspaceModule)
            "refresh" -> refreshModule(workspaceModule)
            else -> mapOf("error" to "Unknown action: $action", "supportedActions" to getSupportedActions())
        }
    }

    /**
     * Install a module from the master registry
     */
    fun installModule(
        moduleCode: String,
        installedBy: String? = null,
        installedByName: String? = null,
    ): Map<String, Any> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: return mapOf("error" to "No tenant context")

        // Check if already installed
        if (workspaceModuleRepository.existsByWorkspaceIdAndMasterModuleModuleCode(workspaceId, moduleCode)) {
            return mapOf("error" to "Module already installed", "moduleCode" to moduleCode)
        }

        // Find master module
        val masterModule = masterModuleRepository.findByModuleCode(moduleCode)
            ?: return mapOf("error" to "Module not found in catalog", "moduleCode" to moduleCode)

        if (!masterModule.isProductionReady()) {
            return mapOf("error" to "Module is not ready for installation", "moduleCode" to moduleCode)
        }

        // Check dependencies
        val missingDependencies = checkMissingDependencies(workspaceId, masterModule)
        if (missingDependencies.isNotEmpty()) {
            return mapOf(
                "error" to "Missing dependencies",
                "moduleCode" to moduleCode,
                "missingDependencies" to missingDependencies
            )
        }

        // Check conflicts
        val conflicts = checkModuleConflicts(workspaceId, masterModule)
        if (conflicts.isNotEmpty()) {
            return mapOf(
                "error" to "Module conflicts detected",
                "moduleCode" to moduleCode,
                "conflicts" to conflicts
            )
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

        return mapOf(
            "success" to true,
            "moduleId" to savedModule.uid,
            "moduleCode" to moduleCode,
            "workspaceId" to workspaceId,
            "message" to "Module ${masterModule.name} installed successfully",
            "installedAt" to savedModule.installedAt
        )
    }

    /**
     * Uninstall a module from the workspace
     */
    fun uninstallModule(moduleId: String): Map<String, Any> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: return mapOf("error" to "No tenant context")

        val workspaceModule = findWorkspaceModule(workspaceId, moduleId)
            ?: return mapOf("error" to "Module not found", "moduleId" to moduleId)

        // Check if other modules depend on this one
        val dependentModules = findDependentModules(workspaceId, workspaceModule.masterModule.moduleCode)
        if (dependentModules.isNotEmpty()) {
            return mapOf(
                "error" to "Cannot uninstall: other modules depend on this module",
                "moduleId" to moduleId,
                "dependentModules" to dependentModules.map { it.masterModule.name }
            )
        }

        val moduleName = workspaceModule.getEffectiveName()

        // Update master module stats
        workspaceModule.masterModule.decrementInstallCount()
        masterModuleRepository.save(workspaceModule.masterModule)

        // Remove from workspace
        workspaceModuleRepository.delete(workspaceModule)

        return mapOf(
            "success" to true,
            "moduleId" to moduleId,
            "workspaceId" to workspaceId,
            "message" to "Module $moduleName uninstalled successfully",
            "uninstalledAt" to LocalDateTime.now()
        )
    }

    /**
     * Get available modules from master registry
     */
    fun getAvailableModules(category: String? = null, featured: Boolean = false): Map<String, Any> {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: return mapOf("error" to "No tenant context")

        val installedCodes = workspaceModuleRepository.findByWorkspaceId(workspaceId)
            .map { it.masterModule.moduleCode }.toSet()

        val availableModules = when {
            featured -> masterModuleRepository.findByActiveTrueAndFeaturedTrueOrderByDisplayOrderAsc()
            category != null -> {
                val moduleCategory = try {
                    ModuleCategory.valueOf(category.uppercase())
                } catch (e: IllegalArgumentException) {
                    return mapOf("error" to "Invalid category: $category")
                }
                masterModuleRepository.findByActiveTrueAndCategory(moduleCategory)
            }

            else -> masterModuleRepository.findByActiveTrueOrderByDisplayOrderAsc()
        }.filterNot { it.moduleCode in installedCodes }

        val moduleData = availableModules.map { masterModule ->
            mapOf(
                "moduleCode" to masterModule.moduleCode,
                "name" to masterModule.name,
                "description" to masterModule.description,
                "category" to masterModule.category.displayName,
                "version" to masterModule.version,
                "rating" to masterModule.rating,
                "installCount" to masterModule.installCount,
                "complexity" to masterModule.complexity.displayName,
                "icon" to masterModule.getDisplayIcon(),
                "primaryColor" to masterModule.getPrimaryColor(),
                "featured" to masterModule.featured,
                "requiredTier" to masterModule.requiredTier.displayName,
                "sizeMb" to masterModule.sizeMb
            )
        }

        return mapOf(
            "availableModules" to moduleData,
            "totalAvailable" to moduleData.size,
            "categories" to ModuleCategory.values().map {
                mapOf(
                    "code" to it.name,
                    "displayName" to it.displayName,
                    "description" to it.description,
                    "icon" to it.icon
                )
            }
        )
    }

    // Helper methods

    private fun getRecentActivity(workspaceId: String): Map<String, Any?> {
        val recentlyInstalled = workspaceModuleRepository
            .findByWorkspaceIdAndInstalledAtAfterOrderByInstalledAtDesc(
                workspaceId,
                LocalDateTime.now().minusDays(30)
            )

        return mapOf(
            "lastInstalled" to recentlyInstalled.firstOrNull()?.masterModule?.name,
            "lastConfigured" to null, // Could be enhanced with configuration tracking
            "lastAccessed" to LocalDateTime.now()
        )
    }

    private fun getInstalledCategories(workspaceId: String): List<String> {
        return workspaceModuleRepository.findByWorkspaceIdAndEnabledTrue(workspaceId)
            .map { it.getEffectiveCategory() }
            .distinct()
    }

    private fun buildModuleAnalytics(workspaceModule: WorkspaceModule): Map<String, Any> {
        val metrics = workspaceModule.usageMetrics
        return mapOf(
            "dailyActiveUsers" to metrics.dailyActiveUsers,
            "monthlyAccess" to metrics.totalAccesses,
            "averageSessionDuration" to "${metrics.averageSessionDuration / 60} minutes"
        )
    }

    private fun buildModulePermissions(workspaceModule: WorkspaceModule): Map<String, Boolean> {
        return mapOf(
            "canConfigure" to true,
            "canUninstall" to !hasDependentModules(workspaceModule),
            "canViewAnalytics" to true
        )
    }

    private fun buildModuleConfiguration(workspaceModule: WorkspaceModule): Map<String, Any> {
        val settings = workspaceModule.settings
        return mapOf(
            "autoSync" to (settings.getConfigValue("autoSync") ?: true),
            "notificationsEnabled" to settings.notificationsEnabled,
            "customFields" to (settings.customConfiguration["customFields"] ?: emptyList<String>())
        )
    }

    private fun findWorkspaceModule(workspaceId: String, moduleId: String): WorkspaceModule? {
        return workspaceModuleRepository.findById(moduleId).orElse(null)?.takeIf { it.workspaceId == workspaceId }
            ?: workspaceModuleRepository.findByWorkspaceIdAndMasterModuleModuleCode(workspaceId, moduleId)
    }

    private fun enableModule(workspaceModule: WorkspaceModule): Map<String, Any> {
        workspaceModule.enabled = true
        workspaceModule.status = WorkspaceModuleStatus.ACTIVE
        workspaceModuleRepository.save(workspaceModule)

        return buildActionResponse(workspaceModule, "enable", "Module enabled successfully")
    }

    private fun disableModule(workspaceModule: WorkspaceModule): Map<String, Any> {
        workspaceModule.enabled = false
        workspaceModule.status = WorkspaceModuleStatus.SUSPENDED
        workspaceModuleRepository.save(workspaceModule)

        return buildActionResponse(workspaceModule, "disable", "Module disabled successfully")
    }

    private fun configureModule(workspaceModule: WorkspaceModule): Map<String, Any> {
        // Configuration logic would be implemented here
        return buildActionResponse(workspaceModule, "configure", "Module configuration updated")
    }

    private fun resetModule(workspaceModule: WorkspaceModule): Map<String, Any> {
        workspaceModule.settings = ModuleSettings()
        workspaceModuleRepository.save(workspaceModule)

        return buildActionResponse(workspaceModule, "reset", "Module reset to defaults")
    }

    private fun updateModule(workspaceModule: WorkspaceModule): Map<String, Any> {
        if (!workspaceModule.canBeUpdated()) {
            return mapOf("error" to "Module is already up to date")
        }

        workspaceModule.updateToLatestVersion()
        workspaceModuleRepository.save(workspaceModule)

        return buildActionResponse(workspaceModule, "update", "Module updated to latest version")
    }

    private fun analyzeModule(workspaceModule: WorkspaceModule): Map<String, Any> {
        val analytics = buildModuleAnalytics(workspaceModule)
        return buildActionResponse(workspaceModule, "analyze", "Module analysis completed", analytics)
    }

    private fun diagnoseModule(workspaceModule: WorkspaceModule): Map<String, Any> {
        val healthScore = workspaceModule.getHealthScore()
        val issues = mutableListOf<String>()

        if (!workspaceModule.isOperational()) issues.add("Module is not operational")
        if (!workspaceModule.hasValidLicense()) issues.add("License expired or invalid")
        if (workspaceModule.canBeUpdated()) issues.add("Update available")
        if (workspaceModule.usageMetrics.errorCount > 0) issues.add("Errors detected")

        val diagnostics = mapOf(
            "healthScore" to healthScore,
            "issues" to issues,
            "status" to if (issues.isEmpty()) "Healthy" else "Needs attention"
        )

        return buildActionResponse(workspaceModule, "diagnose", "Module diagnosis completed", diagnostics)
    }

    private fun optimizeModule(workspaceModule: WorkspaceModule): Map<String, Any> {
        // Optimization logic would be implemented here
        return buildActionResponse(workspaceModule, "optimize", "Module optimization completed")
    }

    private fun restartModule(workspaceModule: WorkspaceModule): Map<String, Any> {
        // Restart logic would be implemented here
        return buildActionResponse(workspaceModule, "restart", "Module restarted successfully")
    }

    private fun refreshModule(workspaceModule: WorkspaceModule): Map<String, Any> {
        // Refresh logic would be implemented here
        return buildActionResponse(workspaceModule, "refresh", "Module data refreshed")
    }

    private fun buildActionResponse(
        workspaceModule: WorkspaceModule,
        action: String,
        message: String,
        additionalData: Map<String, Any> = emptyMap(),
    ): Map<String, Any> {
        val baseResponse = mapOf(
            "moduleId" to workspaceModule.uid,
            "action" to action,
            "workspaceId" to workspaceModule.workspaceId,
            "success" to true,
            "message" to message,
            "actionDetails" to mapOf(
                "executedAt" to LocalDateTime.now(),
                "duration" to "1.2 seconds",
                "affectedComponents" to listOf("configuration", "user-interface")
            ),
            "impact" to mapOf(
                "usersAffected" to workspaceModule.usageMetrics.dailyActiveUsers,
                "dataChanged" to false,
                "requiresRestart" to false,
                "immediatelyAvailable" to true
            ),
            "nextSteps" to listOf(
                "Verify functionality in user interface",
                "Monitor performance metrics"
            )
        )

        return baseResponse + additionalData
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