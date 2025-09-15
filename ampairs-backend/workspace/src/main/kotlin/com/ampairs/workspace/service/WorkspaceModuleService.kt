package com.ampairs.workspace.service

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.workspace.model.MasterModule
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
    ): ModuleInstallationResponse {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: return ModuleInstallationResponse(
                success = false,
                moduleCode = moduleCode,
                workspaceId = "",
                message = "No workspace context available"
            )

        // Check if already installed
        if (workspaceModuleRepository.existsByWorkspaceIdAndMasterModuleModuleCode(workspaceId, moduleCode)) {
            val existingModule =
                workspaceModuleRepository.findByWorkspaceIdAndMasterModuleModuleCode(workspaceId, moduleCode)
            return ModuleInstallationResponse(
                success = true,
                moduleId = existingModule?.uid ?: "",
                moduleCode = moduleCode,
                workspaceId = workspaceId,
                message = "Module $moduleCode is already installed in this workspace",
                installedAt = existingModule?.installedAt ?: LocalDateTime.now()
            )
        }

        // Find master module
        val masterModule = masterModuleRepository.findByModuleCode(moduleCode)
            ?: return ModuleInstallationResponse(
                success = false,
                moduleCode = moduleCode,
                workspaceId = workspaceId,
                message = "Module $moduleCode not found in catalog"
            )

        if (!masterModule.isProductionReady()) {
            return ModuleInstallationResponse(
                success = false,
                moduleCode = moduleCode,
                workspaceId = workspaceId,
                message = "Module ${masterModule.name} is not ready for production use"
            )
        }

        // Check dependencies
        val missingDependencies = checkMissingDependencies(workspaceId, masterModule)
        if (missingDependencies.isNotEmpty()) {
            return ModuleInstallationResponse(
                success = false,
                moduleCode = moduleCode,
                workspaceId = workspaceId,
                message = "Missing required dependencies: ${missingDependencies.joinToString(", ")}"
            )
        }

        // Check conflicts
        val conflicts = checkModuleConflicts(workspaceId, masterModule)
        if (conflicts.isNotEmpty()) {
            return ModuleInstallationResponse(
                success = false,
                moduleCode = moduleCode,
                workspaceId = workspaceId,
                message = "Module conflicts with installed modules: ${conflicts.joinToString(", ")}"
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
    fun uninstallModule(moduleId: String): ModuleUninstallationResponse {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: return ModuleUninstallationResponse(
                success = false,
                moduleId = moduleId,
                workspaceId = "",
                message = "No workspace context available"
            )

        val workspaceModule = findWorkspaceModule(workspaceId, moduleId)
            ?: return ModuleUninstallationResponse(
                success = false,
                moduleId = moduleId,
                workspaceId = workspaceId,
                message = "Module not found in workspace"
            )

        // Check if other modules depend on this one
        val dependentModules = findDependentModules(workspaceId, workspaceModule.masterModule.moduleCode)
        if (dependentModules.isNotEmpty()) {
            val dependentNames = dependentModules.map { it.getEffectiveName() }
            return ModuleUninstallationResponse(
                success = false,
                moduleId = moduleId,
                workspaceId = workspaceId,
                message = "Cannot uninstall ${workspaceModule.getEffectiveName()}. Required by: ${
                    dependentNames.joinToString(
                        ", "
                    )
                }"
            )
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

    /**
     * Get unified module catalog with install/uninstall actions
     */
    fun getModuleCatalog(
        category: String? = null,
        includeDisabled: Boolean = false
    ): ModuleCatalogResponse {
        val workspaceId = TenantContextHolder.getCurrentTenant()
            ?: return ModuleCatalogResponse()

        // Get installed modules
        val installedModules = getInstalledModulesWithActions(workspaceId, category, includeDisabled)

        // Get available modules
        val availableModules = getAvailableModulesWithActions(workspaceId, category)

        // Get categories
        val categories = getAllCategories()

        // Calculate statistics
        val statistics = calculateModuleCatalogStatistics(workspaceId)

        return ModuleCatalogResponse(
            installedModules = installedModules,
            availableModules = availableModules,
            categories = categories,
            statistics = statistics
        )
    }

    private fun getInstalledModulesWithActions(
        workspaceId: String,
        category: String?,
        includeDisabled: Boolean
    ): List<ModuleWithActionsResponse> {
        val installedModules = workspaceModuleRepository.findByWorkspaceId(workspaceId)
            .filter { includeDisabled || (it.enabled && it.status == WorkspaceModuleStatus.ACTIVE) }
            .filter { category == null || it.getEffectiveCategory() == category }

        return installedModules.map { workspaceModule ->
            val masterModule = workspaceModule.masterModule

            ModuleWithActionsResponse(
                moduleCode = masterModule.moduleCode,
                name = workspaceModule.getEffectiveName(),
                description = workspaceModule.getEffectiveDescription(),
                category = workspaceModule.getEffectiveCategory(),
                version = workspaceModule.installedVersion,
                icon = workspaceModule.getEffectiveIcon(),
                primaryColor = workspaceModule.getEffectiveColor(),
                featured = masterModule.featured,
                rating = masterModule.rating,
                installCount = masterModule.installCount,
                complexity = masterModule.complexity.displayName,
                sizeMb = masterModule.sizeMb,
                requiredTier = masterModule.requiredTier.displayName,
                installationStatus = ModuleInstallationStatus(
                    isInstalled = true,
                    workspaceModuleId = workspaceModule.uid,
                    status = workspaceModule.status,
                    enabled = workspaceModule.enabled,
                    installedAt = workspaceModule.installedAt,
                    healthScore = workspaceModule.getHealthScore(),
                    needsAttention = workspaceModule.needsAttention()
                ),
                availableActions = buildAvailableActions(workspaceModule),
                permissions = buildActionPermissions(workspaceModule)
            )
        }
    }

    private fun getAvailableModulesWithActions(
        workspaceId: String,
        category: String?
    ): List<ModuleWithActionsResponse> {
        val installedModulesMap = workspaceModuleRepository.findByWorkspaceId(workspaceId)
            .associateBy { it.masterModule.moduleCode }

        val availableModules = when {
            category != null -> {
                val moduleCategory = try {
                    ModuleCategory.valueOf(category.uppercase())
                } catch (_: IllegalArgumentException) {
                    return emptyList()
                }
                masterModuleRepository.findByActiveTrueAndCategory(moduleCategory)
            }

            else -> masterModuleRepository.findByActiveTrueOrderByDisplayOrderAsc()
        }

        val filteredModules = availableModules.filterNot { masterModule ->
            // Check if this master module is already installed in workspace
            // First check by module code
            val isInstalledByCode = installedModulesMap.containsKey(masterModule.moduleCode)

            // Then check if any installed module references this master module ID
            val isInstalledByMasterModuleId = installedModulesMap.values.any {
                it.masterModule.id == masterModule.id
            }

            isInstalledByCode || isInstalledByMasterModuleId
        }

        return filteredModules.map { masterModule ->
            ModuleWithActionsResponse(
                moduleCode = masterModule.moduleCode,
                name = masterModule.name,
                description = masterModule.description,
                category = masterModule.category.displayName,
                version = masterModule.version,
                icon = masterModule.getDisplayIcon(),
                primaryColor = masterModule.getPrimaryColor(),
                featured = masterModule.featured,
                rating = masterModule.rating,
                installCount = masterModule.installCount,
                complexity = masterModule.complexity.displayName,
                sizeMb = masterModule.sizeMb,
                requiredTier = masterModule.requiredTier.displayName,
                installationStatus = ModuleInstallationStatus(
                    isInstalled = false
                ),
                availableActions = listOf(
                    ModuleActionOption(
                        actionType = ModuleActionType.INSTALL,
                        label = "Install",
                        description = "Add to workspace",
                        enabled = true,
                        requiresConfirmation = false
                    )
                ),
                permissions = ModuleActionPermissions(
                    canInstall = true,
                    canUninstall = false,
                    canConfigure = false,
                    canEnable = false,
                    canDisable = false
                )
            )
        }
    }

    private fun buildAvailableActions(workspaceModule: WorkspaceModule): List<ModuleActionOption> {
        val actions = mutableListOf<ModuleActionOption>()

        // Uninstall action
        if (!hasDependentModules(workspaceModule)) {
            actions.add(
                ModuleActionOption(
                    actionType = ModuleActionType.UNINSTALL,
                    label = "Uninstall",
                    description = "Remove module from workspace",
                    enabled = true,
                    requiresConfirmation = true,
                    confirmationMessage = "This will remove all ${workspaceModule.getEffectiveName()} data. Continue?"
                )
            )
        }

        // Configure action (secondary action)
        actions.add(
            ModuleActionOption(
                actionType = ModuleActionType.CONFIGURE,
                label = "Configure",
                description = "Modify module settings",
                enabled = true,
                requiresConfirmation = false
            )
        )

        // Enable/Disable actions
        if (workspaceModule.enabled) {
            actions.add(
                ModuleActionOption(
                    actionType = ModuleActionType.DISABLE,
                    label = "Disable",
                    description = "Temporarily disable module",
                    enabled = true,
                    requiresConfirmation = false
                )
            )
        } else {
            actions.add(
                ModuleActionOption(
                    actionType = ModuleActionType.ENABLE,
                    label = "Enable",
                    description = "Activate module",
                    enabled = true,
                    requiresConfirmation = false
                )
            )
        }

        // Update action
        if (workspaceModule.canBeUpdated()) {
            actions.add(
                ModuleActionOption(
                    actionType = ModuleActionType.UPDATE,
                    label = "Update",
                    description = "Update to latest version",
                    enabled = true,
                    requiresConfirmation = false
                )
            )
        }

        return actions
    }

    private fun buildActionPermissions(workspaceModule: WorkspaceModule): ModuleActionPermissions {
        return ModuleActionPermissions(
            canInstall = false,
            canUninstall = !hasDependentModules(workspaceModule),
            canConfigure = true,
            canEnable = !workspaceModule.enabled,
            canDisable = workspaceModule.enabled
        )
    }

    private fun getAllCategories(): List<ModuleCategoryResponse> {
        return ModuleCategory.values().map { category ->
            ModuleCategoryResponse(
                code = category.name,
                displayName = category.displayName,
                description = category.description,
                icon = category.icon
            )
        }
    }

    private fun calculateModuleCatalogStatistics(workspaceId: String): ModuleCatalogStatistics {
        val installedModules = workspaceModuleRepository.findByWorkspaceId(workspaceId)
        val availableModules = masterModuleRepository.findByActiveTrueOrderByDisplayOrderAsc()

        val enabledModules = installedModules.count { it.enabled && it.status == WorkspaceModuleStatus.ACTIVE }
        val disabledModules = installedModules.count { !it.enabled || it.status != WorkspaceModuleStatus.ACTIVE }
        val modulesNeedingAttention = installedModules.count { it.needsAttention() }

        val installedCodes = installedModules.map { it.masterModule.moduleCode }.toSet()
        val totalAvailable = availableModules.count { it.moduleCode !in installedCodes }

        return ModuleCatalogStatistics(
            totalInstalled = installedModules.size,
            totalAvailable = totalAvailable,
            enabledModules = enabledModules,
            disabledModules = disabledModules,
            modulesNeedingAttention = modulesNeedingAttention
        )
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

}