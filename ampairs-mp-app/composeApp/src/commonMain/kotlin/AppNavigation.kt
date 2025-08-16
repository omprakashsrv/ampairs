
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
import com.ampairs.company.companyNavigation
import com.ampairs.customer.customerNavigation
import com.ampairs.home.homeNavigation
import com.ampairs.inventory.inventoryNavigation
import com.ampairs.invoice.invoiceNavigation
import com.ampairs.order.orderNavigation
import com.ampairs.product.productNavigation
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        UnauthenticatedHandler.onUnauthenticated.collectLatest {
            navController.navigate(Route.Login) {
                popUpTo(0)
            }
        }
    }

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
                route = Route.Company,
                navOptions = options
            )
        }
        companyNavigation(navController) {
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
