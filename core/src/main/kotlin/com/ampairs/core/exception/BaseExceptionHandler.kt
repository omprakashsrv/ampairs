package com.ampairs.core.exception

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.ErrorCodes
import com.ampairs.core.domain.dto.ErrorDetails
import com.ampairs.core.logging.TraceIdFilter
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.*

abstract class BaseExceptionHandler {

    protected val logger = LoggerFactory.getLogger(this::class.java)

    protected fun <T> createErrorResponse(
        httpStatus: HttpStatus,
        errorCode: String,
        message: String,
        details: String? = null,
        request: HttpServletRequest? = null,
        validationErrors: Map<String, String>? = null,
        moduleName: String? = null,
    ): ResponseEntity<ApiResponse<T>> {

        val traceId = generateTraceId()
        val path = request?.requestURI

        logError(errorCode, message, details, path, traceId, httpStatus)

        val errorDetails = ErrorDetails(
            code = errorCode,
            message = message,
            details = details,
            validationErrors = validationErrors,
            module = moduleName
        )

        val apiResponse = ApiResponse.error<T>(
            errorDetails = errorDetails,
            path = path,
            traceId = traceId
        )

        return ResponseEntity.status(httpStatus).body(apiResponse)
    }

    protected fun <T> createValidationErrorResponse(
        validationErrors: Map<String, String>,
        request: HttpServletRequest? = null,
        moduleName: String? = null,
    ): ResponseEntity<ApiResponse<T>> {

        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "Validation failed",
            details = "One or more fields have invalid values",
            request = request,
            validationErrors = validationErrors,
            moduleName = moduleName
        )
    }

    protected fun logError(
        errorCode: String,
        message: String,
        details: String?,
        path: String?,
        traceId: String,
        httpStatus: HttpStatus,
    ) {
        val logMessage = buildString {
            append("Error occurred - ")
            append("Code: $errorCode, ")
            append("Message: $message, ")
            append("Status: ${httpStatus.value()}, ")
            append("Path: $path, ")
            append("TraceId: $traceId")
            if (details != null) {
                append(", Details: $details")
            }
        }

        when {
            httpStatus.is5xxServerError -> logger.error(logMessage)
            httpStatus.is4xxClientError -> logger.warn(logMessage)
            else -> logger.info(logMessage)
        }
    }

    protected fun extractValidationErrors(bindingResult: org.springframework.validation.BindingResult): Map<String, String> {
        return bindingResult.fieldErrors.associate { error ->
            error.field to (error.defaultMessage ?: "Invalid value")
        }
    }

    protected fun extractConstraintViolationErrors(violations: Set<jakarta.validation.ConstraintViolation<*>>): Map<String, String> {
        return violations.associate { violation ->
            violation.propertyPath.toString() to violation.message
        }
    }

    private fun generateTraceId(): String {
        return MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY) ?: UUID.randomUUID().toString().substring(0, 8)
    }

    protected fun getModuleName(): String {
        return this::class.java.packageName.split(".").getOrElse(2) { "unknown" }
    }
}