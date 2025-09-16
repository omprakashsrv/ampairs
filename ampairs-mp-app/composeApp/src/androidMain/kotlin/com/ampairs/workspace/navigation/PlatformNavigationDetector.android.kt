package com.ampairs.workspace.navigation

/**
 * Android platform implementation for navigation pattern detection
 */
actual object PlatformNavigationDetector {
    actual fun getNavigationPattern(): NavigationPattern {
        return NavigationPattern.SIDE_DRAWER
    }
}