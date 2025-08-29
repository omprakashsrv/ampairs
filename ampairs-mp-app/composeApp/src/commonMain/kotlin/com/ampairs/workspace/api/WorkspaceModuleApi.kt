package com.ampairs.workspace.api

import com.ampairs.workspace.api.model.WorkspaceModuleApiModel
import kotlinx.coroutines.flow.Flow

/**
 * Workspace Module Management API Interface
 *
 * Provides comprehensive module management functionality including:
 * - Module discovery and installation
 * - Configuration and customization
 * - Analytics and monitoring
 * - Bulk operations
 */
interface WorkspaceModuleApi {

    /**
     * Get workspace module overview and basic information
     */
    suspend fun getModuleOverview(): Result<WorkspaceModuleApiModel.ModuleOverviewResponse>

    /**
     * Get detailed information about a specific module
     */
    suspend fun getModule(moduleId: String): Result<WorkspaceModuleApiModel.ModuleDetailResponse>

    /**
     * Search and list installed modules with filtering and pagination
     */
    suspend fun searchInstalledModules(
        query: String? = null,
        category: String? = null,
        status: String? = null,
        enabled: Boolean? = null,
        featured: Boolean? = null,
        sortBy: String? = null,
        sortDirection: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): Result<WorkspaceModuleApiModel.ModuleSearchResponse>

    /**
     * Get available modules that can be installed
     */
    suspend fun getAvailableModules(
        businessType: String? = null,
    ): Result<WorkspaceModuleApiModel.AvailableModulesResponse>

    /**
     * Search master modules (available for installation)
     */
    suspend fun searchMasterModules(
        query: String? = null,
        category: String? = null,
        featured: Boolean? = null,
        sortBy: String? = null,
        sortDirection: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): Result<WorkspaceModuleApiModel.MasterModuleSearchResponse>

    /**
     * Get master module details
     */
    suspend fun getMasterModule(moduleId: String): Result<WorkspaceModuleApiModel.MasterModule>

    /**
     * Perform an action on a specific module
     */
    suspend fun performModuleAction(
        moduleId: String,
        action: String,
        parameters: Map<String, Any>? = null,
    ): Result<WorkspaceModuleApiModel.ModuleActionResponse>

    /**
     * Install a new module
     */
    suspend fun installModule(
        installRequest: WorkspaceModuleApiModel.ModuleInstallationRequest,
    ): Result<WorkspaceModuleApiModel.ModuleOperationResponse>

    /**
     * Uninstall a module
     */
    suspend fun uninstallModule(
        moduleId: String,
        preserveData: Boolean = false,
    ): Result<WorkspaceModuleApiModel.ModuleOperationResponse>

    /**
     * Update module configuration
     */
    suspend fun updateModuleConfiguration(
        moduleId: String,
        configRequest: WorkspaceModuleApiModel.ModuleConfigurationRequest,
    ): Result<WorkspaceModuleApiModel.ModuleOperationResponse>

    /**
     * Bulk operations on multiple modules
     */
    suspend fun performBulkOperation(
        bulkRequest: WorkspaceModuleApiModel.BulkOperationRequest,
    ): Result<WorkspaceModuleApiModel.BulkOperationResponse>

    /**
     * Get module dashboard and analytics
     */
    suspend fun getModuleDashboard(): Result<WorkspaceModuleApiModel.ModuleDashboardResponse>

    /**
     * Get module analytics for a specific module
     */
    suspend fun getModuleAnalytics(
        moduleId: String,
        period: String = "30d",
    ): Result<WorkspaceModuleApiModel.ModuleAnalyticsResponse>

    /**
     * Export module configuration
     */
    suspend fun exportModuleConfiguration(
        moduleIds: List<String>? = null,
    ): Result<WorkspaceModuleApiModel.ModuleConfigurationExportResponse>

    /**
     * Import module configuration
     */
    suspend fun importModuleConfiguration(
        configData: Map<String, Any>,
    ): Result<WorkspaceModuleApiModel.ModuleOperationResponse>

    /**
     * Get module categories
     */
    suspend fun getModuleCategories(): Result<List<String>>

    /**
     * Check for module updates
     */
    suspend fun checkForUpdates(): Result<WorkspaceModuleApiModel.ModuleUpdatesResponse>

    /**
     * Get module health status
     */
    suspend fun getModuleHealthStatus(moduleId: String): Result<WorkspaceModuleApiModel.ModuleHealthResponse>

    /**
     * Enable/disable module
     */
    suspend fun toggleModuleStatus(
        moduleId: String,
        enabled: Boolean,
    ): Result<WorkspaceModuleApiModel.ModuleActionResponse>

    /**
     * Update a specific module to latest version
     */
    suspend fun updateModule(moduleId: String): Result<WorkspaceModuleApiModel.ModuleActionResponse>

    /**
     * Reset module to default configuration
     */
    suspend fun resetModuleConfiguration(moduleId: String): Result<WorkspaceModuleApiModel.ModuleActionResponse>

    /**
     * Run module diagnostics
     */
    suspend fun runModuleDiagnostics(moduleId: String): Result<WorkspaceModuleApiModel.ModuleActionResponse>
}