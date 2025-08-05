package com.ampairs.product.ui.product

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ProductPaneScreen() {

    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val scope = rememberCoroutineScope()

//    BackHandler(navigator.canNavigateBack()) {
//        navigator.navigateBack()
//    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane(Modifier) {
                ProductListScreen(onProductSelected = { productId ->
                    scope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, productId)
                    }
                }) {
                    scope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, "")
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane(Modifier) {
                val productId = navigator.currentDestination?.contentKey ?: ""
                ProductEditScreen(modifier = Modifier, productId) {
                    if (navigator.canNavigateBack()) {
                        scope.launch {
                            navigator.navigateBack()
                        }
                    }
                }
            }
        }
    )
}