package com.ampairs.workspace

import AuthRoute
import Route
import WorkspaceRoute
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ampairs.workspace.ui.WorkspaceCreateScreen
import com.ampairs.workspace.ui.WorkspaceListScreen

fun NavGraphBuilder.workspaceNavigation(navController: NavController, onWorkspaceSelected: () -> Unit) {
    navigation<Route.Workspace>(startDestination = WorkspaceRoute.Root) {
        composable<WorkspaceRoute.Root> {
            WorkspaceListScreen(
                onNavigateToCreateWorkspace = {
                    navController.navigate(WorkspaceRoute.Create)
                },
                onWorkspaceSelected = { workspaceId ->
                    // Call the callback to navigate to main app
                    onWorkspaceSelected()
                },
                onEditProfile = {
                    navController.navigate(AuthRoute.UserUpdate)
                },
                onLogout = {
                    // Clear navigation stack and go to login
                    navController.navigate(Route.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<WorkspaceRoute.Create> {
            WorkspaceCreateScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onWorkspaceCreated = { workspaceId ->
                    // Call the callback to navigate to main app after creation
                    onWorkspaceSelected()
                }
            )
        }

        composable<WorkspaceRoute.Detail> { backStackEntry ->
            // This would be the workspace home/dashboard screen
            // For now, we'll call the callback to navigate to main app
            onWorkspaceSelected()
        }
    }
}