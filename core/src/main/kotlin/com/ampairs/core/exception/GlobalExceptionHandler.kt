package com.ampairs.core.exception

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.ErrorCodes
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.NoHandlerFoundException

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE) // Execute last, after module-specific handlers
class GlobalExceptionHandler : BaseExceptionHandler() {

    // Validation Exceptions
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        val errors = extractValidationErrors(ex.bindingResult)
        return createValidationErrorResponse(errors, request, "global")
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(
        ex: ConstraintViolationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        val errors = extractConstraintViolationErrors(ex.constraintViolations)
        return createValidationErrorResponse(errors, request, "global")
    }

    // HTTP Message Exceptions
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.BAD_REQUEST,
            message = "Invalid JSON format",
            details = "Request body contains invalid JSON",
            request = request,
            moduleName = "global"
        )
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(
        ex: MissingServletRequestParameterException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.BAD_REQUEST,
            message = "Missing required parameter",
            details = "Required parameter '${ex.parameterName}' is missing",
            request = request,
            moduleName = "global"
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.BAD_REQUEST,
            message = "Invalid parameter type",
            details = "Parameter '${ex.name}' should be of type ${ex.requiredType?.simpleName}",
            request = request,
            moduleName = "global"
        )
    }

    // File Upload Exceptions
    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceededException(
        ex: MaxUploadSizeExceededException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.PAYLOAD_TOO_LARGE,
            errorCode = ErrorCodes.PAYLOAD_TOO_LARGE,
            message = "File too large",
            details = "File size exceeds maximum allowed limit",
            request = request,
            moduleName = "global"
        )
    }

    // Database Exceptions
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        val errorCode = when {
            ex.message?.contains("Duplicate entry") == true -> ErrorCodes.DUPLICATE_ENTRY
            ex.message?.contains("foreign key constraint") == true -> ErrorCodes.FOREIGN_KEY_VIOLATION
            else -> ErrorCodes.CONSTRAINT_VIOLATION
        }

        return createErrorResponse(
            httpStatus = HttpStatus.CONFLICT,
            errorCode = errorCode,
            message = "Data integrity violation",
            details = "The operation violates data integrity constraints",
            request = request,
            moduleName = "global"
        )
    }

    // HTTP Method Exceptions
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(
        ex: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.METHOD_NOT_ALLOWED,
            errorCode = ErrorCodes.METHOD_NOT_ALLOWED,
            message = "Method not allowed",
            details = "HTTP method '${ex.method}' is not supported for this endpoint",
            request = request,
            moduleName = "global"
        )
    }

    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNoHandlerFoundException(
        ex: NoHandlerFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.NOT_FOUND,
            errorCode = ErrorCodes.NOT_FOUND,
            message = "Endpoint not found",
            details = "No handler found for ${ex.httpMethod} ${ex.requestURL}",
            request = request,
            moduleName = "global"
        )
    }

    // Rate Limiting Exception
    @ExceptionHandler(RateLimitExceededException::class)
    fun handleRateLimitExceededException(
        ex: RateLimitExceededException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        val detailsText =
            "Limit type: ${ex.limitType}, retry after: ${ex.retryAfterSeconds}s, resets at: ${ex.resetTime}"

        val response = createErrorResponse<Any>(
            httpStatus = HttpStatus.TOO_MANY_REQUESTS,
            errorCode = RateLimitExceededException.ERROR_CODE,
            message = ex.message ?: "Rate limit exceeded",
            details = detailsText,
            request = request,
            moduleName = "global"
        )

        // Add Retry-After header as per HTTP standard
        response.headers.add("Retry-After", ex.retryAfterSeconds.toString())
        response.headers.add("X-RateLimit-Limit-Type", ex.limitType)
        response.headers.add("X-RateLimit-Reset", ex.resetTime.toString())

        return response
    }

    // Generic Exception Handler - catches all other exceptions
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        logger.error("Unexpected error for request {}: {}", request.requestURI, ex.message, ex)

        return createErrorResponse(
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            errorCode = ErrorCodes.INTERNAL_SERVER_ERROR,
            message = "Internal server error",
            details = "An unexpected error occurred",
            request = request,
            moduleName = "global"
        )
    }
}