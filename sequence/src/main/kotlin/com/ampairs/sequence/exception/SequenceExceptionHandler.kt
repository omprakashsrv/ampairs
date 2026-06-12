package com.ampairs.sequence.exception

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
class SequenceExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(SequenceDefinitionNotFoundException::class)
    fun handleDefinitionNotFound(
        ex: SequenceDefinitionNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.NOT_FOUND,
            errorCode = ErrorCodes.SEQUENCE_DEFINITION_NOT_FOUND,
            message = ex.message ?: "Sequence definition not found",
            details = null,
            request = request,
            moduleName = "sequence"
        )
    }

    @ExceptionHandler(SequenceAllocationNotFoundException::class)
    fun handleAllocationNotFound(
        ex: SequenceAllocationNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.NOT_FOUND,
            errorCode = ErrorCodes.SEQUENCE_ALLOCATION_NOT_FOUND,
            message = ex.message ?: "Sequence allocation not found",
            details = null,
            request = request,
            moduleName = "sequence"
        )
    }

    @ExceptionHandler(DuplicateSequenceDefinitionException::class)
    fun handleDuplicateDefinition(
        ex: DuplicateSequenceDefinitionException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.CONFLICT,
            errorCode = ErrorCodes.SEQUENCE_DEFINITION_DUPLICATE,
            message = ex.message ?: "An active sequence definition already exists for this key",
            details = null,
            request = request,
            moduleName = "sequence"
        )
    }

    @ExceptionHandler(InactiveSequenceDefinitionException::class)
    fun handleInactiveDefinition(
        ex: InactiveSequenceDefinitionException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.CONFLICT,
            errorCode = ErrorCodes.SEQUENCE_DEFINITION_INACTIVE,
            message = ex.message ?: "Sequence definition is inactive",
            details = null,
            request = request,
            moduleName = "sequence"
        )
    }

    @ExceptionHandler(InvalidSequenceRequestException::class)
    fun handleInvalidRequest(
        ex: InvalidSequenceRequestException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Any>> {
        return createErrorResponse(
            httpStatus = HttpStatus.BAD_REQUEST,
            errorCode = ErrorCodes.SEQUENCE_INVALID_REQUEST,
            message = ex.message ?: "Invalid sequence request",
            details = null,
            request = request,
            moduleName = "sequence"
        )
    }
}
