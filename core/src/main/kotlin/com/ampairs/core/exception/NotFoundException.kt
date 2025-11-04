package com.ampairs.core.exception

/**
 * Exception thrown when a requested resource is not found
 */
class NotFoundException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)