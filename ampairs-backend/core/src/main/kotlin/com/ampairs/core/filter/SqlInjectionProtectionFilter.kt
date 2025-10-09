package com.ampairs.core.filter

import com.ampairs.core.service.SecurityAuditService
import com.ampairs.core.service.ValidationService
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Filter to prevent SQL injection attacks by sanitizing request parameters
 * This provides defense-in-depth security by filtering malicious input at the servlet level
 */
@Component
@Order(1)
class SqlInjectionProtectionFilter(
    private val validationService: ValidationService,
    private val securityAuditService: SecurityAuditService,
) : Filter {

    private val logger = LoggerFactory.getLogger(SqlInjectionProtectionFilter::class.java)

    companion object {
        private val EXCLUDED_PATHS = setOf(
            "/actuator/",
            "/health/",
            "/metrics/",
            "/favicon.ico",
            "/static/",
            "/css/",
            "/js/",
            "/images/"
        )
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        // Skip filter for excluded paths
        if (shouldSkipFilter(httpRequest)) {
            chain.doFilter(request, response)
            return
        }

        try {
            // Wrap the request to sanitize parameters
            val sanitizedRequest = SqlInjectionSanitizedRequest(httpRequest)
            chain.doFilter(sanitizedRequest, response)
        } catch (e: SecurityException) {
            logger.error("SQL injection attempt blocked: {}", e.message)

            // Log security incident
            securityAuditService.logSuspiciousActivity(
                activityType = "SQL_INJECTION_BLOCKED",
                description = "Potential SQL injection attempt blocked",
                riskLevel = SecurityAuditService.RiskLevel.HIGH,
                request = httpRequest,
                additionalDetails = mapOf<String, Any>(
                    "blocked_reason" to (e.message ?: "Unknown"),
                    "method" to httpRequest.method,
                    "path" to httpRequest.requestURI
                )
            )

            httpResponse.status = HttpServletResponse.SC_BAD_REQUEST
            httpResponse.writer.write("""{"error": "Invalid request parameters"}""")
            return
        } catch (e: Exception) {
            logger.error("Error in SQL injection protection filter", e)
            chain.doFilter(request, response)
        }
    }

    private fun shouldSkipFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return EXCLUDED_PATHS.any { excludedPath -> path.startsWith(excludedPath) }
    }

    /**
     * Request wrapper that sanitizes parameters to prevent SQL injection
     */
    inner class SqlInjectionSanitizedRequest(request: HttpServletRequest) : HttpServletRequestWrapper(request) {

        override fun getParameter(name: String?): String? {
            val value = super.getParameter(name)
            return sanitizeParameter(name, value)
        }

        override fun getParameterValues(name: String?): Array<String>? {
            val values = super.getParameterValues(name)
            return values?.mapNotNull { sanitizeParameter(name, it) }?.toTypedArray()
        }

        override fun getParameterMap(): Map<String, Array<String>> {
            val originalMap = super.getParameterMap()
            val sanitizedMap = mutableMapOf<String, Array<String>>()

            originalMap.forEach { (name, values) ->
                sanitizedMap[name] = values.mapNotNull { sanitizeParameter(name, it) }.toTypedArray()
            }

            return sanitizedMap
        }

        private fun sanitizeParameter(name: String?, value: String?): String? {
            if (value.isNullOrBlank()) return value

            // Check for SQL injection patterns
            if (validationService.containsSqlInjection(value)) {
                val clientIp = getClientIp()
                logger.warn(
                    "SQL injection attempt in parameter '{}': {} from IP: {}",
                    name, value.take(100), clientIp
                )
                throw SecurityException("Invalid characters detected in parameter: $name")
            }

            // Sanitize the value
            return validationService.sanitizeForDatabase(value)
        }

        private fun getClientIp(): String {
            val xForwardedFor = getHeader("X-Forwarded-For")
            if (!xForwardedFor.isNullOrBlank()) {
                return xForwardedFor.split(",")[0].trim()
            }

            val xRealIp = getHeader("X-Real-IP")
            if (!xRealIp.isNullOrBlank()) {
                return xRealIp
            }

            return remoteAddr ?: "unknown"
        }
    }
}