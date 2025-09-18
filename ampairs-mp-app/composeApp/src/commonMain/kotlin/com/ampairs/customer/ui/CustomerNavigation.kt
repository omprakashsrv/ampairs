package com.ampairs.customer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.ampairs.customer.ui.create.CustomerFormScreen
import com.ampairs.customer.ui.details.CustomerDetailsScreen
import com.ampairs.customer.ui.list.CustomersListScreen
import kotlinx.serialization.Serializable

@Serializable
object CustomerListRoute

@Serializable
data class CustomerDetailsRoute(val customerId: String)

@Serializable
data class CustomerCreateRoute(val customerId: String? = null)

fun NavGraphBuilder.customerNavigation(navController: NavController) {
    composable<CustomerListRoute> {
        CustomersListScreen(
            onCustomerClick = { customerId ->
                navController.navigate(CustomerDetailsRoute(customerId))
            },
            onCreateCustomer = {
                navController.navigate(CustomerCreateRoute())
            }
        )
    }

    composable<CustomerDetailsRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<CustomerDetailsRoute>()
        CustomerDetailsScreen(
            customerId = route.customerId,
            onNavigateBack = { navController.popBackStack() },
            onEditCustomer = { customerId ->
                navController.navigate(CustomerCreateRoute(customerId))
            }
        )
    }

    composable<CustomerCreateRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<CustomerCreateRoute>()
        CustomerFormScreen(
            customerId = route.customerId,
            onNavigateBack = { navController.popBackStack() },
            onSaveSuccess = { navController.popBackStack() }
        )
    }
}

@Composable
fun CustomerScreen(
    onCustomerClick: (String) -> Unit,
    onCreateCustomer: () -> Unit,
    modifier: Modifier = Modifier
) {
    CustomersListScreen(
        onCustomerClick = onCustomerClick,
        onCreateCustomer = onCreateCustomer,
        modifier = modifier
    )
}