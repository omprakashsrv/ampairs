package com.ampairs.customer.ui


import ampairsapp.composeapp.generated.resources.Res
import ampairsapp.composeapp.generated.resources.ic_add
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.ampairs.common.model.UiState
import com.ampairs.customer.viewmodel.CustomersViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class, ExperimentalResourceApi::class)
@Composable
fun CustomersScreen(
    onNewCustomer: (() -> (Unit))?,
    onCustomerSelected: (String, String) -> Unit, onCustomer: (String) -> Unit,
) {
    val viewModel: CustomersViewModel = koinInject()

    val customers = viewModel.customers.collectAsLazyPagingItems()
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val customersState = viewModel.customersState.value
    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        when (customersState) {
            UiState.Empty -> {}
            is UiState.Error -> {}
            is UiState.Loading -> {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is UiState.Success -> {}
        }
        TextField(
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search Customer") },
            value = viewModel.searchText, onValueChange = {
                viewModel.searchText = it
                customers.refresh()
                scope.launch {
                    lazyListState.animateScrollToItem(0)
                }
            })

        if (customers.itemCount > 0) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxHeight()
            ) {
                items(
                    count = customers.itemCount,
                    key = { index -> customers[index]?.id ?: index },
                    contentType = { 1 },
                    itemContent = { index ->
                        val customer = customers[index]
                        ListItem(
                            shadowElevation = 2.dp,
                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null).combinedClickable(
                                onClick = {
                                    customer?.id?.let {
                                        onCustomerSelected(it, viewModel.company?.id.toString())
                                    }
                                },
                                onLongClick = {
                                    customer?.id?.let {
                                        onCustomer(it)
                                    }
                                }
                            ),
                            headlineContent = {
                                Text(
                                    customer?.name.toString(),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            supportingContent = {
                                Text(
                                    customer?.address.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            })
                    }
                )
            }
        } else {
            if (onNewCustomer != null) {
                IconButton(
                    modifier = Modifier.padding(20.dp).align(Alignment.CenterHorizontally),
                    onClick = {
                        onNewCustomer()
                    }) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_add),
                        contentDescription = "Visibility",
                        modifier = Modifier.width(16.dp).height(16.dp)
                    )
                }
            }
        }

    }

}
