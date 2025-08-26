package com.ampairs.workspace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.ampairs.workspace.domain.Workspace
import com.ampairs.workspace.viewmodel.OfflineFirstWorkspaceListViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineFirstWorkspaceListScreen(
    onNavigateToCreateWorkspace: () -> Unit,
    onWorkspaceSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OfflineFirstWorkspaceListViewModel = koinInject(),
) {
    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        viewModel.searchWorkspaces(searchQuery)
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar with offline/online indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search workspaces...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                
                // Offline/Online Mode Indicator
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = if (state.isOfflineMode) 
                        MaterialTheme.colorScheme.errorContainer 
                    else 
                        MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (state.isOfflineMode) Icons.Default.WifiOff else Icons.Default.Wifi,
                            contentDescription = if (state.isOfflineMode) "Offline" else "Online",
                            tint = if (state.isOfflineMode) 
                                MaterialTheme.colorScheme.onErrorContainer 
                            else 
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (state.isOfflineMode) "Offline" else "Online",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isOfflineMode) 
                                MaterialTheme.colorScheme.onErrorContainer 
                            else 
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Error message with retry option
            state.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.isOfflineMode && state.workspaces.isNotEmpty())
                            MaterialTheme.colorScheme.secondaryContainer
                        else 
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (state.isOfflineMode) Icons.Default.CloudOff else Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if (state.isOfflineMode && state.workspaces.isNotEmpty())
                                MaterialTheme.colorScheme.onSecondaryContainer
                            else 
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = if (state.isOfflineMode && state.workspaces.isNotEmpty()) 
                                "Showing cached data • $error"
                            else 
                                error,
                            color = if (state.isOfflineMode && state.workspaces.isNotEmpty())
                                MaterialTheme.colorScheme.onSecondaryContainer
                            else 
                                MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Row {
                            // Retry button for network errors
                            if (!state.isRefreshing) {
                                IconButton(
                                    onClick = { viewModel.refreshWorkspaces() }
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Retry",
                                        tint = if (state.isOfflineMode && state.workspaces.isNotEmpty())
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        else 
                                            MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            
                            TextButton(
                                onClick = { viewModel.clearError() }
                            ) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            // Content
            when {
                state.isLoading && state.workspaces.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading workspaces...")
                        }
                    }
                }

                state.hasNoWorkspaces && searchQuery.isEmpty() -> {
                    // No workspaces state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Business,
                                contentDescription = "No workspaces",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No Workspaces Yet",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Create your first workspace to get started",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onNavigateToCreateWorkspace
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Workspace")
                            }
                        }
                    }
                }

                state.workspaces.isEmpty() && searchQuery.isNotEmpty() -> {
                    // No search results
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No workspaces found",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Try a different search term",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                else -> {
                    // Workspace list with pull-to-refresh indicator
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Pull-to-refresh indicator
                        if (state.isRefreshing) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Refreshing...",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                        
                        items(state.workspaces) { workspace ->
                            OfflineFirstWorkspaceCard(
                                workspace = workspace,
                                isOfflineMode = state.isOfflineMode,
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.selectWorkSpace(workspace.id)
                                        onWorkspaceSelected(workspace.id)
                                    }
                                }
                            )
                        }

                        // Bottom padding for FAB
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
        
        // Floating Action Button
        FloatingActionButton(
            onClick = onNavigateToCreateWorkspace,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Workspace")
        }
    }
}

@Composable
private fun OfflineFirstWorkspaceCard(
    workspace: Workspace,
    isOfflineMode: Boolean = false,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOfflineMode) 
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with enhanced visual indicators
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (workspace.avatarUrl.isNullOrEmpty()) {
                    Text(
                        text = workspace.name.take(2).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    // TODO: Load avatar image
                    Icon(
                        Icons.Default.Business,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content with improved layout
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = workspace.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                workspace.description?.let { description ->
                    if (description.isNotEmpty()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Workspace type with enhanced styling
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = workspace.workspaceType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    workspace.memberCount?.let { count ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$count members",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    
                    // Offline sync indicator
                    if (isOfflineMode) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = "Offline",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Cached",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}