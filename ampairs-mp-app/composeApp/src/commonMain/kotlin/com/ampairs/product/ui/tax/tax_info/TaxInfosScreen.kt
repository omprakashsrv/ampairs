package com.ampairs.product.ui.tax.tax_info

import ampairsapp.composeapp.generated.resources.Res
import ampairsapp.composeapp.generated.resources.ic_add
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.ampairs.common.model.UiState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaxInfosScreen(onTaxCodeSelected: (String) -> Unit, onNewTaxCode: (() -> Unit)?) {

    val viewModel: TaxInfosViewModel = koinInject()

    val lazyListState = rememberLazyListState()
    val taxInfos = viewModel.taxInfos.collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()
    val taxCodeState = viewModel.taxInfoState.value

    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        when (taxCodeState) {
            UiState.Empty -> {}
            is UiState.Error -> {}
            is UiState.Loading -> {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is UiState.Success -> {}
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search") },
            value = viewModel.searchText, onValueChange = {
                viewModel.searchText = it
                taxInfos.refresh()
                scope.launch {
                    lazyListState.animateScrollToItem(0)
                }
            })

        if (taxInfos.itemCount > 0) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxHeight()
            ) {
                items(
                    count = taxInfos.itemCount,
                    key = { index -> taxInfos[index]?.id ?: index },
                    contentType = { 1 },
                    itemContent = { index ->
                        val taxCode = taxInfos[index]
                        ListItem(
                            shadowElevation = 2.dp,
                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null).clickable {
                                onTaxCodeSelected(taxCode?.id.toString())
                            },
                            headlineContent = {
                                Text(
                                    taxCode?.name.toString(),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            supportingContent = {
                                Text(
                                    taxCode?.tax_spec.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            })
                    }
                )
            }
        } else {
            if (onNewTaxCode != null) {
                IconButton(
                    modifier = Modifier.padding(20.dp).align(Alignment.CenterHorizontally),
                    onClick = {
                        onNewTaxCode()
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