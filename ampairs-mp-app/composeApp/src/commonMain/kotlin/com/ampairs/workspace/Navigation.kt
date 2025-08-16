package com.ampairs.workspace

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ampairs.workspace.ui.WorkspaceCreateScreen
import com.ampairs.workspace.ui.WorkspaceListScreen
import kotlinx.serialization.Serializable

object WorkspaceRoutes {
    @Serializable
    object WorkspaceList

    @Serializable
    object WorkspaceCreate

    @Serializable
    data class WorkspaceDetail(val workspaceId: String)
}

fun NavGraphBuilder.workspaceNavigation(navController: NavController) {
    composable<WorkspaceRoutes.WorkspaceList> {
        WorkspaceListScreen(
            onNavigateToCreateWorkspace = {
                navController.navigate(WorkspaceRoutes.WorkspaceCreate)
            },
            onWorkspaceSelected = { workspaceId ->
                // Navigate to workspace home/dashboard
                // This would navigate to the main app flow with the selected workspace
                navController.navigate(WorkspaceRoutes.WorkspaceDetail(workspaceId)) {
                    // Clear the workspace selection stack
                    popUpTo(WorkspaceRoutes.WorkspaceList) { inclusive = true }
                }
            }
        )
    }

    composable<WorkspaceRoutes.WorkspaceCreate> {
        WorkspaceCreateScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onWorkspaceCreated = { workspaceId ->
                // Navigate to workspace home and clear the stack
                navController.navigate(WorkspaceRoutes.WorkspaceDetail(workspaceId)) {
                    popUpTo(WorkspaceRoutes.WorkspaceList) { inclusive = true }
                }
            }
        )
    }

    composable<WorkspaceRoutes.WorkspaceDetail> { backStackEntry ->
        // This would be the workspace home/dashboard screen
        // For now, we'll just navigate back to workspace list
        // In a real app, this would be the main workspace dashboard

        // Placeholder - this should be replaced with actual workspace dashboard
        WorkspaceListScreen(
            onNavigateToCreateWorkspace = {
                navController.navigate(WorkspaceRoutes.WorkspaceCreate)
            },
            onWorkspaceSelected = { workspaceId ->
                // Already in workspace, maybe just switch workspace
                navController.navigate(WorkspaceRoutes.WorkspaceDetail(workspaceId)) {
                    popUpTo(WorkspaceRoutes.WorkspaceDetail) { inclusive = true }
                }
            }
        )
    }
}