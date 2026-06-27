package com.ampairs.purchase.exception

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.ErrorCodes
import com.ampairs.core.exception.BaseExceptionHandler
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 42) // Execute before global handler
class PurchaseExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(PurchaseNotFoundException::class)
    fun handlePurchaseNotFoundException(
        ex: PurchaseNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.NOT_FOUND,
            errorCode = ErrorCodes.PURCHASE_NOT_FOUND,
            message = "Purchase not found",
            details = ex.message ?: "The requested purchase was not found",
            request = request,
            moduleName = "purchase"
        )
    }

    @ExceptionHandler(InvalidPurchaseDataException::class)
    fun handleInvalidPurchaseDataException(
        ex: InvalidPurchaseDataException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "Invalid purchase data",
            details = ex.message ?: "The provided purchase data is invalid",
            request = request,
            moduleName = "purchase"
        )
    }
}

// Custom purchase exceptions
class PurchaseNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidPurchaseDataException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
