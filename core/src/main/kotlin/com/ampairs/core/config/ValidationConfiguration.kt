package com.ampairs.core.config

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.ErrorDetails
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice


/**
 * Global exception handler for validation errors
 */
@RestControllerAdvice
class ValidationExceptionHandler {

    private val logger = LoggerFactory.getLogger(ValidationExceptionHandler::class.java)

    /**
     * Handle Bean Validation errors (from request body validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val validationErrors = mutableMapOf<String, String>()

        ex.bindingResult.allErrors.forEach { error ->
            val fieldName = (error as? FieldError)?.field ?: error.objectName
            val errorMessage = error.defaultMessage ?: "Invalid input"
            validationErrors[fieldName] = errorMessage
        }

        logger.warn("Validation error: {}", validationErrors)

        val errorResponse = ErrorDetails(
            code = "VALIDATION_ERROR",
            message = "Invalid input data",
            details = "Request validation failed",
            validationErrors = validationErrors,
            module = "validation"
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(errorResponse))
    }

    /**
     * Handle method-level validation errors (from path variables, request parameters)
     */
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ResponseEntity<ApiResponse<Nothing>> {
        val validationErrors = mutableMapOf<String, String>()

        ex.constraintViolations.forEach { violation: ConstraintViolation<*> ->
            val fieldName = violation.propertyPath.toString().substringAfterLast('.')
            val errorMessage = violation.message
            validationErrors[fieldName] = errorMessage
        }

        logger.warn("Method validation error: {}", validationErrors)

        val errorResponse = ErrorDetails(
            code = "VALIDATION_ERROR",
            message = "Invalid input parameters",
            details = "Parameter validation failed",
            validationErrors = validationErrors,
            module = "validation"
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(errorResponse))
    }

    /**
     * Handle security validation errors
     */
    @ExceptionHandler(SecurityException::class)
    fun handleSecurityException(ex: SecurityException): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Security validation error: {}", ex.message)

        val errorResponse = ErrorDetails(
            code = "SECURITY_VALIDATION_ERROR",
            message = "Input contains invalid or dangerous content",
            details = ex.message ?: "Security validation failed",
            module = "security"
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(errorResponse))
    }
}