package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.auth.db.UserRepository
import com.ampairs.workspace.db.OfflineFirstWorkspaceRepository
import com.ampairs.workspace.domain.Workspace
import org.mobilenativefoundation.store.store5.StoreReadResponse
import com.ampairs.workspace.ui.WorkspaceListState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class WorkspaceListViewModel(
    private val workspaceRepository: OfflineFirstWorkspaceRepository,
    private val userWorkspaceRepository: UserWorkspaceRepository,
    private val tokenRepository: TokenRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkspaceListState())
    val state: StateFlow<WorkspaceListState> = _state.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUserLoading = true)

            try {
                val user = userRepository.getUser()
                val fullName = if (user != null) {
                    "${user.first_name} ${user.last_name}".trim()
                } else {
                    "User"
                }

                _state.value = _state.value.copy(
                    userFullName = fullName,
                    isUserLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    userFullName = "User",
                    isUserLoading = false
                )
            }
        }
    }

    suspend fun selectWorkSpace(workspaceId: String) {
        val currentUserId = tokenRepository.getCurrentUserId()
        if (currentUserId != null) {
            userWorkspaceRepository.setWorkspaceIdForUser(currentUserId, workspaceId)
        }
    }

    fun loadWorkspaces(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null)
            
            try {
                val workspacesFlow = workspaceRepository.getUserWorkspaces(
                    page = 0,
                    size = 50, // Load more workspaces for the list
                    forceRefresh = forceRefresh
                )
                workspacesFlow.onEach { response ->
                    when (response) {
                        is StoreReadResponse.Loading -> {
                            // Show loading only when we don't have any cached data
                            if (_state.value.workspaces.isEmpty()) {
                                _state.value = _state.value.copy(isLoading = true)
                            }
                        }
                        is StoreReadResponse.Data -> {
                            val pageResult = response.value
                            _state.value = _state.value.copy(
                                workspaces = pageResult.content,
                                isLoading = false,
                                isRefreshing = false,
                                error = null,
                                hasNoWorkspaces = pageResult.isEmpty,
                                isOfflineMode = false // TODO: Check if data came from cache
                            )
                        }
                        is StoreReadResponse.Error.Exception -> {
                            _state.value = _state.value.copy(
                                error = response.error.message ?: "Failed to load workspaces",
                                isLoading = false,
                                isRefreshing = false,
                                isOfflineMode = true
                            )
                        }
                        is StoreReadResponse.Error.Message -> {
                            _state.value = _state.value.copy(
                                error = response.message,
                                isLoading = false,
                                isRefreshing = false,
                                isOfflineMode = true
                            )
                        }
                        else -> {
                            // Handle other response types if needed
                        }
                    }
                }.launchIn(this)
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to load workspaces",
                    isLoading = false,
                    isRefreshing = false,
                    isOfflineMode = true
                )
            }
        }
    }

    fun refreshWorkspaces() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true, error = null)
            
            try {
                val workspacesFlow = workspaceRepository.getUserWorkspaces(
                    page = 0,
                    size = 50,
                    forceRefresh = true
                )
                workspacesFlow.onEach { response ->
                    when (response) {
                        is StoreReadResponse.Data -> {
                            val pageResult = response.value
                            _state.value = _state.value.copy(
                                workspaces = pageResult.content,
                                isLoading = false,
                                isRefreshing = false,
                                error = null,
                                hasNoWorkspaces = pageResult.isEmpty,
                                isOfflineMode = false // TODO: Check if data came from cache
                            )
                        }
                        is StoreReadResponse.Error.Exception -> {
                            _state.value = _state.value.copy(
                                error = response.error.message ?: "Failed to refresh workspaces",
                                isLoading = false,
                                isRefreshing = false,
                                isOfflineMode = true
                            )
                        }
                        is StoreReadResponse.Error.Message -> {
                            _state.value = _state.value.copy(
                                error = response.message,
                                isLoading = false,
                                isRefreshing = false,
                                isOfflineMode = true
                            )
                        }
                        else -> {
                            // Handle other states
                        }
                    }
                }.launchIn(this)
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to refresh workspaces",
                    isLoading = false,
                    isRefreshing = false,
                    isOfflineMode = true
                )
            }
        }
    }

    fun searchWorkspaces(query: String) {
        _state.value = _state.value.copy(searchQuery = query)

        viewModelScope.launch {
            try {
                if (query.isBlank()) {
                    // Load all workspaces when search is empty
                    loadWorkspaces(forceRefresh = false)
                } else {
                    // Use repository's search functionality
                    workspaceRepository.searchWorkspaces(query, page = 0, size = 50)
                        .onEach { workspaces ->
                            _state.value = _state.value.copy(
                                workspaces = workspaces,
                                isLoading = false,
                                error = null
                            )
                        }.launchIn(this)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to search workspaces",
                    isLoading = false
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun logout() {
        viewModelScope.launch {
            // Store5 handles cache clearing automatically
            tokenRepository.clearTokens()
        }
    }
    
    /**
     * Get cached workspaces only (useful for offline mode)
     */
    fun loadCachedWorkspaces() {
        viewModelScope.launch {
            try {
                val workspacesFlow = workspaceRepository.getCachedWorkspaces(
                    page = 0,
                    size = 50
                )
                workspacesFlow.onEach { response ->
                    when (response) {
                        is StoreReadResponse.Data -> {
                            val pageResult = response.value
                            _state.value = _state.value.copy(
                                workspaces = pageResult.content,
                                isLoading = false,
                                error = null,
                                hasNoWorkspaces = pageResult.isEmpty,
                                isOfflineMode = true
                            )
                        }
                        is StoreReadResponse.Error.Exception -> {
                            _state.value = _state.value.copy(
                                error = "No cached data available",
                                isLoading = false,
                                isOfflineMode = true
                            )
                        }
                        is StoreReadResponse.Error.Message -> {
                            _state.value = _state.value.copy(
                                error = "No cached data available",
                                isLoading = false,
                                isOfflineMode = true
                            )
                        }
                        is StoreReadResponse.Loading -> {
                            _state.value = _state.value.copy(isLoading = true)
                        }
                        else -> {
                            // Handle other states
                        }
                    }
                }.launchIn(this)
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "No cached data available",
                    isLoading = false,
                    isOfflineMode = true
                )
            }
        }
    }
}