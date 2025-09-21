package com.ampairs.customer.ui.state

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ampairs.customer.domain.State
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateListScreen(
    onStateClick: (String) -> Unit,
    onImportStates: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StateListViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val focusManager = LocalFocusManager.current

    var showImportDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Header with search and add button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "States",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    FloatingActionButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Import States"
                        )
                    }
                }

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    label = { Text("Search states") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { focusManager.clearFocus() }
                    ),
                    singleLine = true
                )
            }
        }

        // Error message
        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Content
        when {
            uiState.isLoading && uiState.states.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.states.isEmpty() && !uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No states found" else "No states available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isBlank()) {
                            Button(onClick = { showImportDialog = true }) {
                                Text("Import States")
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = uiState.states,
                        key = { it.id }
                    ) { state ->
                        StateListItem(
                            state = state,
                            onStateClick = { onStateClick(state.id) },
                            onDeleteClick = { viewModel.deleteState(state.id) }
                        )
                    }

                    if (uiState.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Import States Dialog
    if (showImportDialog) {
        StateImportDialog(
            onDismiss = { showImportDialog = false },
            onImportState = { stateCode ->
                viewModel.importState(stateCode)
                showImportDialog = false
            },
            onBulkImport = { stateCodes ->
                viewModel.bulkImportStates(stateCodes)
                showImportDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StateListItem(
    state: State,
    onStateClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onStateClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = state.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete State",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete State") },
            text = { Text("Are you sure you want to delete \"${state.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StateImportDialog(
    onDismiss: () -> Unit,
    onImportState: (String) -> Unit,
    onBulkImport: (List<String>) -> Unit
) {
    var selectedStates by remember { mutableStateOf(setOf<String>()) }

    // Common Indian state codes for import
    val availableStates = listOf(
        "AP" to "Andhra Pradesh",
        "AR" to "Arunachal Pradesh",
        "AS" to "Assam",
        "BR" to "Bihar",
        "CG" to "Chhattisgarh",
        "GA" to "Goa",
        "GJ" to "Gujarat",
        "HR" to "Haryana",
        "HP" to "Himachal Pradesh",
        "JK" to "Jammu and Kashmir",
        "JH" to "Jharkhand",
        "KA" to "Karnataka",
        "KL" to "Kerala",
        "MP" to "Madhya Pradesh",
        "MH" to "Maharashtra",
        "MN" to "Manipur",
        "ML" to "Meghalaya",
        "MZ" to "Mizoram",
        "NL" to "Nagaland",
        "OR" to "Odisha",
        "PB" to "Punjab",
        "RJ" to "Rajasthan",
        "SK" to "Sikkim",
        "TN" to "Tamil Nadu",
        "TG" to "Telangana",
        "TR" to "Tripura",
        "UP" to "Uttar Pradesh",
        "UT" to "Uttarakhand",
        "WB" to "West Bengal"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import States") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableStates) { (code, name) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedStates.contains(code),
                            onCheckedChange = { isChecked ->
                                selectedStates = if (isChecked) {
                                    selectedStates + code
                                } else {
                                    selectedStates - code
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$name ($code)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        if (selectedStates.isNotEmpty()) {
                            onBulkImport(selectedStates.toList())
                        }
                    },
                    enabled = selectedStates.isNotEmpty()
                ) {
                    Text("Import Selected (${selectedStates.size})")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}