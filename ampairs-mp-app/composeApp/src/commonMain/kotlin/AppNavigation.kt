import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.ampairs.auth.authNavigation
import com.ampairs.common.ui.AppScreenWithHeader
import com.ampairs.common.UnauthenticatedHandler
import com.ampairs.customer.ui.customerNavigation
import com.ampairs.customer.ui.StateListRoute
import com.ampairs.customer.ui.CustomerCreateRoute
// Temporarily commented out pending customer integration updates
// import com.ampairs.inventory.inventoryNavigation
// import com.ampairs.invoice.invoiceNavigation
// import com.ampairs.order.orderNavigation
import com.ampairs.product.productNavigation
import com.ampairs.tax.ui.navigation.taxNavigation
import com.ampairs.workspace.context.WorkspaceContextManager
import com.ampairs.workspace.integration.WorkspaceContextIntegration
import com.ampairs.workspace.navigation.DynamicModuleNavigationService
import com.ampairs.workspace.navigation.GlobalNavigationManager
import com.ampairs.workspace.workspaceNavigation
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppNavigation(
    onNavigationServiceReady: ((DynamicModuleNavigationService?) -> Unit)? = null,
    onNavigationReady: (((String) -> Unit) -> Unit)? = null
) {
    val navController = rememberNavController()
    val workspaceManager = WorkspaceContextManager.getInstance()


    // Set up navigation callback for desktop menu integration
    LaunchedEffect(navController) {
        val navigationCallback: (String) -> Unit = { route ->
            println("AppNavigation: Received navigation request for: $route")
            navigateToMenuItem(navController, route)
        }
        onNavigationReady?.invoke(navigationCallback)
    }

    // Get global navigation manager instance
    val globalNavigationManager = GlobalNavigationManager.getInstance()

    LaunchedEffect(Unit) {
        UnauthenticatedHandler.onUnauthenticated.collectLatest {
            // Clear workspace context and navigation service on logout using integration
            WorkspaceContextIntegration.clearWorkspaceContext()
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

            // Clear navigationService when not in workspace modules or customer modules
            val isInWorkspaceModules = currentRoute?.contains("workspace/modules") == true
            val isInCustomerModule = currentRoute?.contains("Route.Customer") == true ||
                                   currentRoute?.contains("com.ampairs.customer") == true

            if (currentRoute != null && !isInWorkspaceModules && !isInCustomerModule) {
                println("AppNavigation: Clearing navigationService - not in workspace/customer modules")
                onNavigationServiceReady?.invoke(null)
            }
        }
    }

    // Remove the automatic workspace selection redirection for now
    // This was causing infinite loops and should be handled differently
    // The workspace selection should be handled by the individual screens that require workspace context

    NavHost(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars),
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
        // Customer module navigation
        composable<Route.Customer> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                com.ampairs.customer.ui.CustomerScreen(
                    onCustomerClick = { customerId ->
                        navController.navigate(com.ampairs.customer.ui.CustomerDetailsRoute(customerId))
                    },
                    onCreateCustomer = {
                        navController.navigate(CustomerCreateRoute())
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Product module navigation
        composable<Route.Product> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                com.ampairs.product.ProductScreen(
                    onProductClick = { productId ->
                        navController.navigate(ProductRoute.ProductDetails(productId))
                    },
                    onCreateProduct = {
                        navController.navigate(ProductRoute.ProductForm())
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Tax module navigation
        composable<Route.Tax> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                com.ampairs.tax.ui.navigation.TaxScreen(
                    onNavigateToHsnCodes = {
                        navController.navigate(com.ampairs.tax.ui.navigation.HsnCodesListRoute)
                    },
                    onNavigateToTaxCalculator = {
                        navController.navigate(com.ampairs.tax.ui.navigation.TaxCalculatorRoute)
                    },
                    onNavigateToTaxRates = {
                        navController.navigate(com.ampairs.tax.ui.navigation.TaxRatesRoute)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        customerNavigation(navController)
        productNavigation(navController)
        taxNavigation(navController)
        // Temporarily commented out pending customer integration updates
        // inventoryNavigation(navController) { }
        // orderNavigation(navController) { }
        // invoiceNavigation(navController) { }
    }
}

/**
 * Navigate to a menu item based on its route path or module code
 * Maps menu item paths to type-safe navigation routes
 */
fun navigateToMenuItem(navController: androidx.navigation.NavHostController, route: String) {
    when {
        // Handle legacy module codes first (backward compatibility)
        route == "customer" -> navController.navigate(Route.Customer)
        route == "product" -> navController.navigate(Route.Product)
        route == "order" -> navController.navigate(Route.Order)
        route == "invoice" -> navController.navigate(Route.Invoice)
        route == "inventory" -> navController.navigate(Route.Inventory)
        route == "tax" -> navController.navigate(Route.Tax)

        // Handle specific menu item paths
        route.startsWith("/customers") -> {
            when (route) {
                "/customers" -> navController.navigate(Route.Customer)
                "/customers/create" -> navController.navigate(CustomerCreateRoute())
                "/customers/import" -> navController.navigate(CustomerRoute.Root)
                "/customers/states" -> navController.navigate(StateListRoute)
                "/customers/types" -> navController.navigate(com.ampairs.customer.ui.CustomerTypeListRoute)
                "/customers/groups" -> navController.navigate(com.ampairs.customer.ui.CustomerGroupListRoute)
                else -> navController.navigate(Route.Customer)
            }
        }

        route.startsWith("/products") -> {
            when (route) {
                "/products" -> navController.navigate(Route.Product)
                "/products/create" -> navController.navigate(ProductRoute.ProductForm())
                "/products/groups" -> navController.navigate(ProductRoute.Group())
                "/products/import" -> navController.navigate(Route.Product) // Route to main product page
                else -> navController.navigate(Route.Product)
            }
        }

        route.startsWith("/orders") -> {
            when (route) {
                "/orders" -> navController.navigate(Route.Order)
                "/orders/create" -> navController.navigate(OrderRoute.Root())
                "/orders/import" -> navController.navigate(Route.Order)
                else -> navController.navigate(Route.Order)
            }
        }

        route.startsWith("/invoices") -> {
            when (route) {
                "/invoices" -> navController.navigate(Route.Invoice)
                "/invoices/create" -> navController.navigate(InvoiceRoute.Root())
                "/invoices/import" -> navController.navigate(Route.Invoice)
                else -> navController.navigate(Route.Invoice)
            }
        }

        route.startsWith("/inventory") -> {
            navController.navigate(Route.Inventory)
        }

        route.startsWith("/tax") -> {
            navController.navigate(Route.Tax)
        }

        else -> {
            println("AppNavigation: Unknown route: $route")
        }
    }
}
