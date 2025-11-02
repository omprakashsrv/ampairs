package com.ampairs.core.filter

import com.ampairs.core.service.SecurityAuditService
import com.ampairs.core.service.ValidationService
import jakarta.servlet.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

/**
 * Filter to prevent XSS (Cross-Site Scripting) attacks by sanitizing request content
 * This provides defense-in-depth security by filtering malicious scripts at the servlet level
 */
@Component
@Order(2)
class XssProtectionFilter(
    private val validationService: ValidationService,
    private val securityAuditService: SecurityAuditService,
) : Filter {

    private val logger = LoggerFactory.getLogger(XssProtectionFilter::class.java)

    companion object {
        private val EXCLUDED_PATHS = setOf(
            "/actuator/",
            "/health/",
            "/metrics/",
            "/favicon.ico",
            "/static/",
            "/css/",
            "/js/",
            "/images/",
            "/ws"  // WebSocket endpoint - JWT token in query params must not be sanitized
        )

        // Content types that should be checked for XSS
        private val CHECKED_CONTENT_TYPES = setOf(
            "application/json",
            "application/xml",
            "text/xml",
            "text/plain",
            "text/html"
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
            // Wrap the request to sanitize content
            val sanitizedRequest = XssSanitizedRequest(httpRequest)

            // Add security headers
            addSecurityHeaders(httpResponse)

            chain.doFilter(sanitizedRequest, response)
        } catch (e: SecurityException) {
            logger.error("XSS attempt blocked: {}", e.message)

            // Log security incident
            securityAuditService.logSuspiciousActivity(
                activityType = "XSS_BLOCKED",
                description = "Potential XSS attempt blocked",
                riskLevel = SecurityAuditService.RiskLevel.HIGH,
                request = httpRequest,
                additionalDetails = mapOf<String, Any>(
                    "blocked_reason" to (e.message ?: "Unknown"),
                    "method" to httpRequest.method,
                    "path" to httpRequest.requestURI,
                    "content_type" to (httpRequest.contentType ?: "unknown")
                )
            )

            httpResponse.status = HttpServletResponse.SC_BAD_REQUEST
            httpResponse.writer.write("""{"error": "Invalid request content"}""")
            return
        } catch (e: Exception) {
            logger.error("Error in XSS protection filter", e)
            chain.doFilter(request, response)
        }
    }

    private fun shouldSkipFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return EXCLUDED_PATHS.any { excludedPath -> path.startsWith(excludedPath) }
    }

    private fun addSecurityHeaders(response: HttpServletResponse) {
        // X-XSS-Protection header
        response.setHeader("X-XSS-Protection", "1; mode=block")

        // X-Content-Type-Options header
        response.setHeader("X-Content-Type-Options", "nosniff")

        // X-Frame-Options header
        response.setHeader("X-Frame-Options", "DENY")

        // Content-Security-Policy header (basic)
        response.setHeader(
            "Content-Security-Policy",
            "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'"
        )
    }

    /**
     * Request wrapper that sanitizes content to prevent XSS attacks
     */
    inner class XssSanitizedRequest(request: HttpServletRequest) : HttpServletRequestWrapper(request) {

        private var sanitizedBody: ByteArray? = null

        override fun getParameter(name: String?): String? {
            val value = super.getParameter(name)
            return sanitizeForXss(name, value)
        }

        override fun getParameterValues(name: String?): Array<String>? {
            val values = super.getParameterValues(name)
            return values?.mapNotNull { sanitizeForXss(name, it) }?.toTypedArray()
        }

        override fun getParameterMap(): Map<String, Array<String>> {
            val originalMap = super.getParameterMap()
            val sanitizedMap = mutableMapOf<String, Array<String>>()

            originalMap.forEach { (name, values) ->
                sanitizedMap[name] = values.mapNotNull { sanitizeForXss(name, it) }.toTypedArray()
            }

            return sanitizedMap
        }

        override fun getHeader(name: String?): String? {
            val value = super.getHeader(name)
            return if (isHeaderToSanitize(name)) {
                sanitizeForXss("header_$name", value)
            } else {
                value
            }
        }

        override fun getReader(): BufferedReader {
            return BufferedReader(InputStreamReader(inputStream))
        }

        override fun getInputStream(): ServletInputStream {
            if (sanitizedBody == null) {
                val originalBody = super.getInputStream().readAllBytes()
                sanitizedBody = sanitizeRequestBody(originalBody)
            }

            return object : ServletInputStream() {
                private val inputStream = ByteArrayInputStream(sanitizedBody)

                override fun read(): Int = inputStream.read()
                override fun isFinished(): Boolean = inputStream.available() == 0
                override fun isReady(): Boolean = true
                override fun setReadListener(readListener: ReadListener?) {
                    // Not implemented for this use case
                }
            }
        }

        private fun sanitizeForXss(paramName: String?, value: String?): String? {
            if (value.isNullOrBlank()) return value

            // Check for XSS patterns
            if (validationService.containsXss(value)) {
                val clientIp = getClientIp()
                logger.warn(
                    "XSS attempt in parameter/header '{}': {} from IP: {}",
                    paramName, value.take(100), clientIp
                )
                throw SecurityException("Invalid content detected in: $paramName")
            }

            // Sanitize HTML content
            return validationService.sanitizeHtml(value)
        }

        private fun sanitizeRequestBody(body: ByteArray): ByteArray {
            if (body.isEmpty()) return body

            val contentType = contentType?.lowercase()
            if (contentType == null || !CHECKED_CONTENT_TYPES.any { contentType.contains(it) }) {
                return body
            }

            try {
                val bodyString = String(body)

                // Check for XSS patterns
                if (validationService.containsXss(bodyString)) {
                    val clientIp = getClientIp()
                    logger.warn("XSS attempt in request body from IP: {}", clientIp)
                    throw SecurityException("Invalid content detected in request body")
                }

                // For JSON content, skip body sanitization as it will be handled by Spring MVC and validation
                val sanitizedBody = if (contentType.contains("application/json")) {
                    bodyString  // Don't sanitize JSON body - let Spring handle it
                } else {
                    validationService.sanitizeHtml(bodyString)
                }

                return sanitizedBody.toByteArray()
            } catch (e: SecurityException) {
                throw e
            } catch (e: Exception) {
                logger.debug("Could not sanitize request body: {}", e.message)
                return body
            }
        }


        private fun isHeaderToSanitize(headerName: String?): Boolean {
            if (headerName.isNullOrBlank()) return false

            val headerLower = headerName.lowercase()
            return headerLower in setOf(
                "user-agent",
                "referer",
                "origin",
                "x-forwarded-for",
                "x-real-ip"
            )
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