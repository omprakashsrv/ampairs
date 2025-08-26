package com.ampairs.workspace.ui

import com.ampairs.workspace.domain.Workspace

data class OfflineFirstWorkspaceListState(
    val workspaces: List<Workspace> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val hasNoWorkspaces: Boolean = false,
    val userFullName: String = "User",
    val isUserLoading: Boolean = false,
    val isOfflineMode: Boolean = false // Indicates if we're in offline mode due to network issues
)