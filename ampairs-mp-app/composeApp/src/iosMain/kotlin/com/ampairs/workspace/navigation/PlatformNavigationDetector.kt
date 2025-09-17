package com.ampairs.workspace.navigation

/**
 * iOS implementation of platform navigation detector
 * Returns SIDE_DRAWER pattern for iOS navigation drawer
 */
actual object PlatformNavigationDetector {
    actual fun getNavigationPattern(): NavigationPattern {
        return NavigationPattern.SIDE_DRAWER
    }
}