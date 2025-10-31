package com.ampairs.business.exception

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * Exception thrown when business data violates business rules.
 *
 * **HTTP Status**: 400 Bad Request
 *
 * **Usage**: Thrown by service layer for business rule violations:
 * - Closing hours before opening hours
 * - Invalid operating days
 * - Business logic constraints not covered by Jakarta validation
 */
class InvalidBusinessDataException(
    message: String
) : ResponseStatusException(
    HttpStatus.BAD_REQUEST,
    message
) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
