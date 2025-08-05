package com.ampairs.product.ui.tax.tax_code

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
fun TaxCodePaneScreen() {

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
                TaxCodesScreen(onTaxCodeSelected = { taxCodeId ->
                    scope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, taxCodeId)
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
                val taxCodeId = navigator.currentDestination?.contentKey ?: ""
                TaxCodeScreen(Modifier, taxCodeId) {}
            }
        }
    )


}
