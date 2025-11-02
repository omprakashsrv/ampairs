package com.ampairs.core.exception

import java.time.LocalDateTime

/**
 * Exception thrown when rate limit is exceeded
 */
class RateLimitExceededException(
    message: String,
    val retryAfterSeconds: Long,
    val limitType: String,
    val resetTime: LocalDateTime,
) : RuntimeException(message) {

    companion object {
        const val ERROR_CODE = "RATE_LIMIT_EXCEEDED"
    }
}