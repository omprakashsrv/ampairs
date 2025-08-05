package com.ampairs.product.ui.product

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.ampairs.common.components.CartItem
import com.ampairs.product.domain.Product
import com.seiko.imageloader.rememberImagePainter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductItems(
    modifier: Modifier,
    products: LazyPagingItems<Product>,
    onProductClick: (String) -> Unit,
    onProductQtyChanged: (Product) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = 8.dp,
            vertical = 8.dp
        ),
    ) {
        items(products.itemCount, key = { index -> index }, contentType = { 1 }) {
            val product = products.get(it)
            ElevatedCard(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .clickable { }
                    .combinedClickable(
                        onClick = {

                        },
                        onLongClick = {
                            onProductClick(product?.id.toString())
                        }
                    )
            ) {
                if (product?.images?.isEmpty() == true) {


                } else {
                    val painter =
                        rememberImagePainter(product?.images?.get(0)?.url ?: "")
                    Image(
                        modifier = Modifier.fillMaxWidth(),
                        alignment = Alignment.Center,
                        painter = painter,
                        contentDescription = "Translated description of what the image contains"
                    )
                }
                Text(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
                    text = product?.name ?: "",
                    style = MaterialTheme.typography.labelSmall
                )
                product?.inventory?.stock?.let { stock ->
                    Text(
                        color = Color(9, 121, 105),
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
                        text = "Stock : $stock",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatPrice(product?.sellingPrice),
                        modifier = Modifier
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        CartItem(
                            id = product?.id + product?.quantity,
                            qty = product?.quantity ?: 0.0
                        ) { qty ->
                            product?.quantity = qty
                            onProductQtyChanged(product!!)
                        }
                    }

                }
            }
        }
    }
}

