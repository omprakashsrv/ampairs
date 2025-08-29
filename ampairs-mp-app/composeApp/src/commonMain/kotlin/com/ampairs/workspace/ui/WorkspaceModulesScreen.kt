package com.ampairs.workspace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.workspace.api.model.WorkspaceModuleApiModel
import com.ampairs.workspace.viewmodel.WorkspaceModulesViewModel
import com.ampairs.workspace.viewmodel.WorkspaceModulesState

/**
 * Workspace Modules Management Screen
 *
 * Provides comprehensive module management functionality including:
 * - Dashboard with module statistics
 * - Installed modules management
 * - Available modules discovery
 * - Module configuration and actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceModulesScreen(
    viewModel: WorkspaceModulesViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToModuleDetail: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadModuleDashboard()
        viewModel.loadInstalledModules()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Workspace Modules",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Manage business functionality",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Dashboard Statistics
        if (state.dashboardData != null) {
            DashboardSection(
                dashboardData = state.dashboardData!!,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Tab Navigation
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Text("Installed")
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null
                    )
                }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = {
                    selectedTabIndex = 1
                    if (state.availableModules.isEmpty()) {
                        viewModel.loadAvailableModules()
                    }
                },
                text = {
                    Text("Available")
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null
                    )
                }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = {
                    Text("Analytics")
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null
                    )
                }
            )
        }

        // Tab Content
        when (selectedTabIndex) {
            0 -> InstalledModulesTab(
                state = state,
                onModuleClick = onNavigateToModuleDetail,
                onToggleModule = { moduleId, enabled ->
                    viewModel.toggleModuleStatus(moduleId, enabled)
                },
                onInstallModule = { moduleId ->
                    // Navigate to available tab or show install dialog
                },
                onConfigureModule = { moduleId ->
                    onNavigateToModuleDetail(moduleId)
                },
                onUninstallModule = { moduleId ->
                    viewModel.uninstallModule(moduleId)
                },
                modifier = Modifier.fillMaxSize()
            )

            1 -> AvailableModulesTab(
                state = state,
                onInstallModule = { masterModule ->
                    viewModel.installModule(masterModule)
                },
                onRefresh = {
                    viewModel.loadAvailableModules()
                },
                modifier = Modifier.fillMaxSize()
            )

            2 -> AnalyticsTab(
                state = state,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Loading State
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Error State
        state.errorMessage?.let { error ->
            LaunchedEffect(error) {
                // Show snackbar or error dialog
            }
        }
    }
}

@Composable
private fun DashboardSection(
    dashboardData: WorkspaceModuleApiModel.ModuleDashboardResponse,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        item {
            DashboardCard(
                title = "Total Modules",
                value = dashboardData.totalModules.toString(),
                icon = Icons.Default.Apps,
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            DashboardCard(
                title = "Active Modules",
                value = dashboardData.activeModules.toString(),
                icon = Icons.Default.CheckCircle,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        item {
            DashboardCard(
                title = "Need Attention",
                value = dashboardData.modulesNeedingAttention.toString(),
                icon = Icons.Default.Warning,
                color = MaterialTheme.colorScheme.error
            )
        }
        item {
            DashboardCard(
                title = "Storage Used",
                value = formatStorageSize(dashboardData.storageUsageMb),
                icon = Icons.Default.Storage,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.width(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .padding(bottom = 8.dp),
                tint = color
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InstalledModulesTab(
    state: WorkspaceModulesState,
    onModuleClick: (String) -> Unit,
    onToggleModule: (String, Boolean) -> Unit,
    onInstallModule: (String) -> Unit,
    onConfigureModule: (String) -> Unit,
    onUninstallModule: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search and Filters
        item {
            SearchAndFiltersSection(
                searchQuery = state.searchQuery,
                onSearchQueryChange = { /* viewModel.updateSearchQuery(it) */ },
                selectedCategory = state.selectedCategory,
                onCategoryChange = { /* viewModel.updateCategory(it) */ }
            )
        }

        // Modules List
        items(state.installedModules.size) { index ->
            val module = state.installedModules[index]
            InstalledModuleCard(
                module = module,
                onClick = { onModuleClick(module.id) },
                onToggle = { enabled -> onToggleModule(module.id, enabled) },
                onConfigure = { onConfigureModule(module.id) },
                onUninstall = { onUninstallModule(module.id) }
            )
        }

        // Empty State
        if (state.installedModules.isEmpty() && !state.isLoading) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.Extension,
                    title = "No Modules Installed",
                    description = "Install modules from the Available tab to add business functionality.",
                    actionText = "Browse Available",
                    onAction = { onInstallModule("") }
                )
            }
        }
    }
}

@Composable
private fun AvailableModulesTab(
    state: WorkspaceModulesState,
    onInstallModule: (WorkspaceModuleApiModel.MasterModule) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Install New Modules",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Expand your workspace capabilities",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Available Modules
        items(state.availableModules.size) { index ->
            val module = state.availableModules[index]
            AvailableModuleCard(
                module = module,
                isInstalled = state.installedModules.any { it.masterModule.id == module.id },
                onInstall = { onInstallModule(module) }
            )
        }

        // Empty State
        if (state.availableModules.isEmpty() && !state.isLoading) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.CloudOff,
                    title = "No Modules Available",
                    description = "All available modules are already installed.",
                    actionText = "Refresh",
                    onAction = onRefresh
                )
            }
        }
    }
}

@Composable
private fun AnalyticsTab(
    state: WorkspaceModulesState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.dashboardData != null) {
            // Health Overview
            item {
                HealthOverviewCard(state.dashboardData!!.healthOverview)
            }

            // Most Used Modules
            if (state.dashboardData!!.mostUsedModules.isNotEmpty()) {
                item {
                    MostUsedModulesCard(state.dashboardData!!.mostUsedModules)
                }
            }

            // Category Distribution
            if (state.dashboardData!!.categoryDistribution.isNotEmpty()) {
                item {
                    CategoryDistributionCard(state.dashboardData!!.categoryDistribution)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchAndFiltersSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search modules...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Category Filter
        var expanded by remember { mutableStateOf(false) }
        val categories = listOf("ALL", "COMMUNICATION", "PRODUCTIVITY", "ANALYTICS", "INTEGRATION", "SECURITY")
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = { },
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            onCategoryChange(category)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstalledModuleCard(
    module: WorkspaceModuleApiModel.WorkspaceModule,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onConfigure: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Module Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(parseHexColor(module.effectiveColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getModuleIcon(module.effectiveIcon),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = module.effectiveName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = module.effectiveCategory.replace("_", " ").lowercase().split(" ")
                                .joinToString(" ") { it.capitalize() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status Indicator
                Icon(
                    imageVector = if (module.enabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = if (module.enabled) "Enabled" else "Disabled",
                    tint = if (module.enabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = module.effectiveDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Health Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Health:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    LinearProgressIndicator(
                        progress = { module.healthScore.toFloat() },
                        modifier = Modifier.width(80.dp).height(4.dp),
                        color = getHealthScoreColor(module.healthScore),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${(module.healthScore * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Daily Users
                if (module.usageMetrics.dailyActiveUsers != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${module.usageMetrics.dailyActiveUsers}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Warning Chips
            if (module.needsAttention || module.canBeUpdated) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (module.needsAttention) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Needs Attention", style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                    if (module.canBeUpdated) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Update Available", style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Toggle Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = module.enabled,
                        onCheckedChange = onToggle
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (module.enabled) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onConfigure) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configure"
                        )
                    }
                    IconButton(onClick = onUninstall) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Uninstall",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvailableModuleCard(
    module: WorkspaceModuleApiModel.MasterModule,
    isInstalled: Boolean,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Module Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(parseHexColor(module.uiMetadata.color)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getModuleIcon(module.uiMetadata.icon),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = module.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = module.category.replace("_", " ").lowercase().split(" ")
                                    .joinToString(" ") { it.capitalize() },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (module.rating > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%.1f", module.rating),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = module.description ?: "No description available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (module.tagline != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = module.tagline!!,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Module Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatStorageSize(module.sizeMb.toLong()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${module.installCount} installs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (module.complexity.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${module.complexity.lowercase().capitalize()} Setup",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Featured Badge
            if (module.featured) {
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(
                    onClick = { },
                    label = { Text("Featured", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Install Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onInstall,
                    enabled = !isInstalled,
                    modifier = Modifier.fillMaxWidth(0.5f)
                ) {
                    Icon(
                        imageVector = if (isInstalled) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isInstalled) "Installed" else "Install")
                }
            }
        }
    }
}

// Helper functions and other composables would be added here...
private fun getModuleIcon(iconName: String): ImageVector {
    return when (iconName) {
        "people" -> Icons.Default.Group
        "shopping_cart" -> Icons.Default.ShoppingCart
        "inventory" -> Icons.Default.Inventory
        "account_balance" -> Icons.Default.AccountBalance
        "analytics" -> Icons.Default.Analytics
        else -> Icons.Default.Extension
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        val cleanHex = if (hex.startsWith("#")) hex.drop(1) else hex
        val colorValue = cleanHex.toLong(16)
        Color((0xFF000000L or colorValue).toULong())
    } catch (e: Exception) {
        Color(0xFF6200EE) // Default primary color
    }
}

private fun getHealthScoreColor(score: Double): Color {
    return when {
        score >= 0.8 -> Color(0xFF4CAF50) // Green
        score >= 0.6 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
}

private fun formatStorageSize(sizeInMb: Long): String {
    return if (sizeInMb < 1024) {
        "${sizeInMb} MB"
    } else {
        String.format("%.1f GB", sizeInMb / 1024.0)
    }
}

@Composable
private fun MostUsedModulesCard(mostUsedModules: List<WorkspaceModuleApiModel.WorkspaceModule>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Most Used Modules",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            mostUsedModules.take(5).forEachIndexed { index, module ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = module.masterModule.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${module.usageMetrics.monthlyAccess ?: 0} uses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryDistributionCard(categoryDistribution: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Category Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            categoryDistribution.entries.sortedByDescending { it.value }.take(5).forEach { (category, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.replace("_", " ").split(" ")
                            .joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.uppercase() } },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$count modules",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthOverviewCard(healthOverview: WorkspaceModuleApiModel.ModuleHealthOverview) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Module Health Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HealthMetricItem(
                    label = "Healthy",
                    count = healthOverview.healthyModules,
                    color = Color(0xFF4CAF50)
                )
                
                HealthMetricItem(
                    label = "Warning",
                    count = healthOverview.warningModules,
                    color = Color(0xFFFF9800)
                )
                
                HealthMetricItem(
                    label = "Critical",
                    count = healthOverview.criticalModules,
                    color = Color(0xFFF44336)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Overall Health Score: ${String.format("%.1f", healthOverview.overallHealthScore * 100)}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HealthMetricItem(
    label: String,
    count: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    description: String,
    icon: ImageVector = Icons.Default.Extension,
    actionText: String? = null,
    onAction: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            actionText?.let { text ->
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(onClick = onAction) {
                    Text(text)
                }
            }
        }
    }
}

// Additional composables for search, filters, empty states, etc. would be implemented here...