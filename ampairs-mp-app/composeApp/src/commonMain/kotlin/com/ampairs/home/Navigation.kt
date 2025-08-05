package com.ampairs.home

import HomeRoute
import Route
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation

fun NavGraphBuilder.homeNavigation(navigator: NavController, onNavPath: (NavItem) -> Unit) {

    navigation<Route.Home>(startDestination = HomeRoute.Root) {
        composable<HomeRoute.Root> {
            HomeScreen(onNavPath)
        }
    }
}