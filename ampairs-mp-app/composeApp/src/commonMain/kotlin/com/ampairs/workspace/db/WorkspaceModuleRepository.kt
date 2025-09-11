package com.ampairs.workspace.db

import com.ampairs.workspace.api.WorkspaceModuleApi
import com.ampairs.workspace.api.model.InstalledModule
import com.ampairs.workspace.api.model.AvailableModule
import com.ampairs.workspace.api.model.ModuleInstallationResponse
import com.ampairs.workspace.api.model.ModuleUninstallationResponse
import com.ampairs.workspace.db.dao.WorkspaceModuleDao
import com.ampairs.workspace.store.WorkspaceModuleStoreFactory
import com.ampairs.workspace.store.InstalledModuleKey
import com.ampairs.workspace.store.AvailableModuleKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * Offline-first repository for workspace modules
 * Follows the existing Store5 patterns used by other workspace repositories
 */
class WorkspaceModuleRepository(
    private val moduleApi: WorkspaceModuleApi,
    private val moduleDao: WorkspaceModuleDao,
    storeFactory: WorkspaceModuleStoreFactory
) {

    private val installedModuleStore = storeFactory.createInstalledModuleStore()
    private val availableModuleStore = storeFactory.createAvailableModuleStore()

    /**
     * Get installed modules as Flow for reactive UI updates
     * Matches web: this.installedModules = signal<InstalledModule[]>([])
     */
    fun getInstalledModulesFlow(workspaceId: String, refresh: Boolean = false): Flow<List<InstalledModule>> {
        val key = if (refresh) InstalledModuleKey.refresh(workspaceId) else InstalledModuleKey.all(workspaceId)
        val request = if (refresh) {
            StoreReadRequest.fresh(key) // Force API call
        } else {
            StoreReadRequest.cached(key, refresh = false) // Use cache first
        }
        
        return installedModuleStore.stream(request)
            .map { response ->
                when (response) {
                    is StoreReadResponse.Data -> response.value
                    is StoreReadResponse.Loading -> response.dataOrNull() ?: emptyList()
                    is StoreReadResponse.Error -> response.dataOrNull() ?: emptyList()
                    is StoreReadResponse.NoNewData -> response.dataOrNull() ?: emptyList()
                    is StoreReadResponse.Initial -> emptyList()
                }
            }
    }

    /**
     * Get installed modules (suspending)
     * Matches web: async getInstalledModules(): Promise<InstalledModule[]>
     */
    suspend fun getInstalledModules(workspaceId: String, refresh: Boolean = false): List<InstalledModule> {
        return try {
            val key = if (refresh) InstalledModuleKey.refresh(workspaceId) else InstalledModuleKey.all(workspaceId)
            val request = if (refresh) {
                StoreReadRequest.fresh(key) // Force API call
            } else {
                StoreReadRequest.cached(key, refresh = false) // Use cache first
            }
            
            // Get first valid response from Store5
            installedModuleStore.stream(request)
                .map { response ->
                    when (response) {
                        is StoreReadResponse.Data -> response.value
                        is StoreReadResponse.Loading -> response.dataOrNull() ?: emptyList()
                        is StoreReadResponse.Error -> response.dataOrNull() ?: emptyList()
                        is StoreReadResponse.NoNewData -> response.dataOrNull() ?: emptyList()
                        is StoreReadResponse.Initial -> emptyList()
                    }
                }
                .first { it.isNotEmpty() || !refresh } // Get first non-empty or accept empty if not refreshing
        } catch (e: Exception) {
            // Fallback to cached data
            moduleDao.getInstalledModules(workspaceId).map { it.toApiModel() }
        }
    }

    /**
     * Get available modules
     * Matches web: async getAvailableModules(category?: string, featured = false): Promise<AvailableModule[]>
     */
    suspend fun getAvailableModules(
        category: String? = null,
        featured: Boolean = false,
        refresh: Boolean = false
    ): List<AvailableModule> {
        return try {
            val key = when {
                featured -> AvailableModuleKey.featured()
                category != null -> AvailableModuleKey.category(category)
                else -> AvailableModuleKey.all()
            }
            
            val request = if (refresh) {
                StoreReadRequest.fresh(key)
            } else {
                StoreReadRequest.cached(key, refresh = false)
            }
            
            // Get first valid response from Store5
            availableModuleStore.stream(request)
                .map { response ->
                    when (response) {
                        is StoreReadResponse.Data -> response.value
                        is StoreReadResponse.Loading -> response.dataOrNull() ?: emptyList()
                        is StoreReadResponse.Error -> response.dataOrNull() ?: emptyList()
                        is StoreReadResponse.NoNewData -> response.dataOrNull() ?: emptyList()
                        is StoreReadResponse.Initial -> emptyList()
                    }
                }
                .first { it.isNotEmpty() || !refresh }
        } catch (e: Exception) {
            // Fallback to cached data
            val entities = when {
                featured -> moduleDao.getFeaturedModules()
                category != null -> moduleDao.getAvailableModulesByCategory(category)
                else -> moduleDao.getAvailableModules()
            }
            entities.map { it.toApiModel() }
        }
    }

    /**
     * Install a module
     * Matches web: async installModule(moduleCode: string): Promise<ModuleInstallationResponse>
     */
    suspend fun installModule(workspaceId: String, moduleCode: String): Result<ModuleInstallationResponse> {
        return try {
            val result = moduleApi.installModule(workspaceId, moduleCode)
            
            if (result.isSuccess) {
                // Store5 will automatically refresh via SourceOfTruth
                // ViewModel's flow will pick up the changes automatically
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uninstall a module
     * Matches web: async uninstallModule(moduleId: string): Promise<ModuleUninstallationResponse>
     */
    suspend fun uninstallModule(workspaceId: String, moduleId: String): Result<ModuleUninstallationResponse> {
        return try {
            val result = moduleApi.uninstallModule(workspaceId, moduleId)
            
            if (result.isSuccess) {
                // Remove from local database - Store5 will handle sync automatically
                moduleDao.deleteInstalledModuleById(moduleId, workspaceId)
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if module is installed (matches web service method)
     * Matches web: isModuleInstalled(moduleCode: string): boolean
     */
    suspend fun isModuleInstalled(workspaceId: String, moduleCode: String): Boolean {
        return moduleDao.isModuleInstalled(workspaceId, moduleCode)
    }

    /**
     * Get module by code (matches web service method)
     */
    suspend fun getModuleByCode(workspaceId: String, moduleCode: String): InstalledModule? {
        return moduleDao.getInstalledModules(workspaceId)
            .find { it.moduleCode == moduleCode }
            ?.toApiModel()
    }

    /**
     * Get active modules (matches web computed property)
     * Matches web: get activeModules() { return this.installedModules().filter(m => m.status === 'ACTIVE' && m.enabled); }
     */
    suspend fun getActiveModules(workspaceId: String): List<InstalledModule> {
        return moduleDao.getActiveModules(workspaceId).map { it.toApiModel() }
    }

    /**
     * Get inactive modules (matches web computed property)
     * Matches web: get inactiveModules() { return this.installedModules().filter(m => m.status !== 'ACTIVE' || !m.enabled); }
     */
    suspend fun getInactiveModules(workspaceId: String): List<InstalledModule> {
        return moduleDao.getInactiveModules(workspaceId).map { it.toApiModel() }
    }

    /**
     * Clear all caches and refresh from server
     */
    suspend fun refresh() {
        installedModuleStore.clear()
        availableModuleStore.clear()
    }

    /**
     * Clear all cached data
     */
    suspend fun clearCache(workspaceId: String) {
        moduleDao.deleteAllInstalledModules(workspaceId)
        moduleDao.deleteAllAvailableModules()
        installedModuleStore.clear()
        availableModuleStore.clear()
    }
}

// Extension functions for entity-API model conversion
private fun com.ampairs.workspace.db.entity.InstalledModuleEntity.toApiModel(): InstalledModule {
    return InstalledModule(
        id = id,
        workspaceId = workspaceId,
        moduleCode = moduleCode,
        name = name,
        category = category,
        version = version,
        status = status,
        enabled = enabled,
        installedAt = installedAt,
        icon = icon,
        primaryColor = primaryColor,
        healthScore = healthScore,
        needsAttention = needsAttention,
        description = description
    )
}

private fun com.ampairs.workspace.db.entity.AvailableModuleEntity.toApiModel(): AvailableModule {
    return AvailableModule(
        moduleCode = moduleCode,
        name = name,
        description = description,
        category = category,
        version = version,
        rating = rating,
        installCount = installCount,
        complexity = complexity,
        icon = icon,
        primaryColor = primaryColor,
        featured = featured,
        requiredTier = requiredTier,
        sizeMb = sizeMb
    )
}