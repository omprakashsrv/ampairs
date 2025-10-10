package com.ampairs.business.exception

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * Exception thrown when a business profile is not found for a workspace.
 *
 * **HTTP Status**: 404 Not Found
 *
 * **Usage**: Thrown by service layer when GET/PUT operations cannot find business
 */
class BusinessNotFoundException(
    ownerId: String
) : ResponseStatusException(
    HttpStatus.NOT_FOUND,
    "Business profile not found for workspace: $ownerId"
) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
