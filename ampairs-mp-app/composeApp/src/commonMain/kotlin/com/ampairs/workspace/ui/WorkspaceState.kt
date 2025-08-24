package com.ampairs.workspace.ui

import com.ampairs.workspace.domain.Workspace

data class WorkspaceListState(
    val workspaces: List<Workspace> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val hasNoWorkspaces: Boolean = false,
    val userFullName: String = "",
    val isUserLoading: Boolean = false,
)

data class WorkspaceCreateState(
    val name: String = "",
    val slug: String = "",
    val description: String = "",
    val workspaceType: String = "BUSINESS",
    val avatarUrl: String? = null,
    val timezone: String = "UTC",
    val language: String = "en",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSlugChecking: Boolean = false,
    val isSlugAvailable: Boolean = true,
    val isSlugModified: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap(),
    val createdWorkspaceId: String? = null,
)

data class WorkspaceDetailState(
    val workspace: Workspace? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,
)