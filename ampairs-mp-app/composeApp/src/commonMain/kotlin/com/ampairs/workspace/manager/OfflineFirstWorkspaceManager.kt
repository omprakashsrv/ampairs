package com.ampairs.workspace.manager

import com.ampairs.workspace.api.WorkspaceApi
import com.ampairs.workspace.db.WorkspaceRepository
import com.ampairs.workspace.domain.Workspace
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.coroutineScope

/**
 * Simplified offline-first data manager for workspace data synchronization.
 * This implementation provides automatic synchronization between network API 
 * and local Room database without using the Store library.
 */
class OfflineFirstWorkspaceManager(
    private val workspaceApi: WorkspaceApi,
    private val workspaceRepository: WorkspaceRepository,
) {
    
    /**
     * Get workspace list with offline-first approach.
     * Returns cached data immediately if available, then fetches fresh data from network.
     */
    fun getWorkspaces(
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "updatedAt",
        sortDir: String = "desc",
        forceRefresh: Boolean = false
    ): Flow<WorkspaceDataState> = flow {
        try {
            if (!forceRefresh) {
                // Emit loading state
                emit(WorkspaceDataState.Loading)
                
                // First, emit cached data if available
                workspaceRepository.getLocalWorkspaces().collect { cachedWorkspaces ->
                    if (cachedWorkspaces.isNotEmpty()) {
                        emit(WorkspaceDataState.Success(cachedWorkspaces, isFromCache = true))
                    }
                    
                    // Then try to fetch fresh data from network
                    try {
                        val networkResult = workspaceRepository.getUserWorkspaces(page, size, sortBy, sortDir)
                        emit(WorkspaceDataState.Success(networkResult.content, isFromCache = false))
                    } catch (networkError: Exception) {
                        // If network fails but we have cached data, keep the cached data
                        if (cachedWorkspaces.isNotEmpty()) {
                            emit(WorkspaceDataState.Success(cachedWorkspaces, isFromCache = true, networkError = networkError.message))
                        } else {
                            // No cached data and network failed
                            emit(WorkspaceDataState.Error(networkError.message ?: "Failed to load workspaces"))
                        }
                    }
                }
            } else {
                // Force refresh - only fetch from network
                emit(WorkspaceDataState.Loading)
                try {
                    val networkResult = workspaceRepository.getUserWorkspaces(page, size, sortBy, sortDir)
                    emit(WorkspaceDataState.Success(networkResult.content, isFromCache = false))
                } catch (networkError: Exception) {
                    // Fallback to cached data on network error
                    workspaceRepository.getLocalWorkspaces().collect { cachedWorkspaces ->
                        if (cachedWorkspaces.isNotEmpty()) {
                            emit(WorkspaceDataState.Success(cachedWorkspaces, isFromCache = true, networkError = networkError.message))
                        } else {
                            emit(WorkspaceDataState.Error(networkError.message ?: "Failed to refresh workspaces"))
                        }
                    }
                }
            }
        } catch (error: Exception) {
            emit(WorkspaceDataState.Error(error.message ?: "Unexpected error occurred"))
        }
    }
    
    /**
     * Force refresh workspace data from network
     */
    suspend fun refreshWorkspaces(
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "updatedAt",
        sortDir: String = "desc"
    ): WorkspaceDataState {
        return try {
            val networkResult = workspaceRepository.getUserWorkspaces(page, size, sortBy, sortDir)
            WorkspaceDataState.Success(networkResult.content, isFromCache = false)
        } catch (networkError: Exception) {
            // Fallback to cached data
            val cachedData = workspaceRepository.getLocalWorkspaces().firstOrNull() ?: emptyList()
            if (cachedData.isNotEmpty()) {
                WorkspaceDataState.Success(cachedData, isFromCache = true, networkError = networkError.message)
            } else {
                WorkspaceDataState.Error(networkError.message ?: "Failed to refresh workspaces")
            }
        }
    }
    
    /**
     * Get cached workspace data only (no network call)
     */
    fun getCachedWorkspaces(): Flow<WorkspaceDataState> = flow {
        try {
            workspaceRepository.getLocalWorkspaces().collect { workspaces ->
                if (workspaces.isNotEmpty()) {
                    emit(WorkspaceDataState.Success(workspaces, isFromCache = true))
                } else {
                    emit(WorkspaceDataState.Error("No cached data available"))
                }
            }
        } catch (error: Exception) {
            emit(WorkspaceDataState.Error(error.message ?: "Failed to load cached data"))
        }
    }
    
    /**
     * Clear all cached workspace data
     */
    suspend fun clearCache() {
        try {
            workspaceRepository.clearLocalWorkspaces()
        } catch (error: Exception) {
            throw Exception("Failed to clear cache: ${error.message}")
        }
    }
    
    /**
     * Search workspaces locally (cached data only)
     */
    suspend fun searchWorkspaces(query: String): Flow<List<Workspace>> = flow {
        try {
            workspaceRepository.searchWorkspacesLocally(query).collect { workspaces ->
                emit(workspaces)
            }
        } catch (error: Exception) {
            emit(emptyList())
        }
    }
}

/**
 * Represents different states of workspace data
 */
sealed class WorkspaceDataState {
    object Loading : WorkspaceDataState()
    
    data class Success(
        val workspaces: List<Workspace>,
        val isFromCache: Boolean = false,
        val networkError: String? = null // If there was a network error but we still have cached data
    ) : WorkspaceDataState()
    
    data class Error(val message: String) : WorkspaceDataState()
}

/**
 * Key for workspace requests containing pagination and sorting parameters
 */
data class WorkspaceRequestParams(
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: String = "updatedAt",
    val sortDir: String = "desc"
)