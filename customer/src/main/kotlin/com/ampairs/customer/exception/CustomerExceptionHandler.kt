package com.ampairs.customer.exception

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
@Order(Ordered.HIGHEST_PRECEDENCE + 40) // Execute before global handler
class CustomerExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(CustomerNotFoundException::class)
    fun handleCustomerNotFoundException(
        ex: CustomerNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.NOT_FOUND,
            errorCode = ErrorCodes.CUSTOMER_NOT_FOUND,
            message = "Customer not found",
            details = ex.message ?: "The requested customer was not found",
            request = request,
            moduleName = "customer"
        )
    }

    @ExceptionHandler(DuplicateCustomerException::class)
    fun handleDuplicateCustomerException(
        ex: DuplicateCustomerException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.CONFLICT,
            errorCode = ErrorCodes.DUPLICATE_ENTRY,
            message = "Duplicate customer",
            details = ex.message ?: "A customer with the same details already exists",
            request = request,
            moduleName = "customer"
        )
    }

    @ExceptionHandler(InvalidCustomerDataException::class)
    fun handleInvalidCustomerDataException(
        ex: InvalidCustomerDataException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "Invalid customer data",
            details = ex.message ?: "The provided customer data is invalid",
            request = request,
            moduleName = "customer"
        )
    }

    @ExceptionHandler(CustomerAccessDeniedException::class)
    fun handleCustomerAccessDeniedException(
        ex: CustomerAccessDeniedException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.FORBIDDEN,
            errorCode = ErrorCodes.ACCESS_DENIED,
            message = "Customer access denied",
            details = ex.message ?: "You don't have permission to access this customer",
            request = request,
            moduleName = "customer"
        )
    }
}

// Custom customer exceptions
class CustomerNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class DuplicateCustomerException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidCustomerDataException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class CustomerAccessDeniedException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)