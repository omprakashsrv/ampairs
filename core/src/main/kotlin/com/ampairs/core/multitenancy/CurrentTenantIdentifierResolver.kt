package com.ampairs.core.multitenancy

import org.hibernate.context.spi.CurrentTenantIdentifierResolver
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * Spring-native implementation of Hibernate's CurrentTenantIdentifierResolver
 * This integrates directly with Spring Security and Hibernate's multi-tenancy system
 */
@Component
class CurrentTenantIdentifierResolver : CurrentTenantIdentifierResolver<String> {

    companion object {
        private val logger = LoggerFactory.getLogger(CurrentTenantIdentifierResolver::class.java)
        private const val DEFAULT_TENANT = "default"
        const val TENANT_ATTRIBUTE = "TENANT_ID"
    }

    /**
     * Resolve tenant identifier from Spring Security context
     * This method is called automatically by Hibernate for every query
     */
    override fun resolveCurrentTenantIdentifier(): String {
        return try {
            // Try to get tenant from SecurityContext attributes first
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication != null) {
                val tenantId = authentication.details?.let { details ->
                    if (details is Map<*, *>) {
                        details[TENANT_ATTRIBUTE] as? String
                    } else null
                }

                if (!tenantId.isNullOrBlank()) {
                    logger.debug("Resolved tenant from SecurityContext: {}", tenantId)
                    return tenantId
                }
            }

            // Fallback to thread-local context (for backward compatibility)
            val threadTenant = TenantContextHolder.getCurrentTenant()
            if (!threadTenant.isNullOrBlank()) {
                logger.debug("Resolved tenant from ThreadLocal: {}", threadTenant)
                return threadTenant
            }

            logger.debug("No tenant context found, using default tenant")
            DEFAULT_TENANT

        } catch (e: Exception) {
            logger.warn("Error resolving tenant identifier, falling back to default", e)
            DEFAULT_TENANT
        }
    }

    /**
     * Validates that tenant identifier is not null/empty
     */
    override fun validateExistingCurrentSessions(): Boolean {
        return true
    }

    /**
     * Get the default tenant identifier
     */
    fun getDefaultTenant(): String = DEFAULT_TENANT

    /**
     * Check if current context has a valid tenant
     */
    fun hasValidTenant(): Boolean {
        val current = resolveCurrentTenantIdentifier()
        return current != DEFAULT_TENANT && current.isNotBlank()
    }
}