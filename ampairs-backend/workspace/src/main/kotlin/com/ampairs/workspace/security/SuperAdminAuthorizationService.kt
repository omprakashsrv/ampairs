package com.ampairs.workspace.security

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

/**
 * Service for Super Admin authorization checks
 * This service is exposed to Spring Security expressions
 */
@Service("superAdminAuth")
class SuperAdminAuthorizationService(
    private val superAdminSecurityService: SuperAdminSecurityService
) {
    
    /**
     * Check if the current authentication has super admin access
     * This method can be used in @PreAuthorize expressions
     */
    fun isSuperAdmin(authentication: Authentication): Boolean {
        return superAdminSecurityService.isSuperAdmin(authentication)
    }
}