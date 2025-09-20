package com.ampairs.tax.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.ampairs.common.ui.AppScreenWithHeader
import com.ampairs.tax.ui.calculator.TaxCalculatorScreen
import com.ampairs.tax.ui.hsn.HsnCodesListScreen
import kotlinx.serialization.Serializable
import Route

// Tax Navigation Routes
@Serializable
object TaxListRoute

@Serializable
object HsnCodesListRoute

@Serializable
data class HsnCodeDetailsRoute(val hsnCodeId: String)

@Serializable
data class HsnCodeFormRoute(val hsnCodeId: String? = null)

@Serializable
object TaxCalculatorRoute

@Serializable
object TaxRatesRoute

@Serializable
data class TaxRateDetailsRoute(val taxRateId: String)

@Serializable
data class TaxRateFormRoute(val taxRateId: String? = null)

fun NavGraphBuilder.taxNavigation(
    navController: NavController
) {
    navigation<Route.Tax>(startDestination = TaxListRoute) {
        // Main Tax Module Landing Screen
        composable<TaxListRoute> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                TaxModuleScreen(
                    onNavigateToHsnCodes = {
                        navController.navigate(HsnCodesListRoute)
                    },
                    onNavigateToTaxCalculator = {
                        navController.navigate(TaxCalculatorRoute)
                    },
                    onNavigateToTaxRates = {
                        navController.navigate(TaxRatesRoute)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // HSN Codes List Screen
        composable<HsnCodesListRoute> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                HsnCodesListScreen(
                    onHsnCodeClick = { hsnCodeId ->
                        navController.navigate(HsnCodeDetailsRoute(hsnCodeId))
                    },
                    onCreateHsnCode = {
                        navController.navigate(HsnCodeFormRoute())
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // HSN Code Details Screen
        composable<HsnCodeDetailsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<HsnCodeDetailsRoute>()
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                // Placeholder for HSN Code Details Screen
                Text("HSN Code Details: ${route.hsnCodeId}")
                // TODO: Implement HsnCodeDetailsScreen
            }
        }

        // HSN Code Form Screen (Create/Edit)
        composable<HsnCodeFormRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<HsnCodeFormRoute>()
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                // Placeholder for HSN Code Form Screen
                Text("HSN Code Form: ${route.hsnCodeId ?: "New"}")
                // TODO: Implement HsnCodeFormScreen
            }
        }

        // Tax Calculator Screen
        composable<TaxCalculatorRoute> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                TaxCalculatorScreen(
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Tax Rates List Screen
        composable<TaxRatesRoute> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                // Placeholder for Tax Rates List Screen
                Text("Tax Rates List")
                // TODO: Implement TaxRatesListScreen
            }
        }

        // Tax Rate Form Screen (Create/Edit)
        composable<TaxRateFormRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TaxRateFormRoute>()
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                // Placeholder for Tax Rate Form Screen
                Text("Tax Rate Form: ${route.taxRateId ?: "New"}")
                // TODO: Implement TaxRateFormScreen
            }
        }
    }
}

@Composable
fun TaxScreen(
    onNavigateToHsnCodes: () -> Unit,
    onNavigateToTaxCalculator: () -> Unit,
    onNavigateToTaxRates: () -> Unit,
    modifier: Modifier = Modifier
) {
    TaxModuleScreen(
        onNavigateToHsnCodes = onNavigateToHsnCodes,
        onNavigateToTaxCalculator = onNavigateToTaxCalculator,
        onNavigateToTaxRates = onNavigateToTaxRates,
        modifier = modifier
    )
}