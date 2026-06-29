package com.ampairs.trade.exception

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
@Order(Ordered.HIGHEST_PRECEDENCE + 46)
class TradeExceptionHandler : BaseExceptionHandler() {

    @ExceptionHandler(ConsentRequiredException::class)
    fun handleConsentRequired(ex: ConsentRequiredException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.FORBIDDEN, ErrorCodes.ACCESS_DENIED, "Consent required", ex.message ?: "No active link", request, moduleName = "trade")

    @ExceptionHandler(LinkStateException::class)
    fun handleLinkState(ex: LinkStateException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.CONFLICT, ErrorCodes.BAD_REQUEST, "Illegal link transition", ex.message ?: "Illegal link state", request, moduleName = "trade")

    @ExceptionHandler(TradeException::class)
    fun handleTrade(ex: TradeException, request: HttpServletRequest): ResponseEntity<ApiResponse<Any>> =
        createErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCodes.VALIDATION_ERROR, "Trade validation failed", ex.message ?: "Invalid trade request", request, moduleName = "trade")
}
