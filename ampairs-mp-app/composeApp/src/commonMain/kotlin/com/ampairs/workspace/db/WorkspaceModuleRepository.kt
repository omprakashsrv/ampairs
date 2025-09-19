package com.ampairs.workspace.db

import com.ampairs.workspace.api.WorkspaceModuleApi
import com.ampairs.workspace.api.model.InstalledModule
import com.ampairs.workspace.api.model.AvailableModule
import com.ampairs.workspace.api.model.ModuleInstallationResponse
import com.ampairs.workspace.api.model.ModuleUninstallationResponse
import com.ampairs.workspace.db.dao.WorkspaceModuleDao
import com.ampairs.workspace.db.entity.AvailableModuleEntity
import com.ampairs.workspace.db.entity.InstalledModuleWithMenuItems
import com.ampairs.workspace.store.WorkspaceModuleStoreFactory
import com.ampairs.workspace.store.InstalledModuleKey
import com.ampairs.workspace.store.AvailableModuleKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
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
    fun getInstalledModulesFlow(
        workspaceId: String,
        refresh: Boolean = false
    ): Flow<List<InstalledModule>> {
        val key = if (refresh) InstalledModuleKey.refresh(workspaceId) else InstalledModuleKey.all(
            workspaceId
        )
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
    suspend fun getInstalledModules(
        workspaceId: String,
        refresh: Boolean = false
    ): List<InstalledModule> {
        return try {
            val key =
                if (refresh) InstalledModuleKey.refresh(workspaceId) else InstalledModuleKey.all(
                    workspaceId
                )
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
                .first() // Get first response regardless of content for offline support
        } catch (e: Exception) {
            // Fallback to cached data with menu items
            moduleDao.getInstalledModulesWithMenuItems(workspaceId).map { it.toApiModel() }
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
                .first() // Get first response regardless of content for offline support
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
    suspend fun installModule(
        workspaceId: String,
        moduleCode: String
    ): Result<ModuleInstallationResponse> {
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
    suspend fun uninstallModule(
        workspaceId: String,
        moduleId: String
    ): Result<ModuleUninstallationResponse> {
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
        return moduleDao.getInstalledModulesWithMenuItems(workspaceId)
            .find { it.module.moduleCode == moduleCode }
            ?.toApiModel()
    }

    /**
     * Get active modules (matches web computed property)
     * Matches web: get activeModules() { return this.installedModules().filter(m => m.status === 'ACTIVE' && m.enabled); }
     */
    suspend fun getActiveModules(workspaceId: String): List<InstalledModule> {
        return moduleDao.getInstalledModulesWithMenuItems(workspaceId)
            .filter { it.module.status == "ACTIVE" && it.module.enabled }
            .map { it.toApiModel() }
    }

    /**
     * Get inactive modules (matches web computed property)
     * Matches web: get inactiveModules() { return this.installedModules().filter(m => m.status !== 'ACTIVE' || !m.enabled); }
     */
    suspend fun getInactiveModules(workspaceId: String): List<InstalledModule> {
        return moduleDao.getInstalledModulesWithMenuItems(workspaceId)
            .filter { it.module.status != "ACTIVE" || !it.module.enabled }
            .map { it.toApiModel() }
    }

    /**
     * Clear all caches and refresh from server
     */
    @OptIn(ExperimentalStoreApi::class)
    suspend fun refresh() {
        installedModuleStore.clear()
        availableModuleStore.clear()
    }

    /**
     * Clear all cached data
     */
    @OptIn(ExperimentalStoreApi::class)
    suspend fun clearCache(workspaceId: String) {
        moduleDao.deleteAllInstalledModules(workspaceId)
        moduleDao.deleteAllAvailableModules()
        installedModuleStore.clear()
        availableModuleStore.clear()
    }

    /**
     * Seed default modules for offline experience
     */
    suspend fun seedDefaultModules(workspaceId: String): List<InstalledModule> {
        try {
            // Check if modules already exist
            val existing = moduleDao.getInstalledModules(workspaceId)
            if (existing.isNotEmpty()) {
                return existing.map { it.toApiModel() }
            }

            // Create default module entities
            val defaultModules = listOf(
                createDefaultModuleEntity(workspaceId, "customer-management", "Customer Management", "Business", "#2196F3"),
                createDefaultModuleEntity(workspaceId, "product-management", "Product Management", "Business", "#4CAF50"),
                createDefaultModuleEntity(workspaceId, "order-management", "Order Management", "Business", "#9C27B0"),
                createDefaultModuleEntity(workspaceId, "invoice-management", "Invoice Management", "Finance", "#FF9800")
            )

            // Insert into database
            moduleDao.insertInstalledModules(defaultModules)

            // Clear store cache to force refresh from database
            installedModuleStore.clear()

            // Return API models
            return defaultModules.map { it.toApiModel() }
        } catch (e: Exception) {
            // Return empty list on error
            return emptyList()
        }
    }

    private fun createDefaultModuleEntity(
        workspaceId: String,
        moduleCode: String,
        name: String,
        category: String,
        primaryColor: String
    ): com.ampairs.workspace.db.entity.InstalledModuleEntity {
        return com.ampairs.workspace.db.entity.InstalledModuleEntity(
            id = "${workspaceId}_${moduleCode}",
            workspaceId = workspaceId,
            moduleCode = moduleCode,
            name = name,
            category = category,
            version = "1.0.0",
            status = "ACTIVE",
            enabled = true,
            installedAt = System.currentTimeMillis(),
            icon = moduleCode.lowercase().replace("-", "_"),
            primaryColor = primaryColor,
            healthScore = 100.0,
            needsAttention = false,
            description = "Default $name module for offline access",
            routeBasePath = "/workspace/modules/$moduleCode",
            routeDisplayName = name,
            routeIconName = moduleCode.lowercase().replace("-", "_"),
            navigationIndex = 0,
            syncState = "SYNCED",
            lastSyncedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
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
        description = description,
        routeInfo = com.ampairs.workspace.api.model.ModuleRouteInfo(
            basePath = routeBasePath,
            displayName = routeDisplayName,
            iconName = routeIconName,
            menuItems = emptyList() // Will be populated by DAO with relations
        ),
        navigationIndex = navigationIndex
    )
}

// Extension function for complete entity with menu items
private fun InstalledModuleWithMenuItems.toApiModel(): InstalledModule {
    return InstalledModule(
        id = module.id,
        workspaceId = module.workspaceId,
        moduleCode = module.moduleCode,
        name = module.name,
        category = module.category,
        version = module.version,
        status = module.status,
        enabled = module.enabled,
        installedAt = module.installedAt,
        icon = module.icon,
        primaryColor = module.primaryColor,
        healthScore = module.healthScore,
        needsAttention = module.needsAttention,
        description = module.description,
        routeInfo = com.ampairs.workspace.api.model.ModuleRouteInfo(
            basePath = module.routeBasePath,
            displayName = module.routeDisplayName,
            iconName = module.routeIconName,
            menuItems = menuItems.map { menuItem ->
                com.ampairs.workspace.api.model.ModuleMenuItem(
                    id = menuItem.id,
                    label = menuItem.label,
                    routePath = menuItem.routePath,
                    icon = menuItem.icon,
                    order = menuItem.order,
                    isDefault = menuItem.isDefault
                )
            }
        ),
        navigationIndex = module.navigationIndex
    )
}

private fun AvailableModuleEntity.toApiModel(): AvailableModule {
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