package com.ampairs.workspace.security

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

/**
 * Simple approach: Custom authorization expressions
 * Instead of complex expression handler, we use service bean in @PreAuthorize
 */
@Component
class SuperAdminMethodSecurityExpressionHandler {
    // This class is kept for potential future enhancements
    // Current approach uses @superAdminAuth.isSuperAdmin(authentication)
}