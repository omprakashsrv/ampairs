package com.ampairs.core.domain

/**
 * Shared User contract for cross-module communication
 * This interface represents the minimal user information needed by other modules
 * without creating direct dependencies on the auth module
 */
interface User {
    /**
     * Unique user identifier
     */
    val uid: String
    
    /**
     * User's email address
     */
    val email: String?
    
    /**
     * User's first name
     */
    val firstName: String?
    
    /**
     * User's last name
     */
    val lastName: String?
    
    /**
     * User's phone number
     */
    val phone: String?
    
    /**
     * Whether the user account is active
     */
    val isActive: Boolean
    
    /**
     * Profile picture URL (full size, max 512x512)
     */
    val profilePictureUrl: String?

    /**
     * Profile picture thumbnail URL (256x256)
     */
    val profilePictureThumbnailUrl: String?
        get() = null // Default implementation for backward compatibility

    /**
     * Get display name for the user
     */
    fun getDisplayName(): String {
        return when {
            !firstName.isNullOrBlank() && !lastName.isNullOrBlank() -> "$firstName $lastName"
            !firstName.isNullOrBlank() -> firstName!!
            !lastName.isNullOrBlank() -> lastName!!
            !email.isNullOrBlank() -> email!!
            else -> uid
        }
    }
    
    /**
     * Get full name
     */
    fun getFullName(): String? {
        return when {
            !firstName.isNullOrBlank() && !lastName.isNullOrBlank() -> "$firstName $lastName"
            !firstName.isNullOrBlank() -> firstName
            !lastName.isNullOrBlank() -> lastName
            else -> null
        }
    }
}
