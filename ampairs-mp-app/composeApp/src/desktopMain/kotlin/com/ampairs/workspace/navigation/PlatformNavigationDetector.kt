package com.ampairs.workspace.navigation

/**
 * Desktop implementation of platform navigation detector
 * Returns MENU_BAR pattern for native desktop menu bar integration
 */
actual object PlatformNavigationDetector {
    actual fun getNavigationPattern(): NavigationPattern {
        return NavigationPattern.MENU_BAR
    }
}