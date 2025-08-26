package com.ampairs.workspace

import AuthRoute
import Route
import WorkspaceRoute
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ampairs.common.ui.AppScreenWithHeader
import com.ampairs.workspace.ui.WorkspaceCreateScreen
import com.ampairs.workspace.ui.WorkspaceListScreen
import com.ampairs.workspace.ui.OfflineFirstWorkspaceListScreen

fun NavGraphBuilder.workspaceNavigation(navController: NavController, onWorkspaceSelected: () -> Unit) {
    navigation<Route.Workspace>(startDestination = WorkspaceRoute.StoreRoot) {
        // Original workspace list screen
        composable<WorkspaceRoute.Root> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = true
            ) { paddingValues ->
                WorkspaceListScreen(
                    onNavigateToCreateWorkspace = {
                        navController.navigate(WorkspaceRoute.Create)
                    },
                    onWorkspaceSelected = { workspaceId ->
                        // Call the callback to navigate to main app
                        onWorkspaceSelected()
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        
        // New offline-first workspace list screen (default)
        composable<WorkspaceRoute.StoreRoot> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = true
            ) { paddingValues ->
                OfflineFirstWorkspaceListScreen(
                    onNavigateToCreateWorkspace = {
                        navController.navigate(WorkspaceRoute.Create)
                    },
                    onWorkspaceSelected = { workspaceId ->
                        // Call the callback to navigate to main app
                        onWorkspaceSelected()
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        composable<WorkspaceRoute.Create> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = true
            ) { paddingValues ->
                WorkspaceCreateScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onWorkspaceCreated = { workspaceId ->
                        // Call the callback to navigate to main app after creation
                        onWorkspaceSelected()
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        composable<WorkspaceRoute.Detail> { backStackEntry ->
            // This would be the workspace home/dashboard screen
            // For now, we'll call the callback to navigate to main app
            onWorkspaceSelected()
        }
    }
}