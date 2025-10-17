package com.ampairs.business.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ampairs.business.domain.Business
import org.koin.compose.koinInject

@Composable
fun BusinessBrandingScreen(
    modifier: Modifier = Modifier,
    viewModel: BusinessProfileViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val business = uiState.business

    var ownerName by remember(business) { mutableStateOf(business?.ownerName ?: "") }
    var phone by remember(business) { mutableStateOf(business?.phone ?: "") }
    var email by remember(business) { mutableStateOf(business?.email ?: "") }
    var website by remember(business) { mutableStateOf(business?.website ?: "") }
    var addressLine1 by remember(business) { mutableStateOf(business?.addressLine1 ?: "") }
    var addressLine2 by remember(business) { mutableStateOf(business?.addressLine2 ?: "") }
    var city by remember(business) { mutableStateOf(business?.city ?: "") }
    var state by remember(business) { mutableStateOf(business?.state ?: "") }
    var postalCode by remember(business) { mutableStateOf(business?.postalCode ?: "") }
    var country by remember(business) { mutableStateOf(business?.country ?: "") }

    Scaffold(
        floatingActionButton = {
            if (!uiState.isSaving) {
                FloatingActionButton(
                    onClick = {
                        val updatedBusiness = business?.copy(
                            ownerName = ownerName.ifBlank { null },
                            phone = phone.ifBlank { null },
                            email = email.ifBlank { null },
                            website = website.ifBlank { null },
                            addressLine1 = addressLine1.ifBlank { null },
                            addressLine2 = addressLine2.ifBlank { null },
                            city = city.ifBlank { null },
                            state = state.ifBlank { null },
                            postalCode = postalCode.ifBlank { null },
                            country = country.ifBlank { null }
                        ) ?: return@FloatingActionButton

                        viewModel.saveBusiness(updatedBusiness)
                    }
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save Branding")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Business Branding & Contact",
                style = MaterialTheme.typography.headlineMedium
            )

            if (uiState.error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.error ?: "Error",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Owner Information
            Text(
                text = "Owner Information",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text("Owner Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            )

            // Contact Information
            Text(
                text = "Contact Information",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                placeholder = { Text("+91 1234567890") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                placeholder = { Text("contact@business.com") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            )

            OutlinedTextField(
                value = website,
                onValueChange = { website = it },
                label = { Text("Website") },
                placeholder = { Text("https://www.business.com") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            )

            HorizontalDivider()

            // Address Information
            Text(
                text = "Business Address",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = addressLine1,
                onValueChange = { addressLine1 = it },
                label = { Text("Address Line 1") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            )

            OutlinedTextField(
                value = addressLine2,
                onValueChange = { addressLine2 = it },
                label = { Text("Address Line 2") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSaving
                )

                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("State") },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSaving
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = postalCode,
                    onValueChange = { postalCode = it },
                    label = { Text("Postal Code") },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSaving
                )

                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Country") },
                    placeholder = { Text("India") },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSaving
                )
            }

            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
        }
    }
}
