package com.ampairs.business.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToBranding: () -> Unit = {},
    onNavigateToEdit: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: BusinessProfileViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    // When loading completes (after refresh finishes) reset local refreshing state
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            isRefreshing = false
        }
    }

    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = {
            if (!isRefreshing) {
                isRefreshing = true
                viewModel.refresh()
            }
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Business Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(32.dp)
                        )
                    }

                    uiState.error != null -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = uiState.error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    uiState.business != null -> {
                        BusinessProfileContent(
                            business = uiState.business!!,
                            onNavigateToSettings = onNavigateToSettings,
                            onNavigateToBranding = onNavigateToBranding,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    else -> {
                        // No business profile yet
                        EmptyBusinessState(
                            onCreateProfile = onNavigateToSettings, // Use settings screen for profile creation
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        )
                    }
                }

                // Action buttons
                if (uiState.business != null && viewModel.hasWorkspaceContext()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Settings")
                        }

                        OutlinedButton(
                            onClick = onNavigateToBranding,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Branding")
                        }
                    }
                }
            }

            // Pull-to-refresh indicator
            if (isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            // Floating Edit Button
            if (uiState.business != null && !uiState.isLoading) {
                FloatingActionButton(
                    onClick = onNavigateToEdit,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Business Profile")
                }
            }
        }
    }
}

@Composable
private fun BusinessProfileContent(
    business: com.ampairs.business.domain.Business,
    onNavigateToSettings: () -> Unit,
    onNavigateToBranding: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = business.name,
                style = MaterialTheme.typography.titleLarge
            )

            if (business.businessType != null) {
                Text(
                    text = "Type: ${business.businessType.name.replace("_", " ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (business.description != null) {
                Text(
                    text = business.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider()

            // Contact Information
            if (business.phone != null || business.email != null) {
                Text(
                    text = "Contact",
                    style = MaterialTheme.typography.titleMedium
                )

                business.phone?.let {
                    Text(text = "Phone: $it", style = MaterialTheme.typography.bodySmall)
                }

                business.email?.let {
                    Text(text = "Email: $it", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Address
            if (business.addressLine1 != null) {
                Text(
                    text = "Address",
                    style = MaterialTheme.typography.titleMedium
                )

                business.addressLine1?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }
                business.addressLine2?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }
                val cityStateZip = listOfNotNull(business.city, business.state, business.postalCode)
                    .joinToString(", ")
                if (cityStateZip.isNotBlank()) {
                    Text(text = cityStateZip, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Settings
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Currency: ${business.currency}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Timezone: ${business.timezone}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Language: ${business.language}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun EmptyBusinessState(
    onCreateProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Business Profile",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Create your business profile to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = onCreateProfile) {
            Text("Create Profile")
        }
    }
}
