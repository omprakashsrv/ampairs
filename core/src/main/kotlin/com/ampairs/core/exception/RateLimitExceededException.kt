package com.ampairs.core.exception

import java.time.Instant

class RateLimitExceededException(
    message: String,
    val retryAfterSeconds: Long,
    val limitType: String,
    val resetTime: Instant,
) : RuntimeException(message) {

    companion object {
        const val ERROR_CODE = "RATE_LIMIT_EXCEEDED"
    }
}
