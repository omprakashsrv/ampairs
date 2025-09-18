package com.ampairs.customer.ui.create

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.customer.domain.Customer
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFormScreen(
    customerId: String? = null,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerFormViewModel = koinInject { parametersOf(customerId) }
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(customerId) {
        if (customerId != null) {
            viewModel.loadCustomer()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (customerId == null) "New Customer" else "Edit Customer") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                TextButton(
                    onClick = {
                        viewModel.saveCustomer { onSaveSuccess() }
                    },
                    enabled = uiState.canSave && !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save")
                    }
                }
            }
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                CustomerForm(
                    formState = uiState.formState,
                    onFormChange = viewModel::updateForm,
                    error = uiState.error,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CustomerForm(
    formState: CustomerFormState,
    onFormChange: (CustomerFormState) -> Unit,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (error != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Basic Information
        FormSection(title = "Basic Information") {
            OutlinedTextField(
                value = formState.name,
                onValueChange = { onFormChange(formState.copy(name = it)) },
                label = { Text("Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = formState.nameError != null,
                supportingText = formState.nameError?.let { { Text(it) } }
            )

            OutlinedTextField(
                value = formState.email,
                onValueChange = { onFormChange(formState.copy(email = it)) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = formState.emailError != null,
                supportingText = formState.emailError?.let { { Text(it) } }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = formState.countryCode.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { code ->
                            onFormChange(formState.copy(countryCode = code))
                        }
                    },
                    label = { Text("Code") },
                    modifier = Modifier.weight(0.3f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = formState.phone,
                    onValueChange = { onFormChange(formState.copy(phone = it)) },
                    label = { Text("Phone") },
                    modifier = Modifier.weight(0.7f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }

            OutlinedTextField(
                value = formState.gstin,
                onValueChange = { onFormChange(formState.copy(gstin = it)) },
                label = { Text("GSTIN") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Address Information
        FormSection(title = "Address") {
            OutlinedTextField(
                value = formState.address,
                onValueChange = { onFormChange(formState.copy(address = it)) },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            OutlinedTextField(
                value = formState.street,
                onValueChange = { onFormChange(formState.copy(street = it)) },
                label = { Text("Street") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = formState.city,
                    onValueChange = { onFormChange(formState.copy(city = it)) },
                    label = { Text("City") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = formState.pincode,
                    onValueChange = { onFormChange(formState.copy(pincode = it)) },
                    label = { Text("PIN Code") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = formState.state,
                    onValueChange = { onFormChange(formState.copy(state = it)) },
                    label = { Text("State") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = formState.country,
                    onValueChange = { onFormChange(formState.copy(country = it)) },
                    label = { Text("Country") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}