package com.ampairs.product.ui.tax.tax_info

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
fun TaxInfoPane() {
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
                TaxInfosScreen(onTaxCodeSelected = { taxInfoId ->
                    scope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, taxInfoId)
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
                val taxInfoId = navigator.currentDestination?.contentKey ?: ""
                TaxInfoScreen(modifier = Modifier, taxInfoId) {}
            }
        }
    )


}
