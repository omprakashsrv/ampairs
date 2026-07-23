package com.ampairs.supplier.exception

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
@Order(Ordered.HIGHEST_PRECEDENCE + 41) // Execute before global handler
class SupplierExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(SupplierNotFoundException::class)
    fun handleSupplierNotFoundException(
        ex: SupplierNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.NOT_FOUND,
            errorCode = ErrorCodes.SUPPLIER_NOT_FOUND,
            message = "Supplier not found",
            details = ex.message ?: "The requested supplier was not found",
            request = request,
            moduleName = "supplier"
        )
    }

    @ExceptionHandler(DuplicateSupplierException::class)
    fun handleDuplicateSupplierException(
        ex: DuplicateSupplierException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.CONFLICT,
            errorCode = ErrorCodes.DUPLICATE_ENTRY,
            message = "Duplicate supplier",
            details = ex.message ?: "A supplier with the same details already exists",
            request = request,
            moduleName = "supplier"
        )
    }

    @ExceptionHandler(InvalidSupplierDataException::class)
    fun handleInvalidSupplierDataException(
        ex: InvalidSupplierDataException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "Invalid supplier data",
            details = ex.message ?: "The provided supplier data is invalid",
            request = request,
            moduleName = "supplier"
        )
    }
}

// Custom supplier exceptions
class SupplierNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class DuplicateSupplierException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class InvalidSupplierDataException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
