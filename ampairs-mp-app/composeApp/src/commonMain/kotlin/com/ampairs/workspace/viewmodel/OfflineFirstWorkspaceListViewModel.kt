package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.auth.db.UserRepository
import com.ampairs.workspace.manager.OfflineFirstWorkspaceManager
import com.ampairs.workspace.manager.WorkspaceDataState
import com.ampairs.workspace.ui.OfflineFirstWorkspaceListState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class OfflineFirstWorkspaceListViewModel(
    private val workspaceManager: OfflineFirstWorkspaceManager,
    private val userWorkspaceRepository: UserWorkspaceRepository,
    private val tokenRepository: TokenRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OfflineFirstWorkspaceListState())
    val state: StateFlow<OfflineFirstWorkspaceListState> = _state.asStateFlow()

    init {
        loadUserData()
        loadWorkspaces()
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

            workspaceManager.getWorkspaces(
                page = 0,
                size = 100, // Load more workspaces for better offline experience
                forceRefresh = forceRefresh
            ).onEach { dataState ->
                when (dataState) {
                    is WorkspaceDataState.Loading -> {
                        // Show loading only when we don't have any cached data
                        if (_state.value.workspaces.isEmpty()) {
                            _state.value = _state.value.copy(isLoading = true)
                        }
                    }
                    is WorkspaceDataState.Success -> {
                        _state.value = _state.value.copy(
                            workspaces = dataState.workspaces,
                            isLoading = false,
                            isRefreshing = false,
                            error = dataState.networkError, // Show network error if exists but keep data
                            hasNoWorkspaces = dataState.workspaces.isEmpty(),
                            isOfflineMode = dataState.isFromCache && dataState.networkError != null
                        )
                    }
                    is WorkspaceDataState.Error -> {
                        _state.value = _state.value.copy(
                            error = dataState.message,
                            isLoading = false,
                            isRefreshing = false,
                            isOfflineMode = true
                        )
                    }
                }
            }.launchIn(this)
        }
    }

    fun refreshWorkspaces() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true, error = null)
            
            val dataState = workspaceManager.refreshWorkspaces()
            
            when (dataState) {
                is WorkspaceDataState.Success -> {
                    _state.value = _state.value.copy(
                        workspaces = dataState.workspaces,
                        isLoading = false,
                        isRefreshing = false,
                        error = dataState.networkError,
                        hasNoWorkspaces = dataState.workspaces.isEmpty(),
                        isOfflineMode = dataState.isFromCache && dataState.networkError != null
                    )
                }
                is WorkspaceDataState.Error -> {
                    _state.value = _state.value.copy(
                        error = dataState.message,
                        isLoading = false,
                        isRefreshing = false,
                        isOfflineMode = true
                    )
                }
                is WorkspaceDataState.Loading -> {
                    // Should not happen in refreshWorkspaces but handle anyway
                    _state.value = _state.value.copy(isRefreshing = true)
                }
            }
        }
    }

    fun searchWorkspaces(query: String) {
        _state.value = _state.value.copy(searchQuery = query)

        if (query.isEmpty()) {
            loadWorkspaces()
        } else {
            viewModelScope.launch {
                workspaceManager.searchWorkspaces(query)
                    .onEach { workspaces ->
                        _state.value = _state.value.copy(
                            workspaces = workspaces,
                            isLoading = false,
                            error = null
                        )
                    }
                    .launchIn(this)
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun logout() {
        viewModelScope.launch {
            workspaceManager.clearCache()
            tokenRepository.clearTokens()
        }
    }
    
    /**
     * Get cached workspaces only (useful for offline mode)
     */
    fun loadCachedWorkspaces() {
        viewModelScope.launch {
            workspaceManager.getCachedWorkspaces()
                .onEach { dataState ->
                    when (dataState) {
                        is WorkspaceDataState.Success -> {
                            _state.value = _state.value.copy(
                                workspaces = dataState.workspaces,
                                isLoading = false,
                                error = null,
                                hasNoWorkspaces = dataState.workspaces.isEmpty(),
                                isOfflineMode = true
                            )
                        }
                        is WorkspaceDataState.Error -> {
                            _state.value = _state.value.copy(
                                error = "No cached data available",
                                isLoading = false,
                                isOfflineMode = true
                            )
                        }
                        is WorkspaceDataState.Loading -> {
                            _state.value = _state.value.copy(isLoading = true)
                        }
                    }
                }
                .launchIn(this)
        }
    }
}