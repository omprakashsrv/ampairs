package com.ampairs.customer.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampairs.customer.domain.Customer
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    customerId: String,
    onNavigateBack: () -> Unit,
    onEditCustomer: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerDetailsViewModel = koinInject { parametersOf(customerId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(customerId) {
        viewModel.loadCustomer()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(uiState.customer?.name ?: "Customer Details") },
            actions = {
                if (uiState.customer != null) {
                    IconButton(onClick = { onEditCustomer(customerId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
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

            uiState.error != null -> {
                val errorMessage = uiState.error ?: return@Column
                ErrorMessage(
                    error = errorMessage,
                    onRetry = viewModel::loadCustomer,
                    modifier = Modifier.fillMaxSize()
                )
            }

            uiState.customer != null -> {
                val currentCustomer = uiState.customer ?: return@Column
                CustomerDetailsContent(
                    customer = currentCustomer,
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Customer not found")
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            customerName = uiState.customer?.name ?: "",
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteCustomer {
                    onNavigateBack()
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun CustomerDetailsContent(
    customer: Customer,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Basic Information
        InfoSection(title = "Basic Information") {
            InfoRow(label = "Name", value = customer.name)
            customer.email?.let {
                InfoRow(label = "Email", value = it)
            }
            customer.phone?.let {
                InfoRow(label = "Phone", value = it)
            }
            customer.gstin?.let {
                InfoRow(label = "GSTIN", value = it)
            }
        }

        // Address Information
        if (customer.address != null || customer.street != null || customer.city != null) {
            InfoSection(title = "Address") {
                customer.address?.let {
                    InfoRow(label = "Address", value = it)
                }
                customer.street?.let {
                    InfoRow(label = "Street", value = it)
                }
                customer.city?.let {
                    InfoRow(label = "City", value = it)
                }
                customer.state?.let {
                    InfoRow(label = "State", value = it)
                }
                customer.pincode?.let {
                    InfoRow(label = "PIN Code", value = it)
                }
                InfoRow(label = "Country", value = customer.country)
            }
        }

        // Billing Address
        customer.billingAddress?.let { billingAddress ->
            InfoSection(title = "Billing Address") {
                InfoRow(label = "Street", value = billingAddress.street)
                InfoRow(label = "City", value = billingAddress.city)
                InfoRow(label = "State", value = billingAddress.state)
                InfoRow(label = "PIN Code", value = billingAddress.pincode)
                InfoRow(label = "Country", value = billingAddress.country)
            }
        }

        // Shipping Address
        customer.shippingAddress?.let { shippingAddress ->
            InfoSection(title = "Shipping Address") {
                InfoRow(label = "Street", value = shippingAddress.street)
                InfoRow(label = "City", value = shippingAddress.city)
                InfoRow(label = "State", value = shippingAddress.state)
                InfoRow(label = "PIN Code", value = shippingAddress.pincode)
                InfoRow(label = "Country", value = shippingAddress.country)
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(2f)
        )
    }
}

@Composable
private fun ErrorMessage(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Failed to load customer",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    customerName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Customer") },
        text = {
            Text("Are you sure you want to delete $customerName? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}