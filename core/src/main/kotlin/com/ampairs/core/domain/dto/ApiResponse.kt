package com.ampairs.core.domain.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    @JsonProperty("success")
    val success: Boolean,

    @JsonProperty("data")
    val data: T? = null,

    @JsonProperty("error")
    val error: ErrorDetails? = null,

    @JsonProperty("timestamp")
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @JsonProperty("path")
    val path: String? = null,

    @JsonProperty("trace_id")
    val traceId: String? = null,
) {
    companion object {
        fun <T> success(data: T, path: String? = null, traceId: String? = null): ApiResponse<T> {
            return ApiResponse(
                success = true,
                data = data,
                path = path,
                traceId = traceId
            )
        }

        fun <T> error(
            errorDetails: ErrorDetails,
            path: String? = null,
            traceId: String? = null,
        ): ApiResponse<T> {
            return ApiResponse(
                success = false,
                error = errorDetails,
                path = path,
                traceId = traceId
            )
        }

        fun <T> error(
            code: String,
            message: String,
            details: String? = null,
            path: String? = null,
            traceId: String? = null,
            validationErrors: Map<String, String>? = null,
        ): ApiResponse<T> {
            return ApiResponse(
                success = false,
                error = ErrorDetails(
                    code = code,
                    message = message,
                    details = details,
                    validationErrors = validationErrors
                ),
                path = path,
                traceId = traceId
            )
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorDetails(
    @JsonProperty("code")
    val code: String,

    @JsonProperty("message")
    val message: String,

    @JsonProperty("details")
    val details: String? = null,

    @JsonProperty("validation_errors")
    val validationErrors: Map<String, String>? = null,

    @JsonProperty("module")
    val module: String? = null,
)

// Common error codes
object ErrorCodes {
    // Generic errors
    const val INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR"
    const val BAD_REQUEST = "BAD_REQUEST"
    const val VALIDATION_ERROR = "VALIDATION_ERROR"
    const val NOT_FOUND = "NOT_FOUND"
    const val METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED"
    const val PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE"

    // Authentication & Authorization
    const val AUTHENTICATION_FAILED = "AUTH_001"
    const val INVALID_CREDENTIALS = "AUTH_002"
    const val TOKEN_EXPIRED = "AUTH_003"
    const val TOKEN_INVALID = "AUTH_004"
    const val TOKEN_GENERATION_FAILED = "AUTH_005"
    const val ACCESS_DENIED = "AUTH_006"
    const val INSUFFICIENT_PERMISSIONS = "AUTH_007"

    // File operations
    const val FILE_NOT_FOUND = "FILE_001"
    const val FILE_UPLOAD_FAILED = "FILE_002"
    const val FILE_DELETION_FAILED = "FILE_003"
    const val FILE_ACCESS_DENIED = "FILE_004"
    const val FILE_SIZE_EXCEEDED = "FILE_005"

    // Tally integration
    const val TALLY_CONNECTION_FAILED = "TALLY_001"
    const val TALLY_PARSING_ERROR = "TALLY_002"
    const val TALLY_SYNC_FAILED = "TALLY_003"
    const val TALLY_TIMEOUT = "TALLY_004"

    // Business logic
    const val CUSTOMER_NOT_FOUND = "CUSTOMER_001"
    const val PRODUCT_NOT_FOUND = "PRODUCT_001"
    const val ORDER_NOT_FOUND = "ORDER_001"
    const val INVOICE_NOT_FOUND = "INVOICE_001"
    const val COMPANY_NOT_FOUND = "COMPANY_001"

    // Data integrity
    const val DUPLICATE_ENTRY = "DATA_001"
    const val CONSTRAINT_VIOLATION = "DATA_002"
    const val FOREIGN_KEY_VIOLATION = "DATA_003"

    // Multi-tenancy
    const val TENANT_NOT_FOUND = "TENANT_001"
    const val TENANT_ACCESS_DENIED = "TENANT_002"
    const val INVALID_TENANT_CONTEXT = "TENANT_003"
}