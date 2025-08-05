package com.ampairs.product.ui.tax.tax_info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.product.domain.TaxInfo
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxInfoScreen(modifier: Modifier, id: String?, onTaxInfoUpdate: (String) -> Unit) {

    val viewModel: TaxInfoViewModel = koinInject { parametersOf(id ?: "") }

    if (id.isNullOrEmpty()) {
        viewModel.taxInfoState = TaxInfoState(TaxInfo())
    }
    val taxInfoState = viewModel.taxInfoState

    if (!id.isNullOrEmpty() && taxInfoState.taxInfo.id.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
        viewModel.reSyncTaxInfo(id)
    } else {
        LazyColumn {
            item {
                Column(
                    modifier = modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier.padding(2.dp).fillMaxWidth()
                    ) {
                        val inputModifier =
                            Modifier.padding(horizontal = 4.dp, vertical = 4.dp).fillMaxWidth()
                        OutlinedTextField(
                            modifier = inputModifier,
                            label = { Text("Name") },
                            value = taxInfoState.name,
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                            onValueChange = {
                                taxInfoState.name = it
                            },
                        )
                        OutlinedTextField(
                            modifier = inputModifier,
                            label = { Text("percent") },
                            value = taxInfoState.percentage.toString(),
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            onValueChange = { text ->
                                taxInfoState.percentage = text.filter { it.isDigit() }
                            },
                            maxLines = 1,
                        )
                        OutlinedTextField(
                            modifier = inputModifier,
                            label = { Text("Formatted Name") },
                            value = taxInfoState.formattedName.toString(),
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                            onValueChange = {
                                taxInfoState.formattedName = it
                            },
                            maxLines = 1,
                        )
                        ExposedDropdownMenuBox(
                            modifier = inputModifier,
                            expanded = taxInfoState.taxSpecExpanded, onExpandedChange = {
                                taxInfoState.taxSpecExpanded = it
                            }) {
                            OutlinedTextField(
                                readOnly = true,
                                value = taxInfoState.taxSpec.name,
                                onValueChange = {},
                                label = { Text("Tax Spec") },
//                                modifier = Modifier.menuAnchor(type, enabled).fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = taxInfoState.taxSpecExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            )
                            ExposedDropdownMenu(
                                expanded = taxInfoState.taxSpecExpanded,
                                onDismissRequest = { taxInfoState.taxSpecExpanded = false },
                                modifier = inputModifier
                            ) {
                                taxInfoState.taxSpecs.forEachIndexed { index, taxSpec ->
                                    DropdownMenuItem(
                                        text = { Text(text = taxSpec.name) },
                                        onClick = {
                                            taxInfoState.selectedSpecIndex = index
                                            taxInfoState.taxSpecExpanded = false
                                            taxInfoState.taxSpec = taxSpec
                                        })
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ElevatedButton(onClick = {
                            val updatedCustomerId = viewModel.updateTaxInfo()
                            onTaxInfoUpdate(updatedCustomerId)
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

}