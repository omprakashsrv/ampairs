package com.ampairs.claim.exception

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
@Order(Ordered.HIGHEST_PRECEDENCE + 47)
class ClaimExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(ClaimStateException::class)
    fun handleClaimState(ex: ClaimStateException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.CONFLICT, ErrorCodes.BAD_REQUEST, "Illegal claim transition", ex.message ?: "Illegal claim state", request, moduleName = "claim")

    @ExceptionHandler(ClaimException::class)
    fun handleClaim(ex: ClaimException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCodes.VALIDATION_ERROR, "Claim validation failed", ex.message ?: "Invalid claim request", request, moduleName = "claim")
}
