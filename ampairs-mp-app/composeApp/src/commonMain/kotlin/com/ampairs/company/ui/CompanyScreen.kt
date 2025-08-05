package com.ampairs.company.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.common.model.UiState
import com.ampairs.company.domain.Company
import com.ampairs.company.viewmodel.CompanyViewModel
import com.ampairs.ui.components.Phone
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf


@Composable
fun CompanyScreen(modifier: Modifier = Modifier, id: String?, onCustomerUpdate: (String) -> Unit) {

    val viewModel: CompanyViewModel = koinInject { parametersOf(id) }
    if (id.isNullOrEmpty()) {
        viewModel.company = CompanyState(Company())
    }
    val company = viewModel.company
    if (!id.isNullOrEmpty() && company.company.id.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
        viewModel.reSyncCompany(id)
    } else {
        val inputModifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp).fillMaxWidth()


        val uiState = viewModel.companyState.value

        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.widthIn(400.dp).weight(1f)) {
                Column(
                    modifier = Modifier
                        .padding(4.dp)
                        .widthIn(0.dp, 400.dp)
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    OutlinedTextField(
                        modifier = inputModifier,
                        label = { Text("Name") },
                        value = company.name,
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                        onValueChange = {
                            company.name = it
                        },
                    )
                    Phone(
                        modifier = inputModifier,
                        countryCode = company.countryCode,
                        phone = company.phone.toString(),
                        onValueChange = {
                            company.phone = it.filter { it.isDigit() }
                        },
                        onValidChange = {

                        })
                    OutlinedTextField(
                        modifier = inputModifier,
                        label = { Text("Landline") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone),
                        value = company.landline ?: "",
                        onValueChange = { landline ->
                            company.landline = landline.filter { it.isDigit() }
                        },
                        maxLines = 1,
                    )
                    OutlinedTextField(
                        modifier = inputModifier,
                        label = { Text("Email") },
                        value = company.email ?: "",
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                        onValueChange = {
                            company.email = it.lowercase().replace(" ", "")
                        },
                        maxLines = 1,
                    )
                    OutlinedTextField(
                        modifier = inputModifier,
                        label = { Text("GSTIN") },
                        value = company.gstin ?: "",
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                        onValueChange = {
                            company.gstin = it.uppercase().replace(" ", "")
                        },
                        maxLines = 1,
                    )
                    OutlinedTextField(
                        modifier = inputModifier,
                        label = { Text("Address") },
                        value = company.address ?: "",
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                        onValueChange = {
                            company.address = it
                        },
                        maxLines = 2,
                        textStyle = MaterialTheme.typography.labelMedium
                    )
                    OutlinedTextField(
                        modifier = inputModifier,
                        label = { Text("Pincode") },
                        value = company.pincode ?: "",
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        onValueChange = {
                            company.pincode = it.filter { it.isDigit() }
                        },
                        maxLines = 1,
                    )
                    OutlinedTextField(
                        modifier = inputModifier,
                        label = { Text("State") },
                        value = company.state ?: "",
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                        onValueChange = {
                            company.state = it
                        },
                        maxLines = 1,
                    )
                }
            }

            ElevatedButton(onClick = {
                val updatedCustomerId = viewModel.updateCompany()
                onCustomerUpdate(updatedCustomerId)
            }) {
                if (uiState is UiState.Loading) {
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