package com.ampairs.workspace.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ampairs.workspace.api.model.AvailableModule
import com.ampairs.workspace.api.model.InstalledModule
import com.ampairs.workspace.viewmodel.WorkspaceModulesViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * Workspace modules screen showing active modules
 * Users can select modules to enter or browse the store to install new ones
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceModulesScreen(
    onModuleSelected: (moduleCode: String) -> Unit = {},
    workspaceId: String = "",
    showStoreByDefault: Boolean = false, // New parameter to control if module store should be shown by default
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: WorkspaceModulesViewModel = koinInject { parametersOf(workspaceId.takeIf { it.isNotEmpty() }) }
) {
    var showModuleStore by remember { mutableStateOf(showStoreByDefault) }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val activeModules by viewModel.activeModules.collectAsState()

    // Load data on first composition - force refresh to get fresh data from backend
    LaunchedEffect(Unit) {
        viewModel.loadInstalledModules(refresh = true) // Force API call via Store5
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        // Error handling
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Active modules list
            if (activeModules.isEmpty()) {
                EmptyStateCard(
                    title = "No Active Modules",
                    description = "Install modules from the store to get started with your workspace",
                    onInstallClick = { showModuleStore = true }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    items(activeModules) { module ->
                        InstalledModuleCard(
                            module = module,
                            onSelect = { moduleCode ->
                                onModuleSelected(moduleCode)
                            }
                        )
                    }
                }
            }
        }
    }

    // Module Store Dialog (matching web module-store-dialog)
    if (showModuleStore) {
        ModuleStoreDialog(
            workspaceId = workspaceId,
            onDismiss = { showModuleStore = false },
            onInstall = { moduleCode ->
                // The dialog's ViewModel will handle the install
                // No need to close dialog here - dialog will handle its own state
            },
            onUninstall = { moduleId ->
                // The dialog's ViewModel will handle the uninstall
            },
            onNavigate = { moduleCode ->
                onModuleSelected(moduleCode)
                showModuleStore = false
            }
        )
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    description: String,
    onInstallClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            onInstallClick?.let { onClick ->
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onClick) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Install Modules")
                }
            }
        }
    }
}

@Composable
private fun InstalledModuleCard(
    module: InstalledModule,
    onSelect: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(enabled = module.status == "ACTIVE") {
                onSelect(module.moduleCode)
            },
        colors = CardDefaults.cardColors(
            containerColor = if (module.status == "ACTIVE")
                MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Module icon
            Card(
                modifier = Modifier.size(48.dp),
                colors = CardDefaults.cardColors(
                    containerColor = parseHexColor(module.primaryColor).copy(
                        alpha = if (module.status == "ACTIVE") 1f else 0.6f
                    )
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = module.name.first().uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Module name
            Text(
                text = module.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                color = if (module.status == "ACTIVE")
                    MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ModuleStoreDialog(
    workspaceId: String,
    onDismiss: () -> Unit,
    onInstall: (String) -> Unit,
    onUninstall: (String) -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: WorkspaceModulesViewModel = koinInject { parametersOf(workspaceId.takeIf { it.isNotEmpty() }) }
) {
    var selectedStoreTab by remember { mutableStateOf(0) }
    var installingModules by remember { mutableStateOf(setOf<String>()) }

    // Observe ViewModel state
    val vmErrorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val availableModules by viewModel.availableModules.collectAsState()
    val featuredModules by viewModel.featuredModules.collectAsState()
    val installedModules by viewModel.installedModules.collectAsState()

    // Load initial data when dialog opens
    LaunchedEffect(Unit) {
        viewModel.loadInstalledModules(refresh = true)
        viewModel.loadAvailableModules(refresh = true) // Load all available modules
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Install Modules") },
        text = {
            Column {
                // Store tabs: Featured/All (matching web)
                TabRow(selectedTabIndex = selectedStoreTab) {
                    Tab(
                        selected = selectedStoreTab == 0,
                        onClick = {
                            selectedStoreTab = 0
                            viewModel.loadAvailableModules(
                                featured = true,
                                refresh = true
                            ) // Load featured
                        },
                        text = { Text("Featured") }
                    )
                    Tab(
                        selected = selectedStoreTab == 1,
                        onClick = {
                            selectedStoreTab = 1
                            viewModel.loadAvailableModules(
                                featured = false,
                                refresh = true
                            ) // Load all
                        },
                        text = { Text("All") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Error display
                vmErrorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(
                                onClick = { viewModel.clearError() }
                            ) {
                                Text("Dismiss", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Module list
                val modulesToShow = if (selectedStoreTab == 0) featuredModules else availableModules

                if (modulesToShow.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (selectedStoreTab == 0) "No Featured Modules" else "No Modules Available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Available: ${availableModules.size}, Featured: ${featuredModules.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(modulesToShow) { module ->
                            val installedModule =
                                installedModules.find { it.moduleCode == module.moduleCode }
                            AvailableModuleCard(
                                module = module,
                                installedModule = installedModule,
                                onInstall = {
                                    viewModel.clearError() // Clear any previous errors
                                    installingModules = installingModules + module.moduleCode

                                    // Use dialog's ViewModel to install
                                    viewModel.installModule(module.moduleCode) { response ->
                                        // Clear loading state after operation completes
                                        installingModules = installingModules - module.moduleCode

                                        // Call parent callback if needed
                                        onInstall(module.moduleCode)

                                        // Close dialog on successful install if needed
                                        if (response != null && response.success) {
                                            // Parent will handle dialog closing via onNavigate
                                        }
                                    }
                                },
                                onUninstall = { moduleId ->
                                    viewModel.clearError() // Clear any previous errors
                                    viewModel.uninstallModule(moduleId) { response ->
                                        // Call parent callback
                                        onUninstall(moduleId)
                                    }
                                },
                                onNavigate = { moduleCode ->
                                    onNavigate(moduleCode)
                                },
                                isInstalling = installingModules.contains(module.moduleCode)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun AvailableModuleCard(
    module: AvailableModule,
    installedModule: InstalledModule? = null,
    onInstall: () -> Unit,
    onUninstall: (String) -> Unit,
    onNavigate: (String) -> Unit,
    isInstalling: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Module icon
            Card(
                modifier = Modifier.size(40.dp),
                colors = CardDefaults.cardColors(
                    containerColor = parseHexColor(module.primaryColor)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = module.name.first().uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = module.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (module.featured) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.height(16.dp)
                        ) {
                            Text(
                                text = "Featured",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${module.category} • ${module.requiredTier}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⭐ ${module.rating}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${module.sizeMb}MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = module.complexity,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (module.complexity) {
                            "Simple" -> MaterialTheme.colorScheme.primary
                            "Medium" -> MaterialTheme.colorScheme.tertiary
                            "Advanced" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                module.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (installedModule != null) {
                // Module is installed - show Navigate and Uninstall actions
                Column {
                    Button(
                        onClick = { onNavigate(module.moduleCode) },
                        enabled = installedModule.status == "ACTIVE",
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Navigate", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { onUninstall(installedModule.id) },
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Uninstall", fontSize = 10.sp)
                    }
                }
            } else {
                // Module is not installed - show Install action
                Button(
                    onClick = onInstall,
                    enabled = !isInstalling,
                    modifier = Modifier.height(32.dp)
                ) {
                    if (isInstalling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Install", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}


/**
 * Parse hex color string to Compose Color (cross-platform)
 */
private fun parseHexColor(hexColor: String): Color {
    val cleanHex = hexColor.removePrefix("#")
    return try {
        val colorInt = cleanHex.toLong(16)
        Color(
            red = ((colorInt shr 16) and 0xFF) / 255f,
            green = ((colorInt shr 8) and 0xFF) / 255f,
            blue = (colorInt and 0xFF) / 255f,
            alpha = 1f
        )
    } catch (e: Exception) {
        Color.Gray // Fallback color
    }
}