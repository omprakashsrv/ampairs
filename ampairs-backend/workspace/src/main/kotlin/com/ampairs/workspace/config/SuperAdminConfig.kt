package com.ampairs.workspace.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration for Super Admin users
 */
@Configuration
@ConfigurationProperties(prefix = "ampairs.admin")
data class SuperAdminConfig(
    /**
     * List of phone numbers that should have SUPER_ADMIN role
     * Format: "countryCode:phoneNumber" (e.g., "91:9876543210")
     */
    var superAdminPhones: List<String> = listOf(),
    
    /**
     * List of usernames that should have SUPER_ADMIN role
     */
    var superAdminUsers: List<String> = listOf(),
    
    /**
     * Enable super admin functionality
     */
    var enabled: Boolean = true
)