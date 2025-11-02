package com.ampairs.core.exception

/**
 * Exception thrown when business rules are violated
 */
class BusinessException(
    val errorCode: String,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {

    constructor(message: String) : this("BUSINESS_ERROR", message)
    constructor(message: String, cause: Throwable) : this("BUSINESS_ERROR", message, cause)
}