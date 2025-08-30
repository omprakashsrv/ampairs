package com.ampairs.workspace.service

/**
 * User detail data structure for cross-module integration
 */
data class UserDetail(
    val userId: String,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val avatarUrl: String?,
    val isActive: Boolean = true
)

/**
 * Interface for providing user details to workspace module
 * This allows for future integration with user service while maintaining loose coupling
 */
interface UserDetailProvider {
    
    /**
     * Get user details by user ID
     */
    fun getUserDetail(userId: String): UserDetail?
    
    /**
     * Get user details for multiple user IDs (batch loading for performance)
     */
    fun getUserDetails(userIds: List<String>): Map<String, UserDetail>
    
    /**
     * Check if user details are available from external service
     */
    fun isUserServiceAvailable(): Boolean
}

/**
 * Default implementation that uses local member data
 * This is used when user service is not available or configured
 */
class LocalUserDetailProvider : UserDetailProvider {
    
    override fun getUserDetail(userId: String): UserDetail? {
        // Return null to indicate no external user data available
        return null
    }
    
    override fun getUserDetails(userIds: List<String>): Map<String, UserDetail> {
        // Return empty map to indicate no external user data available
        return emptyMap()
    }
    
    override fun isUserServiceAvailable(): Boolean = false
}