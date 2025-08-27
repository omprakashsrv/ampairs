package com.ampairs.workspace.db

import com.ampairs.workspace.api.WorkspaceApi
import com.ampairs.workspace.api.model.CreateWorkspaceRequest
import com.ampairs.workspace.db.dao.WorkspaceDao
import com.ampairs.workspace.domain.Workspace
import com.ampairs.common.model.PageResult
import com.ampairs.workspace.domain.asDatabaseModel
import com.ampairs.workspace.domain.asDomainModel
import com.ampairs.auth.api.TokenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkspaceRepository(
    private val workspaceApi: WorkspaceApi,
    private val workspaceDao: WorkspaceDao,
    private val tokenRepository: TokenRepository, // Add dependency to get current user
) {

    private suspend fun getCurrentUserId(): String? {
        return tokenRepository.getCurrentUserId()
    }

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

            // Save to local database with current user association
            val currentUserId = getCurrentUserId() ?: "unknown_user"
            workspaces.forEach { workspace ->
                val workspaceEntity = workspace.asDatabaseModel().copy(user_id = currentUserId)
                workspaceDao.insertWorkspace(workspaceEntity)
            }

            PageResult(
                content = workspaces,
                totalElements = pagedResponse.totalElements,
                totalPages = pagedResponse.totalPages,
                currentPage = pagedResponse.pageNumber,
                pageSize = pagedResponse.pageSize,
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

            // Save to local database with current user association
            val currentUserId = getCurrentUserId() ?: "unknown_user"
            workspaceDao.insertWorkspace(workspace.asDatabaseModel().copy(user_id = currentUserId))

            workspace
        } else {
            throw Exception(response.error?.message ?: "Failed to create workspace")
        }
    }
    
    /**
     * Update an existing workspace
     */
    suspend fun updateWorkspace(workspaceId: String, request: com.ampairs.workspace.api.model.UpdateWorkspaceRequest): Workspace {
        val response = workspaceApi.updateWorkspace(workspaceId, request)

        return if (response.error == null && response.data != null) {
            val workspaceData = response.data!!
            val workspace = workspaceData.asDomainModel()

            // Update in local database with current user association
            val currentUserId = getCurrentUserId() ?: "unknown_user"
            workspaceDao.insertWorkspace(workspace.asDatabaseModel().copy(user_id = currentUserId))

            workspace
        } else {
            throw Exception(response.error?.message ?: "Failed to update workspace")
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
     * Get workspaces from local database for current user
     */
    suspend fun getLocalWorkspaces(): Flow<List<Workspace>> {
        val currentUserId = getCurrentUserId() ?: return workspaceDao.getAllWorkspaces().map { entities ->
            entities.map { it.asDomainModel() }
        }

        return workspaceDao.getAllWorkspacesForUser(currentUserId).map { entities ->
            entities.map { it.asDomainModel() }
        }
    }

    /**
     * Search workspaces locally for current user
     */
    suspend fun searchWorkspacesLocally(query: String): Flow<List<Workspace>> {
        val currentUserId = getCurrentUserId() ?: return workspaceDao.searchWorkspaces(query).map { entities ->
            entities.map { it.asDomainModel() }
        }

        return workspaceDao.searchWorkspacesForUser(currentUserId, query).map { entities ->
            entities.map { it.asDomainModel() }
        }
    }

    /**
     * Get workspace by ID for current user
     */
    suspend fun getWorkspaceById(workspaceId: String): Workspace? {
        val currentUserId = getCurrentUserId()
        return if (currentUserId != null) {
            workspaceDao.getWorkspaceByIdForUser(workspaceId, currentUserId)?.asDomainModel()
        } else {
            workspaceDao.getWorkspaceById(workspaceId)?.asDomainModel()
        }
    }

    /**
     * Insert multiple workspaces to local database
     */
    suspend fun insertWorkspaces(workspaceEntities: List<com.ampairs.workspace.db.entity.WorkspaceEntity>) {
        val currentUserId = getCurrentUserId() ?: "unknown_user"
        workspaceEntities.forEach { entity ->
            workspaceDao.insertWorkspace(entity.copy(user_id = currentUserId))
        }
    }

    /**
     * Clear local workspaces for current user
     */
    suspend fun clearLocalWorkspaces() {
        val currentUserId = getCurrentUserId()
        if (currentUserId != null) {
            workspaceDao.deleteAllWorkspacesForUser(currentUserId)
        } else {
            workspaceDao.deleteAllWorkspaces()
        }
    }
}