package com.ampairs.core.interceptor

import com.ampairs.core.service.SecurityAuditService
import com.ampairs.core.service.ValidationService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.util.ContentCachingRequestWrapper
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Security validation interceptor that performs additional input validation
 * before requests reach controllers. Provides defense-in-depth security.
 */
@Component
class SecurityValidationInterceptor(
    private val validationService: ValidationService,
    private val securityAuditService: SecurityAuditService,
) : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(SecurityValidationInterceptor::class.java)

    companion object {
        private val EXCLUDED_PATHS = setOf(
            "/actuator",
            "/health",
            "/metrics",
            "/favicon.ico",
            "/static/",
            "/css/",
            "/js/",
            "/images/"
        )

        private val MAX_REQUEST_SIZE = 10 * 1024 * 1024 // 10MB
        private val MAX_PARAMETER_LENGTH = 10000
        private val MAX_HEADER_LENGTH = 8192
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {

        // Skip validation for excluded paths
        if (shouldSkipValidation(request)) {
            return true
        }

        try {
            // Validate request size
            if (!validateRequestSize(request)) {
                logger.warn("Request size too large from IP: {}", getClientIp(request))
                response.status = HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE
                return false
            }

            // Validate headers
            if (!validateHeaders(request)) {
                logSuspiciousActivity(request, "Malicious headers detected")
                response.status = HttpServletResponse.SC_BAD_REQUEST
                return false
            }

            // Validate URL parameters
            if (!validateParameters(request)) {
                logSuspiciousActivity(request, "Malicious parameters detected")
                response.status = HttpServletResponse.SC_BAD_REQUEST
                return false
            }

            // Validate request body for POST/PUT requests
            if (request.method in listOf("POST", "PUT", "PATCH")) {
                if (!validateRequestBody(request)) {
                    logSuspiciousActivity(request, "Malicious request body detected")
                    response.status = HttpServletResponse.SC_BAD_REQUEST
                    return false
                }
            }

            return true

        } catch (e: Exception) {
            logger.error("Error during security validation for {}: {}", request.requestURI, e.message)
            response.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            return false
        }
    }

    private fun shouldSkipValidation(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return EXCLUDED_PATHS.any { excludedPath -> path.startsWith(excludedPath) }
    }

    private fun validateRequestSize(request: HttpServletRequest): Boolean {
        val contentLength = request.contentLength
        return contentLength == -1 || contentLength <= MAX_REQUEST_SIZE
    }

    private fun validateHeaders(request: HttpServletRequest): Boolean {
        val headerNames = request.headerNames

        while (headerNames.hasMoreElements()) {
            val headerName = headerNames.nextElement()
            val headerValues = request.getHeaders(headerName)

            // Check header name
            if (headerName.length > MAX_HEADER_LENGTH) {
                logger.warn("Header name too long: {}", headerName.take(100))
                return false
            }

            if (validationService.containsSqlInjection(headerName) ||
                validationService.containsXss(headerName)
            ) {
                logger.warn("Malicious header name detected: {}", headerName)
                return false
            }

            // Check header values
            while (headerValues.hasMoreElements()) {
                val headerValue = headerValues.nextElement()

                if (headerValue.length > MAX_HEADER_LENGTH) {
                    logger.warn("Header value too long for {}: {}", headerName, headerValue.take(100))
                    return false
                }

                // Skip validation for standard HTTP headers that commonly contain patterns that might trigger false positives
                if (isStandardHttpHeader(headerName, headerValue)) {
                    continue
                }

                if (validationService.containsSqlInjection(headerValue) ||
                    validationService.containsXss(headerValue)
                ) {
                    logger.warn("Malicious header value detected in {}: {}", headerName, headerValue.take(100))
                    return false
                }
            }
        }

        return true
    }

    private fun isStandardHttpHeader(headerName: String, headerValue: String): Boolean {
        val normalizedHeaderName = headerName.lowercase()

        return when (normalizedHeaderName) {
            "accept" -> {
                // Common accept header patterns
                headerValue.matches(Regex("^[\\w\\-*/,;=. ]+$"))
            }
            "accept-encoding" -> {
                // Common encoding patterns
                headerValue.matches(Regex("^[\\w\\-,; ]+$"))
            }
            "accept-language" -> {
                // Language patterns
                headerValue.matches(Regex("^[\\w\\-,;=. ]+$"))
            }
            "user-agent" -> {
                // User agent strings are complex but generally safe
                headerValue.length <= 512 && !headerValue.contains("<script")
            }
            "referer", "referrer" -> {
                // URL patterns
                headerValue.matches(Regex("^https?://[\\w\\-./:%?&=]+$"))
            }
            "origin" -> {
                // Origin patterns
                headerValue.matches(Regex("^https?://[\\w\\-.:]+$"))
            }
            "content-type" -> {
                // Content type patterns
                headerValue.matches(Regex("^[\\w\\-/;=. ]+$"))
            }
            "authorization" -> {
                // Bearer tokens, basic auth, etc.
                headerValue.matches(Regex("^(Bearer|Basic|Digest) [\\w\\-=+/]+$"))
            }
            "cache-control", "pragma", "expires" -> {
                // Cache control patterns
                true
            }
            else -> false
        }
    }

    private fun validateParameters(request: HttpServletRequest): Boolean {
        val parameterNames = request.parameterNames

        while (parameterNames.hasMoreElements()) {
            val paramName = parameterNames.nextElement()
            val paramValues = request.getParameterValues(paramName)

            // Check parameter name
            if (paramName.length > MAX_PARAMETER_LENGTH) {
                logger.warn("Parameter name too long: {}", paramName.take(100))
                return false
            }

            if (validationService.containsSqlInjection(paramName) ||
                validationService.containsXss(paramName)
            ) {
                logger.warn("Malicious parameter name detected: {}", paramName)
                return false
            }

            // Check parameter values
            paramValues?.forEach { paramValue ->
                if (paramValue.length > MAX_PARAMETER_LENGTH) {
                    logger.warn("Parameter value too long for {}: {}", paramName, paramValue.take(100))
                    return false
                }

                if (validationService.containsSqlInjection(paramValue) ||
                    validationService.containsXss(paramValue)
                ) {
                    logger.warn("Malicious parameter value detected in {}: {}", paramName, paramValue.take(100))
                    return false
                }
            }
        }

        return true
    }

    private fun validateRequestBody(request: HttpServletRequest): Boolean {
        try {
            // Only validate if request wrapper is available
            if (request is ContentCachingRequestWrapper) {
                val body = request.contentAsByteArray
                if (body.isNotEmpty()) {
                    val bodyString = String(body, StandardCharsets.UTF_8)

                    // Check for malicious patterns in request body
                    if (validationService.containsSqlInjection(bodyString)) {
                        logger.warn("SQL injection detected in request body from IP: {}", getClientIp(request))
                        return false
                    }

                    if (validationService.containsXss(bodyString)) {
                        logger.warn("XSS detected in request body from IP: {}", getClientIp(request))
                        return false
                    }
                }
            }

            return true

        } catch (e: IOException) {
            logger.warn("Error reading request body: {}", e.message)
            return false
        } catch (e: Exception) {
            logger.error("Unexpected error validating request body: {}", e.message)
            return false
        }
    }

    private fun logSuspiciousActivity(request: HttpServletRequest, reason: String) {
        securityAuditService.logSuspiciousActivity(
            activityType = "MALICIOUS_INPUT_DETECTED",
            description = reason,
            riskLevel = SecurityAuditService.RiskLevel.HIGH,
            request = request,
            additionalDetails = mapOf(
                "method" to request.method,
                "path" to request.requestURI,
                "query_string" to (request.queryString ?: ""),
                "user_agent" to (request.getHeader("User-Agent") ?: "Unknown"),
                "referer" to (request.getHeader("Referer") ?: "None")
            )
        )
    }

    private fun getClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",")[0].trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }

        return request.remoteAddr ?: "unknown"
    }
}