package com.ampairs.workspace.db

import com.ampairs.workspace.api.WorkspaceApi
import com.ampairs.workspace.api.model.CreateWorkspaceRequest
import com.ampairs.workspace.api.model.UpdateWorkspaceRequest
import com.ampairs.workspace.db.dao.WorkspaceDao
import com.ampairs.workspace.domain.Workspace
import com.ampairs.common.model.PageResult
import com.ampairs.workspace.domain.asDatabaseModel
import com.ampairs.workspace.domain.asDomainModel
import com.ampairs.auth.api.TokenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

/**
 * Offline-first workspace repository - simplified version for compilation
 * TODO: Implement full Store5 pattern with conflict resolution
 */
class OfflineFirstWorkspaceRepository(
    private val workspaceApi: WorkspaceApi,
    private val workspaceDao: WorkspaceDao,
    private val tokenRepository: TokenRepository
    // TODO: Add workspace store and sync manager
) {

    private suspend fun getCurrentUserId(): String? {
        return tokenRepository.getCurrentUserId()
    }

    /**
     * Get workspace by ID with offline-first approach
     * TODO: Implement Store5 pattern
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
     * Update workspace - simplified implementation
     * TODO: Implement optimistic updates and conflict resolution
     */
    suspend fun updateWorkspace(
        workspaceId: String, 
        request: UpdateWorkspaceRequest
    ): Workspace {
        val response = workspaceApi.updateWorkspace(workspaceId, request)

        return if (response.error == null && response.data != null) {
            val workspaceData = response.data!!
            val workspace = workspaceData.asDomainModel()

            // Update in local database with current user association
            val currentUserId = getCurrentUserId() ?: "unknown_user"
            val currentTime = System.currentTimeMillis()
            
            val workspaceEntity = workspace.asDatabaseModel().copy(
                user_id = currentUserId,
                sync_state = "SYNCED",
                last_synced_at = currentTime,
                server_updated_at = currentTime,
                local_updated_at = currentTime
            )
            workspaceDao.insertWorkspace(workspaceEntity)

            workspace
        } else {
            throw Exception(response.error?.message ?: "Failed to update workspace")
        }
    }

    /**
     * Get user's workspaces from API with pagination info
     * Falls back to local data when offline
     */
    suspend fun getUserWorkspaces(
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "createdAt",
        sortDir: String = "desc",
        forceRefresh: Boolean = false
    ): PageResult<Workspace> {
        // logger.d { "Getting user workspaces: page=$page, size=$size, forceRefresh=$forceRefresh" }
        
        return try {
            if (!forceRefresh) {
                // Try to get from local first
                val localWorkspaces = getLocalWorkspaces().first()
                if (localWorkspaces.isNotEmpty()) {
                    // logger.d { "Returning ${localWorkspaces.size} local workspaces" }
                    
                    // Return paginated local results
                    val startIndex = page * size
                    val endIndex = minOf(startIndex + size, localWorkspaces.size)
                    val pageContent = if (startIndex < localWorkspaces.size) {
                        localWorkspaces.subList(startIndex, endIndex)
                    } else {
                        emptyList()
                    }
                    
                    return PageResult(
                        content = pageContent,
                        totalElements = localWorkspaces.size,
                        totalPages = (localWorkspaces.size + size - 1) / size,
                        currentPage = page,
                        pageSize = size,
                        isFirst = page == 0,
                        isLast = endIndex >= localWorkspaces.size,
                        isEmpty = localWorkspaces.isEmpty()
                    )
                }
            }
            
            // Fetch from network
            val response = workspaceApi.getUserWorkspaces(page, size, sortBy, sortDir)
            
            if (response.error == null && response.data != null) {
                val pagedResponse = response.data!!
                
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
                val currentTime = System.currentTimeMillis()
                
                workspaces.forEach { workspace ->
                    val workspaceEntity = workspace.asDatabaseModel().copy(
                        user_id = currentUserId,
                        sync_state = "SYNCED",
                        last_synced_at = currentTime,
                        server_updated_at = currentTime,
                        local_updated_at = currentTime
                    )
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
                // Network failed, return local data
                // logger.w { "Network failed, falling back to local data" }
                val localWorkspaces = getLocalWorkspaces().first()
                
                val startIndex = page * size
                val endIndex = minOf(startIndex + size, localWorkspaces.size)
                val pageContent = if (startIndex < localWorkspaces.size) {
                    localWorkspaces.subList(startIndex, endIndex)
                } else {
                    emptyList()
                }
                
                PageResult(
                    content = pageContent,
                    totalElements = localWorkspaces.size,
                    totalPages = (localWorkspaces.size + size - 1) / size,
                    currentPage = page,
                    pageSize = size,
                    isFirst = page == 0,
                    isLast = endIndex >= localWorkspaces.size,
                    isEmpty = localWorkspaces.isEmpty()
                )
            }
        } catch (e: Exception) {
            // logger.e(e) { "Error getting user workspaces" }
            throw Exception("Failed to fetch workspaces: ${e.message}")
        }
    }

    /**
     * Create a new workspace
     */
    suspend fun createWorkspace(request: CreateWorkspaceRequest): Workspace {
        // logger.d { "Creating workspace: ${request.name}" }
        
        val response = workspaceApi.createWorkspace(request)

        return if (response.error == null && response.data != null) {
            val workspaceData = response.data!!
            val workspace = workspaceData.asDomainModel()

            // Save to local database with current user association
            val currentUserId = getCurrentUserId() ?: "unknown_user"
            val currentTime = System.currentTimeMillis()
            
            val workspaceEntity = workspace.asDatabaseModel().copy(
                user_id = currentUserId,
                sync_state = "SYNCED",
                last_synced_at = currentTime,
                server_updated_at = currentTime,
                local_updated_at = currentTime
            )
            workspaceDao.insertWorkspace(workspaceEntity)

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

/**
 * Result of workspace update operation
 */
sealed class WorkspaceUpdateResult {
    data class Success(val message: String) : WorkspaceUpdateResult()
    data class Failed(val message: String) : WorkspaceUpdateResult()
    data class ConflictDetected(val message: String) : WorkspaceUpdateResult()
}