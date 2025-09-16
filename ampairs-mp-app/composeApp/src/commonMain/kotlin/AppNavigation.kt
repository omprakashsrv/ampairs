import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.ampairs.auth.authNavigation
import com.ampairs.common.UnauthenticatedHandler
import com.ampairs.customer.customerNavigation
import com.ampairs.home.homeNavigation
import com.ampairs.inventory.inventoryNavigation
import com.ampairs.invoice.invoiceNavigation
import com.ampairs.order.orderNavigation
import com.ampairs.product.productNavigation
import com.ampairs.workspace.context.WorkspaceContextManager
import com.ampairs.workspace.workspaceNavigation
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppNavigation(
    onNavigationServiceReady: ((com.ampairs.workspace.navigation.DynamicModuleNavigationService?) -> Unit)? = null
) {
    val navController = rememberNavController()
    val workspaceManager = WorkspaceContextManager.getInstance()

    LaunchedEffect(Unit) {
        UnauthenticatedHandler.onUnauthenticated.collectLatest {
            // Clear workspace context on logout
            workspaceManager.clearWorkspaceContext()
            navController.navigate(Route.Login) {
                popUpTo(0)
            }
        }
    }

    // Clear navigationService when navigating away from workspace modules
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collectLatest { backStackEntry ->
            val currentRoute = backStackEntry.destination.route
            println("AppNavigation: Current route: $currentRoute")

            // Clear navigationService when not in workspace modules
            if (currentRoute != null && !currentRoute.contains("workspace/modules")) {
                println("AppNavigation: Clearing navigationService - not in workspace modules")
                onNavigationServiceReady?.invoke(null)
            }
        }
    }

    // Remove the automatic workspace selection redirection for now
    // This was causing infinite loops and should be handled differently
    // The workspace selection should be handled by the individual screens that require workspace context

    NavHost(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        navController = navController, startDestination = Route.Login
    ) {
        authNavigation(navController) {
            val options = navOptions {
                popUpTo<AuthRoute.LoginRoot> {
                    this.inclusive = true
                }
                launchSingleTop = true // Avoid multiple instances of the same destination
            }
            navController.navigate(
                route = Route.Workspace,
                navOptions = options
            )
        }
        workspaceNavigation(navController, onNavigationServiceReady) {
            navController.navigate(Route.Home)
        }
        homeNavigation(navController) {
            navController.navigate(it.navPath)
        }
        customerNavigation(navController) { from, to ->
//            navController.navigate(
//                CustomerRoute.Redirect(fromCustomer = from, toCustomer = to)
//            )
        }
        productNavigation(navController)
        inventoryNavigation(navController) {

        }
        orderNavigation(navController) {

        }
        invoiceNavigation(navController) {

        }
    }
}
