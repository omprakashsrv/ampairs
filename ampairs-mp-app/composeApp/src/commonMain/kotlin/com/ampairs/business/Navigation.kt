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

/**
 * Placeholder screens - to be implemented
 */
@Composable
private fun BusinessSettingsScreen(modifier: Modifier = Modifier) {
    // TODO: Implement business settings UI
    androidx.compose.material3.Text(
        "Business Settings Screen",
        modifier = modifier
    )
}

@Composable
private fun BusinessBrandingScreen(modifier: Modifier = Modifier) {
    // TODO: Implement business branding UI
    androidx.compose.material3.Text(
        "Business Branding Screen",
        modifier = modifier
    )
}
