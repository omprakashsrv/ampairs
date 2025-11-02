package com.ampairs.core.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*

/**
 * Filter to generate and set trace ID for request tracking and correlation
 * This filter runs before all other filters to ensure trace ID is available throughout the request lifecycle
 */
@Component
@Order(0) // Highest priority to run before all other filters
class TraceIdFilter : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(TraceIdFilter::class.java)

    companion object {
        const val TRACE_ID_HEADER = "X-Trace-ID"
        const val TRACE_ID_MDC_KEY = "traceId"
        const val TRACE_ID_RESPONSE_HEADER = "X-Trace-ID"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = generateOrExtractTraceId(request)

        try {
            // Set trace ID in MDC for logging
            MDC.put(TRACE_ID_MDC_KEY, traceId)

            // Add trace ID to response headers for client tracking
            response.setHeader(TRACE_ID_RESPONSE_HEADER, traceId)

            logger.debug("Trace ID set for request: {} -> {}", request.requestURI, traceId)

            filterChain.doFilter(request, response)
        } finally {
            // Clean up MDC to prevent memory leaks
            MDC.remove(TRACE_ID_MDC_KEY)
            logger.debug("Trace ID cleared after request: {}", request.requestURI)
        }
    }

    /**
     * Generate or extract trace ID from the request
     * Priority: Header -> Generate new UUID
     */
    private fun generateOrExtractTraceId(request: HttpServletRequest): String {
        // Try to get trace ID from incoming request header (for distributed tracing)
        request.getHeader(TRACE_ID_HEADER)?.let { headerTraceId ->
            if (headerTraceId.isNotBlank() && isValidTraceId(headerTraceId)) {
                logger.debug("Using trace ID from request header: {}", headerTraceId)
                return headerTraceId
            }
        }

        // Generate new trace ID if not provided or invalid
        val newTraceId = UUID.randomUUID().toString().substring(0, 8)
        logger.debug("Generated new trace ID: {}", newTraceId)
        return newTraceId
    }

    /**
     * Validate trace ID format - should be alphanumeric and reasonable length
     */
    private fun isValidTraceId(traceId: String): Boolean {
        return traceId.matches(Regex("[a-zA-Z0-9-]{4,36}")) && traceId.length <= 36
    }
}