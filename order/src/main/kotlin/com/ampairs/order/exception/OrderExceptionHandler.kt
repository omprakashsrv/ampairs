package com.ampairs.order.exception

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
@Order(Ordered.HIGHEST_PRECEDENCE + 60) // Execute before global handler
class OrderExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(OrderNotFoundException::class)
    fun handleOrderNotFoundException(
        ex: OrderNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.NOT_FOUND,
            errorCode = ErrorCodes.ORDER_NOT_FOUND,
            message = "Order not found",
            details = ex.message ?: "The requested order was not found",
            request = request,
            moduleName = "order"
        )
    }

    @ExceptionHandler(InvalidOrderStatusException::class)
    fun handleInvalidOrderStatusException(
        ex: InvalidOrderStatusException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "Invalid order status",
            details = ex.message ?: "The order status transition is not valid",
            request = request,
            moduleName = "order"
        )
    }

    @ExceptionHandler(OrderProcessingException::class)
    fun handleOrderProcessingException(
        ex: OrderProcessingException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        logger.error("Order processing failed for request {}: {}", request.requestURI, ex.message, ex)

        return createErrorResponse(
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            errorCode = ErrorCodes.INTERNAL_SERVER_ERROR,
            message = "Order processing failed",
            details = ex.message ?: "Failed to process the order",
            request = request,
            moduleName = "order"
        )
    }

    @ExceptionHandler(OrderCancellationException::class)
    fun handleOrderCancellationException(
        ex: OrderCancellationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.CONFLICT,
            errorCode = ErrorCodes.CONSTRAINT_VIOLATION,
            message = "Order cancellation failed",
            details = ex.message ?: "The order cannot be cancelled in its current state",
            request = request,
            moduleName = "order"
        )
    }

    @ExceptionHandler(InvalidOrderItemException::class)
    fun handleInvalidOrderItemException(
        ex: InvalidOrderItemException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "Invalid order item",
            details = ex.message ?: "One or more order items are invalid",
            request = request,
            moduleName = "order"
        )
    }

    @ExceptionHandler(EmptyOrderException::class)
    fun handleEmptyOrderException(
        ex: EmptyOrderException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "Empty order",
            details = ex.message ?: "Order must contain at least one item",
            request = request,
            moduleName = "order"
        )
    }
}

// Custom order exceptions
class OrderNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidOrderStatusException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class OrderProcessingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class OrderCancellationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidOrderItemException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class EmptyOrderException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)