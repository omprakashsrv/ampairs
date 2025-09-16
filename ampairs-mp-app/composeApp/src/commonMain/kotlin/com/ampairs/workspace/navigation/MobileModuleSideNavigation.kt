package com.ampairs.workspace.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ampairs.workspace.api.model.ModuleMenuItem

/**
 * Mobile-style side navigation for module navigation
 * Displays modules as expandable groups with menu items
 */
@Composable
fun MobileModuleSideNavigation(
    navigationService: DynamicModuleNavigationService,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navigationRoutes by navigationService.navigationRoutes.collectAsState()
    val isLoading by navigationService.isLoading.collectAsState()
    val error by navigationService.error.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Installed Modules",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        // Error state
        error?.let { errorMessage ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Loading state
        if (isLoading && navigationRoutes.isEmpty()) {
            item {
                repeat(3) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        // Skeleton loading
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Module list
        if (navigationRoutes.isEmpty() && !isLoading) {
            item {
                EmptyModulesCard()
            }
        } else {
            items(navigationRoutes) { moduleRoute ->
                MobileModuleNavigationItem(
                    moduleRoute = moduleRoute,
                    onNavigate = onNavigate
                )
            }
        }

        // Statistics
        if (navigationRoutes.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ModuleStatistics(
                    moduleCount = navigationRoutes.size,
                    totalMenuItems = navigationRoutes.sumOf { it.menuItems.size }
                )
            }
        }
    }
}

@Composable
private fun MobileModuleNavigationItem(
    moduleRoute: DynamicModuleRoute,
    onNavigate: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded)
                MaterialTheme.colorScheme.surfaceContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Module header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (moduleRoute.menuItems.size == 1) {
                            // Single menu item - navigate directly
                            val menuItem = moduleRoute.menuItems.first()
                            onNavigate("/workspace/modules/${moduleRoute.moduleCode}${menuItem.routePath}")
                        } else {
                            // Multiple menu items - toggle expansion
                            expanded = !expanded
                        }
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Module icon with color
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(parseHexColor(moduleRoute.primaryColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = moduleRoute.displayName.first().uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Module info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = moduleRoute.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${moduleRoute.menuItems.size} menu items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Expand/collapse icon (only if multiple menu items)
                if (moduleRoute.menuItems.size > 1) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Menu items (when expanded)
            if (expanded && moduleRoute.menuItems.size > 1) {
                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(modifier = Modifier.padding(start = 68.dp, end = 16.dp, bottom = 8.dp)) {
                    moduleRoute.menuItems.forEach { menuItem ->
                        MobileMenuItemRow(
                            menuItem = menuItem,
                            moduleRoute = moduleRoute,
                            onNavigate = onNavigate
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileMenuItemRow(
    menuItem: ModuleMenuItem,
    moduleRoute: DynamicModuleRoute,
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onNavigate("/workspace/modules/${moduleRoute.moduleCode}${menuItem.routePath}")
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = menuItem.label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        if (menuItem.isDefault) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(18.dp)
            ) {
                Text(
                    text = "Default",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyModulesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No Modules Available",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Install modules from the module store to see navigation options.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModuleStatistics(
    moduleCount: Int,
    totalMenuItems: Int
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = moduleCount.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Modules",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = totalMenuItems.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Menu Items",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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