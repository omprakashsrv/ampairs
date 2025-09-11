package com.ampairs.workspace.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
    onBackClick: () -> Unit = {},
    onModuleSelected: (moduleCode: String) -> Unit = {},
    workspaceId: String = "",
    viewModel: WorkspaceModulesViewModel = koinInject { parametersOf(workspaceId.takeIf { it.isNotEmpty() }) }
) {
    var showModuleStore by remember { mutableStateOf(false) }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val activeModules by viewModel.activeModules.collectAsState()
    val availableModules by viewModel.availableModules.collectAsState()
    val featuredModules by viewModel.featuredModules.collectAsState()

    // Load data on first composition - force refresh to get fresh data from backend
    LaunchedEffect(Unit) {
        viewModel.loadInstalledModules(refresh = true) // Force API call via Store5
        viewModel.loadAvailableModules(refresh = true) // Force API call for available modules
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Modules") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { onModuleSelected("") }) {
                        Text("Skip")
                    }
                }
            )
        }
    ) { paddingValues ->
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
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeModules) { module ->
                            InstalledModuleCard(
                                module = module,
                                onUninstall = { moduleId ->
                                    viewModel.uninstallModule(moduleId) { response ->
                                        // Handle uninstall result
                                    }
                                },
                                onSelect = { moduleCode ->
                                    onModuleSelected(moduleCode)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Module Store Dialog (matching web module-store-dialog)
    if (showModuleStore) {
        ModuleStoreDialog(
            availableModules = availableModules,
            featuredModules = featuredModules,
            onDismiss = { showModuleStore = false },
            onInstall = { moduleCode ->
                viewModel.installModule(moduleCode) { response ->
                    // Handle install result
                    if (response != null) {
                        showModuleStore = false
                    }
                }
            },
            onLoadAvailable = { category, featured ->
                viewModel.loadAvailableModules(category, featured)
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
    onUninstall: (String) -> Unit,
    onSelect: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Module icon (simplified - using first letter)
            Card(
                modifier = Modifier.size(48.dp),
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
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = module.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = module.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Status: ${module.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (module.status == "ACTIVE") 
                        MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.error
                )
            }

            Column {
                Button(
                    onClick = { onSelect(module.moduleCode) },
                    enabled = module.status == "ACTIVE"
                ) {
                    Text("Select")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onUninstall(module.id) }
                ) {
                    Text("Uninstall")
                }
            }
        }
    }
}

@Composable
private fun ModuleStoreDialog(
    availableModules: List<AvailableModule>,
    featuredModules: List<AvailableModule>,
    onDismiss: () -> Unit,
    onInstall: (String) -> Unit,
    onLoadAvailable: (String?, Boolean) -> Unit
) {
    var selectedStoreTab by remember { mutableStateOf(0) }
    var installingModules by remember { mutableStateOf(setOf<String>()) }

    // Load initial data when dialog opens
    LaunchedEffect(Unit) {
        onLoadAvailable(null, true) // Load featured modules
        onLoadAvailable(null, false) // Load all modules
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
                            onLoadAvailable(null, true) // Load featured
                        },
                        text = { Text("Featured") }
                    )
                    Tab(
                        selected = selectedStoreTab == 1,
                        onClick = { 
                            selectedStoreTab = 1
                            onLoadAvailable(null, false) // Load all
                        },
                        text = { Text("All") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            AvailableModuleCard(
                                module = module,
                                onInstall = { 
                                    installingModules = installingModules + module.moduleCode
                                    onInstall(module.moduleCode) 
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
    onInstall: () -> Unit,
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
                        color = when(module.complexity) {
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