package com.ampairs.workspace.store

import com.ampairs.workspace.api.WorkspaceModuleApi
import com.ampairs.workspace.api.model.InstalledModule
import com.ampairs.workspace.api.model.AvailableModule
import com.ampairs.workspace.db.dao.WorkspaceModuleDao
import com.ampairs.workspace.db.entity.InstalledModuleEntity
import com.ampairs.workspace.db.entity.AvailableModuleEntity
import org.mobilenativefoundation.store.store5.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Store5 Factory for Module data management following existing patterns
 */

// Type aliases for clarity
typealias InstalledModuleStore = Store<InstalledModuleKey, List<InstalledModule>>
typealias AvailableModuleStore = Store<AvailableModuleKey, List<AvailableModule>>

/**
 * Keys for Store5 operations
 */
data class InstalledModuleKey(
    val workspaceId: String,
    val refresh: Boolean = false
) {
    companion object {
        fun all(workspaceId: String) = InstalledModuleKey(workspaceId = workspaceId)
        fun refresh(workspaceId: String) = InstalledModuleKey(workspaceId = workspaceId, refresh = true)
    }
}

data class AvailableModuleKey(
    val category: String? = null,
    val featured: Boolean = false
) {
    companion object {
        fun all() = AvailableModuleKey()
        fun featured() = AvailableModuleKey(featured = true)
        fun category(category: String) = AvailableModuleKey(category = category)
    }
}

class WorkspaceModuleStoreFactory(
    private val moduleApi: WorkspaceModuleApi,
    private val moduleDao: WorkspaceModuleDao,
) {

    fun createInstalledModuleStore(): InstalledModuleStore {
        return StoreBuilder
            .from<InstalledModuleKey, List<InstalledModule>, List<InstalledModule>>(
                fetcher = createInstalledModuleFetcher(),
                sourceOfTruth = createInstalledModuleSourceOfTruth()
            )
            .build()
    }

    fun createAvailableModuleStore(): AvailableModuleStore {
        return StoreBuilder
            .from<AvailableModuleKey, List<AvailableModule>, List<AvailableModule>>(
                fetcher = createAvailableModuleFetcher(),
                sourceOfTruth = createAvailableModuleSourceOfTruth()
            )
            .build()
    }

    private fun createInstalledModuleFetcher(): Fetcher<InstalledModuleKey, List<InstalledModule>> {
        return Fetcher.of { key ->
            val result = moduleApi.getInstalledModules(key.workspaceId)
            result.getOrThrow()
        }
    }

    private fun createInstalledModuleSourceOfTruth(): SourceOfTruth<InstalledModuleKey, List<InstalledModule>, List<InstalledModule>> {
        return SourceOfTruth.of(
            reader = { key ->
                moduleDao.getInstalledModulesFlow(key.workspaceId).map { entities ->
                    entities.map { it.toApiModel() }
                }
            },
            writer = { key, modules ->
                val entities = modules.map { it.toEntity(key.workspaceId) }
                moduleDao.insertInstalledModules(entities)
                
                // Mark all as synced
                entities.forEach { entity ->
                    moduleDao.updateInstalledModuleSyncState(
                        entity.id,
                        entity.workspaceId,
                        "SYNCED", 
                        System.currentTimeMillis()
                    )
                }
            }
        )
    }

    private fun createAvailableModuleFetcher(): Fetcher<AvailableModuleKey, List<AvailableModule>> {
        return Fetcher.of { key ->
            val result = moduleApi.getAvailableModules(
                category = key.category,
                featured = key.featured
            )
            result.getOrThrow()
        }
    }

    private fun createAvailableModuleSourceOfTruth(): SourceOfTruth<AvailableModuleKey, List<AvailableModule>, List<AvailableModule>> {
        return SourceOfTruth.of(
            reader = { key ->
                kotlinx.coroutines.flow.flow {
                    val entities = when {
                        key.featured -> moduleDao.getFeaturedModules()
                        key.category != null -> moduleDao.getAvailableModulesByCategory(key.category!!)
                        else -> moduleDao.getAvailableModules()
                    }
                    emit(entities.map { it.toApiModel() })
                }
            },
            writer = { key, modules ->
                val entities = modules.map { it.toEntity() }
                moduleDao.insertAvailableModules(entities)
                
                // Mark all as synced
                entities.forEach { entity ->
                    moduleDao.updateAvailableModuleSyncState(
                        entity.moduleCode, 
                        "SYNCED", 
                        System.currentTimeMillis()
                    )
                }
            }
        )
    }
}

// Extension functions to convert between API models and entities
private fun InstalledModule.toEntity(workspaceId: String): InstalledModuleEntity {
    return InstalledModuleEntity(
        id = id,
        workspaceId = workspaceId, // Use the workspaceId from the Store key context
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
        sync_state = "SYNCED",
        created_at = System.currentTimeMillis(),
        updated_at = System.currentTimeMillis(),
        last_synced_at = System.currentTimeMillis()
    )
}

private fun InstalledModuleEntity.toApiModel(): InstalledModule {
    return InstalledModule(
        id = id,
        workspaceId = workspaceId, // Use the workspaceId from the entity
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

private fun AvailableModule.toEntity(): AvailableModuleEntity {
    return AvailableModuleEntity(
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
        sizeMb = sizeMb,
        sync_state = "SYNCED",
        created_at = System.currentTimeMillis(),
        updated_at = System.currentTimeMillis(),
        last_synced_at = System.currentTimeMillis()
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