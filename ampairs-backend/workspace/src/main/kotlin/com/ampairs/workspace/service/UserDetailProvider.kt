package com.ampairs.workspace.service

import com.ampairs.core.domain.User
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

/**
 * Interface for providing user details to workspace module
 * This allows for future integration with user service while maintaining loose coupling
 */
interface UserDetailProvider {
    /**
     * Get user details by user ID
     */
    fun getUserDetail(userId: String): User?

    /**
     * Get user details for multiple user IDs (batch loading for performance)
     */
    fun getUserDetails(userIds: List<String>): Map<String, User>

    /**
     * Check if user details are available from external service
     */
    fun isUserServiceAvailable(): Boolean
}

/**
 * Default implementation that uses local member data
 * This is used when user service is not available or configured
 */
@ConditionalOnMissingBean(UserDetailProvider::class)
@Service
class LocalUserDetailProvider : UserDetailProvider {

    override fun getUserDetail(userId: String): User? {
        // Return null to indicate no external user data available
        return null
    }

    override fun getUserDetails(userIds: List<String>): Map<String, User> {
        // Return empty map to indicate no external user data available
        return emptyMap()
    }

    override fun isUserServiceAvailable(): Boolean = false
}

/**
 * Adapter implementation that uses the core UserService contract.
 * The actual implementation of com.ampairs.core.service.UserService is provided by the auth module.
 * This keeps workspace decoupled from auth while still enabling runtime integration.
 */
@ConditionalOnBean(com.ampairs.core.service.UserService::class)
@Service
class CoreUserDetailProvider(
    private val userService: com.ampairs.core.service.UserService
) : UserDetailProvider {

    override fun getUserDetail(userId: String): User? {
        return try {
            userService.getUserById(userId)
        } catch (_: Exception) {
            null
        }
    }

    override fun getUserDetails(userIds: List<String>): Map<String, User> {
        return try {
            userService.getUsersByIds(userIds)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    override fun isUserServiceAvailable(): Boolean {
        // Conservative check: if userService is present, assume available. Concrete availability
        // (auth up/down) can be improved later with health checks.
        return true
    }
}
