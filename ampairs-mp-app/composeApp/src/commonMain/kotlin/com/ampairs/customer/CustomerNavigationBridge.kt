package com.ampairs.customer

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.ampairs.customer.ui.customerNavigation
import CustomerRoute

/**
 * Bridge function to integrate new customer module with existing app navigation
 */
fun NavGraphBuilder.customerNavigation(
    navController: NavController,
    onCustomerRedirect: (String, String) -> Unit = { _, _ -> }
) {
    // Customer root route
    composable<CustomerRoute.Root> {
        com.ampairs.customer.ui.CustomerScreen(
            onCustomerClick = { customerId ->
                navController.navigate(CustomerRoute.CustomerEdit(customerId))
            },
            onCreateCustomer = {
                navController.navigate(CustomerRoute.CustomerEdit())
            }
        )
    }

    // Customer view route
    composable<CustomerRoute.CustomerView> {
        com.ampairs.customer.ui.CustomerScreen(
            onCustomerClick = { customerId ->
                navController.navigate(CustomerRoute.CustomerEdit(customerId))
            },
            onCreateCustomer = {
                navController.navigate(CustomerRoute.CustomerEdit())
            }
        )
    }

    // Customer edit/create route
    composable<CustomerRoute.CustomerEdit> { backStackEntry ->
        val route = backStackEntry.toRoute<CustomerRoute.CustomerEdit>()
        val customerId = route.id.takeIf { it.isNotBlank() }

        com.ampairs.customer.ui.create.CustomerFormScreen(
            customerId = customerId,
            onNavigateBack = { navController.popBackStack() },
            onSaveSuccess = { navController.popBackStack() }
        )
    }

    // Redirect route
    composable<CustomerRoute.Redirect> { backStackEntry ->
        val route = backStackEntry.toRoute<CustomerRoute.Redirect>()
        onCustomerRedirect(route.fromCustomer, route.toCustomer)
        navController.popBackStack()
    }

    // Include the new customer navigation for internal routing
    customerNavigation(navController)
}