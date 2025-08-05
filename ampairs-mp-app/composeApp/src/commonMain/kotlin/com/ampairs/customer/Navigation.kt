package com.ampairs.customer

import CustomerRoute
import Route
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.ampairs.customer.ui.CustomerPaneScreen
import com.ampairs.customer.ui.CustomerScreen
import com.ampairs.customer.ui.CustomersScreen

fun NavGraphBuilder.customerNavigation(
    navigator: NavController,
    onCustomerSelected: (String, String) -> Unit
) {

    navigation<Route.Customer>(startDestination = CustomerRoute.Root) {
        composable<CustomerRoute.Root> {
            CustomersScreen(null, onCustomerSelected) { customerId ->
                navigator.navigate(CustomerRoute.CustomerEdit(id = customerId))
            }
        }
        composable<CustomerRoute.CustomerView> {
            CustomerPaneScreen()
        }
        composable<CustomerRoute.CustomerEdit> { backStackEntry ->
            val customerRoute = backStackEntry.toRoute<CustomerRoute.CustomerEdit>()
            CustomerScreen(Modifier, customerRoute.id) {
                navigator.popBackStack()
            }
        }
    }
}