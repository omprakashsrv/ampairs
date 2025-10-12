package com.ampairs.business.exception

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * Exception thrown when attempting to create a duplicate business profile.
 *
 * **HTTP Status**: 409 Conflict
 *
 * **Usage**: Thrown by service layer when POST operation tries to create
 * a business for a workspace that already has one (unique constraint)
 */
class BusinessAlreadyExistsException(
    ownerId: String
) : ResponseStatusException(
    HttpStatus.CONFLICT,
    "Business profile already exists for workspace: $ownerId"
) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
