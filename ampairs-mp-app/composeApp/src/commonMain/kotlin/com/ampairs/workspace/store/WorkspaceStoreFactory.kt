package com.ampairs.workspace.store

import com.ampairs.workspace.api.WorkspaceApi
import com.ampairs.workspace.api.model.WorkspaceApiModel
import com.ampairs.workspace.api.model.WorkspaceListApiModel
import com.ampairs.workspace.db.dao.WorkspaceDao
import com.ampairs.workspace.db.entity.WorkspaceEntity
import com.ampairs.workspace.domain.Workspace
import org.mobilenativefoundation.store.store5.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Store5 Factory for Workspace data management
 * Following Store5 guidelines for proper offline-first architecture
 */
typealias WorkspaceStore = Store<WorkspaceKey, List<Workspace>>

data class WorkspaceKey(
    val workspaceId: String? = null, // null for list all workspaces
    val userId: String
)

class WorkspaceStoreFactory(
    private val workspaceApi: WorkspaceApi,
    private val workspaceDao: WorkspaceDao,
) {

    fun create(): WorkspaceStore {
        return StoreBuilder
            .from(
                fetcher = createFetcher(),
                sourceOfTruth = createSourceOfTruth()
            )
            .build()
    }

    private fun createFetcher(): Fetcher<WorkspaceKey, List<Workspace>> {
        return Fetcher.of { key ->
            val result: List<Workspace> = if (key.workspaceId != null) {
                // Fetch single workspace
                val response = workspaceApi.getWorkspace(key.workspaceId)
                if (response.data != null && response.error == null) {
                    listOf(response.data!!.toDomainModel())
                } else {
                    throw Exception(response.error?.message ?: "Failed to fetch workspace")
                }
            } else {
                // Fetch all user workspaces
                val response = workspaceApi.getUserWorkspaces()
                if (response.data != null && response.error == null) {
                    response.data!!.content.map { it.toDomainModel() }
                } else {
                    throw Exception(response.error?.message ?: "Failed to fetch workspaces")
                }
            }
            result
        }
    }

    private fun createSourceOfTruth(): SourceOfTruth<WorkspaceKey, List<Workspace>, List<Workspace>> {
        return SourceOfTruth.of(
            reader = { key ->
                if (key.workspaceId != null) {
                    // Read single workspace
                    workspaceDao.getWorkspaceByIdForUserFlow(key.workspaceId, key.userId)
                        .map { entity -> entity?.let { listOf(it.toDomainModel()) } ?: emptyList() }
                } else {
                    // Read all user workspaces
                    workspaceDao.getAllWorkspacesForUser(key.userId)
                        .map { entities -> entities.map { it.toDomainModel() } }
                }
            },
            writer = { key, networkData ->
                // Convert Domain models to entities
                val entities = networkData.map { it.toEntityModel(key.userId) }
                
                // Clear existing data for this user/workspace
                if (key.workspaceId != null) {
                    workspaceDao.deleteWorkspaceForUser(key.workspaceId, key.userId)
                } else {
                    workspaceDao.deleteAllWorkspacesForUser(key.userId)
                }
                
                // Insert new data
                workspaceDao.insertWorkspaces(entities)
            }
        )
    }

}

// Extension functions for data model conversions
private fun WorkspaceApiModel.toDomainModel(): Workspace {
    return Workspace(
        id = this.id,
        name = this.name,
        slug = this.slug,
        description = this.description,
        workspaceType = this.workspaceType,
        avatarUrl = this.avatarUrl,
        isActive = this.isActive,
        subscriptionPlan = this.subscriptionPlan,
        maxMembers = this.maxMembers,
        storageLimitGb = this.storageLimitGb,
        storageUsedGb = this.storageUsedGb,
        timezone = this.timezone,
        language = this.language,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastActivityAt = this.lastActivityAt,
        trialExpiresAt = this.trialExpiresAt,
        memberCount = this.memberCount,
        isTrial = this.isTrial,
        storagePercentage = this.storagePercentage
    )
}

private fun WorkspaceListApiModel.toDomainModel(): Workspace {
    return Workspace(
        id = this.id,
        name = this.name,
        slug = this.slug,
        description = this.description,
        workspaceType = this.workspaceType,
        avatarUrl = this.avatarUrl,
        isActive = true, // Default for list view
        subscriptionPlan = this.subscriptionPlan,
        maxMembers = 5, // Default from API
        storageLimitGb = 1, // Default from API
        storageUsedGb = 0, // Default from API
        timezone = "UTC", // Default from API
        language = "en", // Default from API
        createdBy = "", // Not available in list view
        createdAt = this.createdAt,
        updatedAt = this.createdAt, // Use createdAt as fallback
        lastActivityAt = this.lastActivityAt,
        trialExpiresAt = null, // Not available in list view
        memberCount = this.memberCount,
        isTrial = null, // Not available in list view
        storagePercentage = null // Not available in list view
    )
}

private fun WorkspaceApiModel.toEntityModel(userId: String): WorkspaceEntity {
    return WorkspaceEntity(
        id = this.id,
        user_id = userId,
        name = this.name,
        slug = this.slug,
        description = this.description ?: "",
        workspaceType = this.workspaceType,
        avatarUrl = this.avatarUrl ?: "",
        isActive = this.isActive,
        subscriptionPlan = this.subscriptionPlan,
        maxMembers = this.maxMembers,
        storageLimitGb = this.storageLimitGb,
        storageUsedGb = this.storageUsedGb,
        timezone = this.timezone,
        language = this.language,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastActivityAt = this.lastActivityAt ?: "",
        trialExpiresAt = this.trialExpiresAt ?: "",
        memberCount = this.memberCount ?: 0,
        isTrial = this.isTrial ?: false,
        storagePercentage = this.storagePercentage ?: 0.0,
        sync_state = "SYNCED",
        last_synced_at = System.currentTimeMillis(),
        local_updated_at = System.currentTimeMillis(),
        server_updated_at = System.currentTimeMillis()
    )
}

private fun WorkspaceEntity.toDomainModel(): Workspace {
    return Workspace(
        id = this.id,
        name = this.name,
        slug = this.slug,
        description = if (this.description.isEmpty()) null else this.description,
        workspaceType = this.workspaceType,
        avatarUrl = if (this.avatarUrl.isEmpty()) null else this.avatarUrl,
        isActive = this.isActive,
        subscriptionPlan = this.subscriptionPlan,
        maxMembers = this.maxMembers,
        storageLimitGb = this.storageLimitGb,
        storageUsedGb = this.storageUsedGb,
        timezone = this.timezone,
        language = this.language,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastActivityAt = if (this.lastActivityAt.isEmpty()) null else this.lastActivityAt,
        trialExpiresAt = if (this.trialExpiresAt.isEmpty()) null else this.trialExpiresAt,
        memberCount = if (this.memberCount == 0) null else this.memberCount,
        isTrial = if (this.isTrial) true else null,
        storagePercentage = if (this.storagePercentage == 0.0) null else this.storagePercentage
    )
}

private fun Workspace.toEntityModel(userId: String): WorkspaceEntity {
    return WorkspaceEntity(
        id = this.id,
        user_id = userId,
        name = this.name,
        slug = this.slug,
        description = this.description ?: "",
        workspaceType = this.workspaceType,
        avatarUrl = this.avatarUrl ?: "",
        isActive = this.isActive,
        subscriptionPlan = this.subscriptionPlan,
        maxMembers = this.maxMembers,
        storageLimitGb = this.storageLimitGb,
        storageUsedGb = this.storageUsedGb,
        timezone = this.timezone,
        language = this.language,
        createdBy = this.createdBy,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastActivityAt = this.lastActivityAt ?: "",
        trialExpiresAt = this.trialExpiresAt ?: "",
        memberCount = this.memberCount ?: 0,
        isTrial = this.isTrial ?: false,
        storagePercentage = this.storagePercentage ?: 0.0,
        sync_state = "SYNCED",
        last_synced_at = System.currentTimeMillis(),
        local_updated_at = System.currentTimeMillis(),
        server_updated_at = System.currentTimeMillis()
    )
}