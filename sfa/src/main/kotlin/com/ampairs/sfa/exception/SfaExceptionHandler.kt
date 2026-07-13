package com.ampairs.sfa.exception

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
@Order(Ordered.HIGHEST_PRECEDENCE + 45) // Execute before the global handler
class SfaExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(SfaValidationException::class)
    fun handleSfaValidationException(
        ex: SfaValidationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
            errorCode = ErrorCodes.VALIDATION_ERROR,
            message = "SFA validation failed",
            details = ex.message ?: "Invalid SFA request",
            request = request,
            moduleName = "sfa",
        )
    }
}
