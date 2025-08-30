package com.ampairs.core.security

import org.springframework.security.core.Authentication

/**
 * Helper utilities for working with Spring Security Authentication
 * without creating direct dependencies on auth module entities
 */
object AuthenticationHelper {
    
    /**
     * Extract user ID from authentication principal
     * This works regardless of the actual principal type
     */
    fun getUserId(authentication: Authentication): String? {
        val principal = authentication.principal
        return when {
            // If principal has a uid property (reflection-based)
            hasProperty(principal, "uid") -> getProperty(principal, "uid") as? String
            // If principal has getId method
            hasMethod(principal, "getId") -> callMethod(principal, "getId") as? String
            // If principal is a string (e.g., username)
            principal is String -> principal
            // Fallback to authentication name
            else -> authentication.name
        }
    }
    
    /**
     * Get user ID from current user - alias for getUserId
     */
    fun getCurrentUserId(authentication: Authentication): String? = getUserId(authentication)
    
    /**
     * Check if an object has a specific property using reflection
     */
    private fun hasProperty(obj: Any?, propertyName: String): Boolean {
        if (obj == null) return false
        return try {
            obj::class.java.getDeclaredField(propertyName) != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get property value using reflection
     */
    private fun getProperty(obj: Any?, propertyName: String): Any? {
        if (obj == null) return null
        return try {
            val field = obj::class.java.getDeclaredField(propertyName)
            field.isAccessible = true
            field.get(obj)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if an object has a specific method using reflection
     */
    private fun hasMethod(obj: Any?, methodName: String): Boolean {
        if (obj == null) return false
        return try {
            obj::class.java.getMethod(methodName) != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Call method using reflection
     */
    private fun callMethod(obj: Any?, methodName: String): Any? {
        if (obj == null) return null
        return try {
            val method = obj::class.java.getMethod(methodName)
            method.invoke(obj)
        } catch (e: Exception) {
            null
        }
    }
}