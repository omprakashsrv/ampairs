package com.ampairs.product.ui.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems
import com.ampairs.common.model.UiState

@Composable
fun ProductCategoryScreen(
    viewModel: ProductCategoryViewModel,
    onProductClick: (String) -> Unit,
    onProductQtyChanged: () -> Unit,
) {
    val productCategories = viewModel.productCategories.value
    viewModel.setQtyChangeListener(onProductQtyChanged)
    when (productCategories) {
        UiState.Empty -> {}
        is UiState.Error -> TODO()
        is UiState.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }

        is UiState.Success -> {
            val products = viewModel.products.collectAsLazyPagingItems()
            Column(modifier = Modifier.fillMaxWidth()) {
                val tabIndex = viewModel.tabIndex
                ScrollableTabRow(selectedTabIndex = tabIndex) {
                    val categories = productCategories.data
                    if (categories!!.isEmpty()) {
                        Text("No Categories", style = MaterialTheme.typography.titleMedium)
                    } else {
                        categories.forEachIndexed { index, group ->
                            Tab(
                                text = {
                                    Text(
                                        group.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                },
                                selected = tabIndex == index,
                                onClick = {
                                    viewModel.tabIndex = index
                                    viewModel.selectedCategory = group
                                    products.refresh()
                                },
                            )
                        }
                    }

                }

                ProductItems(modifier = Modifier.weight(1f), products, onProductClick) { product ->
                    if (product.quantity > 0) {
                        viewModel.addCartItem(product)
                    } else {
                        viewModel.removeCartItem(product)
                    }
                    onProductQtyChanged()
                }
            }
        }
    }
}

fun formatPrice(price: Double?): String {
    if (price == null) return ""
    if (price.toString().endsWith(".0")) {
        return price.toInt().toString()
    }
    return price.toString()
}
