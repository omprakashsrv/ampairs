package com.ampairs.common.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreenLayout(
    navController: NavController,
    currentWorkspaceName: String?,
    currentWorkspaceId: String?,
    userFullName: String,
    isUserLoading: Boolean = false,
    isWorkspaceLoading: Boolean = false,
    onWorkspaceClick: () -> Unit,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onSwitchUser: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AppHeader(
                navController = navController,
                currentWorkspaceName = currentWorkspaceName,
                currentWorkspaceId = currentWorkspaceId,
                userFullName = userFullName,
                isUserLoading = isUserLoading,
                isWorkspaceLoading = isWorkspaceLoading,
                onWorkspaceClick = onWorkspaceClick,
                onEditProfile = onEditProfile,
                onLogout = onLogout,
                onSwitchUser = onSwitchUser,
            )
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}