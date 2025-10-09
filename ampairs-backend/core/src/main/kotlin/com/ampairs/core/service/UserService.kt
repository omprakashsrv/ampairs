package com.ampairs.core.service

import com.ampairs.core.domain.User

/**
 * Core UserService interface for cross-module user operations
 * This interface defines the contract for user-related operations that other modules need
 * The actual implementation will be provided by the auth module or ampairs_service
 */
interface UserService {
    
    /**
     * Get user by ID
     * @param userId The user ID to look up
     * @return User instance or null if not found
     */
    fun getUserById(userId: String): User?
    
    /**
     * Get multiple users by their IDs (batch operation for performance)
     * @param userIds List of user IDs to look up
     * @return Map of user ID to User instance (missing users will not be in the map)
     */
    fun getUsersByIds(userIds: List<String>): Map<String, User>
    
    /**
     * Get current authenticated user
     * @return Current user or null if not authenticated
     */
    fun getCurrentUser(): User?
    
    /**
     * Check if a user exists
     * @param userId The user ID to check
     * @return true if user exists, false otherwise
     */
    fun userExists(userId: String): Boolean
    
    /**
     * Get user ID from authentication context
     * @return Current user ID or null if not authenticated
     */
    fun getCurrentUserId(): String?
}