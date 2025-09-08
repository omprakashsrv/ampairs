package com.ampairs.core.multitenancy

import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Thread-safe tenant context holder that integrates with Spring Security
 * This replaces the old custom TenantContext implementation
 */
object TenantContextHolder {

    private val logger = LoggerFactory.getLogger(TenantContextHolder::class.java)

    // Fallback ThreadLocal for cases where SecurityContext is not available
    private val tenantThreadLocal: ThreadLocal<String?> = InheritableThreadLocal()

    /**
     * Get current tenant identifier
     * First tries Spring Security context, then falls back to ThreadLocal
     */
    fun getCurrentTenant(): String? {
        return try {
            // Try SecurityContext first (preferred approach)
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication?.details is Map<*, *>) {
                val details = authentication.details as Map<*, *>
                val tenantId = details[CurrentTenantIdentifierResolver.TENANT_ATTRIBUTE] as? String
                if (!tenantId.isNullOrBlank()) {
                    return tenantId
                }
            }

            // Fallback to ThreadLocal
            tenantThreadLocal.get()
        } catch (e: Exception) {
            logger.warn("Error getting current tenant, falling back to ThreadLocal", e)
            tenantThreadLocal.get()
        }
    }

    /**
     * Set current tenant identifier
     * Sets both SecurityContext and ThreadLocal for maximum compatibility
     */
    fun setCurrentTenant(tenantId: String?) {
        try {
            // Set in ThreadLocal
            if (tenantId != null) {
                tenantThreadLocal.set(tenantId)
                logger.debug("Tenant set in ThreadLocal: {}", tenantId)
            } else {
                tenantThreadLocal.remove()
                logger.debug("Tenant cleared from ThreadLocal")
            }

            // Try to set in SecurityContext if available
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication != null) {
                setTenantInSecurityContext(tenantId)
            }

        } catch (e: Exception) {
            logger.warn("Error setting tenant context", e)
        }
    }

    /**
     * Clear tenant context
     */
    fun clearTenantContext() {
        tenantThreadLocal.remove()
        clearTenantFromSecurityContext()
        logger.debug("Tenant context cleared")
    }

    /**
     * Execute code block with specific tenant context
     */
    fun <T> withTenant(tenantId: String, block: () -> T): T {
        val originalTenant = getCurrentTenant()
        return try {
            setCurrentTenant(tenantId)
            block()
        } finally {
            if (originalTenant != null) {
                setCurrentTenant(originalTenant)
            } else {
                clearTenantContext()
            }
        }
    }

    /**
     * Get current tenant or throw exception if not set
     */
    fun requireCurrentTenant(): String {
        return getCurrentTenant()
            ?: throw IllegalStateException("No tenant context available. Ensure tenant is set via X-Workspace header or programmatically.")
    }

    /**
     * Check if tenant is currently set
     */
    fun hasTenant(): Boolean {
        return getCurrentTenant() != null
    }

    // Private helper methods

    private fun setTenantInSecurityContext(tenantId: String?) {
        try {
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication != null) {
                // Create or update details map with tenant information
                val details = when (val existingDetails = authentication.details) {
                    is MutableMap<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        existingDetails as MutableMap<String, Any?>
                    }

                    is Map<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        (existingDetails as Map<String, Any?>).toMutableMap()
                    }

                    else -> mutableMapOf<String, Any?>()
                }

                if (tenantId != null) {
                    details[CurrentTenantIdentifierResolver.TENANT_ATTRIBUTE] = tenantId
                    logger.debug("Tenant set in SecurityContext: {}", tenantId)
                } else {
                    details.remove(CurrentTenantIdentifierResolver.TENANT_ATTRIBUTE)
                    logger.debug("Tenant cleared from SecurityContext")
                }
            }
        } catch (e: Exception) {
            logger.debug("Could not set tenant in SecurityContext (this is normal for some contexts)", e)
        }
    }

    private fun clearTenantFromSecurityContext() {
        setTenantInSecurityContext(null)
    }
}