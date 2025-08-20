package com.ampairs.home

import HomeRoute
import Route
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ampairs.common.ui.AppScreenWithHeader

fun NavGraphBuilder.homeNavigation(navigator: NavController, onNavPath: (NavItem) -> Unit) {

    navigation<Route.Home>(startDestination = HomeRoute.Root) {
        composable<HomeRoute.Root> {
            AppScreenWithHeader(navController = navigator) { paddingValues ->
                // Apply padding to the home screen content
                Column(
                    modifier = Modifier.padding(paddingValues)
                ) {
                    HomeScreen(onNavPath)
                }
            }
        }
    }
}