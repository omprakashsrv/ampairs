package com.ampairs.common.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.db.UserRepository
import com.ampairs.auth.domain.UserInfo
import com.ampairs.common.state.AppHeaderStateManager
import com.ampairs.workspace.db.WorkspaceRepository
import com.ampairs.workspace.domain.WorkspaceList
import kotlinx.coroutines.flow.firstOrNull
import org.koin.compose.koinInject

@Composable
fun AppScreenWithHeader(
    navController: NavController,
    modifier: Modifier = Modifier,
    isWorkspaceSelection: Boolean = false,
    content: @Composable (PaddingValues) -> Unit
) {
    val userRepository: UserRepository = koinInject()
    val workspaceRepository: WorkspaceRepository = koinInject()
    val tokenRepository: TokenRepository = koinInject()
    val headerStateManager = remember { AppHeaderStateManager.instance }
    val headerState by headerStateManager.headerState.collectAsState()

    // Track workspace selection changes and refresh header accordingly
    var currentWorkspaceId by remember { mutableStateOf("") }

    // Poll for workspace selection changes
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val selectedWorkspaceId = tokenRepository.getCompanyId()
                if (selectedWorkspaceId != currentWorkspaceId) {
                    currentWorkspaceId = selectedWorkspaceId
                }
            } catch (e: Exception) {
                // Ignore polling errors
            }
            kotlinx.coroutines.delay(1000) // Check every second
        }
    }

    // Initialize header data on first composition and when workspace changes
    LaunchedEffect(currentWorkspaceId) {
        try {
            // Load current user
            userRepository.getUser()?.let { userEntity ->
                val userInfo = UserInfo(
                    id = userEntity.id,
                    firstName = userEntity.first_name,
                    lastName = userEntity.last_name,
                    userName = userEntity.user_name,
                    countryCode = userEntity.country_code,
                    phone = userEntity.phone,
                    lastLogin = 0L, // Not available in current UserEntity
                    loginCount = 0, // Not available in current UserEntity
                    isAuthenticated = true,
                    hasSelectedWorkspace = true
                )
                headerStateManager.updateUser(userInfo)
            }

            // Load currently selected workspace
            val selectedWorkspaceId = tokenRepository.getCompanyId()
            if (selectedWorkspaceId.isNotEmpty()) {
                // Load the selected workspace by ID
                workspaceRepository.getWorkspaceById(selectedWorkspaceId)?.let { workspace ->
                    val workspaceList = WorkspaceList(
                        id = workspace.id,
                        name = workspace.name,
                        slug = workspace.slug,
                        description = workspace.description,
                        workspaceType = workspace.workspaceType,
                        avatarUrl = workspace.avatarUrl,
                        subscriptionPlan = workspace.subscriptionPlan,
                        memberCount = workspace.memberCount ?: 1,
                        lastActivityAt = workspace.lastActivityAt,
                        createdAt = workspace.createdAt
                    )
                    headerStateManager.updateWorkspace(workspaceList)
                }
            } else {
                // Fallback: load first workspace if no workspace is selected
                workspaceRepository.getLocalWorkspaces().firstOrNull()?.let { workspaces ->
                    workspaces.firstOrNull()?.let { workspace ->
                        val workspaceList = WorkspaceList(
                            id = workspace.id,
                            name = workspace.name,
                            slug = workspace.slug,
                            description = workspace.description,
                            workspaceType = workspace.workspaceType,
                            avatarUrl = workspace.avatarUrl,
                            subscriptionPlan = workspace.subscriptionPlan,
                            memberCount = workspace.memberCount ?: 1,
                            lastActivityAt = workspace.lastActivityAt,
                            createdAt = workspace.createdAt
                        )
                        headerStateManager.updateWorkspace(workspaceList)
                    }
                }
            }
        } catch (e: Exception) {
            // Handle error gracefully
            println("Error loading header data: ${e.message}")
        }
    }
    
    AppScreenLayout(
        currentWorkspaceName = if (isWorkspaceSelection) null else headerState.currentWorkspace?.name,
        userFullName = "${headerState.currentUser?.firstName ?: ""} ${headerState.currentUser?.lastName ?: ""}".trim().ifEmpty { "User" },
        isUserLoading = headerState.isUserLoading,
        isWorkspaceLoading = headerState.isWorkspaceLoading,
        onWorkspaceClick = {
            if (!isWorkspaceSelection) {
                navController.navigate(Route.Workspace)
            }
        },
        onEditProfile = {
            navController.navigate(AuthRoute.UserUpdate)
        },
        onLogout = {
            // Clear user data and navigate to login
            headerStateManager.reset()
            navController.navigate(Route.Login) {
                popUpTo(0)
            }
        },
        onSwitchUser = {
            // Use a coroutine to clear current user before navigation
            kotlinx.coroutines.runBlocking {
                // Clear the current user so they stay on user selection screen
                tokenRepository.clearCurrentUser()
            }
            // Clear header state before navigation to prevent scope issues
            headerStateManager.reset()
            navController.navigate(AuthRoute.UserSelection) {
                // Clear the back stack to prevent navigation issues
                popUpTo(0) { inclusive = false }
            }
        },
        modifier = modifier,
        isWorkspaceSelection = isWorkspaceSelection,
        content = content
    )
}