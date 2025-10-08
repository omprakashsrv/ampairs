package com.ampairs.auth.exception

import com.ampairs.auth.service.RecaptchaValidationResult

/**
 * Exception thrown when reCAPTCHA validation fails
 */
class RecaptchaValidationException(
    message: String,
    val validationResult: RecaptchaValidationResult,
) : RuntimeException(message) {

    constructor(validationResult: RecaptchaValidationResult) : this(
        validationResult.message,
        validationResult
    )
}

/**
 * Exception thrown when reCAPTCHA token is missing for protected endpoints
 */
class RecaptchaTokenMissingException(
    message: String = "reCAPTCHA token is required for this operation",
) : RuntimeException(message)