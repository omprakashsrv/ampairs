package com.ampairs.workspace.config

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

/**
 * Configuration to enable method-level security for workspace operations
 * Enables @PreAuthorize, @PostAuthorize, @Secured annotations
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
class WorkspaceSecurityConfig {
    // Method security configuration
    // This enables:
    // - @PreAuthorize for pre-execution checks
    // - @PostAuthorize for post-execution checks  
    // - @Secured for role-based security
    // - Custom expressions via @superAdminAuth.isSuperAdmin(authentication)
}