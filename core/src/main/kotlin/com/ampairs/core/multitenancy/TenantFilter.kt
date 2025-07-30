package com.ampairs.core.multitenancy

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.Order
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(1)
@ConditionalOnProperty(name = ["application.multitenancy.enabled"], havingValue = "true", matchIfMissing = true)
class TenantFilter : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(TenantFilter::class.java)

    companion object {
        const val TENANT_HEADER = "X-Tenant-ID"
        const val TENANT_PARAM = "tenantId"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val tenantId = extractTenantId(request)

            if (tenantId != null) {
                TenantContext.setCurrentTenant(tenantId)
                logger.debug("Tenant context set for request: {} to {}", request.requestURI, tenantId)
            } else {
                logger.debug("No tenant ID found for request: {}", request.requestURI)
            }

            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clearTenantContext()
            logger.debug("Tenant context cleared after request: {}", request.requestURI)
        }
    }

    private fun extractTenantId(request: HttpServletRequest): String? {
        // Try header first
        request.getHeader(TENANT_HEADER)?.let { return it }

        // Try request parameter
        request.getParameter(TENANT_PARAM)?.let { return it }

        // Try to extract from authentication context
        SecurityContextHolder.getContext().authentication?.let { auth ->
            when {
                auth.principal is TenantAware -> {
                    return (auth.principal as TenantAware).getTenantId()
                }

                auth.details is Map<*, *> -> {
                    val details = auth.details as Map<String, Any>
                    return details["tenantId"] as? String
                }
            }
        }

        // Try to extract from subdomain (if applicable)
        extractTenantFromSubdomain(request)?.let { return it }

        return null
    }

    private fun extractTenantFromSubdomain(request: HttpServletRequest): String? {
        val serverName = request.serverName
        if (serverName.contains(".")) {
            val parts = serverName.split(".")
            if (parts.size >= 3) { // subdomain.domain.tld
                val subdomain = parts[0]
                // Only consider valid tenant subdomains (not www, api, etc.)
                if (subdomain.matches(Regex("[a-zA-Z0-9-]{3,}"))) {
                    return subdomain
                }
            }
        }
        return null
    }
}

interface TenantAware {
    fun getTenantId(): String?
}