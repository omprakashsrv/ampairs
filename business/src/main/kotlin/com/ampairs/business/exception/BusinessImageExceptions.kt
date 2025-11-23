package com.ampairs.business.exception

/**
 * Exception thrown when a business image is not found.
 */
class BusinessImageNotFoundException(message: String) : RuntimeException(message)

/**
 * Exception thrown when business image validation fails.
 */
class BusinessImageValidationException(message: String) : RuntimeException(message)
