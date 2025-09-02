package com.ampairs.workspace.model.dto

import java.time.LocalDateTime

/**
 * Response for workspace module overview (GET /workspace/v1/modules)
 */
data class WorkspaceModuleOverviewResponse(
    var workspaceId: String = "",
    var message: String = "",
    var totalModules: Int = 0,
    var activeModules: Int = 0,
    var moduleCategories: List<String> = emptyList(),
    var recentActivity: RecentActivityResponse = RecentActivityResponse(),
    var quickActions: List<String> = emptyList(),
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