package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.enums.WorkspaceModuleStatus
import com.ampairs.workspace.model.ModuleRouteInfo
import java.time.LocalDateTime

/**
 * Installed module information for overview
 */
data class InstalledModuleResponse(
    var id: String = "",
    var moduleCode: String = "",
    var name: String = "",
    var category: String = "",
    var version: String = "",
    var status: WorkspaceModuleStatus = WorkspaceModuleStatus.ACTIVE,
    var enabled: Boolean = true,
    var installedAt: LocalDateTime = LocalDateTime.now(),
    var icon: String = "",
    var primaryColor: String = "",
    var healthScore: Double = 1.0,
    var needsAttention: Boolean = false,
    var routeInfo: ModuleRouteInfo = ModuleRouteInfo(),
    var navigationIndex: Int = 0
)

/**
 * Recent activity information
 */
data class RecentActivityResponse(
    var lastInstalled: String? = null,
    var lastConfigured: String? = null,
    var lastAccessed: LocalDateTime? = null,
)

/**
 * Detailed response for specific module (GET /workspace/v1/modules/{moduleId})
 */
data class ModuleDetailResponse(
    var moduleId: String = "",
    var workspaceId: String = "",
    var moduleInfo: ModuleInfoResponse = ModuleInfoResponse(),
    var configuration: ModuleConfigurationResponse = ModuleConfigurationResponse(),
    var analytics: ModuleAnalyticsResponse = ModuleAnalyticsResponse(),
    var permissions: ModulePermissionsResponse = ModulePermissionsResponse(),
    var healthScore: Double = 1.0,
    var needsAttention: Boolean = false,
)

/**
 * Module basic information
 */
data class ModuleInfoResponse(
    var name: String = "",
    var category: String = "",
    var description: String = "",
    var version: String = "",
    var status: String = "",
    var enabled: Boolean = true,
    var installedAt: LocalDateTime = LocalDateTime.now(),
    var lastUpdated: LocalDateTime? = null,
)

/**
 * Module configuration settings
 */
data class ModuleConfigurationResponse(
    var autoSync: Boolean = true,
    var notificationsEnabled: Boolean = true,
    var customFields: List<String> = emptyList(),
)

/**
 * Module analytics and metrics
 */
data class ModuleAnalyticsResponse(
    var dailyActiveUsers: Int = 0,
    var monthlyAccess: Int = 0,
    var averageSessionDuration: String = "",
)

/**
 * Module permissions for current user
 */
data class ModulePermissionsResponse(
    var canConfigure: Boolean = false,
    var canUninstall: Boolean = false,
    var canViewAnalytics: Boolean = false,
)

/**
 * Response for module action operations (POST /workspace/v1/modules/{moduleId}/action)
 */
data class ModuleActionResponse(
    var moduleId: String = "",
    var action: String = "",
    var workspaceId: String = "",
    var success: Boolean = true,
    var message: String = "",
    var actionDetails: ActionDetailsResponse = ActionDetailsResponse(),
    var impact: ActionImpactResponse = ActionImpactResponse(),
    var nextSteps: List<String> = emptyList(),
)

/**
 * Details about the executed action
 */
data class ActionDetailsResponse(
    var executedAt: LocalDateTime = LocalDateTime.now(),
    var duration: String = "",
    var affectedComponents: List<String> = emptyList(),
)

/**
 * Impact information of the action
 */
data class ActionImpactResponse(
    var usersAffected: Int = 0,
    var dataChanged: Boolean = false,
    var requiresRestart: Boolean = false,
    var immediatelyAvailable: Boolean = true,
)

/**
 * Response for available modules (GET /workspace/v1/modules/available)
 */
data class AvailableModulesCatalogResponse(
    var availableModules: List<AvailableModuleResponse> = emptyList(),
    var totalAvailable: Int = 0,
    var categories: List<ModuleCategoryResponse> = emptyList(),
)

/**
 * Individual available module information
 */
data class AvailableModuleResponse(
    var moduleCode: String = "",
    var name: String = "",
    var description: String? = null,
    var category: String = "",
    var version: String = "",
    var rating: Double = 0.0,
    var installCount: Int = 0,
    var complexity: String = "",
    var icon: String = "",
    var primaryColor: String = "",
    var featured: Boolean = false,
    var requiredTier: String = "",
    var sizeMb: Int = 0,
    var routeInfo: ModuleRouteInfo = ModuleRouteInfo(),
    var navigationIndex: Int = 0
)

/**
 * Module category information
 */
data class ModuleCategoryResponse(
    var code: String = "",
    var displayName: String = "",
    var description: String = "",
    var icon: String = "",
)

/**
 * Response for module installation (POST /workspace/v1/modules/install/{moduleCode})
 */
data class ModuleInstallationResponse(
    var success: Boolean = true,
    var moduleId: String = "",
    var moduleCode: String = "",
    var workspaceId: String = "",
    var message: String = "",
    var installedAt: LocalDateTime = LocalDateTime.now(),
)

/**
 * Response for module uninstallation (DELETE /workspace/v1/modules/{moduleId})
 */
data class ModuleUninstallationResponse(
    var success: Boolean = true,
    var moduleId: String = "",
    var workspaceId: String = "",
    var message: String = "",
    var uninstalledAt: LocalDateTime = LocalDateTime.now(),
)

/**
 * Unified module catalog response that includes both installed and available modules
 * with their respective action states (GET /workspace/v1/modules/catalog)
 */
data class ModuleCatalogResponse(
    var installedModules: List<ModuleWithActionsResponse> = emptyList(),
    var availableModules: List<ModuleWithActionsResponse> = emptyList(),
    var categories: List<ModuleCategoryResponse> = emptyList(),
    var statistics: ModuleCatalogStatistics = ModuleCatalogStatistics(),
)

/**
 * Module information with available actions
 */
data class ModuleWithActionsResponse(
    var moduleCode: String = "",
    var name: String = "",
    var description: String? = null,
    var category: String = "",
    var version: String = "",
    var icon: String = "",
    var primaryColor: String = "",
    var featured: Boolean = false,
    var rating: Double = 0.0,
    var installCount: Int = 0,
    var complexity: String = "",
    var sizeMb: Int = 0,
    var requiredTier: String = "",
    var installationStatus: ModuleInstallationStatus = ModuleInstallationStatus(),
    var availableActions: List<ModuleActionOption> = emptyList(),
    var permissions: ModuleActionPermissions = ModuleActionPermissions(),
    var routeInfo: ModuleRouteInfo = ModuleRouteInfo(),
    var navigationIndex: Int = 0
)

/**
 * Installation status information for a module
 */
data class ModuleInstallationStatus(
    var isInstalled: Boolean = false,
    var workspaceModuleId: String? = null,
    var status: WorkspaceModuleStatus? = null,
    var enabled: Boolean? = null,
    var installedAt: LocalDateTime? = null,
    var healthScore: Double? = null,
    var needsAttention: Boolean = false,
)

/**
 * Available action options for a module
 */
data class ModuleActionOption(
    var actionType: ModuleActionType = ModuleActionType.INSTALL,
    var label: String = "",
    var description: String = "",
    var enabled: Boolean = true,
    var requiresConfirmation: Boolean = false,
    var confirmationMessage: String? = null,
)

/**
 * User permissions for module actions
 */
data class ModuleActionPermissions(
    var canInstall: Boolean = false,
    var canUninstall: Boolean = false,
    var canConfigure: Boolean = false,
    var canEnable: Boolean = false,
    var canDisable: Boolean = false,
)

/**
 * Statistics for the module catalog
 */
data class ModuleCatalogStatistics(
    var totalInstalled: Int = 0,
    var totalAvailable: Int = 0,
    var enabledModules: Int = 0,
    var disabledModules: Int = 0,
    var modulesNeedingAttention: Int = 0,
)

/**
 * Enum for module action types
 */
enum class ModuleActionType {
    INSTALL,
    UNINSTALL,
    ENABLE,
    DISABLE,
    CONFIGURE,
    UPDATE
}