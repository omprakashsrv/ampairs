package com.ampairs.common.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreenLayout(
    currentWorkspaceName: String?,
    userFullName: String,
    isUserLoading: Boolean = false,
    isWorkspaceLoading: Boolean = false,
    onWorkspaceClick: () -> Unit,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onSwitchUser: () -> Unit,
    modifier: Modifier = Modifier,
    isWorkspaceSelection: Boolean = false,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AppHeader(
                currentWorkspaceName = currentWorkspaceName,
                userFullName = userFullName,
                isUserLoading = isUserLoading,
                isWorkspaceLoading = isWorkspaceLoading,
                onWorkspaceClick = onWorkspaceClick,
                onEditProfile = onEditProfile,
                onLogout = onLogout,
                onSwitchUser = onSwitchUser,
                isWorkspaceSelection = isWorkspaceSelection
            )
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}