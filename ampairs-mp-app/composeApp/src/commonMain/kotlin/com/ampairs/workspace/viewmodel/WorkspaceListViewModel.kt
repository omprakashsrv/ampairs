package com.ampairs.workspace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.workspace.db.WorkspaceRepository
import com.ampairs.workspace.ui.WorkspaceListState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class WorkspaceListViewModel(
    private val workspaceRepository: WorkspaceRepository,
    private val tokenRepository: TokenRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkspaceListState())
    val state: StateFlow<WorkspaceListState> = _state.asStateFlow()

    init {
        loadWorkspaces()
    }

    fun loadWorkspaces(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                if (forceRefresh) {
                    // Fetch from network
                    val result = workspaceRepository.getUserWorkspaces()
                    _state.value = _state.value.copy(
                        workspaces = result.content,
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                        hasNoWorkspaces = result.content.isEmpty()
                    )
                } else {
                    // Load from local database first
                    workspaceRepository.getLocalWorkspaces()
                        .onEach { workspaces ->
                            _state.value = _state.value.copy(
                                workspaces = workspaces,
                                isLoading = false,
                                error = null,
                                hasNoWorkspaces = workspaces.isEmpty()
                            )

                            // If no local data, fetch from network
                            if (workspaces.isEmpty()) {
                                refreshWorkspaces()
                            }
                        }
                        .launchIn(this)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to load workspaces",
                    isLoading = false,
                    isRefreshing = false
                )
            }
        }
    }

    fun refreshWorkspaces() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true, error = null)

            try {
                val result = workspaceRepository.getUserWorkspaces()
                _state.value = _state.value.copy(
                    workspaces = result.content,
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    hasNoWorkspaces = result.content.isEmpty()
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to refresh workspaces",
                    isLoading = false,
                    isRefreshing = false
                )
            }
        }
    }

    fun searchWorkspaces(query: String) {
        _state.value = _state.value.copy(searchQuery = query)

        if (query.isEmpty()) {
            loadWorkspaces()
        } else {
            viewModelScope.launch {
                workspaceRepository.searchWorkspacesLocally(query)
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

    fun selectWorkspace(workspaceId: String) {
        viewModelScope.launch {
            // Store selected workspace ID for API calls
            tokenRepository.setCompanyId(workspaceId)

            // Navigation will be handled in the UI
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun logout() {
        viewModelScope.launch {
            tokenRepository.clearTokens()
            workspaceRepository.clearLocalWorkspaces()
        }
    }
}