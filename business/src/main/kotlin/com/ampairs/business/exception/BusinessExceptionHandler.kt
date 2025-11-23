package com.ampairs.business.exception

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.ErrorDetails
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant
import java.util.*

/**
 * Global exception handler for Business module.
 *
 * **Pattern**: Centralized error handling, no try-catch in controllers
 * **Response Format**: ApiResponse<T> with error details and trace IDs
 *
 * Handles:
 * - BusinessNotFoundException → 404
 * - BusinessAlreadyExistsException → 409
 * - InvalidBusinessDataException → 400
 * - MethodArgumentNotValidException → 400 (validation errors)
 */
@RestControllerAdvice
class BusinessExceptionHandler {

    /**
     * Handle business not found errors (404)
     */
    @ExceptionHandler(BusinessNotFoundException::class)
    fun handleBusinessNotFound(
        ex: BusinessNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        val error = ErrorDetails(
            code = "BUSINESS_NOT_FOUND",
            message = ex.reason ?: "Business profile not found",
            details = null
        )

        val response = ApiResponse<Nothing>(
            success = false,
            data = null,
            error = error,
            timestamp = Instant.now(),
            path = request.requestURI,
            traceId = generateTraceId()
        )

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
    }

    /**
     * Handle duplicate business errors (409)
     */
    @ExceptionHandler(BusinessAlreadyExistsException::class)
    fun handleBusinessAlreadyExists(
        ex: BusinessAlreadyExistsException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        val error = ErrorDetails(
            code = "BUSINESS_ALREADY_EXISTS",
            message = ex.reason ?: "Business profile already exists",
            details = null
        )

        val response = ApiResponse<Nothing>(
            success = false,
            data = null,
            error = error,
            timestamp = Instant.now(),
            path = request.requestURI,
            traceId = generateTraceId()
        )

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response)
    }

    /**
     * Handle business rule violation errors (400)
     */
    @ExceptionHandler(InvalidBusinessDataException::class)
    fun handleInvalidBusinessData(
        ex: InvalidBusinessDataException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        val error = ErrorDetails(
            code = "INVALID_BUSINESS_DATA",
            message = ex.reason ?: "Invalid business data",
            details = null
        )

        val response = ApiResponse<Nothing>(
            success = false,
            data = null,
            error = error,
            timestamp = Instant.now(),
            path = request.requestURI,
            traceId = generateTraceId()
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    /**
     * Handle business image not found errors (404)
     */
    @ExceptionHandler(BusinessImageNotFoundException::class)
    fun handleBusinessImageNotFound(
        ex: BusinessImageNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        val error = ErrorDetails(
            code = "BUSINESS_IMAGE_NOT_FOUND",
            message = ex.message ?: "Business image not found",
            details = null
        )

        val response = ApiResponse<Nothing>(
            success = false,
            data = null,
            error = error,
            timestamp = Instant.now(),
            path = request.requestURI,
            traceId = generateTraceId()
        )

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
    }

    /**
     * Handle business image validation errors (400)
     */
    @ExceptionHandler(BusinessImageValidationException::class)
    fun handleBusinessImageValidation(
        ex: BusinessImageValidationException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        val error = ErrorDetails(
            code = "IMAGE_VALIDATION_ERROR",
            message = ex.message ?: "Image validation failed",
            details = null
        )

        val response = ApiResponse<Nothing>(
            success = false,
            data = null,
            error = error,
            timestamp = Instant.now(),
            path = request.requestURI,
            traceId = generateTraceId()
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    /**
     * Handle validation errors from @Valid annotations (400)
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        val validationErrors = ex.bindingResult.fieldErrors.associate { fieldError ->
            fieldError.field to (fieldError.defaultMessage ?: "Validation error")
        }

        val error = ErrorDetails(
            code = "VALIDATION_ERROR",
            message = "Invalid input data",
            details = null,
            validationErrors = validationErrors
        )

        val response = ApiResponse<Nothing>(
            success = false,
            data = null,
            error = error,
            timestamp = Instant.now(),
            path = request.requestURI,
            traceId = generateTraceId()
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    /**
     * Generate unique trace ID for error tracking
     */
    private fun generateTraceId(): String {
        return UUID.randomUUID().toString().substring(0, 8)
    }
}
