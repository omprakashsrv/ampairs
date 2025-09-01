package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.auth.db.UserRepository
import com.ampairs.workspace.store.WorkspaceStore
import com.ampairs.workspace.store.WorkspaceKey
import com.ampairs.workspace.domain.Workspace
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import com.ampairs.workspace.ui.WorkspaceListState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class WorkspaceListViewModel(
    private val workspaceStore: WorkspaceStore,
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
                val userId = tokenRepository.getCurrentUserId() 
                    ?: throw Exception("User not authenticated")
                
                val key = WorkspaceKey(workspaceId = null, userId = userId)
                val request = if (forceRefresh) {
                    StoreReadRequest.fresh(key)
                } else {
                    StoreReadRequest.cached(key, refresh = true)
                }
                
                workspaceStore.stream(request).onEach { response ->
                    when (response) {
                        is StoreReadResponse.Loading -> {
                            // Show loading only when we don't have any cached data
                            if (_state.value.workspaces.isEmpty()) {
                                _state.value = _state.value.copy(isLoading = true)
                            }
                        }
                        is StoreReadResponse.Data -> {
                            _state.value = _state.value.copy(
                                workspaces = response.value,
                                isLoading = false,
                                isRefreshing = false,
                                error = null,
                                hasNoWorkspaces = response.value.isEmpty(),
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
                val userId = tokenRepository.getCurrentUserId() 
                    ?: throw Exception("User not authenticated")
                
                val key = WorkspaceKey(workspaceId = null, userId = userId)
                val request = StoreReadRequest.fresh(key)
                
                workspaceStore.stream(request).onEach { response ->
                    when (response) {
                        is StoreReadResponse.Data -> {
                            _state.value = _state.value.copy(
                                workspaces = response.value,
                                isLoading = false,
                                isRefreshing = false,
                                error = null,
                                hasNoWorkspaces = response.value.isEmpty(),
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
                val userId = tokenRepository.getCurrentUserId() 
                    ?: throw Exception("User not authenticated")
                
                val key = WorkspaceKey(workspaceId = null, userId = userId)
                val request = StoreReadRequest.cached(key, refresh = false)
                
                workspaceStore.stream(request).onEach { response ->
                    when (response) {
                        is StoreReadResponse.Data -> {
                            // Filter workspaces locally based on search query
                            val filteredWorkspaces = if (query.isBlank()) {
                                response.value
                            } else {
                                response.value.filter { workspace ->
                                    workspace.name.contains(query, ignoreCase = true) ||
                                    workspace.description?.contains(query, ignoreCase = true) == true
                                }
                            }
                            
                            _state.value = _state.value.copy(
                                workspaces = filteredWorkspaces,
                                isLoading = false,
                                error = null
                            )
                        }
                        is StoreReadResponse.Error.Exception -> {
                            _state.value = _state.value.copy(
                                error = response.error.message ?: "Failed to search workspaces",
                                isLoading = false
                            )
                        }
                        is StoreReadResponse.Error.Message -> {
                            _state.value = _state.value.copy(
                                error = response.message,
                                isLoading = false
                            )
                        }
                        else -> {
                            // Handle other states
                        }
                    }
                }.launchIn(this)
                
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
                val userId = tokenRepository.getCurrentUserId() 
                    ?: throw Exception("User not authenticated")
                
                val key = WorkspaceKey(workspaceId = null, userId = userId)
                // Use cached-only request to avoid network calls
                val request = StoreReadRequest.cached(key, refresh = false)
                
                workspaceStore.stream(request).onEach { response ->
                    when (response) {
                        is StoreReadResponse.Data -> {
                            _state.value = _state.value.copy(
                                workspaces = response.value,
                                isLoading = false,
                                error = null,
                                hasNoWorkspaces = response.value.isEmpty(),
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