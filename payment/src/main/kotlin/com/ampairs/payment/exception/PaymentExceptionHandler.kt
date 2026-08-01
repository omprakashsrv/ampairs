package com.ampairs.payment.exception

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
@Order(Ordered.HIGHEST_PRECEDENCE + 72) // Execute before global handler
class PaymentExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(PartyBalanceNotFoundException::class)
    fun handleNotFound(
        ex: PartyBalanceNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> = createErrorResponse(
        httpStatus = HttpStatus.NOT_FOUND,
        errorCode = ErrorCodes.NOT_FOUND,
        message = "Party balance not found",
        details = ex.message,
        request = request,
        moduleName = "payment",
    )

    @ExceptionHandler(PaymentVoucherNotFoundException::class)
    fun handleVoucherNotFound(
        ex: PaymentVoucherNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> = createErrorResponse(
        httpStatus = HttpStatus.NOT_FOUND,
        errorCode = ErrorCodes.NOT_FOUND,
        message = "Payment voucher not found",
        details = ex.message,
        request = request,
        moduleName = "payment",
    )

    @ExceptionHandler(AllocationExceedsTotalException::class)
    fun handleAllocationExceeds(
        ex: AllocationExceedsTotalException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> = createErrorResponse(
        httpStatus = HttpStatus.BAD_REQUEST,
        errorCode = ErrorCodes.VALIDATION_ERROR,
        message = "Allocated amount exceeds voucher total",
        details = ex.message,
        request = request,
        validationErrors = ex.validationErrors,
        moduleName = "payment",
    )

    @ExceptionHandler(InvalidClearanceTransitionException::class)
    fun handleInvalidClearance(
        ex: InvalidClearanceTransitionException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> = createErrorResponse(
        httpStatus = HttpStatus.CONFLICT,
        errorCode = ErrorCodes.CONSTRAINT_VIOLATION,
        message = "Invalid clearance transition",
        details = ex.message,
        request = request,
        moduleName = "payment",
    )

    @ExceptionHandler(InvalidPaymentDataException::class)
    fun handleInvalidData(
        ex: InvalidPaymentDataException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> = createErrorResponse(
        httpStatus = HttpStatus.BAD_REQUEST,
        errorCode = ErrorCodes.VALIDATION_ERROR,
        message = "Invalid payment data",
        details = ex.message,
        request = request,
        moduleName = "payment",
    )
}

class PartyBalanceNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class PaymentVoucherNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidClearanceTransitionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidPaymentDataException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class AllocationExceedsTotalException(
    message: String,
    val validationErrors: Map<String, String>? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
