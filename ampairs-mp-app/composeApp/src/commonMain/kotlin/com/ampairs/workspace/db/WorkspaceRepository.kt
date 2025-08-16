package com.ampairs.workspace.db

import com.ampairs.workspace.api.WorkspaceApi
import com.ampairs.workspace.api.model.CreateWorkspaceRequest
import com.ampairs.workspace.db.dao.WorkspaceDao
import com.ampairs.workspace.domain.Workspace
import com.ampairs.common.model.PageResult
import com.ampairs.workspace.domain.asDatabaseModel
import com.ampairs.workspace.domain.asDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkspaceRepository(
    private val workspaceApi: WorkspaceApi,
    private val workspaceDao: WorkspaceDao,
) {

    /**
     * Get user's workspaces from API with pagination info
     */
    suspend fun getUserWorkspaces(
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "createdAt",
        sortDir: String = "desc",
    ): PageResult<Workspace> {
        val response = workspaceApi.getUserWorkspaces(page, size, sortBy, sortDir)

        return if (response.error == null && response.data != null) {
            val pagedResponse = response.data!!
            
            // Convert WorkspaceListApiModel to Workspace for consistency
            val workspaces = pagedResponse.content.map { workspaceListItem ->
                Workspace(
                    id = workspaceListItem.id,
                    name = workspaceListItem.name,
                    slug = workspaceListItem.slug,
                    description = workspaceListItem.description,
                    workspaceType = workspaceListItem.workspaceType,
                    avatarUrl = workspaceListItem.avatarUrl,
                    isActive = true,
                    subscriptionPlan = workspaceListItem.subscriptionPlan,
                    maxMembers = 5,
                    storageLimitGb = 1,
                    storageUsedGb = 0,
                    timezone = "UTC",
                    language = "en",
                    createdBy = "",
                    createdAt = workspaceListItem.createdAt,
                    updatedAt = workspaceListItem.createdAt,
                    lastActivityAt = workspaceListItem.lastActivityAt,
                    trialExpiresAt = null,
                    memberCount = workspaceListItem.memberCount,
                    isTrial = null,
                    storagePercentage = null,
                )
            }

            // Save to local database
            workspaces.forEach { workspace ->
                val workspaceEntity = workspace.asDatabaseModel()
                workspaceDao.insertWorkspace(workspaceEntity)
            }

            PageResult(
                content = workspaces,
                totalElements = pagedResponse.totalElements,
                totalPages = pagedResponse.totalPages,
                currentPage = pagedResponse.number,
                pageSize = pagedResponse.size,
                isFirst = pagedResponse.first,
                isLast = pagedResponse.last,
                isEmpty = pagedResponse.empty
            )
        } else {
            throw Exception(response.error?.message ?: "Failed to fetch workspaces")
        }
    }

    /**
     * Create a new workspace
     */
    suspend fun createWorkspace(request: CreateWorkspaceRequest): Workspace {
        val response = workspaceApi.createWorkspace(request)

        return if (response.error == null && response.data != null) {
            val workspaceData = response.data!!
            val workspace = workspaceData.asDomainModel()

            // Save to local database
            workspaceDao.insertWorkspace(workspace.asDatabaseModel())

            workspace
        } else {
            throw Exception(response.error?.message ?: "Failed to create workspace")
        }
    }

    /**
     * Check slug availability
     */
    suspend fun checkSlugAvailability(slug: String): Map<String, Boolean> {
        val response = workspaceApi.checkSlugAvailability(slug)

        return if (response.error == null && response.data != null) {
            response.data!!
        } else {
            throw Exception(response.error?.message ?: "Failed to check slug availability")
        }
    }

    /**
     * Get workspaces from local database
     */
    fun getLocalWorkspaces(): Flow<List<Workspace>> {
        return workspaceDao.getAllWorkspaces().map { entities ->
            entities.map { it.asDomainModel() }
        }
    }

    /**
     * Search workspaces locally
     */
    fun searchWorkspacesLocally(query: String): Flow<List<Workspace>> {
        return workspaceDao.searchWorkspaces(query).map { entities ->
            entities.map { it.asDomainModel() }
        }
    }

    /**
     * Clear all local workspaces
     */
    suspend fun clearLocalWorkspaces() {
        workspaceDao.deleteAllWorkspaces()
    }
}