package com.ampairs.product.ui.tax.tax_code

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.product.domain.TaxCode
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun TaxCodeScreen(modifier: Modifier, id: String?, onTaxInfoUpdate: (String) -> Unit) {

    val viewModel: TaxCodeViewModel = koinInject()

    if (id.isNullOrEmpty()) {
        viewModel.taxCodeState = TaxCodeState(TaxCode())
    }
    val taxCodeState = viewModel.taxCodeState

    if (!id.isNullOrEmpty() && taxCodeState.taxCode.id.isEmpty()) {
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
                            label = { Text("HSN/SAC Code") },
                            value = taxCodeState.code,
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                            onValueChange = {
                                taxCodeState.code = it
                            },
                        )
                        OutlinedTextField(
                            modifier = inputModifier,
                            label = { Text("Description") },
                            value = taxCodeState.description,
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                            onValueChange = {
                                taxCodeState.description = it
                            },
                        )

                        OutlinedTextField(
                            readOnly = true,
                            enabled = false,
                            modifier = inputModifier.clickable {
                                taxCodeState.showDatePicker = true
                            },
                            value = if (taxCodeState.effectiveFrom != null) taxCodeState.effectiveFrom.toString() else "",
                            onValueChange = {},
                            trailingIcon = {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null
                                )
                            },
                            label = { Text("Effective From") },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        if (taxCodeState.showDatePicker) {
                            val datePickerState = rememberDatePickerState(
                                initialDisplayMode = DisplayMode.Picker,
                                initialDisplayedMonthMillis = taxCodeState.effectiveFrom?.atStartOfDayIn(
                                    TimeZone.currentSystemDefault()
                                )?.toEpochMilliseconds() ?: Clock.System.now().toEpochMilliseconds()
                            )
                            DatePickerDialog(onDismissRequest = {
                                taxCodeState.showDatePicker = false
                            }, confirmButton = {
                                Button(onClick = {
                                    taxCodeState.showDatePicker = false
                                    taxCodeState.effectiveFrom =
                                        datePickerState.selectedDateMillis?.let {
                                            Instant.fromEpochMilliseconds(it).toLocalDateTime(
                                                TimeZone.currentSystemDefault()
                                            ).date
                                        }
                                }) {
                                    Text("Confirm")
                                }
                            }, dismissButton = {
                                TextButton(onClick = {
                                    taxCodeState.showDatePicker = false
                                }) {
                                    Text("Cancel")
                                }
                            }) {
                                DatePicker(
                                    state = datePickerState,
                                )
                            }
                        }

                        ExposedDropdownMenuBox(modifier = inputModifier,
                            expanded = taxCodeState.taxTypeExpanded, onExpandedChange = {
                                taxCodeState.taxTypeExpanded = it
                            }) {

                            OutlinedTextField(
                                readOnly = true,
                                value = taxCodeState.type.name,
                                onValueChange = {},
                                label = { Text("Tax Spec") },
//                                modifier = Modifier.menuAnchor(type, enabled).fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = taxCodeState.taxTypeExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            )

                            ExposedDropdownMenu(
                                expanded = taxCodeState.taxTypeExpanded,
                                onDismissRequest = { taxCodeState.taxTypeExpanded = false },
                                modifier = inputModifier
                            ) {
                                taxCodeState.taxTypes.forEachIndexed { index, taxType ->
                                    DropdownMenuItem(
                                        text = { Text(text = taxType.name) },
                                        onClick = {
                                            taxCodeState.selectedTypeIndex = index
                                            taxCodeState.taxTypeExpanded = false
                                            taxCodeState.type = taxType
                                        })
                                }
                            }

                        }

                        Text("Taxes", modifier = inputModifier.fillMaxWidth().clickable {
                            taxCodeState.taxInfoExpanded = true
                        }, style = MaterialTheme.typography.labelSmall)

                        ExposedDropdownMenuBox(modifier = inputModifier.fillMaxWidth(),
                            expanded = taxCodeState.taxInfoExpanded, onExpandedChange = {
                                taxCodeState.taxInfoExpanded = it
                            }) {

                            BasicTextField(
                                readOnly = true,
                                value = taxCodeState.type.name,
                                onValueChange = {},
//                                modifier = Modifier.menuAnchor(type, enabled).fillMaxWidth(),
                                decorationBox = {}
                            )

                            Row(
                                modifier = Modifier.clickable {
                                    taxCodeState.taxInfoExpanded = true
                                }.heightIn(min = 48.dp).fillMaxWidth()
                            ) {
                                taxCodeState.taxInfos.forEachIndexed { index, taxInfo ->
                                    InputChip(
                                        label = { Text(taxInfo.formattedName) },
                                        modifier = Modifier.padding(
                                            horizontal = 2.dp,
                                            vertical = 4.dp
                                        ),
                                        onClick = { taxCodeState.taxInfoExpanded = true },
                                        selected = false,
                                        trailingIcon = {
                                            IconButton(
                                                onClick = {
                                                    taxCodeState.taxInfos =
                                                        taxCodeState.taxInfos.toMutableList()
                                                            .also { it.remove(taxInfo) }
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Filled.Close,
                                                    "contentDescription",
                                                )
                                            }
                                        }
                                    )
                                }
                            }

                            ExposedDropdownMenu(
                                expanded = taxCodeState.taxInfoExpanded,
                                onDismissRequest = { taxCodeState.taxInfoExpanded = false },
                                modifier = inputModifier
                            ) {
                                viewModel.taxInfoList.forEachIndexed { index, taxInfo ->
                                    DropdownMenuItem(
                                        text = { Text(text = taxInfo.formattedName) },
                                        onClick = {
                                            val indexOfFirst =
                                                taxCodeState.taxInfos.indexOfFirst { it.id == taxInfo.id }
                                            if (indexOfFirst == -1) {
                                                taxCodeState.taxInfos =
                                                    taxCodeState.taxInfos.toMutableList()
                                                        .also { it.add(taxInfo) }
                                            }
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