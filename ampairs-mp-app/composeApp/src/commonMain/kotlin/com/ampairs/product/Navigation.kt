package com.ampairs.product

import ProductRoute
import Route
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.ampairs.product.ui.group.GroupType
import com.ampairs.product.ui.group.ProductGroupEditScreen
import com.ampairs.product.ui.group.ProductGroupScreen
import com.ampairs.product.ui.product.ProductEditScreen
import com.ampairs.product.ui.product.ProductPaneScreen
import com.ampairs.product.ui.product.ProductScreen
import com.ampairs.product.ui.product.ProductViewModel
import com.ampairs.product.ui.tax.tax_code.TaxCodePaneScreen
import com.ampairs.product.ui.tax.tax_info.TaxInfoPane
import org.koin.compose.koinInject

fun NavGraphBuilder.productNavigation(
    navigator: NavController
) {
    navigation<Route.Product>(startDestination = ProductRoute.Group()) {
        composable<ProductRoute.Group> { backStackEntry ->
            val groupRoute = backStackEntry.toRoute<ProductRoute.Group>()
            val groupType = GroupType.valueOf(groupRoute.type)
            val editMode = groupRoute.edit
            if (editMode) {
                ProductGroupEditScreen(groupType) {}
            } else {
                val productViewModel: ProductViewModel = koinInject()
                ProductGroupScreen(productViewModel, groupType) { group ->
                    navigator.navigate(ProductRoute.Product(groupId = group.id))
                }
            }
        }
        composable<ProductRoute.Product> { backStackEntry ->
            val productRoute = backStackEntry.toRoute<ProductRoute.Product>()
            val productViewModel: ProductViewModel = koinInject()
            ProductScreen(productRoute.groupId, productViewModel, onProductClick = { id ->
                navigator.navigate(ProductRoute.ProductEdit(productId = id))
            }) {
                navigator.popBackStack()
            }
        }
        composable<ProductRoute.ProductEdit> { backStackEntry ->
            val productEditRoute = backStackEntry.toRoute<ProductRoute.ProductEdit>()
            ProductEditScreen(Modifier, productEditRoute.productId) {
                navigator.popBackStack()
            }
        }
        composable<ProductRoute.TaxInfo> {
            TaxInfoPane()
        }
        composable<ProductRoute.TaxCode> {
            TaxCodePaneScreen()
        }
        composable<ProductRoute.Products> {
            ProductPaneScreen()
        }
    }
}