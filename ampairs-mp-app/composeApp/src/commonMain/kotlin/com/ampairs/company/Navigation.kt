package com.ampairs.company

import CompanyRoute
import Route
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.ampairs.company.ui.CompanyListScreen
import com.ampairs.company.ui.CompanyScreen

fun NavGraphBuilder.companyNavigation(
    navigator: NavController,
    onCompanySelected: (String) -> Unit
) {

    navigation<Route.Company>(startDestination = CompanyRoute.Root) {
        composable<CompanyRoute.Root> {
            CompanyListScreen(onCompanySelected, { companyId ->
                navigator.navigate(CompanyRoute.Update(id = companyId))
            }, {
                navigator.navigate(CompanyRoute.Update())
            })
        }
        composable<CompanyRoute.Update> { backStackEntry ->
            val companyRoute = backStackEntry.toRoute<CompanyRoute.Update>()
            CompanyScreen(modifier = Modifier, id = companyRoute.id) {
                navigator.popBackStack()
            }
        }
    }
}