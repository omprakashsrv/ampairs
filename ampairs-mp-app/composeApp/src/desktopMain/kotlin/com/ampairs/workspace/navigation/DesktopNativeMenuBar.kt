package com.ampairs.workspace.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.*

/**
 * Desktop native MenuBar implementation using Compose Multiplatform MenuBar API
 * This function should be called from within a MenuBar scope in main.kt
 * Reference: https://github.com/JetBrains/compose-multiplatform/tree/master/tutorials/Tray_Notifications_MenuBar_new#menubar
 */
@Composable
fun MenuBarScope.WorkspaceModuleMenuBar(
    navigationService: DynamicModuleNavigationService,
    onNavigate: (String) -> Unit,
    onSwitchWorkspace: () -> Unit = {},
    onManageMembers: () -> Unit = {},
    onManageInvitations: () -> Unit = {},
    onSettings: () -> Unit = {},
    onThemeChange: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onExit: () -> Unit = {}
) {
    val navigationRoutes by navigationService.navigationRoutes.collectAsState()
    val isLoading by navigationService.isLoading.collectAsState()

    // Debug logging
    LaunchedEffect(navigationRoutes) {
        println("DesktopMenuBar: navigationRoutes updated with ${navigationRoutes.size} routes")
        navigationRoutes.forEach { route ->
            println("  Route: ${route.moduleCode} with ${route.menuItems.size} menu items")
        }
    }

    // File Menu
    Menu("File", mnemonic = 'F') {
        Item(
            "Switch Workspace",
            onClick = onSwitchWorkspace,
            shortcut = KeyShortcut(Key.W, ctrl = true)
        )
        Separator()
        Item(
            "Exit",
            onClick = onExit,
            shortcut = KeyShortcut(Key.Q, ctrl = true)
        )
    }

    // Workspace Menu
    Menu("Workspace", mnemonic = 'W') {
        Item("Team Members", onClick = onManageMembers)
        Item("Invitations", onClick = onManageInvitations)
        Separator()
        Item("Settings", onClick = onSettings)
    }

    // Dynamic Modules Menu
    if (!isLoading && navigationRoutes.isNotEmpty()) {
        Menu("Modules", mnemonic = 'M') {
            navigationRoutes.forEach { moduleRoute ->
                if (moduleRoute.menuItems.size == 1) {
                    // Single menu item - direct navigation
                    val menuItem = moduleRoute.menuItems.first()
                    Item(
                        moduleRoute.displayName,
                        onClick = {
                            onNavigate("/workspace/modules/${moduleRoute.moduleCode}${menuItem.routePath}")
                        }
                    )
                } else {
                    // Multiple menu items - submenu
                    Menu(moduleRoute.displayName) {
                        moduleRoute.menuItems.forEach { menuItem ->
                            Item(
                                text = if (menuItem.isDefault) "${menuItem.label} (Default)" else menuItem.label,
                                onClick = {
                                    onNavigate("/workspace/modules/${moduleRoute.moduleCode}${menuItem.routePath}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // View Menu
    Menu("View", mnemonic = 'V') {
        Menu("Theme") {
            Item("Light Theme", onClick = { onThemeChange("LIGHT") })
            Item("Dark Theme", onClick = { onThemeChange("DARK") })
            Item("System Theme", onClick = { onThemeChange("SYSTEM") })
        }
        Separator()
        Item(
            "Refresh",
            onClick = onRefresh,
            shortcut = KeyShortcut(Key.F5)
        )
    }

    // Help Menu
    Menu("Help", mnemonic = 'H') {
        Item("Documentation", onClick = { /* TODO: Open docs */ })
        Item("Report Bug", onClick = { /* TODO: Open bug report */ })
        Separator()
        Item("About Ampairs", onClick = { /* TODO: Show about dialog */ })
    }
}

/**
 * Complete MenuBar example that can replace the entire MenuBar in main.kt
 * This includes both existing static menus and dynamic workspace modules
 */
@Composable
fun MenuBarScope.CompleteDesktopMenuBar(
    navigationService: DynamicModuleNavigationService,
    appNavigator: Any, // Replace with actual AppNavigator type
    loggedIn: Boolean,
    onExit: () -> Unit,
    onThemeChange: (String) -> Unit = {}
) {
    if (loggedIn) {
        // Existing static menus (keep these from main.kt)
        Menu("Product", mnemonic = 'P') {
            // Static product menu items would go here
            Item("All Products", onClick = { /* Navigate to products */ })
        }

        Menu("Customer", mnemonic = 'C') {
            // Static customer menu items would go here
            Item("All Customers", onClick = { /* Navigate to customers */ })
        }

        Menu("Order", mnemonic = 'O') {
            // Static order menu items would go here
            Item("All Orders", onClick = { /* Navigate to orders */ })
        }

        // Dynamic workspace modules
        WorkspaceModuleMenuBar(
            navigationService = navigationService,
            onNavigate = { route ->
                // appNavigator.navigate(route)
            },
            onSwitchWorkspace = {
                // Navigate to workspace list
            },
            onManageMembers = {
                // Navigate to members management
            },
            onManageInvitations = {
                // Navigate to invitations
            },
            onSettings = {
                // Navigate to settings
            },
            onThemeChange = onThemeChange,
            onRefresh = {
                // Refresh current view
            },
            onExit = onExit
        )
    }
}

/**
 * Just the dynamic modules menu - can be integrated into existing MenuBars
 */
@Composable
fun MenuBarScope.DynamicModulesMenu(
    navigationService: DynamicModuleNavigationService,
    onNavigate: (String) -> Unit
) {
    val navigationRoutes by navigationService.navigationRoutes.collectAsState()
    val isLoading by navigationService.isLoading.collectAsState()

    // Debug logging
    LaunchedEffect(navigationRoutes) {
        println("DesktopMenuBar: navigationRoutes updated with ${navigationRoutes.size} routes")
        navigationRoutes.forEach { route ->
            println("  Route: ${route.moduleCode} displayName='${route.displayName}' with ${route.menuItems.size} menu items")
        }
    }

    // Dynamic Module Menus - each module becomes its own top-level menu
    if (!isLoading && navigationRoutes.isNotEmpty()) {
        navigationRoutes.forEach { moduleRoute ->
            val mnemonic = if (moduleRoute.displayName.isNotEmpty()) moduleRoute.displayName.first() else 'M'
            Menu(moduleRoute.displayName, mnemonic = mnemonic) {
                if (moduleRoute.menuItems.isNotEmpty()) {
                    // Add menu items for this module
                    moduleRoute.menuItems.forEach { menuItem ->
                        Item(
                            text = if (menuItem.isDefault) "${menuItem.label} (Default)" else menuItem.label,
                            onClick = {
                                onNavigate("/workspace/modules/${moduleRoute.moduleCode}${menuItem.routePath}")
                            }
                        )
                    }
                } else {
                    // If no menu items, add a default item to navigate to the module
                    Item(
                        "Open ${moduleRoute.displayName}",
                        onClick = {
                            onNavigate("/workspace/modules/${moduleRoute.moduleCode}")
                        }
                    )
                }
            }
        }
    }
}