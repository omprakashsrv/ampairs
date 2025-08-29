package com.ampairs.workspace.model.enums

/**
 * Supported platforms for app deployment
 */
enum class Platform(val displayName: String, val supportsMenuConfig: Boolean) {
    WEB("Web Browser", false),
    ANDROID("Android", false),
    IOS("iOS", false),
    DESKTOP_WINDOWS("Windows Desktop", true),
    DESKTOP_MACOS("macOS Desktop", true),
    DESKTOP_LINUX("Linux Desktop", true);
    
    /**
     * Check if this is a desktop platform
     */
    fun isDesktop(): Boolean = supportsMenuConfig
    
    /**
     * Check if this is a mobile platform
     */
    fun isMobile(): Boolean = this in listOf(ANDROID, IOS)
    
    /**
     * Check if this is a web platform
     */
    fun isWeb(): Boolean = this == WEB
}