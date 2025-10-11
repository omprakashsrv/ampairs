package com.ampairs.business

import BusinessRoute
import Route
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ampairs.common.ui.AppScreenWithHeader
import com.ampairs.business.ui.BusinessProfileScreen
import com.ampairs.business.ui.BusinessSettingsScreen
import com.ampairs.business.ui.BusinessBrandingScreen

/**
 * Business module navigation graph.
 * Maps backend routes to mobile screens:
 * - /business/profile → BusinessRoute.Profile (default)
 * - /business/settings → BusinessRoute.Settings
 * - /business/branding → BusinessRoute.Branding
 */
fun NavGraphBuilder.businessNavigation(
    navController: NavHostController
) {
    navigation<Route.Business>(startDestination = BusinessRoute.Profile) {
        // Main Business Profile Screen (default view)
        composable<BusinessRoute.Profile> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                BusinessProfileScreen(
                    onNavigateToSettings = {
                        navController.navigate(BusinessRoute.Settings)
                    },
                    onNavigateToBranding = {
                        navController.navigate(BusinessRoute.Branding)
                    },
                    onNavigateToEdit = {
                        navController.navigate(BusinessRoute.Settings)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Business Settings Screen
        composable<BusinessRoute.Settings> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                BusinessSettingsScreen(
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Business Branding Screen
        composable<BusinessRoute.Branding> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                BusinessBrandingScreen(
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}
