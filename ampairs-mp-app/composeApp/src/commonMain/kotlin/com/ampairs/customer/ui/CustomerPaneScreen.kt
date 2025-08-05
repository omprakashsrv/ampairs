package com.ampairs.customer.ui

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
fun CustomerPaneScreen() {

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
                CustomersScreen(onNewCustomer = {
                    scope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, "")
                    }
                }, onCustomerSelected = { customerId, _ ->
                    scope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, customerId)
                    }
                }, onCustomer = { customerId ->
                    scope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, customerId)
                    }
                })
            }
        },
        detailPane = {
            AnimatedPane(Modifier) {
                val customerId = navigator.currentDestination?.contentKey ?: ""
                CustomerScreen(Modifier, customerId) {}
            }
        }
    )

}
