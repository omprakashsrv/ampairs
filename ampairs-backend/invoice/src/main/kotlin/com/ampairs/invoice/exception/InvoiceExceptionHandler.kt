package com.ampairs.invoice.exception

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
@Order(Ordered.HIGHEST_PRECEDENCE + 70) // Execute before global handler
class InvoiceExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(InvoiceNotFoundException::class)
    fun handleInvoiceNotFoundException(
        ex: InvoiceNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.NOT_FOUND,
            errorCode = ErrorCodes.INVOICE_NOT_FOUND,
            message = "Invoice not found",
            details = ex.message ?: "The requested invoice was not found",
            request = request,
            moduleName = "invoice"
        )
    }

    @ExceptionHandler(InvoiceGenerationException::class)
    fun handleInvoiceGenerationException(
        ex: InvoiceGenerationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        logger.error("Invoice generation failed for request {}: {}", request.requestURI, ex.message, ex)

        return createErrorResponse(
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            errorCode = ErrorCodes.INTERNAL_SERVER_ERROR,
            message = "Invoice generation failed",
            details = ex.message ?: "Failed to generate the invoice",
            request = request,
            moduleName = "invoice"
        )
    }

    @ExceptionHandler(InvalidInvoiceStatusException::class)
    fun handleInvalidInvoiceStatusException(
        ex: InvalidInvoiceStatusException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "Invalid invoice status",
            details = ex.message ?: "The invoice status transition is not valid",
            request = request,
            moduleName = "invoice"
        )
    }

    @ExceptionHandler(InvoiceCancellationException::class)
    fun handleInvoiceCancellationException(
        ex: InvoiceCancellationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.CONFLICT,
            errorCode = ErrorCodes.CONSTRAINT_VIOLATION,
            message = "Invoice cancellation failed",
            details = ex.message ?: "The invoice cannot be cancelled in its current state",
            request = request,
            moduleName = "invoice"
        )
    }

    @ExceptionHandler(InvalidInvoiceDataException::class)
    fun handleInvalidInvoiceDataException(
        ex: InvalidInvoiceDataException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "Invalid invoice data",
            details = ex.message ?: "The provided invoice data is invalid",
            request = request,
            moduleName = "invoice"
        )
    }

    @ExceptionHandler(DuplicateInvoiceException::class)
    fun handleDuplicateInvoiceException(
        ex: DuplicateInvoiceException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.CONFLICT,
            errorCode = ErrorCodes.DUPLICATE_ENTRY,
            message = "Duplicate invoice",
            details = ex.message ?: "An invoice with the same details already exists",
            request = request,
            moduleName = "invoice"
        )
    }

    @ExceptionHandler(InvalidTaxCalculationException::class)
    fun handleInvalidTaxCalculationException(
        ex: InvalidTaxCalculationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "Invalid tax calculation",
            details = ex.message ?: "Tax calculation failed for the invoice",
            request = request,
            moduleName = "invoice"
        )
    }
}

// Custom invoice exceptions
class InvoiceNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvoiceGenerationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidInvoiceStatusException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvoiceCancellationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidInvoiceDataException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class DuplicateInvoiceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidTaxCalculationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)