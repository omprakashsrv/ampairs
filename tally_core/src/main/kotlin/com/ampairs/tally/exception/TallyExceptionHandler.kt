package com.ampairs.tally.exception

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.ErrorCodes
import com.ampairs.core.exception.BaseExceptionHandler
import com.ampairs.tally.service.TallyIntegrationException
import com.ampairs.tally.service.TallyParsingException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 30) // Execute before global handler
class TallyExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(TallyIntegrationException::class)
    fun handleTallyIntegrationException(
        ex: TallyIntegrationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        logger.error("Tally integration error for request {}: {}", request.requestURI, ex.message, ex)

        return createErrorResponse(
            httpStatus = HttpStatus.SERVICE_UNAVAILABLE,
            errorCode = ErrorCodes.TALLY_CONNECTION_FAILED,
            message = "Tally integration error",
            details = ex.message ?: "Failed to communicate with Tally ERP",
            request = request,
            moduleName = "tally"
        )
    }

    @ExceptionHandler(TallyParsingException::class)
    fun handleTallyParsingException(
        ex: TallyParsingException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        logger.error("Tally parsing error for request {}: {}", request.requestURI, ex.message, ex)

        return createErrorResponse(
            httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
            errorCode = ErrorCodes.TALLY_PARSING_ERROR,
            message = "Tally data parsing error",
            details = ex.message ?: "Failed to parse Tally XML data",
            request = request,
            moduleName = "tally"
        )
    }

    @ExceptionHandler(ConnectException::class)
    fun handleConnectException(
        ex: ConnectException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        logger.error("Connection failed to Tally for request {}: {}", request.requestURI, ex.message)

        return createErrorResponse(
            httpStatus = HttpStatus.SERVICE_UNAVAILABLE,
            errorCode = ErrorCodes.TALLY_CONNECTION_FAILED,
            message = "Cannot connect to Tally",
            details = "Unable to establish connection to Tally ERP server",
            request = request,
            moduleName = "tally"
        )
    }

    @ExceptionHandler(SocketTimeoutException::class, TimeoutException::class)
    fun handleTimeoutException(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        logger.error("Tally operation timeout for request {}: {}", request.requestURI, ex.message)

        return createErrorResponse(
            httpStatus = HttpStatus.REQUEST_TIMEOUT,
            errorCode = ErrorCodes.TALLY_TIMEOUT,
            message = "Tally operation timeout",
            details = "The operation with Tally ERP server timed out",
            request = request,
            moduleName = "tally"
        )
    }

    @ExceptionHandler(TallySyncException::class)
    fun handleTallySyncException(
        ex: TallySyncException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        logger.error("Tally sync failed for request {}: {}", request.requestURI, ex.message, ex)

        return createErrorResponse(
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            errorCode = ErrorCodes.TALLY_SYNC_FAILED,
            message = "Tally synchronization failed",
            details = ex.message ?: "Failed to synchronize data with Tally ERP",
            request = request,
            moduleName = "tally"
        )
    }

    @ExceptionHandler(TallyValidationException::class)
    fun handleTallyValidationException(
        ex: TallyValidationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "Tally data validation failed",
            details = ex.message ?: "The provided data is not valid for Tally integration",
            request = request,
            moduleName = "tally"
        )
    }

    @ExceptionHandler(TallyAuthenticationException::class)
    fun handleTallyAuthenticationException(
        ex: TallyAuthenticationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.UNAUTHORIZED,
            errorCode = ErrorCodes.AUTHENTICATION_FAILED,
            message = "Tally authentication failed",
            details = ex.message ?: "Failed to authenticate with Tally ERP server",
            request = request,
            moduleName = "tally"
        )
    }

    @ExceptionHandler(TallyConfigurationException::class)
    fun handleTallyConfigurationException(
        ex: TallyConfigurationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        logger.error("Tally configuration error for request {}: {}", request.requestURI, ex.message, ex)

        return createErrorResponse(
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            errorCode = ErrorCodes.INTERNAL_SERVER_ERROR,
            message = "Tally configuration error",
            details = ex.message ?: "Tally integration is not properly configured",
            request = request,
            moduleName = "tally"
        )
    }

    @ExceptionHandler(TallyDataNotFoundException::class)
    fun handleTallyDataNotFoundException(
        ex: TallyDataNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.NOT_FOUND,
            errorCode = ErrorCodes.NOT_FOUND,
            message = "Tally data not found",
            details = ex.message ?: "The requested data was not found in Tally ERP",
            request = request,
            moduleName = "tally"
        )
    }
}

// Custom Tally exceptions
class TallySyncException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class TallyValidationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class TallyAuthenticationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class TallyConfigurationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class TallyDataNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)