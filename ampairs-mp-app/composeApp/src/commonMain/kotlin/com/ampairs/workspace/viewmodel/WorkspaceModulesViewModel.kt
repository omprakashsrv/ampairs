package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.api.WorkspaceModuleApi
import com.ampairs.workspace.api.model.WorkspaceModuleApiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Workspace Modules Screen
 *
 * Manages state for module management functionality including:
 * - Module dashboard and analytics
 * - Installed modules management
 * - Available modules discovery
 * - Module operations (install, uninstall, configure)
 */
class WorkspaceModulesViewModel(
    private val moduleApi: WorkspaceModuleApi,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkspaceModulesState())
    val state: StateFlow<WorkspaceModulesState> = _state.asStateFlow()

    /**
     * Load module dashboard data
     */
    fun loadModuleDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            moduleApi.getModuleDashboard().fold(
                onSuccess = { dashboard ->
                    _state.value = _state.value.copy(
                        dashboardData = dashboard,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load dashboard"
                    )
                }
            )
        }
    }

    /**
     * Load installed modules
     */
    fun loadInstalledModules() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            val currentState = _state.value
            moduleApi.searchInstalledModules(
                query = currentState.searchQuery.takeIf { it.isNotBlank() },
                category = currentState.selectedCategory.takeIf { it.isNotBlank() },
                status = currentState.selectedStatus.takeIf { it.isNotBlank() },
                page = currentState.currentPage,
                size = currentState.pageSize,
                sortBy = "display_order",
                sortDirection = "asc"
            ).fold(
                onSuccess = { response ->
                    _state.value = _state.value.copy(
                        installedModules = response.modules,
                        totalElements = response.totalElements,
                        totalPages = response.totalPages,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load modules"
                    )
                }
            )
        }
    }

    /**
     * Load available modules for installation
     */
    fun loadAvailableModules() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            moduleApi.getAvailableModules().fold(
                onSuccess = { response ->
                    val allModules = response.essentialModules +
                            response.recommendedModules +
                            response.availableModules
                    _state.value = _state.value.copy(
                        availableModules = allModules,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load available modules"
                    )
                }
            )
        }
    }

    /**
     * Install a module
     */
    fun installModule(masterModule: WorkspaceModuleApiModel.MasterModule) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            val installRequest = WorkspaceModuleApiModel.ModuleInstallationRequest(
                masterModuleId = masterModule.id,
                displayOrder = _state.value.installedModules.size + 1
            )

            moduleApi.installModule(installRequest).fold(
                onSuccess = { response ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "${masterModule.name} installed successfully"
                    )
                    // Refresh data
                    loadInstalledModules()
                    loadModuleDashboard()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to install ${masterModule.name}"
                    )
                }
            )
        }
    }

    /**
     * Uninstall a module
     */
    fun uninstallModule(moduleId: String, preserveData: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            val moduleName = _state.value.installedModules.find { it.id == moduleId }?.effectiveName ?: "Module"

            moduleApi.uninstallModule(moduleId, preserveData).fold(
                onSuccess = { response ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "$moduleName uninstalled successfully"
                    )
                    // Refresh data
                    loadInstalledModules()
                    loadModuleDashboard()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to uninstall $moduleName"
                    )
                }
            )
        }
    }

    /**
     * Toggle module enabled/disabled status
     */
    fun toggleModuleStatus(moduleId: String, enabled: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            val moduleName = _state.value.installedModules.find { it.id == moduleId }?.effectiveName ?: "Module"

            moduleApi.toggleModuleStatus(moduleId, enabled).fold(
                onSuccess = { response ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "$moduleName ${if (enabled) "enabled" else "disabled"} successfully"
                    )
                    // Update the module in the list
                    val updatedModules = _state.value.installedModules.map { module ->
                        if (module.id == moduleId) {
                            module.copy(enabled = enabled)
                        } else {
                            module
                        }
                    }
                    _state.value = _state.value.copy(installedModules = updatedModules)
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to update $moduleName status"
                    )
                }
            )
        }
    }

    /**
     * Update module configuration
     */
    fun updateModuleConfiguration(
        moduleId: String,
        settings: Map<String, kotlinx.serialization.json.JsonElement>,
        notes: String? = null,
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            val configRequest = WorkspaceModuleApiModel.ModuleConfigurationRequest(
                settings = settings,
                notes = notes
            )

            val moduleName = _state.value.installedModules.find { it.id == moduleId }?.effectiveName ?: "Module"

            moduleApi.updateModuleConfiguration(moduleId, configRequest).fold(
                onSuccess = { response ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "$moduleName configuration updated successfully"
                    )
                    loadInstalledModules()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to update $moduleName configuration"
                    )
                }
            )
        }
    }

    /**
     * Update module to latest version
     */
    fun updateModule(moduleId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            val moduleName = _state.value.installedModules.find { it.id == moduleId }?.effectiveName ?: "Module"

            moduleApi.updateModule(moduleId).fold(
                onSuccess = { response ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "$moduleName updated successfully"
                    )
                    loadInstalledModules()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to update $moduleName"
                    )
                }
            )
        }
    }

    /**
     * Reset module configuration to defaults
     */
    fun resetModuleConfiguration(moduleId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            val moduleName = _state.value.installedModules.find { it.id == moduleId }?.effectiveName ?: "Module"

            moduleApi.resetModuleConfiguration(moduleId).fold(
                onSuccess = { response ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "$moduleName reset to default configuration"
                    )
                    loadInstalledModules()
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to reset $moduleName"
                    )
                }
            )
        }
    }

    /**
     * Get module analytics
     */
    fun loadModuleAnalytics(moduleId: String, period: String = "30d") {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            moduleApi.getModuleAnalytics(moduleId, period).fold(
                onSuccess = { analytics ->
                    _state.value = _state.value.copy(
                        moduleAnalytics = _state.value.moduleAnalytics + (moduleId to analytics),
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load module analytics"
                    )
                }
            )
        }
    }

    /**
     * Check for module updates
     */
    fun checkForUpdates() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            moduleApi.checkForUpdates().fold(
                onSuccess = { updates ->
                    _state.value = _state.value.copy(
                        availableUpdates = updates,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to check for updates"
                    )
                }
            )
        }
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(
            searchQuery = query,
            currentPage = 0
        )
        loadInstalledModules()
    }

    /**
     * Update selected category filter
     */
    fun updateCategory(category: String) {
        _state.value = _state.value.copy(
            selectedCategory = category,
            currentPage = 0
        )
        loadInstalledModules()
    }

    /**
     * Update selected status filter
     */
    fun updateStatus(status: String) {
        _state.value = _state.value.copy(
            selectedStatus = status,
            currentPage = 0
        )
        loadInstalledModules()
    }

    /**
     * Change page
     */
    fun changePage(page: Int) {
        _state.value = _state.value.copy(currentPage = page)
        loadInstalledModules()
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        _state.value = _state.value.copy(
            searchQuery = "",
            selectedCategory = "",
            selectedStatus = "",
            currentPage = 0
        )
        loadInstalledModules()
    }

    /**
     * Refresh all data
     */
    fun refresh() {
        loadModuleDashboard()
        loadInstalledModules()
        if (_state.value.availableModules.isNotEmpty()) {
            loadAvailableModules()
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        _state.value = _state.value.copy(successMessage = null)
    }
}

/**
 * State class for Workspace Modules Screen
 */
data class WorkspaceModulesState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Dashboard data
    val dashboardData: WorkspaceModuleApiModel.ModuleDashboardResponse? = null,

    // Installed modules
    val installedModules: List<WorkspaceModuleApiModel.WorkspaceModule> = emptyList(),
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val pageSize: Int = 20,

    // Available modules
    val availableModules: List<WorkspaceModuleApiModel.MasterModule> = emptyList(),

    // Filters
    val searchQuery: String = "",
    val selectedCategory: String = "",
    val selectedStatus: String = "",

    // Analytics
    val moduleAnalytics: Map<String, WorkspaceModuleApiModel.ModuleAnalyticsResponse> = emptyMap(),
    val availableUpdates: WorkspaceModuleApiModel.ModuleUpdatesResponse? = null,
)