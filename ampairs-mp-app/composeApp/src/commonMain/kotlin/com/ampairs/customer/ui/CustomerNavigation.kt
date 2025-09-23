package com.ampairs.customer.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.ampairs.common.ui.AppScreenWithHeader
import com.ampairs.customer.ui.create.CustomerFormScreen
import com.ampairs.customer.ui.details.CustomerDetailsScreen
import com.ampairs.customer.ui.list.CustomersListScreen
import com.ampairs.customer.ui.state.StateListScreen
import kotlinx.serialization.Serializable

@Serializable
object CustomerListRoute

@Serializable
data class CustomerDetailsRoute(val customerId: String)

@Serializable
data class CustomerCreateRoute(val customerId: String? = null)

@Serializable
object StateListRoute


fun NavGraphBuilder.customerNavigation(navController: NavHostController) {
    composable<CustomerListRoute> {
        AppScreenWithHeader(
            navController = navController,
            isWorkspaceSelection = false
        ) { paddingValues ->
            CustomersListScreen(
                onCustomerClick = { customerId ->
                    navController.navigate(CustomerDetailsRoute(customerId))
                },
                onCreateCustomer = {
                    navController.navigate(CustomerCreateRoute())
                },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }

    composable<CustomerDetailsRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<CustomerDetailsRoute>()
        AppScreenWithHeader(
            navController = navController,
            isWorkspaceSelection = false
        ) { paddingValues ->
            CustomerDetailsScreen(
                customerId = route.customerId,
                onNavigateBack = { navController.popBackStack() },
                onEditCustomer = { customerId ->
                    navController.navigate(CustomerCreateRoute(customerId))
                },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }

    composable<CustomerCreateRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<CustomerCreateRoute>()
        AppScreenWithHeader(
            navController = navController,
            isWorkspaceSelection = false
        ) { paddingValues ->
            CustomerFormScreen(
                customerId = route.customerId,
                onSaveSuccess = { navController.popBackStack() },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }

    composable<StateListRoute> {
        AppScreenWithHeader(
            navController = navController,
            isWorkspaceSelection = false
        ) { paddingValues ->
            StateListScreen(
                onStateClick = { /* Handle state click if needed */ },
                onImportStates = { /* Handle state import */ },
                modifier = Modifier.padding(paddingValues)
            )
        }
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