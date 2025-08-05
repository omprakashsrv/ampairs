package com.ampairs.product.ui.group

import ampairsapp.composeapp.generated.resources.Res
import ampairsapp.composeapp.generated.resources.ic_add
import ampairsapp.composeapp.generated.resources.ic_image
import ampairsapp.composeapp.generated.resources.ic_visibility_off
import ampairsapp.composeapp.generated.resources.ic_visibility_on
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ampairs.common.model.UiState
import com.ampairs.product.domain.Group
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import com.seiko.imageloader.rememberImagePainter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

val GROUP_ITEM = 0
val ADD_ITEM = GROUP_ITEM + 1

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductGroupEditScreen(groupType: GroupType, onGroupSelected: (Group) -> Unit) {

    val viewModel: ProductGroupEditViewModel = koinInject<ProductGroupEditViewModel>()
    val coroutineScope = rememberCoroutineScope()

    val productGroups = viewModel.groupsState.value

    val fileType = listOf("jpg", "jpeg", "png")
    FilePicker(viewModel.showFilePicker, fileExtensions = fileType) { files ->
        viewModel.showFilePicker = false
        viewModel.uploadImage(files)
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
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
                    modifier = Modifier.weight(1.0f),
                    columns = GridCells.Adaptive(minSize = 160.dp)
                ) {
                    items(
                        productGroups.data!!.size,
                        key = { index ->
                            val groupState = productGroups.data[index]
                            groupState.group.id.ifEmpty { index }
                        },
                        contentType = {
                            if (it == productGroups.data.size - 1) ADD_ITEM else GROUP_ITEM
                        }) { index ->
                        val group = productGroups.data[index]
                        Modifier.padding(8.dp).height(160.dp)
                        ElevatedCard(
                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                        ) {
                            if (index == productGroups.data.size - 1) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    IconButton(
                                        modifier = Modifier.align(Alignment.Center),
                                        onClick = {
                                            group.changed = true
                                            viewModel.addGroup()
                                        }) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_add),
                                            contentDescription = "Visibility",
                                            modifier = Modifier.width(16.dp).height(16.dp)
                                        )
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    val image = group.image
                                    if (image != null) {
                                        val painter =
                                            rememberImagePainter(image.url ?: "")
                                        Image(
                                            modifier = Modifier.fillMaxWidth(),
                                            alignment = Alignment.Center,
                                            painter = painter,
                                            contentDescription = "Translated description of what the image contains"
                                        )
                                    }
                                    IconButton(
                                        modifier = Modifier.align(Alignment.TopStart),
                                        onClick = {
                                            group.active = !group.active
                                            group.changed = true
                                        }) {
                                        Icon(
                                            painter = painterResource(if (group.active) Res.drawable.ic_visibility_on else Res.drawable.ic_visibility_off),
                                            contentDescription = "Visibility",
                                            modifier = Modifier.width(16.dp).height(16.dp)
                                        )
                                    }
                                    IconButton(
                                        modifier = Modifier.align(Alignment.TopEnd),
                                        onClick = {
                                            viewModel.group = group
                                            viewModel.showFilePicker = true
                                        }) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_image),
                                            contentDescription = "Visibility",
                                            modifier = Modifier.width(16.dp).height(16.dp)
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    enabled = group.active,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    value = group.name, onValueChange = {
                                        group.name = it
                                        group.changed = true
                                    },
                                    maxLines = 2,
                                    textStyle = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
                Row(modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)) {
                    Column(
                        modifier = Modifier.weight(0.5f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TextButton(onClick = {
                            viewModel.syncGroups()
                        }) {
                            Text("Discard")
                        }
                    }
                    Column(
                        modifier = Modifier.weight(0.5f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(onClick = {
                            coroutineScope.launch {
                                viewModel.saveGroups()
                            }
                        }) {
                            Text("Update")
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
