package com.ampairs.workspace

import Route
import WorkspaceRoute
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.ampairs.common.ui.AppScreenWithHeader
import com.ampairs.workspace.ui.MemberDetailsScreen
import com.ampairs.workspace.ui.WorkspaceCreateScreen
import com.ampairs.workspace.ui.WorkspaceInvitationCreateScreen
import com.ampairs.workspace.ui.WorkspaceInvitationsScreen
import com.ampairs.workspace.ui.WorkspaceListScreen
import com.ampairs.workspace.ui.WorkspaceMembersScreen
import com.ampairs.workspace.ui.WorkspaceModulesScreen

fun NavGraphBuilder.workspaceNavigation(
    navController: NavHostController,
    onNavigationServiceReady: ((com.ampairs.workspace.navigation.DynamicModuleNavigationService?) -> Unit)? = null,
    onWorkspaceSelected: () -> Unit
) {
    // Create navigation service at workspace level when needed
    var workspaceNavigationService: com.ampairs.workspace.navigation.DynamicModuleNavigationService? = null
    navigation<Route.Workspace>(startDestination = WorkspaceRoute.Root) {
        
        // Workspace list screen (with offline-first data synchronization)
        composable<WorkspaceRoute.Root> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = true
            ) { paddingValues ->
                WorkspaceListScreen(
                    onNavigateToCreateWorkspace = {
                        navController.navigate(WorkspaceRoute.Create)
                    },
                    onWorkspaceSelected = { workspaceId: String ->
                        // Navigate to modules list for the selected workspace
                        navController.navigate(WorkspaceRoute.Modules(workspaceId))
                    },
                    onWorkspaceEdit = { workspaceId: String ->
                        navController.navigate(WorkspaceRoute.Edit(workspaceId))
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
        
        composable<WorkspaceRoute.Edit> { backStackEntry ->
            val editRoute = backStackEntry.toRoute<WorkspaceRoute.Edit>()
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = true
            ) { paddingValues ->
                WorkspaceCreateScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onWorkspaceCreated = { workspaceId ->
                        // After update, go back to the list
                        navController.popBackStack()
                    },
                    workspaceId = editRoute.workspaceId,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        composable<WorkspaceRoute.Detail> { backStackEntry ->
            // This would be the workspace home/dashboard screen
            // For now, we'll call the callback to navigate to main app
            onWorkspaceSelected()
        }

        // Workspace members management screen
        composable<WorkspaceRoute.Members> { backStackEntry ->
            val membersRoute = backStackEntry.toRoute<WorkspaceRoute.Members>()
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                WorkspaceMembersScreen(
                    workspaceId = membersRoute.workspaceId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onMemberClick = { memberId ->
                        navController.navigate(WorkspaceRoute.MemberDetail(membersRoute.workspaceId, memberId))
                    },
                    onInviteClick = {
                        navController.navigate(WorkspaceRoute.CreateInvitation(membersRoute.workspaceId))
                    }
                )
            }
        }

        // Workspace invitations management screen
        composable<WorkspaceRoute.Invitations> { backStackEntry ->
            val invitationsRoute = backStackEntry.toRoute<WorkspaceRoute.Invitations>()
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                WorkspaceInvitationsScreen(
                    workspaceId = invitationsRoute.workspaceId,
                    onInviteClick = {
                        navController.navigate(WorkspaceRoute.CreateInvitation(invitationsRoute.workspaceId))
                    }
                )
            }
        }

        // Member detail screen
        composable<WorkspaceRoute.MemberDetail> { backStackEntry ->
            val memberDetailRoute = backStackEntry.toRoute<WorkspaceRoute.MemberDetail>()
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                MemberDetailsScreen(
                    workspaceId = memberDetailRoute.workspaceId,
                    memberId = memberDetailRoute.memberId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        composable<WorkspaceRoute.CreateInvitation> { backStackEntry ->
            val createInvitationRoute = backStackEntry.toRoute<WorkspaceRoute.CreateInvitation>()
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                WorkspaceInvitationCreateScreen(
                    workspaceId = createInvitationRoute.workspaceId,
                    onNavigateBack = { navController.navigateUp() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Workspace modules management screen
        composable<WorkspaceRoute.Modules> { backStackEntry ->
            val modulesRoute = backStackEntry.toRoute<WorkspaceRoute.Modules>()
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                WorkspaceModulesScreen(
                    navController = navController,
                    onModuleSelected = { moduleCode ->
                        // Navigate to main app after module selection
                        onWorkspaceSelected()
                    },
                    onNavigationServiceReady = onNavigationServiceReady,
                    workspaceId = modulesRoute.workspaceId,
                    showStoreByDefault = modulesRoute.showStoreByDefault,
                    paddingValues = paddingValues
                )
            }
        }

        // Accept invitation screen (public access)
        composable<WorkspaceRoute.AcceptInvitation> { backStackEntry ->
            val acceptInvitationRoute = backStackEntry.toRoute<WorkspaceRoute.AcceptInvitation>()
            // TODO: Implement accept invitation screen
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                // Placeholder - implement accept invitation screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Accept Invitation Screen - Coming Soon")
                }
            }
        }
    }
}