package com.ampairs.core.security

import com.ampairs.core.config.ApplicationProperties
import org.springframework.stereotype.Service

/**
 * Service to check if a user is an admin.
 *
 * Admin users are configured in application.yml under `application.auth.admin-user-ids`.
 * This allows granting admin privileges to specific users via configuration.
 */
@Service
class AdminService(
    private val applicationProperties: ApplicationProperties
) {

    /**
     * Check if the given user ID is an admin.
     *
     * @param userId User ID to check
     * @return true if user is admin, false otherwise
     */
    fun isAdmin(userId: String): Boolean {
        return applicationProperties.auth.adminUserIds.contains(userId)
    }

    /**
     * Get all configured admin user IDs.
     *
     * @return List of admin user IDs
     */
    fun getAdminUserIds(): List<String> {
        return applicationProperties.auth.adminUserIds
    }
}