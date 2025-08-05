package com.ampairs.product.ui.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.product.domain.Product
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditScreen(
    modifier: Modifier = Modifier,
    id: String?,
    onProductUpdated: (String?) -> Unit
) {

    val viewModel: ProductEditViewModel = koinInject<ProductEditViewModel>()

    if (id.isNullOrEmpty()) {
        viewModel.productState = ProductState(Product())
    }
    val productState = viewModel.productState
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        productState.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onProductUpdated(null) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Localized description"
                        )
                    }
                },
            )
        },
    ) {

        if (!id.isNullOrEmpty() && productState.product.id.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
            viewModel.reSyncProductState(id)
        } else {
            val inputModifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp).fillMaxWidth()
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.padding(2.dp).fillMaxWidth().weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        modifier = inputModifier,
                        label = { Text("Name") },
                        value = productState.name,
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                        onValueChange = {
                            productState.name = it
                        },
                    )
                    OutlinedTextField(
                        modifier = inputModifier,
                        label = { Text("Code") },
                        value = productState.code,
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                        onValueChange = {
                            productState.code = it
                        },
                    )
                    ExposedDropdownMenuBox(
                        modifier = inputModifier,
                        expanded = productState.groupExpanded, onExpandedChange = {
                            productState.groupExpanded = it
                        }) {

                        OutlinedTextField(
                            readOnly = true,
                            value = productState.group?.name ?: "",
                            onValueChange = {},
                            label = { Text("Group") },
//                            modifier = Modifier.menuAnchor(type, enabled).fillMaxWidth(),
                            trailingIcon = { TrailingIcon(expanded = productState.groupExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        )

                        ExposedDropdownMenu(
                            expanded = productState.groupExpanded,
                            onDismissRequest = { productState.groupExpanded = false },
                            modifier = inputModifier
                        ) {
                            viewModel.groups.forEachIndexed { _, brand ->
                                DropdownMenuItem(
                                    text = { Text(text = brand.name) },
                                    onClick = {
                                        productState.groupExpanded = false
                                        productState.group = brand
                                    })
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        modifier = inputModifier,
                        expanded = productState.categoryExpanded, onExpandedChange = {
                            productState.categoryExpanded = it
                        }) {

                        OutlinedTextField(
                            readOnly = true,
                            value = productState.category?.name ?: "",
                            onValueChange = {},
                            label = { Text("Category") },
//                            modifier = Modifier.menuAnchor(type, enabled).fillMaxWidth(),
                            trailingIcon = { TrailingIcon(expanded = productState.categoryExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        )

                        ExposedDropdownMenu(
                            expanded = productState.categoryExpanded,
                            onDismissRequest = { productState.categoryExpanded = false },
                            modifier = inputModifier
                        ) {
                            viewModel.categories.forEachIndexed { _, brand ->
                                DropdownMenuItem(
                                    text = { Text(text = brand.name) },
                                    onClick = {
                                        productState.categoryExpanded = false
                                        productState.category = brand
                                    })
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        modifier = inputModifier,
                        expanded = productState.subCategoryExpanded, onExpandedChange = {
                            productState.subCategoryExpanded = it
                        }) {

                        OutlinedTextField(
                            readOnly = true,
                            value = productState.subCategory?.name ?: "",
                            onValueChange = {},
                            label = { Text("Sub Category") },
//                            modifier = Modifier.menuAnchor(type, enabled).fillMaxWidth(),
                            trailingIcon = { TrailingIcon(expanded = productState.categoryExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        )

                        ExposedDropdownMenu(
                            expanded = productState.subCategoryExpanded,
                            onDismissRequest = { productState.subCategoryExpanded = false },
                            modifier = inputModifier
                        ) {
                            viewModel.subCategories.forEachIndexed { _, subCategory ->
                                DropdownMenuItem(
                                    text = { Text(text = subCategory.name) },
                                    onClick = {
                                        productState.subCategoryExpanded = false
                                        productState.subCategory = subCategory
                                    })
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        modifier = inputModifier,
                        expanded = productState.brandExpanded, onExpandedChange = {
                            productState.brandExpanded = it
                        }) {

                        OutlinedTextField(
                            readOnly = true,
                            value = productState.brand?.name ?: "",
                            onValueChange = {},
                            label = { Text("Brand") },
//                            modifier = Modifier.menuAnchor(type, enabled).fillMaxWidth(),
                            trailingIcon = { TrailingIcon(expanded = productState.brandExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        )

                        ExposedDropdownMenu(
                            expanded = productState.brandExpanded,
                            onDismissRequest = { productState.brandExpanded = false },
                            modifier = inputModifier
                        ) {
                            viewModel.brands.forEachIndexed { _, brand ->
                                DropdownMenuItem(
                                    text = { Text(text = brand.name) },
                                    onClick = {
                                        productState.brandExpanded = false
                                        productState.brand = brand
                                    })
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        modifier = inputModifier,
                        expanded = productState.taxCodeExpanded, onExpandedChange = {
                            productState.taxCodeExpanded = it
                        }) {

                        OutlinedTextField(
                            readOnly = true,
                            value = productState.taxCode?.code ?: "",
                            onValueChange = {},
                            label = { Text("Tax Code") },
//                            modifier = Modifier.menuAnchor(type, enabled).fillMaxWidth(),
                            trailingIcon = { TrailingIcon(expanded = productState.taxCodeExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        )

                        ExposedDropdownMenu(
                            expanded = productState.taxCodeExpanded,
                            onDismissRequest = { productState.taxCodeExpanded = false },
                            modifier = inputModifier
                        ) {
                            viewModel.taxCodes.forEachIndexed { _, taxCode ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(text = taxCode.code)
                                            Row {
                                                taxCode.taxInfos.forEachIndexed { _, taxInfo ->
                                                    Text(
                                                        text = taxInfo.formattedName,
                                                        modifier = Modifier.padding(horizontal = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        productState.taxCodeExpanded = false
                                        productState.taxCode = taxCode
                                    })
                            }
                        }
                    }
                    OutlinedTextField(
                        modifier = inputModifier,
                        label = { Text("MRP") },
                        value = if (productState.mrp == 0.0) "" else (if (productState.mrp - productState.mrp.toInt() > 0 || productState.mrpFraction) "${productState.mrp}" else "${productState.mrp.toInt()}"),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        onValueChange = {
                            productState.mrp = if (it.isEmpty()) 0.0 else try {
                                if (productState.mrpFraction && !it.contains(".")) productState.mrp.toInt()
                                    .toDouble() else it.toDouble()
                            } catch (e: Exception) {
                                productState.mrp
                            }
                            productState.mrpFraction = it.contains(".")
                        },
                        maxLines = 1,
                    )
                    OutlinedTextField(
                        modifier = inputModifier,
                        label = { Text("DP") },
                        value = if (productState.dp == 0.0) "" else (if (productState.dp - productState.dp.toInt() > 0 || productState.dpFraction) "${productState.dp}" else "${productState.dp.toInt()}"),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        onValueChange = {
                            productState.dp = if (it.isEmpty()) 0.0 else try {
                                if (productState.dpFraction && !it.contains(".")) productState.dp.toInt()
                                    .toDouble() else it.toDouble()
                            } catch (e: Exception) {
                                productState.dp
                            }
                            productState.dpFraction = it.contains(".")
                        },
                        maxLines = 1,
                    )
                    OutlinedTextField(
                        modifier = inputModifier,
                        label = { Text("Selling Price") },
                        value = if (productState.sellingPrice == 0.0) "" else (if (productState.sellingPrice - productState.sellingPrice.toInt() > 0 || productState.sellingPriceFraction) "${productState.sellingPrice}" else "${productState.sellingPrice.toInt()}"),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        onValueChange = {
                            productState.sellingPrice = if (it.isEmpty()) 0.0 else try {
                                if (productState.sellingPriceFraction && !it.contains(".")) productState.sellingPrice.toInt()
                                    .toDouble() else it.toDouble()
                            } catch (e: Exception) {
                                productState.sellingPrice
                            }
                            productState.sellingPriceFraction = it.contains(".")
                        },
                        maxLines = 1,
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ElevatedButton(onClick = {
                        val updateProductId = viewModel.updateProduct()
                        onProductUpdated(updateProductId)
                    }) {
                        if (viewModel.loading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .progressSemantics()
                                    .size(24.dp)
                            )
                        } else {
                            Text(if (viewModel.id.isNullOrEmpty()) "Create New" else "Update")
                        }
                    }
                }
            }
        }
    }

}