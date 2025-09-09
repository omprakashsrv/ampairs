package com.ampairs.workspace.security

import com.ampairs.workspace.config.SuperAdminConfig
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service

/**
 * Service to handle Super Admin authorization
 */
@Service
class SuperAdminSecurityService(
    private val superAdminConfig: SuperAdminConfig
) {
    
    private val logger = LoggerFactory.getLogger(SuperAdminSecurityService::class.java)
    
    /**
     * Check if the current user has SUPER_ADMIN role
     */
    fun isSuperAdmin(authentication: Authentication): Boolean {
        if (!superAdminConfig.enabled) {
            logger.debug("Super admin functionality is disabled")
            return false
        }
        
        // Check if user already has SUPER_ADMIN authority in their token
        val hasRole = authentication.authorities?.any { 
            it.authority == "ROLE_SUPER_ADMIN" || it.authority == "SUPER_ADMIN" 
        } ?: false
        
        if (hasRole) {
            logger.debug("User has SUPER_ADMIN role in token")
            return true
        }
        
        // Check configuration-based super admin access
        return isConfiguredSuperAdmin(authentication)
    }
    
    /**
     * Check if user is configured as super admin
     */
    private fun isConfiguredSuperAdmin(authentication: Authentication): Boolean {
        try {
            // Get current user details
            // Get current user details from authentication
            val currentUserId = authentication.name
            val userName = authentication.name
            
            logger.debug("Checking super admin access for user: {}, userId: {}", userName, currentUserId)
            
            // Check by username
            if (superAdminConfig.superAdminUsers.contains(userName)) {
                logger.info("User {} granted SUPER_ADMIN access via username configuration", userName)
                return true
            }
            
            // Check by phone number (if available in authentication details)
            val userPhone = extractPhoneFromAuthentication(authentication)
            if (userPhone != null && superAdminConfig.superAdminPhones.contains(userPhone)) {
                logger.info("User {} granted SUPER_ADMIN access via phone configuration", userName)
                return true
            }
            
            logger.debug("User {} does not have super admin access", userName)
            return false
            
        } catch (e: Exception) {
            logger.error("Error checking super admin access", e)
            return false
        }
    }
    
    /**
     * Extract phone number from authentication context
     * Format: "countryCode:phoneNumber"
     */
    private fun extractPhoneFromAuthentication(authentication: Authentication): String? {
        return try {
            // Try to get phone from JWT claims
            val principal = authentication.principal
            // For now, we'll focus on username-based super admin access
            // Phone-based access can be implemented when JWT token structure is clarified
            
            // Could also check other sources like UserDetails if needed
            null
        } catch (e: Exception) {
            logger.debug("Could not extract phone from authentication", e)
            null
        }
    }
    
    /**
     * Get enhanced authorities including SUPER_ADMIN if applicable
     */
    fun getEnhancedAuthorities(authentication: Authentication): Collection<GrantedAuthority> {
        val originalAuthorities = authentication.authorities?.toMutableList() ?: mutableListOf()
        
        if (isSuperAdmin(authentication)) {
            val hasSuperAdmin = originalAuthorities.any { 
                it.authority == "ROLE_SUPER_ADMIN" || it.authority == "SUPER_ADMIN" 
            }
            
            if (!hasSuperAdmin) {
                originalAuthorities.add(SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                logger.debug("Added ROLE_SUPER_ADMIN to user authorities")
            }
        }
        
        return originalAuthorities
    }
}