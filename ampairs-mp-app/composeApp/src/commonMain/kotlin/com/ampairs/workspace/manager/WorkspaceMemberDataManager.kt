package com.ampairs.workspace.manager

import com.ampairs.workspace.api.WorkspaceMemberApi
import com.ampairs.workspace.db.WorkspaceMemberRepository
import com.ampairs.workspace.domain.WorkspaceMember
import com.ampairs.common.flower_core.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.coroutineScope

/**
 * Data manager for workspace member data synchronization with offline-first approach.
 * This implementation provides automatic synchronization between network API 
 * and local Room database.
 */
class WorkspaceMemberDataManager(
    private val workspaceMemberApi: WorkspaceMemberApi,
    private val workspaceMemberRepository: WorkspaceMemberRepository,
) {
    
    /**
     * Get workspace members with offline-first approach.
     * Returns cached data immediately if available, then fetches fresh data from network.
     */
    fun getWorkspaceMembers(
        workspaceId: String,
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "joinedAt",
        sortDir: String = "desc",
        forceRefresh: Boolean = false
    ): Flow<WorkspaceMemberDataState> = flow {
        try {
            if (!forceRefresh) {
                // Emit loading state
                emit(WorkspaceMemberDataState.Loading)
                
                // First, get cached data
                val cachedMembers = workspaceMemberRepository.getLocalWorkspaceMembers(workspaceId).firstOrNull() ?: emptyList()
                if (cachedMembers.isNotEmpty()) {
                    emit(WorkspaceMemberDataState.Success(cachedMembers, isFromCache = true))
                }
                
                // Then try to fetch fresh data from network
                try {
                    val networkResult = workspaceMemberRepository.getWorkspaceMembers(workspaceId, page, size, sortBy, sortDir)
                    emit(WorkspaceMemberDataState.Success(networkResult.content, isFromCache = false))
                } catch (networkError: Exception) {
                    // If network fails but we have cached data, keep the cached data
                    if (cachedMembers.isNotEmpty()) {
                        emit(WorkspaceMemberDataState.Success(cachedMembers, isFromCache = true, networkError = networkError.message))
                    } else {
                        // No cached data and network failed
                        emit(WorkspaceMemberDataState.Error(networkError.message ?: "Failed to load workspace members"))
                    }
                }
            } else {
                // Force refresh - only fetch from network
                emit(WorkspaceMemberDataState.Loading)
                try {
                    val networkResult = workspaceMemberRepository.getWorkspaceMembers(workspaceId, page, size, sortBy, sortDir)
                    emit(WorkspaceMemberDataState.Success(networkResult.content, isFromCache = false))
                } catch (networkError: Exception) {
                    // Fallback to cached data on network error
                    val cachedMembers = workspaceMemberRepository.getLocalWorkspaceMembers(workspaceId).firstOrNull() ?: emptyList()
                    if (cachedMembers.isNotEmpty()) {
                        emit(WorkspaceMemberDataState.Success(cachedMembers, isFromCache = true, networkError = networkError.message))
                    } else {
                        emit(WorkspaceMemberDataState.Error(networkError.message ?: "Failed to refresh workspace members"))
                    }
                }
            }
        } catch (error: Exception) {
            emit(WorkspaceMemberDataState.Error(error.message ?: "Unexpected error occurred"))
        }
    }
    
    /**
     * Force refresh workspace member data from network
     */
    suspend fun refreshWorkspaceMembers(
        workspaceId: String,
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "joinedAt",
        sortDir: String = "desc"
    ): WorkspaceMemberDataState {
        return try {
            val networkResult = workspaceMemberRepository.getWorkspaceMembers(workspaceId, page, size, sortBy, sortDir)
            WorkspaceMemberDataState.Success(networkResult.content, isFromCache = false)
        } catch (networkError: Exception) {
            // Fallback to cached data
            val cachedData = workspaceMemberRepository.getLocalWorkspaceMembers(workspaceId).firstOrNull() ?: emptyList()
            if (cachedData.isNotEmpty()) {
                WorkspaceMemberDataState.Success(cachedData, isFromCache = true, networkError = networkError.message)
            } else {
                WorkspaceMemberDataState.Error(networkError.message ?: "Failed to refresh workspace members")
            }
        }
    }
    
    /**
     * Get cached workspace member data only (no network call)
     */
    fun getCachedWorkspaceMembers(workspaceId: String): Flow<WorkspaceMemberDataState> = flow {
        try {
            emit(WorkspaceMemberDataState.Loading)
            val members = workspaceMemberRepository.getLocalWorkspaceMembers(workspaceId).firstOrNull() ?: emptyList()
            if (members.isNotEmpty()) {
                emit(WorkspaceMemberDataState.Success(members, isFromCache = true))
            } else {
                emit(WorkspaceMemberDataState.Error("No cached data available"))
            }
        } catch (error: Exception) {
            emit(WorkspaceMemberDataState.Error(error.message ?: "Failed to load cached data"))
        }
    }
    
    /**
     * Get member details with offline-first approach
     */
    fun getMemberDetails(
        workspaceId: String,
        memberId: String,
        forceRefresh: Boolean = false
    ): Flow<WorkspaceMemberDataState> = flow {
        try {
            if (!forceRefresh) {
                // First try cached data
                val cachedMember = workspaceMemberRepository.getLocalWorkspaceMember(workspaceId, memberId)
                if (cachedMember != null) {
                    emit(WorkspaceMemberDataState.Success(listOf(cachedMember), isFromCache = true))
                }
            }
            
            // Fetch from network
            try {
                val networkMemberResult = workspaceMemberRepository.getMemberDetails(workspaceId, memberId)
                when (networkMemberResult.status) {
                    is Resource.Status.Success -> {
                        val networkMember = networkMemberResult.status.data
                        if (networkMember != null) {
                            emit(WorkspaceMemberDataState.Success(listOf(networkMember), isFromCache = false))
                        } else {
                            throw Exception("Member not found")
                        }
                    }
                    is Resource.Status.Error -> {
                        throw Exception(networkMemberResult.status.errorMessage)
                    }
                    is Resource.Status.Loading -> {
                        // Handle loading state if needed
                    }
                    is Resource.Status.EmptySuccess -> {
                        throw Exception("Member not found")
                    }
                }
            } catch (networkError: Exception) {
                val cachedMember = workspaceMemberRepository.getLocalWorkspaceMember(workspaceId, memberId)
                if (cachedMember != null) {
                    emit(WorkspaceMemberDataState.Success(listOf(cachedMember), isFromCache = true, networkError = networkError.message))
                } else {
                    emit(WorkspaceMemberDataState.Error(networkError.message ?: "Failed to load member details"))
                }
            }
        } catch (error: Exception) {
            emit(WorkspaceMemberDataState.Error(error.message ?: "Unexpected error occurred"))
        }
    }
    
    /**
     * Clear all cached workspace member data
     */
    suspend fun clearCache(workspaceId: String) {
        try {
            workspaceMemberRepository.clearLocalWorkspaceMembers(workspaceId)
        } catch (error: Exception) {
            throw Exception("Failed to clear cache: ${error.message}")
        }
    }
    
    /**
     * Search workspace members locally (cached data only)
     */
    suspend fun searchWorkspaceMembers(workspaceId: String, query: String): Flow<List<WorkspaceMember>> = flow {
        try {
            val members = workspaceMemberRepository.searchWorkspaceMembersLocally(workspaceId, query).firstOrNull() ?: emptyList()
            emit(members)
        } catch (error: Exception) {
            emit(emptyList())
        }
    }
}

/**
 * Represents different states of workspace member data
 */
sealed class WorkspaceMemberDataState {
    object Loading : WorkspaceMemberDataState()
    
    data class Success(
        val members: List<WorkspaceMember>,
        val isFromCache: Boolean = false,
        val networkError: String? = null // If there was a network error but we still have cached data
    ) : WorkspaceMemberDataState()
    
    data class Error(val message: String) : WorkspaceMemberDataState()
}

/**
 * Key for workspace member requests containing pagination and sorting parameters
 */
data class WorkspaceMemberRequestParams(
    val workspaceId: String,
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: String = "joinedAt",
    val sortDir: String = "desc"
)