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
import com.ampairs.business.domain.BusinessType
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: BusinessProfileViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val business = uiState.business

    var name by remember(business) { mutableStateOf(business?.name ?: "") }
    var businessType by remember(business) { mutableStateOf(business?.businessType ?: BusinessType.RETAIL) }
    var description by remember(business) { mutableStateOf(business?.description ?: "") }
    var timezone by remember(business) { mutableStateOf(business?.timezone ?: "UTC") }
    var currency by remember(business) { mutableStateOf(business?.currency ?: "INR") }
    var language by remember(business) { mutableStateOf(business?.language ?: "en") }
    var dateFormat by remember(business) { mutableStateOf(business?.dateFormat ?: "DD-MM-YYYY") }
    var timeFormat by remember(business) { mutableStateOf(business?.timeFormat ?: "12H") }
    var showTypeDropdown by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (!uiState.isSaving) {
                FloatingActionButton(
                    onClick = {
                        val updatedBusiness = business?.copy(
                            name = name,
                            businessType = businessType,
                            description = description.ifBlank { null },
                            timezone = timezone,
                            currency = currency,
                            language = language,
                            dateFormat = dateFormat,
                            timeFormat = timeFormat
                        ) ?: Business(
                            name = name,
                            businessType = businessType,
                            description = description.ifBlank { null },
                            timezone = timezone,
                            currency = currency,
                            language = language,
                            dateFormat = dateFormat,
                            timeFormat = timeFormat
                        )
                        viewModel.saveBusiness(updatedBusiness)
                    }
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save Settings")
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
                text = "Business Settings",
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

            // Basic Information
            Text(
                text = "Basic Information",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Business Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            )

            ExposedDropdownMenuBox(
                expanded = showTypeDropdown,
                onExpandedChange = { showTypeDropdown = !showTypeDropdown }
            ) {
                OutlinedTextField(
                    value = businessType.name.replace("_", " "),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Business Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    enabled = !uiState.isSaving
                )

                ExposedDropdownMenu(
                    expanded = showTypeDropdown,
                    onDismissRequest = { showTypeDropdown = false }
                ) {
                    BusinessType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name.replace("_", " ")) },
                            onClick = {
                                businessType = type
                                showTypeDropdown = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                enabled = !uiState.isSaving
            )

            HorizontalDivider()

            // Regional Settings
            Text(
                text = "Regional Settings",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = timezone,
                onValueChange = { timezone = it },
                label = { Text("Timezone") },
                placeholder = { Text("e.g., Asia/Kolkata") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = { Text("Currency") },
                    placeholder = { Text("INR") },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSaving
                )

                OutlinedTextField(
                    value = language,
                    onValueChange = { language = it },
                    label = { Text("Language") },
                    placeholder = { Text("en") },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSaving
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = dateFormat,
                    onValueChange = { dateFormat = it },
                    label = { Text("Date Format") },
                    placeholder = { Text("DD-MM-YYYY") },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSaving
                )

                OutlinedTextField(
                    value = timeFormat,
                    onValueChange = { timeFormat = it },
                    label = { Text("Time Format") },
                    placeholder = { Text("12H or 24H") },
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
