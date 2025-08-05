package com.ampairs.product.ui.group

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.ampairs.common.model.UiState
import com.ampairs.product.domain.Group
import com.ampairs.product.ui.product.ProductItems
import com.ampairs.product.ui.product.ProductViewModel
import com.seiko.imageloader.rememberImagePainter
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductGroupScreen(
    productViewModel: ProductViewModel,
    groupType: GroupType,
    onGroupSelected: (Group) -> Unit
) {

    val viewModel: ProductGroupViewModel = koinInject()
    val productSearchViewModel: ProductSearchViewModel = koinInject()

    val productGroups = viewModel.groups.value
    val products = productSearchViewModel.products.collectAsLazyPagingItems()
    val productsState = productSearchViewModel.productsState.value

    productSearchViewModel.setQtyChangeListener {
        productViewModel.onProductQtyChange()
    }
    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
//        val onActiveChange = {
//            productSearchViewModel.searchActive = it
//        }
//        val colors1 = SearchBarDefaults.colors()
        SearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { traversalIndex = -1f },
            query = productSearchViewModel.searchText,
            onQueryChange = {
                productSearchViewModel.searchText = it
                products.refresh()
            },
            onSearch = { productSearchViewModel.searchActive = false },
            active = productSearchViewModel.searchActive,
            onActiveChange = {
                productSearchViewModel.searchActive = it
            },
            placeholder = { Text("Search Products") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (productSearchViewModel.searchActive) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.clickable {
                            productSearchViewModel.searchActive = false
                        })
                }
            },
        ) {
            when (productsState) {
                UiState.Empty -> {}
                is UiState.Error -> {}
                is UiState.Loading -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is UiState.Success -> {}
            }
            ProductItems(modifier = Modifier, products, onProductClick = {}) { product ->
                if (product.quantity > 0) {
                    productSearchViewModel.addCartItem(product)
                } else {
                    productSearchViewModel.removeCartItem(product)
                }
                productViewModel.onProductQtyChange()
            }
        }
        when (productGroups) {
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
                Text(
                    text = "Shop by " + groupType.name, modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 128.dp)
                ) {
                    items(productGroups.data!!.size) { index ->
                        val group = productGroups.data.get(index)
                        ElevatedCard(
                            modifier = Modifier.padding(8.dp).height(128.dp),
                            onClick = { onGroupSelected(group) }
                        ) {
                            if (group.image != null && !group.image!!.url.isNullOrEmpty()) {
                                val painter =
                                    rememberImagePainter(group.image!!.url ?: "")
                                Image(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    alignment = Alignment.Center,
                                    painter = painter,
                                    contentDescription = "Translated description of what the image contains"
                                )
                            } else {
                                Box(modifier = Modifier.weight(1f))
                            }
                            Text(
                                text = group.name,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(2.dp).fillMaxWidth()
                                    .heightIn(min = 40.dp),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 2,
                            )
                        }
                    }
                }
            }

            UiState.Empty -> {

            }

            is UiState.Error -> {}
        }
    }

}
