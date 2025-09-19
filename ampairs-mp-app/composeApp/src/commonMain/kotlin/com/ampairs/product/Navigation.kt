package com.ampairs.product

import ProductRoute
import Route
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.ampairs.product.ui.create.ProductFormScreen
import com.ampairs.product.ui.details.ProductDetailsScreen
import com.ampairs.product.ui.list.ProductsListScreen

fun NavGraphBuilder.productNavigation(
    navigator: NavController
) {
    navigation<Route.Product>(startDestination = ProductRoute.Products) {
        // Main Product List Screen
        composable<ProductRoute.Products> {
            ProductsListScreen(
                onProductClick = { productId ->
                    navigator.navigate(ProductRoute.ProductDetails(productId = productId))
                },
                onCreateProduct = {
                    navigator.navigate(ProductRoute.ProductForm())
                }
            )
        }

        // Product Details Screen
        composable<ProductRoute.ProductDetails> { backStackEntry ->
            val productDetailsRoute = backStackEntry.toRoute<ProductRoute.ProductDetails>()
            ProductDetailsScreen(
                productId = productDetailsRoute.productId,
                onNavigateBack = {
                    navigator.popBackStack()
                },
                onEditProduct = { productId ->
                    navigator.navigate(ProductRoute.ProductForm(productId = productId))
                }
            )
        }

        // Product Form Screen (Create/Edit)
        composable<ProductRoute.ProductForm> { backStackEntry ->
            val productFormRoute = backStackEntry.toRoute<ProductRoute.ProductForm>()
            ProductFormScreen(
                productId = productFormRoute.productId,
                onNavigateBack = {
                    navigator.popBackStack()
                },
                onSaveSuccess = {
                    navigator.popBackStack()
                }
            )
        }

        // Legacy routes - commented out as these UI components don't exist yet
        // composable<ProductRoute.Group> { backStackEntry ->
        //     val groupRoute = backStackEntry.toRoute<ProductRoute.Group>()
        //     val groupType = GroupType.valueOf(groupRoute.type)
        //     val editMode = groupRoute.edit
        //     if (editMode) {
        //         ProductGroupEditScreen(groupType) {}
        //     } else {
        //         val productViewModel: ProductViewModel = koinInject()
        //         ProductGroupScreen(productViewModel, groupType) { group ->
        //             navigator.navigate(ProductRoute.Product(groupId = group.id))
        //         }
        //     }
        // }

        // composable<ProductRoute.Product> { backStackEntry ->
        //     val productRoute = backStackEntry.toRoute<ProductRoute.Product>()
        //     val productViewModel: ProductViewModel = koinInject()
        //     ProductScreen(productRoute.groupId, productViewModel, onProductClick = { id ->
        //         navigator.navigate(ProductRoute.ProductEdit(productId = id))
        //     }) {
        //         navigator.popBackStack()
        //     }
        // }

        // composable<ProductRoute.ProductEdit> { backStackEntry ->
        //     val productEditRoute = backStackEntry.toRoute<ProductRoute.ProductEdit>()
        //     ProductEditScreen(Modifier, productEditRoute.productId) {
        //         navigator.popBackStack()
        //     }
        // }

        // composable<ProductRoute.TaxInfo> {
        //     TaxInfoPane()
        // }

        // composable<ProductRoute.TaxCode> {
        //     TaxCodePaneScreen()
        // }
    }
}