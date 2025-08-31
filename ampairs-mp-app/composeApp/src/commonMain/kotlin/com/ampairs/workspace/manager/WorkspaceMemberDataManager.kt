package com.ampairs.workspace.manager

import com.ampairs.workspace.db.WorkspaceMemberRepository
import com.ampairs.workspace.domain.WorkspaceMember
import kotlinx.coroutines.flow.*

/**
 * Data manager for workspace member data synchronization with offline-first approach.
 * This implementation provides automatic synchronization between network API 
 * and local Room database.
 */
class WorkspaceMemberDataManager(
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
        emit(WorkspaceMemberDataState.Loading)

        if (!forceRefresh) {
            val cachedMembers = workspaceMemberRepository.getLocalWorkspaceMembers(workspaceId).firstOrNull() ?: emptyList()
            if (cachedMembers.isNotEmpty()) {
                emit(WorkspaceMemberDataState.Success(cachedMembers, isFromCache = true))
            }
        }

        val networkResult = workspaceMemberRepository.getWorkspaceMembers(workspaceId, page, size, sortBy, sortDir)
        emit(WorkspaceMemberDataState.Success(networkResult.content, isFromCache = false))
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
        val networkResult = workspaceMemberRepository.getWorkspaceMembers(workspaceId, page, size, sortBy, sortDir)
        return WorkspaceMemberDataState.Success(networkResult.content, isFromCache = false)
    }

    /**
     * Get cached workspace member data only (no network call)
     */
    fun getCachedWorkspaceMembers(workspaceId: String): Flow<WorkspaceMemberDataState> = flow {
        emit(WorkspaceMemberDataState.Loading)
        val members = workspaceMemberRepository.getLocalWorkspaceMembers(workspaceId).firstOrNull() ?: emptyList()
        if (members.isNotEmpty()) {
            emit(WorkspaceMemberDataState.Success(members, isFromCache = true))
        } else {
            emit(WorkspaceMemberDataState.Error("No cached data available"))
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
        if (!forceRefresh) {
            val cachedMember = workspaceMemberRepository.getLocalWorkspaceMember(workspaceId, memberId)
            if (cachedMember != null) {
                emit(WorkspaceMemberDataState.Success(listOf(cachedMember), isFromCache = true))
            }
        }

        val networkMember = workspaceMemberRepository.getMemberDetails(workspaceId, memberId)
        emit(WorkspaceMemberDataState.Success(listOf(networkMember), isFromCache = false))
    }

    /**
     * Clear all cached workspace member data
     */
    suspend fun clearCache(workspaceId: String) {
        workspaceMemberRepository.clearLocalWorkspaceMembers(workspaceId)
    }

    /**
     * Search workspace members locally (cached data only)
     */
    fun searchWorkspaceMembers(workspaceId: String, query: String): Flow<List<WorkspaceMember>> = flow {
        val members = workspaceMemberRepository.searchWorkspaceMembersLocally(workspaceId, query).firstOrNull() ?: emptyList()
        emit(members)
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