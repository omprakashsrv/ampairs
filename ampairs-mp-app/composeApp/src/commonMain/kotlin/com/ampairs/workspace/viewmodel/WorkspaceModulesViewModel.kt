package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.workspace.api.model.InstalledModule
import com.ampairs.workspace.api.model.AvailableModule
import com.ampairs.workspace.api.model.ModuleInstallationResponse
import com.ampairs.workspace.api.model.ModuleUninstallationResponse
import com.ampairs.workspace.db.WorkspaceModuleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Workspace Modules matching web implementation
 * Follows web: workspace-modules.component.ts with signals pattern
 */
class WorkspaceModulesViewModel(
    private val moduleRepository: WorkspaceModuleRepository,
    private val workspaceId: String? = null // Optional workspace context
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Installed modules - matches web: installedModules = signal<InstalledModule[]>([])
    val installedModules: StateFlow<List<InstalledModule>> = workspaceId?.let { wsId ->
        moduleRepository
            .getInstalledModulesFlow(wsId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } ?: MutableStateFlow<List<InstalledModule>>(emptyList()).asStateFlow()

    // Active modules - matches web: get activeModules()
    val activeModules: StateFlow<List<InstalledModule>> = installedModules
        .map { modules -> modules.filter { it.status == "ACTIVE" && it.enabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Inactive modules - matches web: get inactiveModules()
    val inactiveModules: StateFlow<List<InstalledModule>> = installedModules
        .map { modules -> modules.filter { it.status != "ACTIVE" || !it.enabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Available modules for store - matches web: availableModules
    private val _availableModules = MutableStateFlow<List<AvailableModule>>(emptyList())
    val availableModules: StateFlow<List<AvailableModule>> = _availableModules.asStateFlow()

    // Featured modules - matches web: get featuredModules()
    val featuredModules: StateFlow<List<AvailableModule>> = availableModules
        .map { modules -> modules.filter { it.featured } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Note: Data loading is handled by the UI screen with explicit refresh=true
    // This ensures fresh data is fetched from the backend via Store5

    /**
     * Load installed modules - matches web: async getInstalledModules()
     */
    fun loadInstalledModules(refresh: Boolean = false) {
        val wsId = workspaceId ?: return // No workspace context
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                // Repository automatically updates StateFlow via Store5
                moduleRepository.getInstalledModules(wsId, refresh)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load installed modules"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load available modules - matches web: async getAvailableModules()
     */
    fun loadAvailableModules(category: String? = null, featured: Boolean = false, refresh: Boolean = false) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                // TODO: Replace with actual API call when backend is ready
                // For now, using mock data for UI testing
                val mockModules = createMockAvailableModules()
                _availableModules.value = mockModules
                
                // Uncomment when backend is ready:
                // val modules = moduleRepository.getAvailableModules(category, featured, refresh)
                // _availableModules.value = modules
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load available modules"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Mock data for testing - remove when backend is ready
     */
    private fun createMockAvailableModules(): List<AvailableModule> {
        return listOf(
            AvailableModule(
                moduleCode = "CUSTOMER_MGMT",
                name = "Customer Management",
                description = "Comprehensive customer relationship management with advanced analytics",
                category = "Business",
                version = "2.1.0",
                rating = 4.8,
                installCount = 15420,
                complexity = "Medium",
                icon = "customers",
                primaryColor = "#2196F3",
                featured = true,
                requiredTier = "FREE",
                sizeMb = 45
            ),
            AvailableModule(
                moduleCode = "INVENTORY_MGMT",
                name = "Inventory Management",
                description = "Real-time inventory tracking with automated alerts",
                category = "Operations",
                version = "1.8.2",
                rating = 4.6,
                installCount = 12300,
                complexity = "Simple",
                icon = "inventory",
                primaryColor = "#4CAF50",
                featured = true,
                requiredTier = "FREE",
                sizeMb = 32
            ),
            AvailableModule(
                moduleCode = "ANALYTICS_PRO",
                name = "Analytics Dashboard",
                description = "Advanced business intelligence and reporting tools",
                category = "Analytics",
                version = "3.0.1",
                rating = 4.9,
                installCount = 8750,
                complexity = "Advanced",
                icon = "analytics",
                primaryColor = "#FF9800",
                featured = true,
                requiredTier = "PRO",
                sizeMb = 78
            ),
            AvailableModule(
                moduleCode = "ORDER_MGMT",
                name = "Order Management",
                description = "Streamlined order processing and fulfillment",
                category = "Business",
                version = "2.3.0",
                rating = 4.5,
                installCount = 9850,
                complexity = "Simple",
                icon = "orders",
                primaryColor = "#9C27B0",
                featured = false,
                requiredTier = "FREE",
                sizeMb = 28
            ),
            AvailableModule(
                moduleCode = "FINANCIAL_MGMT",
                name = "Financial Management",
                description = "Complete accounting and financial tracking solution",
                category = "Finance",
                version = "2.0.5",
                rating = 4.7,
                installCount = 6420,
                complexity = "Advanced",
                icon = "finance",
                primaryColor = "#E91E63",
                featured = false,
                requiredTier = "PRO",
                sizeMb = 95
            ),
            AvailableModule(
                moduleCode = "HR_MGMT",
                name = "Human Resources",
                description = "Employee management and HR workflows",
                category = "HR",
                version = "1.5.8",
                rating = 4.3,
                installCount = 4200,
                complexity = "Medium",
                icon = "hr",
                primaryColor = "#607D8B",
                featured = false,
                requiredTier = "BUSINESS",
                sizeMb = 52
            )
        )
    }

    /**
     * Install module - matches web: async installModule(moduleCode: string)
     */
    fun installModule(moduleCode: String, onResult: (ModuleInstallationResponse?) -> Unit) {
        val wsId = workspaceId ?: run {
            onResult(null)
            return
        }
        
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                
                // TODO: Replace with actual API call when backend is ready
                // For now, simulate installation
                val mockResponse = ModuleInstallationResponse(
                    moduleId = "inst_${moduleCode}_${System.currentTimeMillis()}",
                    moduleCode = moduleCode,
                    name = _availableModules.value.find { it.moduleCode == moduleCode }?.name ?: "Unknown Module",
                    status = "INSTALLED",
                    installedAt = "2025-01-11T10:30:00Z",
                    message = "Module installed successfully"
                )
                
                // Simulate network delay
                kotlinx.coroutines.delay(1500)
                
                onResult(mockResponse)
                
                // Refresh installed modules to show the new installation
                loadInstalledModules(refresh = true)
                
                // Uncomment when backend is ready:
                // val result = moduleRepository.installModule(wsId, moduleCode)
                // if (result.isSuccess) {
                //     onResult(result.getOrThrow())
                //     loadInstalledModules(refresh = true)
                // } else {
                //     val error = result.exceptionOrNull()?.message ?: "Failed to install module"
                //     _errorMessage.value = error
                //     onResult(null)
                // }
            } catch (e: Exception) {
                val error = e.message ?: "Failed to install module"
                _errorMessage.value = error
                onResult(null)
            }
        }
    }

    /**
     * Uninstall module - matches web: async uninstallModule(moduleId: string)
     */
    fun uninstallModule(moduleId: String, onResult: (ModuleUninstallationResponse?) -> Unit) {
        val wsId = workspaceId ?: run {
            onResult(null)
            return
        }
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val result = moduleRepository.uninstallModule(wsId, moduleId)
                if (result.isSuccess) {
                    onResult(result.getOrThrow())
                    // Refresh installed modules after successful uninstallation
                    loadInstalledModules(refresh = true)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to uninstall module"
                    _errorMessage.value = error
                    onResult(null)
                }
            } catch (e: Exception) {
                val error = e.message ?: "Failed to uninstall module"
                _errorMessage.value = error
                onResult(null)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Check if module is installed - matches web: isModuleInstalled(moduleCode: string)
     */
    suspend fun isModuleInstalled(moduleCode: String): Boolean {
        val wsId = workspaceId ?: return false
        return moduleRepository.isModuleInstalled(wsId, moduleCode)
    }

    /**
     * Get module by code - helper method
     */
    suspend fun getModuleByCode(moduleCode: String): InstalledModule? {
        val wsId = workspaceId ?: return null
        return moduleRepository.getModuleByCode(wsId, moduleCode)
    }

    /**
     * Refresh all data
     */
    fun refresh() {
        loadInstalledModules(refresh = true)
        if (_availableModules.value.isNotEmpty()) {
            loadAvailableModules(refresh = true)
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}