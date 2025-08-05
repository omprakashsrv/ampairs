package com.ampairs.company.ui

import ampairsapp.composeapp.generated.resources.Res
import ampairsapp.composeapp.generated.resources.ic_add
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.ampairs.common.model.UiState
import com.ampairs.company.viewmodel.CompanyListViewModel
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject


@Composable
fun CompanyListScreen(
    onCompanySelected: (String) -> Unit,
    onCompanyEdit: (String) -> Unit,
    onNewCompany: (() -> Unit)?,
) {

    val viewModel: CompanyListViewModel = koinInject()


    val companiesState = viewModel.companyState.value
    val companies = viewModel.companies.collectAsLazyPagingItems()

    val lazyListState = rememberLazyListState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (companiesState) {
            UiState.Empty -> {}
            is UiState.Error -> {}
            is UiState.Loading -> {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is UiState.Success -> {}
        }

        Row(modifier = Modifier.widthIn(400.dp).weight(1f)) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxHeight(1.0f)
            ) {
                items(
                    count = companies.itemCount,
                    key = { index -> companies[index]?.id ?: index },
                    contentType = { 1 },
                    itemContent = { index ->
                        val company = companies[index]
                        ListItem(
                            shadowElevation = 2.dp,
                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                                .clickable {
                                    viewModel.selectCompany(company)
                                    onCompanySelected(company?.id ?: "")
                                },
                            headlineContent = {
                                Text(
                                    company?.name.toString(),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingContent = {
                                Text(
                                    (index + 1).toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            supportingContent = {
                                Text(
                                    company?.address ?: "",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { onCompanyEdit(company?.id ?: "") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Localized description"
                                    )
                                }
                            })

                    }
                )
            }
        }

        if (onNewCompany != null) {
            IconButton(
                modifier = Modifier.padding(20.dp).align(Alignment.CenterHorizontally),
                onClick = {
                    onNewCompany()
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