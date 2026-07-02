package com.ampairs.core.domain.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

/**
 * Standard API response wrapper for all endpoints.
 *
 * **Timezone Note**:
 * - `timestamp` uses Instant (always UTC)
 * - Serializes as ISO-8601 with 'Z' suffix: "2025-01-09T14:30:00Z"
 * - Clients should convert to local timezone for display
 *
 * @param T The type of data being returned
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorDetails? = null,
    val timestamp: Instant = Instant.now(),
    val path: String? = null,
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
    val code: String,
    val message: String,
    val details: String? = null,
    val validationErrors: Map<String, String>? = null,
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
    const val ACCOUNT_LOCKED = "AUTH_008"
    const val RATE_LIMIT_EXCEEDED = "AUTH_009"

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
    const val SUPPLIER_NOT_FOUND = "SUPPLIER_001"
    const val PRODUCT_NOT_FOUND = "PRODUCT_001"
    const val ORDER_NOT_FOUND = "ORDER_001"
    const val PURCHASE_NOT_FOUND = "PURCHASE_001"
    const val INVOICE_NOT_FOUND = "INVOICE_001"
    const val WORKSPACE_NOT_FOUND = "WORKSPACE_001"
    const val UNIT_NOT_FOUND = "UNIT_001"
    const val UNIT_IN_USE = "UNIT_002"
    const val CIRCULAR_CONVERSION = "UNIT_003"
    const val SEQUENCE_DEFINITION_NOT_FOUND = "SEQUENCE_001"
    const val SEQUENCE_DEFINITION_DUPLICATE = "SEQUENCE_002"
    const val SEQUENCE_DEFINITION_INACTIVE = "SEQUENCE_003"
    const val SEQUENCE_INVALID_REQUEST = "SEQUENCE_004"
    const val SEQUENCE_ALLOCATION_NOT_FOUND = "SEQUENCE_005"

    // Data integrity
    const val DUPLICATE_ENTRY = "DATA_001"
    const val CONSTRAINT_VIOLATION = "DATA_002"
    const val FOREIGN_KEY_VIOLATION = "DATA_003"

    // Multi-tenancy
    const val TENANT_NOT_FOUND = "TENANT_001"
    const val TENANT_ACCESS_DENIED = "TENANT_002"
    const val INVALID_TENANT_CONTEXT = "TENANT_003"
}
