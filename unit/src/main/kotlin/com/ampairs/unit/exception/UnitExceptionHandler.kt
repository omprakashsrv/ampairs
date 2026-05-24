package com.ampairs.unit.exception

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
@Order(Ordered.HIGHEST_PRECEDENCE + 70)
class UnitExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(UnitNotFoundException::class)
    fun handleUnitNotFoundException(
        ex: UnitNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.NOT_FOUND,
            errorCode = ErrorCodes.UNIT_NOT_FOUND,
            message = ex.message ?: "Unit not found",
            details = null,
            request = request,
            moduleName = "unit"
        )
    }

    @ExceptionHandler(UnitInUseException::class)
    fun handleUnitInUseException(
        ex: UnitInUseException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        val details = buildString {
            if (ex.entityIds.isNotEmpty()) append("Entities: ${ex.entityIds.joinToString()}")
            if (ex.conversionIds.isNotEmpty()) {
                if (isNotEmpty()) append("; ")
                append("Conversions: ${ex.conversionIds.joinToString()}")
            }
        }.ifBlank { null }

        return createErrorResponse(
            httpStatus = HttpStatus.CONFLICT,
            errorCode = ErrorCodes.UNIT_IN_USE,
            message = ex.message ?: "Unit is currently in use",
            details = details,
            request = request,
            moduleName = "unit"
        )
    }

    @ExceptionHandler(CircularConversionException::class)
    fun handleCircularConversionException(
        ex: CircularConversionException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.CIRCULAR_CONVERSION,
            message = ex.message ?: "Circular conversion detected",
            details = ex.cycle.joinToString(" -> "),
            request = request,
            moduleName = "unit"
        )
    }
}
